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
    /** Per-cell pencil marks. 81 sets, one per cell. Empty set = no notes. */
    val notes: List<Set<Int>> = emptyList(),
    /** Set of grid positions (row*9+col) that contain conflicts. */
    val errors: Set<Int> = emptySet(),
    /** Currently selected cell index (row*9+col), or -1 if none. */
    val selectedCell: Int = -1,
    /** When true, number pad toggles pencil marks instead of placing digits. */
    val isNotesMode: Boolean = false,
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val difficulty: Difficulty = Difficulty.Easy,
    /**
     * Undo stack — each entry is a snapshot of (playerGrid, notes) before an action.
     * Restoring both lets undo work seamlessly across digit placements and note toggles.
     */
    val moveHistory: List<Pair<List<Int>, List<Set<Int>>>> = emptyList()
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
                    notes        = List(81) { emptySet() },
                    difficulty   = difficulty,
                    selectedCell = -1
                )
            }
            startTimer()
        }
    }

    /** Selects or deselects a cell. */
    fun selectCell(pos: Int) {
        if (_uiState.value.isComplete) return
        _uiState.update { state ->
            state.copy(selectedCell = if (state.selectedCell == pos) -1 else pos)
        }
    }

    /** Toggles notes mode on/off. */
    fun toggleNotesMode() {
        _uiState.update { it.copy(isNotesMode = !it.isNotesMode) }
    }

    /**
     * In **digit mode**: places [digit] (1–9) or erases (0) in the selected cell.
     *   - Clears all pencil marks in that cell.
     *   - Removes [digit] from peer cells' pencil marks (same row/col/box).
     *
     * In **notes mode**: toggles [digit] (1–9) in the selected cell's pencil marks.
     *   - Ignored for digit == 0 (erase has no meaning in notes mode).
     *   - Does not modify [playerGrid].
     */
    fun enterDigit(digit: Int) {
        val state = _uiState.value
        val pos   = state.selectedCell
        if (pos < 0 || state.isComplete) return
        val board = state.board ?: return
        if (board.given[pos]) return

        // Save snapshot before mutation
        val snapshot = state.playerGrid to state.notes

        if (state.isNotesMode) {
            // Notes mode — toggle pencil mark (ignore erase)
            if (digit == 0) return
            val newNotes = state.notes.toMutableList()
            val current  = newNotes[pos]
            newNotes[pos] = if (digit in current) current - digit else current + digit
            _uiState.update {
                it.copy(
                    notes       = newNotes,
                    moveHistory = it.moveHistory + snapshot
                )
            }
        } else {
            // Digit mode — place or erase
            val oldValue = state.playerGrid[pos]
            if (oldValue == digit) return

            val newGrid  = state.playerGrid.toMutableList().also { it[pos] = digit }
            val newNotes = state.notes.toMutableList()

            // Clear notes in the cell being filled
            newNotes[pos] = emptySet()

            // Remove this digit from peer cells' notes
            if (digit > 0) {
                for (peer in peerPositions(pos)) {
                    val peerNotes = newNotes[peer]
                    if (digit in peerNotes) newNotes[peer] = peerNotes - digit
                }
            }

            val errors   = engine.computeErrors(newGrid)
            val complete = engine.isSolved(newGrid)

            _uiState.update {
                it.copy(
                    playerGrid  = newGrid,
                    notes       = newNotes,
                    errors      = errors,
                    isComplete  = complete,
                    moveHistory = it.moveHistory + snapshot
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
    }

    fun eraseCell() = enterDigit(0)

    /** Restores the last saved snapshot (works across digit and note changes). */
    fun undoLast() {
        val state = _uiState.value
        if (state.isComplete || state.moveHistory.isEmpty()) return
        val (prevGrid, prevNotes) = state.moveHistory.last()
        val errors = engine.computeErrors(prevGrid)
        _uiState.update {
            it.copy(
                playerGrid  = prevGrid,
                notes       = prevNotes,
                errors      = errors,
                isComplete  = false,
                moveHistory = it.moveHistory.dropLast(1)
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns all cell positions that share a row, column, or 3×3 box with [pos]. */
    private fun peerPositions(pos: Int): List<Int> {
        val row    = pos / 9
        val col    = pos % 9
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        val peers  = mutableSetOf<Int>()
        for (c in 0 until 9) if (c != col) peers += row * 9 + c
        for (r in 0 until 9) if (r != row) peers += r * 9 + col
        for (r in boxRow until boxRow + 3)
            for (c in boxCol until boxCol + 3)
                if (r != row || c != col) peers += r * 9 + c
        return peers.toList()
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

