package com.github.mantis133.puzzleapp.puzzle.sudoku

/**
 * Backtracking Sudoku solver with MRV (Minimum Remaining Values) heuristic.
 * Platform-agnostic — no Android imports.
 *
 * Counting solutions up to [maxSolutions] allows both puzzle-solving and
 * uniqueness verification with a single code path.
 */
object SudokuSolver {

    /**
     * Returns up to [maxSolutions] distinct solutions for the given grid.
     * Pass [maxSolutions] = 2 to cheaply verify uniqueness.
     */
    fun solve(grid: IntArray, maxSolutions: Int = 1): List<IntArray> {
        require(grid.size == 81)
        val results = mutableListOf<IntArray>()
        backtrack(grid.copyOf(), results, maxSolutions)
        return results
    }

    /** Returns true iff [grid] has exactly one solution. */
    fun isUnique(grid: IntArray): Boolean = solve(grid, maxSolutions = 2).size == 1

    // ── Private backtracking ──────────────────────────────────────────────────

    private fun backtrack(grid: IntArray, results: MutableList<IntArray>, max: Int) {
        if (results.size >= max) return

        // MRV: choose the empty cell with the fewest valid candidates.
        var bestPos   = -1
        var bestCount = 10
        for (i in 0 until 81) {
            if (grid[i] != 0) continue
            val count = candidateCount(grid, i)
            if (count == 0) return          // Dead end — no candidates, backtrack immediately
            if (count < bestCount) {
                bestCount = count
                bestPos   = i
                if (count == 1) break       // Can't do better
            }
        }

        if (bestPos == -1) {
            // All cells filled → valid solution
            results += grid.copyOf()
            return
        }

        val row = bestPos / 9
        val col = bestPos % 9
        for (digit in 1..9) {
            if (isValid(grid, row, col, digit)) {
                grid[bestPos] = digit
                backtrack(grid, results, max)
                if (results.size >= max) return
                grid[bestPos] = 0
            }
        }
    }

    private fun candidateCount(grid: IntArray, pos: Int): Int {
        val row = pos / 9
        val col = pos % 9
        var count = 0
        for (digit in 1..9) if (isValid(grid, row, col, digit)) count++
        return count
    }

    /**
     * Returns true if [digit] can legally be placed at ([row], [col]) in [grid].
     * Exposed internally so [SudokuGenerator] can use it directly.
     */
    internal fun isValid(grid: IntArray, row: Int, col: Int, digit: Int): Boolean {
        // Row
        for (c in 0 until 9) if (c != col && grid[row * 9 + c] == digit) return false
        // Column
        for (r in 0 until 9) if (r != row && grid[r * 9 + col] == digit) return false
        // 3×3 box
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3)
            for (c in bc until bc + 3)
                if ((r != row || c != col) && grid[r * 9 + c] == digit) return false
        return true
    }
}

