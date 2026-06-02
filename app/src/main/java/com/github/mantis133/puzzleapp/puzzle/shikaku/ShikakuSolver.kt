package com.github.mantis133.puzzleapp.puzzle.shikaku

import kotlin.math.max
import kotlin.math.min

/**
 * Constraint-satisfaction solver for Shikaku puzzles.
 *
 * Uses Minimum-Remaining-Values (MRV) backtracking:
 *  - Pre-computes every valid rectangle for each clue (respecting grid bounds
 *    and the rule that each rectangle must contain exactly one clue).
 *  - At each step picks the clue with fewest remaining legal rectangles,
 *    tries each, and recurses.
 *
 * Platform-agnostic — no Android dependencies.
 */
class ShikakuSolver {

    /**
     * Solves [board] and returns up to [maxSolutions] distinct solutions.
     * Each solution is a map from clue-index → placed rectangle.
     */
    fun solve(board: ShikakuBoard, maxSolutions: Int = 1): List<Map<Int, ShikakuRectangle>> {
        val solutions = mutableListOf<Map<Int, ShikakuRectangle>>()
        val allOptions: List<List<ShikakuRectangle>> = board.clues.mapIndexed { idx, clue ->
            candidatesFor(clue, idx, board)
        }
        backtrack(allOptions, board.clues.size, board.rows * board.cols,
            mutableMapOf(), solutions, maxSolutions)
        return solutions
    }

    /** Returns true iff [board] has exactly one solution. */
    fun hasUniqueSolution(board: ShikakuBoard): Boolean = solve(board, maxSolutions = 2).size == 1

    /**
     * Returns all valid rectangle placements for [clue] given the full board.
     * A rectangle is valid when:
     *  - Its area equals [ShikakuClue.value]
     *  - It fits inside the grid
     *  - It contains [clue]'s cell
     *  - It does NOT contain any other clue's cell
     */
    fun candidatesFor(clue: ShikakuClue, clueIdx: Int, board: ShikakuBoard): List<ShikakuRectangle> {
        val n = clue.value
        val result = mutableListOf<ShikakuRectangle>()
        for (h in 1..n) {
            if (n % h != 0) continue
            val w = n / h
            if (h > board.rows || w > board.cols) continue
            val topMin  = max(0, clue.row - h + 1)
            val topMax  = min(board.rows - h, clue.row)
            val leftMin = max(0, clue.col - w + 1)
            val leftMax = min(board.cols - w, clue.col)
            for (top in topMin..topMax) {
                for (left in leftMin..leftMax) {
                    val rect = ShikakuRectangle(top, left, top + h - 1, left + w - 1)
                    // Reject if the rectangle would swallow another clue
                    val blocked = board.clues.indices.any { i ->
                        i != clueIdx && rect.contains(board.clues[i].row, board.clues[i].col)
                    }
                    if (!blocked) result.add(rect)
                }
            }
        }
        return result
    }

    // ---------------------------------------------------------------------------

    private fun backtrack(
        allOptions: List<List<ShikakuRectangle>>,
        numClues: Int,
        totalCells: Int,
        assigned: MutableMap<Int, ShikakuRectangle>,
        solutions: MutableList<Map<Int, ShikakuRectangle>>,
        maxSolutions: Int
    ) {
        if (solutions.size >= maxSolutions) return

        if (assigned.size == numClues) {
            // All clues placed; verify total coverage (non-overlapping areas sum correctly)
            if (assigned.values.sumOf { it.area } == totalCells) {
                solutions.add(assigned.toMap())
            }
            return
        }

        // MRV: pick unassigned clue with fewest legal options
        var bestIdx = -1
        var bestCount = Int.MAX_VALUE
        for (idx in 0 until numClues) {
            if (idx in assigned) continue
            val count = allOptions[idx].count { r -> assigned.values.none { it.overlaps(r) } }
            if (count == 0) return   // Dead end
            if (count < bestCount) { bestCount = count; bestIdx = idx }
        }
        if (bestIdx == -1) return

        val legal = allOptions[bestIdx].filter { r -> assigned.values.none { it.overlaps(r) } }
        for (rect in legal) {
            assigned[bestIdx] = rect
            backtrack(allOptions, numClues, totalCells, assigned, solutions, maxSolutions)
            assigned.remove(bestIdx)
            if (solutions.size >= maxSolutions) return
        }
    }
}

