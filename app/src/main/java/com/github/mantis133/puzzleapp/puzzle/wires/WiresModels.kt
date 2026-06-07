package com.github.mantis133.puzzleapp.puzzle.wires

/**
 * The set of distinct colours available for wire pairs.
 * Platform-agnostic — no Android dependencies.
 */
enum class WireColor {
    RED, ORANGE, YELLOW, GREEN, BLUE, MAROON, PINK, PURPLE, CYAN, BROWN
}

/**
 * One endpoint of a coloured wire.  Two [WireTerminal]s share the same [color]; the
 * player must draw a continuous path between them.
 */
data class WireTerminal(val row: Int, val col: Int, val color: WireColor)

/**
 * Immutable puzzle description exposed to the player: only the grid size and the
 * terminal positions are visible — the solution path is intentionally withheld.
 */
data class WiresBoard(
    val rows: Int,
    val cols: Int,
    /** Always contains exactly two [WireTerminal]s per [WireColor] used. */
    val terminals: List<WireTerminal>
)
