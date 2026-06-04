package com.github.mantis133.puzzleapp.puzzle.core

/** Metadata describing a puzzle type shown in the home screen. Platform-agnostic. */
data class PuzzleTypeInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val emoji: String
)

object PuzzleTypes {
    val SHIKAKU = PuzzleTypeInfo(
        id          = "shikaku",
        displayName = "Shikaku",
        description = "Divide the grid into rectangles. Each number shows the area of its rectangle.",
        emoji       = "⬜"
    )

    val CHESS = PuzzleTypeInfo(
        id          = "chess",
        displayName = "Chess Puzzles",
        description = "Tactics puzzles from the Lichess open database. Find the winning move.",
        emoji       = "♟"
    )

    val SUDOKU = PuzzleTypeInfo(
        id          = "sudoku",
        displayName = "Sudoku",
        description = "Fill the 9×9 grid so every row, column, and 3×3 box contains each digit 1–9.",
        emoji       = "🔢"
    )

    val MINESWEEPER = PuzzleTypeInfo(
        id          = "minesweeper",
        displayName = "Minesweeper",
        description = "Uncover every safe cell without detonating a mine. Tap a cell for dig/flag options.",
        emoji       = "💣"
    )

    val all: List<PuzzleTypeInfo> = listOf(SHIKAKU, SUDOKU, CHESS, MINESWEEPER)
}

