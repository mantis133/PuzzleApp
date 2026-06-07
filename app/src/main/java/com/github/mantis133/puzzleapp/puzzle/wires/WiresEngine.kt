package com.github.mantis133.puzzleapp.puzzle.wires

import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.core.PuzzleEngine

/**
 * Facade for the Wires puzzle: generation and solution validation.
 * Platform-agnostic — no Android dependencies.
 */
class WiresEngine : PuzzleEngine {

    override val typeId = "wires"

    private val generator = WiresGenerator()

    /**
     * Generates a new puzzle for the given difficulty.
     * Returns null only if generation fails after all internal retry attempts (very rare).
     */
    fun generate(difficulty: Difficulty): WiresBoard? =
        generator.generate(difficulty, colorsForDifficulty(difficulty))

    /**
     * Number of coloured wire pairs the player must connect for [difficulty].
     *   Easy   → 4 pairs  (5 × 5  grid)
     *   Medium → 6 pairs  (7 × 7  grid)
     *   Hard   → 8 pairs  (9 × 9  grid)
     */
    fun colorsForDifficulty(difficulty: Difficulty): Int = when (difficulty) {
        is Difficulty.Easy   -> 4
        is Difficulty.Medium -> 6
        is Difficulty.Hard   -> 8
        is Difficulty.Custom ->
            (difficulty.rows * difficulty.cols / 8).coerceIn(3, WireColor.entries.size)
    }

    /**
     * Returns true when every cell is covered by a wire path **and** every colour's path
     * forms a connected chain between its two terminals.
     *
     * [cellColors] is a flat row-major list of size [board.rows] × [board.cols]; a null
     * entry means the cell is uncovered.
     */
    fun isSolved(board: WiresBoard, cellColors: List<WireColor?>): Boolean {
        if (cellColors.size != board.rows * board.cols) return false
        if (cellColors.any { it == null }) return false

        val terminalsByColor = board.terminals.groupBy { it.color }
        for ((color, terminals) in terminalsByColor) {
            if (terminals.size != 2) return false
            val idx1 = terminals[0].row * board.cols + terminals[0].col
            val idx2 = terminals[1].row * board.cols + terminals[1].col
            if (!bfsConnected(board.rows, board.cols, cellColors, color, idx1, idx2)) return false
        }
        return true
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun bfsConnected(
        rows: Int, cols: Int,
        cellColors: List<WireColor?>,
        color: WireColor,
        from: Int, to: Int
    ): Boolean {
        if (from == to) return true
        val visited = BooleanArray(rows * cols)
        val queue   = ArrayDeque<Int>()
        queue.add(from)
        visited[from] = true

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur == to) return true
            val r = cur / cols; val c = cur % cols
            // Up, Down, Left, Right
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val next = nr * cols + nc
                if (!visited[next] && cellColors[next] == color) {
                    visited[next] = true
                    queue.add(next)
                }
            }
        }
        return false
    }
}
