package com.github.mantis133.puzzleapp.puzzle.sudoku

import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import kotlin.random.Random

/**
 * Generates Sudoku puzzles with a guaranteed unique solution.
 * Platform-agnostic — no Android imports.
 *
 * Algorithm:
 * 1. Fill the three diagonal 3×3 boxes (they are mutually independent).
 * 2. Complete the rest of the grid via randomised backtracking.
 * 3. Remove digits in random order, restoring each one that would break uniqueness.
 * 4. Stop once the target number of givens is reached (or all positions exhausted).
 */
object SudokuGenerator {

    // ── Difficulty → target givens ────────────────────────────────────────────
    private fun targetGivens(difficulty: Difficulty): Int = when (difficulty) {
        is Difficulty.Easy   -> 43   // 38 empty cells
        is Difficulty.Medium -> 33   // 48 empty cells
        is Difficulty.Hard   -> 26   // 55 empty cells (solver must work harder)
        is Difficulty.Custom -> 43   // Custom not shown for Sudoku, default to Easy
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun generate(difficulty: Difficulty, random: Random = Random.Default): SudokuBoard {
        val full   = buildFullGrid(random)
        val puzzle = digHoles(full, targetGivens(difficulty), random)
        val given  = BooleanArray(81) { puzzle[it] != 0 }
        return SudokuBoard(grid = puzzle, given = given)
    }

    // ── Step 1: fill a complete valid grid ────────────────────────────────────

    private fun buildFullGrid(random: Random): IntArray {
        val grid = IntArray(81)

        // Fill diagonal boxes first (box 0, 4, 8 — never conflict with each other)
        for (box in 0..2) {
            val digits = (1..9).shuffled(random)
            val startRow = box * 3
            val startCol = box * 3
            var i = 0
            for (r in startRow until startRow + 3)
                for (c in startCol until startCol + 3)
                    grid[r * 9 + c] = digits[i++]
        }

        // Fill remaining cells with randomised backtracking
        fillBacktrack(grid, random)
        return grid
    }

    private fun fillBacktrack(grid: IntArray, random: Random): Boolean {
        val pos = grid.indexOfFirst { it == 0 }
        if (pos == -1) return true           // Done

        val row    = pos / 9
        val col    = pos % 9
        val digits = (1..9).shuffled(random)

        for (digit in digits) {
            if (SudokuSolver.isValid(grid, row, col, digit)) {
                grid[pos] = digit
                if (fillBacktrack(grid, random)) return true
                grid[pos] = 0
            }
        }
        return false
    }

    // ── Step 2: remove digits while preserving uniqueness ─────────────────────

    private fun digHoles(full: IntArray, targetGivens: Int, random: Random): IntArray {
        val puzzle    = full.copyOf()
        val positions = (0 until 81).shuffled(random)
        var givens    = 81

        for (pos in positions) {
            if (givens <= targetGivens) break

            val backup   = puzzle[pos]
            puzzle[pos]  = 0

            if (SudokuSolver.isUnique(puzzle)) {
                givens--
            } else {
                puzzle[pos] = backup      // Restore — would break uniqueness
            }
        }

        return puzzle
    }
}

