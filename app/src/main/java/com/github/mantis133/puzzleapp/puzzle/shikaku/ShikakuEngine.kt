package com.github.mantis133.puzzleapp.puzzle.shikaku

import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.core.PuzzleEngine

enum class PlacementResult { VALID, OVERLAPS, WRONG_CLUE_COUNT, WRONG_AREA, CLUE_ALREADY_PLACED }

/**
 * Facade that combines [ShikakuGenerator] and [ShikakuSolver].
 * Platform-agnostic — no Android dependencies.
 */
class ShikakuEngine : PuzzleEngine {

    override val typeId = "shikaku"

    private val generator = ShikakuGenerator()
    private val solver    = ShikakuSolver()

    /**
     * Generates a puzzle, optionally retrying until a unique solution is found.
     * Falls back to the last generated board if uniqueness cannot be guaranteed
     * within [maxAttempts] tries (very rare for the chosen difficulty parameters).
     */
    fun generate(
        difficulty: Difficulty,
        seed: Long? = null,
        ensureUnique: Boolean = true,
        maxAttempts: Int = 20
    ): ShikakuBoard? {
        if (!ensureUnique) return generator.generate(difficulty, seed)

        var lastBoard: ShikakuBoard? = null
        repeat(maxAttempts) { attempt ->
            val attemptSeed = seed?.plus(attempt)
            val board = generator.generate(difficulty, attemptSeed) ?: return@repeat
            lastBoard = board
            if (solver.hasUniqueSolution(board)) return board
        }
        return lastBoard   // Non-unique fallback
    }

    /**
     * Validates whether [rect] can legally be placed given [existingRects].
     * Does NOT modify any state.
     */
    fun validatePlacement(
        board: ShikakuBoard,
        rect: ShikakuRectangle,
        existingRects: List<ShikakuRectangle>
    ): PlacementResult {
        if (existingRects.any { it.overlaps(rect) }) return PlacementResult.OVERLAPS

        val contained = board.clues.filter { rect.contains(it.row, it.col) }
        if (contained.size != 1) return PlacementResult.WRONG_CLUE_COUNT
        if (rect.area != contained[0].value) return PlacementResult.WRONG_AREA

        return PlacementResult.VALID
    }

    /** Returns true when every cell is covered by a valid, non-overlapping rectangle. */
    fun isSolved(board: ShikakuBoard, placedRects: List<ShikakuRectangle>): Boolean {
        if (placedRects.size != board.clues.size) return false
        if (placedRects.sumOf { it.area } != board.rows * board.cols) return false
        for (rect in placedRects) {
            val contained = board.clues.filter { rect.contains(it.row, it.col) }
            if (contained.size != 1 || rect.area != contained[0].value) return false
        }
        for (i in placedRects.indices) {
            for (j in i + 1 until placedRects.size) {
                if (placedRects[i].overlaps(placedRects[j])) return false
            }
        }
        return true
    }

    /** Returns the set of candidate rectangles for a single clue (useful for hints). */
    fun hintsForClue(clueIdx: Int, board: ShikakuBoard): List<ShikakuRectangle> =
        solver.candidatesFor(board.clues[clueIdx], clueIdx, board)
}

