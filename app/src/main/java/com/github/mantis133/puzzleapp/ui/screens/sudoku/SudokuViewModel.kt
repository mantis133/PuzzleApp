package com.github.mantis133.puzzleapp.ui.screens.sudoku

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mantis133.puzzleapp.data.StatsRepository
import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.sudoku.SudokuBoard
import com.github.mantis133.puzzleapp.puzzle.sudoku.SudokuEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SudokuUiState(
    val board: SudokuBoard? = null,
    /** Current 81-cell grid including player entries. Immutable List for Compose stability. */
    val playerGrid: List<Int> = emptyList(),
    /** Set of grid positions (row*9+col) that contain conflicts. */
    val errors: Set<Int> = emptySet(),
    /** Currently selected cell index (row*9+col), or -1 if none. */
    val selectedCell: Int = -1,
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val difficulty: Difficulty = Difficulty.Easy,
    /** Stack of (position, previousValue) pairs for undo. */
    val moveHistory: List<Pair<Int, Int>> = emptyList()
)

class SudokuViewModel(application: Application) : AndroidViewModel(application) {

    private val engine    = SudokuEngine()
    private val statsRepo = StatsRepository(application)

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun generatePuzzle(difficulty: Difficulty) {
        timerJob?.cancel()
        _uiState.update { SudokuUiState(isLoading = true, difficulty = difficulty) }

        viewModelScope.launch(Dispatchers.Default) {
            val board = engine.generate(difficulty)
            _uiState.update {
                SudokuUiState(
                    board        = board,
                    playerGrid   = board.grid.toList(),
                    difficulty   = difficulty,
                    selectedCell = -1
                )
            }
            startTimer()
        }
    }

    /** Selects or deselects a cell. Given cells can still be selected (for highlighting). */
    fun selectCell(pos: Int) {
        if (_uiState.value.isComplete) return
        _uiState.update { state ->
            state.copy(selectedCell = if (state.selectedCell == pos) -1 else pos)
        }
    }

    /** Places [digit] (1–9) in the selected cell, or 0 to erase. */
    fun enterDigit(digit: Int) {
        val state = _uiState.value
        val pos   = state.selectedCell
        if (pos < 0 || state.isComplete) return
        val board = state.board ?: return
        if (board.given[pos]) return                    // Cannot change given cells
        val oldValue = state.playerGrid[pos]
        if (oldValue == digit) return                   // No-op

        val newGrid   = state.playerGrid.toMutableList().also { it[pos] = digit }
        val errors    = engine.computeErrors(newGrid)
        val complete  = engine.isSolved(newGrid)
        val history   = state.moveHistory + (pos to oldValue)

        _uiState.update {
            it.copy(
                playerGrid   = newGrid,
                errors       = errors,
                isComplete   = complete,
                moveHistory  = history
            )
        }

        if (complete) {
            timerJob?.cancel()
            val elapsed = _uiState.value.elapsedSeconds
            viewModelScope.launch {
                statsRepo.recordSudokuCompletion(state.difficulty, elapsed)
            }
        }
    }

    fun eraseCell() = enterDigit(0)

    fun undoLast() {
        val state = _uiState.value
        if (state.isComplete || state.moveHistory.isEmpty()) return
        val (pos, oldValue) = state.moveHistory.last()
        val newGrid = state.playerGrid.toMutableList().also { it[pos] = oldValue }
        val errors  = engine.computeErrors(newGrid)
        _uiState.update {
            it.copy(
                playerGrid  = newGrid,
                errors      = errors,
                isComplete  = false,
                moveHistory = it.moveHistory.dropLast(1),
                selectedCell = pos
            )
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

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

