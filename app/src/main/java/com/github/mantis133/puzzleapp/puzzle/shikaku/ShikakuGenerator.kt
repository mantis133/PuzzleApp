package com.github.mantis133.puzzleapp.puzzle.shikaku

import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import kotlin.random.Random

/**
 * Generates valid Shikaku puzzles via randomised recursive backtracking.
 *
 * Strategy:
 *  1. Scan for the first uncovered cell (top-left order).
 *  2. Try all rectangle dimensions (h × w) whose top-left is that cell,
 *     whose area ≤ [Difficulty.maxRectArea], and whose cells are all free.
 *  3. Shuffle the candidates for variety, then recurse.
 *  4. Once every cell is covered, place one numbered clue inside each rectangle
 *     at a random position.
 *
 * Platform-agnostic — no Android dependencies.
 */
class ShikakuGenerator {

    /**
     * Returns a [ShikakuBoard] or null if all attempts fail (extremely rare).
     * @param seed Optional seed for reproducible puzzles.
     */
    fun generate(difficulty: Difficulty, seed: Long? = null): ShikakuBoard? {
        val random = if (seed != null) Random(seed) else Random(System.currentTimeMillis())
        val rows = difficulty.rows
        val cols = difficulty.cols
        val maxArea = difficulty.maxRectArea

        repeat(MAX_ATTEMPTS) {
            val assigned = Array(rows) { IntArray(cols) { UNASSIGNED } }
            val rectangles = mutableListOf<ShikakuRectangle>()
            if (fillGrid(assigned, rectangles, rows, cols, maxArea, random)) {
                val clues = buildClues(rectangles, random)
                return ShikakuBoard(rows, cols, clues, rectangles.toList())
            }
        }
        return null
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun fillGrid(
        assigned: Array<IntArray>,
        rectangles: MutableList<ShikakuRectangle>,
        rows: Int,
        cols: Int,
        maxArea: Int,
        random: Random
    ): Boolean {
        // Find first uncovered cell
        var startRow = -1; var startCol = -1
        outer@ for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (assigned[r][c] == UNASSIGNED) { startRow = r; startCol = c; break@outer }
            }
        }
        if (startRow == -1) return true // All cells covered ✓

        // Build candidate (height, width) pairs with top-left fixed at (startRow, startCol)
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (h in 1..(rows - startRow)) {
            for (w in 1..(cols - startCol)) {
                val area = h * w
                if (area < MIN_RECT_AREA || area > maxArea) continue
                if (!allFree(assigned, startRow, startCol, startRow + h - 1, startCol + w - 1)) continue
                candidates.add(h to w)
            }
        }
        candidates.shuffle(random)

        val rectIdx = rectangles.size
        for ((h, w) in candidates) {
            val rect = ShikakuRectangle(startRow, startCol, startRow + h - 1, startCol + w - 1)
            markRect(assigned, rect, rectIdx)
            rectangles.add(rect)

            if (fillGrid(assigned, rectangles, rows, cols, maxArea, random)) return true

            // Backtrack
            clearRect(assigned, rect)
            rectangles.removeAt(rectangles.size - 1)
        }
        return false
    }

    private fun allFree(assigned: Array<IntArray>, top: Int, left: Int, bottom: Int, right: Int): Boolean {
        for (r in top..bottom) for (c in left..right) if (assigned[r][c] != UNASSIGNED) return false
        return true
    }

    private fun markRect(assigned: Array<IntArray>, rect: ShikakuRectangle, idx: Int) {
        for (r in rect.top..rect.bottom) for (c in rect.left..rect.right) assigned[r][c] = idx
    }

    private fun clearRect(assigned: Array<IntArray>, rect: ShikakuRectangle) {
        for (r in rect.top..rect.bottom) for (c in rect.left..rect.right) assigned[r][c] = UNASSIGNED
    }

    private fun buildClues(rectangles: List<ShikakuRectangle>, random: Random): List<ShikakuClue> =
        rectangles.map { rect ->
            val cells = buildList {
                for (r in rect.top..rect.bottom) for (c in rect.left..rect.right) add(r to c)
            }
            val (row, col) = cells.random(random)
            ShikakuClue(row, col, rect.area)
        }

    companion object {
        private const val UNASSIGNED = -1
        private const val MAX_ATTEMPTS = 5
        /** Minimum rectangle area — prevents boring single-cell "1" clues. */
        private const val MIN_RECT_AREA = 2
    }
}



