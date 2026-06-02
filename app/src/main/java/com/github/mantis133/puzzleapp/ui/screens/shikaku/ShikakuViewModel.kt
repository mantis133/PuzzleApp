package com.github.mantis133.puzzleapp.ui.screens.shikaku

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mantis133.puzzleapp.data.StatsRepository
import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.shikaku.PlacementResult
import com.github.mantis133.puzzleapp.puzzle.shikaku.ShikakuBoard
import com.github.mantis133.puzzleapp.puzzle.shikaku.ShikakuEngine
import com.github.mantis133.puzzleapp.puzzle.shikaku.ShikakuRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A rectangle placed by the player, annotated with which clue it satisfies. */
data class PlacedRectangle(
    val rect: ShikakuRectangle,
    /** Index into [ShikakuBoard.clues]. */
    val clueIndex: Int,
    /** Stable colour slot (= clueIndex mod palette size). */
    val colorIndex: Int
)

data class ShikakuUiState(
    val board: ShikakuBoard? = null,
    val placedRectangles: List<PlacedRectangle> = emptyList(),
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val difficulty: Difficulty = Difficulty.Easy
)

class ShikakuViewModel(application: Application) : AndroidViewModel(application) {

    private val engine     = ShikakuEngine()
    private val statsRepo  = StatsRepository(application)

    private val _uiState = MutableStateFlow(ShikakuUiState())
    val uiState: StateFlow<ShikakuUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    fun generatePuzzle(difficulty: Difficulty) {
        timerJob?.cancel()
        _uiState.update { ShikakuUiState(isLoading = true, difficulty = difficulty) }

        viewModelScope.launch(Dispatchers.Default) {
            val board = engine.generate(difficulty, ensureUnique = true)
            _uiState.update { ShikakuUiState(board = board, difficulty = difficulty) }
            if (board != null) startTimer()
        }
    }

    /**
     * Attempts to place [rect].  Only accepted if it is non-overlapping,
     * contains exactly one clue, and its area matches that clue's value.
     */
    fun tryPlaceRectangle(rect: ShikakuRectangle) {
        val state = _uiState.value
        val board = state.board ?: return
        if (state.isComplete) return

        val existingRects = state.placedRectangles.map { it.rect }
        if (engine.validatePlacement(board, rect, existingRects) != PlacementResult.VALID) return

        val clueIndex = board.clues.indexOfFirst { rect.contains(it.row, it.col) }
        if (state.placedRectangles.any { it.clueIndex == clueIndex }) return  // Already placed

        val newPlaced = state.placedRectangles + PlacedRectangle(rect, clueIndex, clueIndex)
        val complete  = engine.isSolved(board, newPlaced.map { it.rect })

        _uiState.update { it.copy(placedRectangles = newPlaced, isComplete = complete) }

        if (complete) {
            timerJob?.cancel()
            // Record stats for preset difficulties
            val elapsed = _uiState.value.elapsedSeconds
            viewModelScope.launch {
                statsRepo.recordShikakuCompletion(state.difficulty, elapsed)
            }
        }
    }

    /** Removes whichever rectangle covers cell (row, col), if any. */
    fun removeRectangleAt(row: Int, col: Int) {
        val state = _uiState.value
        if (state.isComplete) return
        val hit = state.placedRectangles.find { it.rect.contains(row, col) } ?: return
        _uiState.update { it.copy(placedRectangles = it.placedRectangles - hit) }
    }

    fun undoLast() {
        _uiState.update { state ->
            if (state.isComplete || state.placedRectangles.isEmpty()) state
            else state.copy(placedRectangles = state.placedRectangles.dropLast(1))
        }
    }

    // ------------------------------------------------------------------

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
