package com.github.mantis133.puzzleapp.puzzle.minesweeper

/** The three classic Minesweeper presets. */
enum class MinesweeperDifficulty(
    val displayName: String,
    val rows: Int,
    val cols: Int,
    val mineCount: Int
) {
    BEGINNER    ("Beginner",     9,  9,  10),
    INTERMEDIATE("Intermediate", 16, 16, 40),
    EXPERT      ("Expert",       20, 20, 70);

    fun toNavArg(): String = name

    companion object {
        fun fromNavArg(s: String): MinesweeperDifficulty =
            entries.firstOrNull { it.name == s } ?: BEGINNER
    }
}

/** Visibility/action state of a single cell. */
enum class CellState { HIDDEN, REVEALED, FLAGGED }

/** Immutable snapshot of a single cell. */
data class MinesweeperCell(
    val isMine: Boolean = false,
    /** Count of mines in the 8 neighbouring cells (0 if this cell is a mine). */
    val adjacentMines: Int = 0,
    val state: CellState = CellState.HIDDEN
)

/** High-level game phase. */
enum class GameState { IDLE, PLAYING, WON, LOST }

