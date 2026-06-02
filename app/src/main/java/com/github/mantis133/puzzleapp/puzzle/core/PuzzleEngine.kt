package com.github.mantis133.puzzleapp.puzzle.core

/**
 * Marker interface for platform-agnostic puzzle engines.
 * Each puzzle type provides its own typed engine with generation and validation logic.
 */
interface PuzzleEngine {
    val typeId: String
    fun supportsAutoGeneration(): Boolean = true
}

