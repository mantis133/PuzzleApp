package com.github.mantis133.puzzleapp.puzzle.core

/**
 * Platform-agnostic difficulty descriptor.
 * Presets are singleton objects; Custom carries user-chosen dimensions.
 */
sealed class Difficulty {
    abstract val displayName: String
    abstract val rows: Int
    abstract val cols: Int
    abstract val maxRectArea: Int

    object Easy : Difficulty() {
        override val displayName = "Easy"
        override val rows = 5
        override val cols = 5
        override val maxRectArea = 6
    }

    object Medium : Difficulty() {
        override val displayName = "Medium"
        override val rows = 7
        override val cols = 7
        override val maxRectArea = 9
    }

    object Hard : Difficulty() {
        override val displayName = "Hard"
        override val rows = 9
        override val cols = 9
        override val maxRectArea = 12
    }

    data class Custom(
        override val rows: Int,
        override val cols: Int
    ) : Difficulty() {
        override val displayName = "Custom"
        /** Scale max area with grid size so the puzzle stays interesting. */
        override val maxRectArea: Int
            get() = (rows * cols / 4).coerceIn(4, 16)
    }

    companion object {
        /** The three preset difficulties shown in the segmented button. */
        val presets: List<Difficulty> = listOf(Easy, Medium, Hard)
    }
}

// ── Navigation serialisation ─────────────────────────────────────────────────

/** Encode a [Difficulty] as a single nav-argument string. */
fun Difficulty.toNavArg(): String = when (this) {
    is Difficulty.Easy   -> "EASY"
    is Difficulty.Medium -> "MEDIUM"
    is Difficulty.Hard   -> "HARD"
    is Difficulty.Custom -> "CUSTOM_${rows}_${cols}"
}

/** Decode a nav-argument string back to a [Difficulty]. */
fun difficultyFromNavArg(arg: String): Difficulty = when (arg) {
    "EASY"   -> Difficulty.Easy
    "MEDIUM" -> Difficulty.Medium
    "HARD"   -> Difficulty.Hard
    else     -> {
        val parts = arg.removePrefix("CUSTOM_").split("_")
        Difficulty.Custom(
            rows = parts.getOrNull(0)?.toIntOrNull() ?: 6,
            cols = parts.getOrNull(1)?.toIntOrNull() ?: 6
        )
    }
}
