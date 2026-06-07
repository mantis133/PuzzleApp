package com.github.mantis133.puzzleapp.puzzle.wires

import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import kotlin.random.Random

/**
 * Generates Wires puzzles via a two-step algorithm:
 *
 *  1. Find a random Hamiltonian path through the entire rows × cols grid using randomised
 *     DFS guided by Warnsdorff's heuristic (choose the neighbour with the fewest onward
 *     unvisited moves).  This reliably finds a full path in O(N) time.
 *
 *  2. Cut the path into [numColors] segments at random positions.  The first and last cell
 *     of each segment become the two terminals of that colour.  Because the path visits
 *     every cell, the resulting puzzle always has a full-board solution.
 *
 * Platform-agnostic — no Android dependencies.
 */
class WiresGenerator {

    /**
     * Returns a [WiresBoard] or null when all [MAX_ATTEMPTS] retry attempts fail (extremely rare).
     *
     * @param numColors Number of coloured wire pairs to embed.
     * @param seed      Optional RNG seed for reproducible puzzles.
     */
    fun generate(difficulty: Difficulty, numColors: Int, seed: Long? = null): WiresBoard? {
        val rows   = difficulty.rows
        val cols   = difficulty.cols
        val colors = WireColor.entries

        repeat(MAX_ATTEMPTS) { attempt ->
            val random = if (seed != null) Random(seed + attempt) else Random(System.currentTimeMillis() + attempt * 997L)

            val path = findHamiltonianPath(rows, cols, random) ?: return@repeat
            val cuts = chooseCuts(numColors, path.size, MIN_SEG_LEN, random)

            val terminals = mutableListOf<WireTerminal>()
            var segStart = 0
            for (i in 0 until numColors) {
                val segEnd = if (i < cuts.size) cuts[i] - 1 else path.size - 1
                if (segEnd <= segStart) return@repeat  // degenerate segment — retry

                val (r1, c1) = path[segStart]
                val (r2, c2) = path[segEnd]
                terminals.add(WireTerminal(r1, c1, colors[i]))
                terminals.add(WireTerminal(r2, c2, colors[i]))
                segStart = if (i < cuts.size) cuts[i] else path.size
            }

            return WiresBoard(rows, cols, terminals)
        }
        return null
    }

    // ── Hamiltonian path (Warnsdorff DFS) ───────────────────────────────────

    private fun findHamiltonianPath(rows: Int, cols: Int, random: Random): List<Pair<Int, Int>>? {
        val visited = Array(rows) { BooleanArray(cols) }
        val path    = ArrayList<Pair<Int, Int>>(rows * cols)

        val startRow = random.nextInt(rows)
        val startCol = random.nextInt(cols)
        visited[startRow][startCol] = true
        path.add(startRow to startCol)

        return if (dfs(rows, cols, visited, path, random)) path else null
    }

    private fun dfs(
        rows: Int, cols: Int,
        visited: Array<BooleanArray>,
        path: ArrayList<Pair<Int, Int>>,
        random: Random
    ): Boolean {
        if (path.size == rows * cols) return true

        val (r, c) = path.last()

        // Collect unvisited neighbours, shuffle for random tiebreaking,
        // then stable-sort by Warnsdorff score (fewest onward moves).
        val neighbours = DIRS
            .mapNotNull { (dr, dc) ->
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols && !visited[nr][nc]) nr to nc else null
            }
            .shuffled(random)
            .sortedBy { (nr, nc) -> countFree(nr, nc, rows, cols, visited) }

        for ((nr, nc) in neighbours) {
            visited[nr][nc] = true
            path.add(nr to nc)
            if (dfs(rows, cols, visited, path, random)) return true
            path.removeAt(path.size - 1)
            visited[nr][nc] = false
        }
        return false
    }

    private fun countFree(r: Int, c: Int, rows: Int, cols: Int, visited: Array<BooleanArray>): Int =
        DIRS.count { (dr, dc) ->
            val nr = r + dr; val nc = c + dc
            nr in 0 until rows && nc in 0 until cols && !visited[nr][nc]
        }

    // ── Cut-point selection ──────────────────────────────────────────────────

    /**
     * Returns a sorted list of [numColors]−1 cut indices such that each resulting segment
     * has at least [minSegLen] cells.  The cut at position [k] means segment k+1 starts
     * at path[k].
     */
    private fun chooseCuts(numColors: Int, totalCells: Int, minSegLen: Int, random: Random): List<Int> {
        if (numColors <= 1) return emptyList()

        val jitter = maxOf(1, totalCells / (numColors * 3))
        val cuts   = mutableListOf<Int>()

        for (i in 1 until numColors) {
            val base   = i * totalCells / numColors
            val offset = random.nextInt(jitter * 2 + 1) - jitter

            val minVal = (cuts.lastOrNull() ?: 0) + minSegLen
            val maxVal = totalCells - (numColors - i) * minSegLen
            cuts.add((base + offset).coerceIn(minVal, maxVal))
        }

        return cuts
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
        /** Minimum path cells per segment, including both terminal endpoints.
         *  4 = terminal₁ + 2 intermediate cells + terminal₂, so terminals are never adjacent. */
        private const val MIN_SEG_LEN  = 4
        private val DIRS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    }
}
