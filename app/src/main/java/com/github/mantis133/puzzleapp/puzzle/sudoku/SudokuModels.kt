package com.github.mantis133.puzzleapp.puzzle.sudoku

/**
 * Immutable snapshot of a Sudoku board. Platform-agnostic.
 *
 * The grid is stored as a flat 81-element [IntArray] in row-major order
 * ([row * 9 + col]). 0 = empty, 1–9 = digit.
 *
 * [given] is a parallel [BooleanArray] marking cells that belong to the
 * original puzzle clue set — these may not be changed by the player.
 */
data class SudokuBoard(
    val grid: IntArray,
    val given: BooleanArray
) {
    init {
        require(grid.size == 81)   { "grid must have 81 elements" }
        require(given.size == 81)  { "given must have 81 elements" }
    }

    fun value(row: Int, col: Int): Int     = grid[row * 9 + col]
    fun isGiven(row: Int, col: Int): Boolean = given[row * 9 + col]

    /** Box index 0–8 (reading order) for a given cell. */
    fun boxOf(row: Int, col: Int): Int = (row / 3) * 3 + (col / 3)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SudokuBoard) return false
        return grid.contentEquals(other.grid) && given.contentEquals(other.given)
    }

    override fun hashCode(): Int {
        var result = grid.contentHashCode()
        result = 31 * result + given.contentHashCode()
        return result
    }
}

