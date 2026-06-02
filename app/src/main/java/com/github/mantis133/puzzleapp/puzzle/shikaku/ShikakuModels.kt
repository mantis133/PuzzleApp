package com.github.mantis133.puzzleapp.puzzle.shikaku

/**
 * A single numbered clue on the board.
 * [value] is the area the rectangle containing this clue must have.
 * Platform-agnostic — no Android dependencies.
 */
data class ShikakuClue(val row: Int, val col: Int, val value: Int)

/**
 * An axis-aligned rectangle defined by inclusive corner indices.
 */
data class ShikakuRectangle(
    val top: Int,
    val left: Int,
    val bottom: Int,
    val right: Int
) {
    val height: Int = bottom - top + 1
    val width: Int = right - left + 1
    val area: Int = height * width

    fun contains(row: Int, col: Int): Boolean =
        row in top..bottom && col in left..right

    fun overlaps(other: ShikakuRectangle): Boolean =
        top <= other.bottom && bottom >= other.top &&
        left <= other.right && right >= other.left
}

/**
 * Immutable puzzle board — clues visible to the player plus the generated solution for hint/check use.
 * Platform-agnostic.
 */
data class ShikakuBoard(
    val rows: Int,
    val cols: Int,
    val clues: List<ShikakuClue>,
    /** The canonical solution produced by the generator. */
    val solution: List<ShikakuRectangle>
)

