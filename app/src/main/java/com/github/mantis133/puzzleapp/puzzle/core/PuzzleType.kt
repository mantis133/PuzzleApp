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

    val all: List<PuzzleTypeInfo> = listOf(SHIKAKU, CHESS)
}

