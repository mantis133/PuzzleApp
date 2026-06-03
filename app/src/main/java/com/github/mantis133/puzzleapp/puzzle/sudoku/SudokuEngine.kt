package com.github.mantis133.puzzleapp.puzzle.sudoku

import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.core.PuzzleEngine

/**
 * Platform-agnostic façade over the Sudoku generator and solver.
 *
 * All methods operate on primitive types / value objects so the engine
 * can be reused outside Android without modification.
 */
class SudokuEngine : PuzzleEngine {

    override val typeId: String = "sudoku"

    /** Generate a new puzzle board for the given [difficulty]. */
    fun generate(difficulty: Difficulty): SudokuBoard =
        SudokuGenerator.generate(difficulty)

    /**
     * Computes the set of grid indices (row*9+col) that contain a conflict
     * (same digit in the same row, column, or 3×3 box).
     *
     * Empty cells (value == 0) are never flagged.
     */
    fun computeErrors(grid: List<Int>): Set<Int> {
        val errors = mutableSetOf<Int>()
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val v = grid[row * 9 + col]
                if (v == 0) continue
                val pos = row * 9 + col

                // Row conflict
                for (c in 0 until 9) {
                    if (c != col && grid[row * 9 + c] == v) {
                        errors += pos; errors += row * 9 + c
                    }
                }
                // Column conflict
                for (r in 0 until 9) {
                    if (r != row && grid[r * 9 + col] == v) {
                        errors += pos; errors += r * 9 + col
                    }
                }
                // Box conflict
                val br = (row / 3) * 3
                val bc = (col / 3) * 3
                for (r in br until br + 3) {
                    for (c in bc until bc + 3) {
                        if ((r != row || c != col) && grid[r * 9 + c] == v) {
                            errors += pos; errors += r * 9 + c
                        }
                    }
                }
            }
        }
        return errors
    }

    /**
     * Returns true when every cell is filled and there are no conflicts.
     */
    fun isSolved(grid: List<Int>): Boolean =
        grid.none { it == 0 } && computeErrors(grid).isEmpty()
}


