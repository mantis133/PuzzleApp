package com.github.mantis133.puzzleapp.ui.screens.solitaire

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mantis133.puzzleapp.data.StatsRepository
import com.github.mantis133.puzzleapp.puzzle.solitaire.CardLocation
import com.github.mantis133.puzzleapp.puzzle.solitaire.SolitaireBoard
import com.github.mantis133.puzzleapp.puzzle.solitaire.SolitaireEngine
import com.github.mantis133.puzzleapp.puzzle.solitaire.Card
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

data class SolitaireUiState(
    val board: SolitaireBoard? = null,
    val selectedLocation: CardLocation? = null,
    val selectedCards: List<Card> = emptyList(),
    val isComplete: Boolean = false,
    val isAutoCompletable: Boolean = false,
    val isAutoCompleting: Boolean = false,
    val elapsedSeconds: Long = 0L,
    /** Snapshot stack for undo — most recent state is last. Max 50 entries. */
    val boardHistory: List<SolitaireBoard> = emptyList()
)

class SolitaireViewModel(application: Application) : AndroidViewModel(application) {

    private val statsRepo = StatsRepository(application)

    private val _uiState = MutableStateFlow(SolitaireUiState())
    val uiState: StateFlow<SolitaireUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var autoCompleteJob: Job? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    init {
        startGame()
    }

    fun startGame() {
        timerJob?.cancel()
        autoCompleteJob?.cancel()
        val board = SolitaireEngine.deal()
        _uiState.value = SolitaireUiState(
            board        = board,
            boardHistory = listOf(board)
        )
        startTimer()
    }

    fun newGame() = startGame()

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val state = _uiState.value
                if (state.isComplete) break
                _uiState.value = state.copy(elapsedSeconds = state.elapsedSeconds + 1)
            }
        }
    }

    // ── Stock ─────────────────────────────────────────────────────────────────

    fun onStockTap() {
        val state = _uiState.value
        val board = state.board ?: return
        clearSelection()
        val newBoard = if (board.stock.isNotEmpty()) {
            SolitaireEngine.flipStock(board)
        } else {
            SolitaireEngine.recycleWaste(board)
        }
        pushBoard(newBoard)
    }

    // ── Selection / Move ──────────────────────────────────────────────────────

    fun onLocationTap(location: CardLocation) {
        val state = _uiState.value
        val board = state.board ?: return

        // Phase 2: something already selected — try to move
        if (state.selectedLocation != null) {
            if (tryMove(board, state.selectedLocation, state.selectedCards, location)) return
            // Couldn't move: treat as a new selection attempt at the tapped location
        }

        // Phase 1: select the tapped location
        val movable = SolitaireEngine.getMovableCards(board, location)
        if (movable.isEmpty()) {
            clearSelection()
            return
        }
        _uiState.value = state.copy(
            selectedLocation = location,
            selectedCards    = movable
        )
    }

    private fun tryMove(
        board: SolitaireBoard,
        from: CardLocation,
        cards: List<Card>,
        to: CardLocation
    ): Boolean {
        val legal = when (to) {
            is CardLocation.Tableau -> {
                SolitaireEngine.canMoveToTableau(cards, board.tableau[to.column])
            }
            is CardLocation.Foundation -> {
                cards.size == 1 && SolitaireEngine.canMoveToFoundation(
                    cards.first(),
                    board.foundations[to.suit.ordinal]
                )
            }
            is CardLocation.Waste -> false
        }

        return if (legal) {
            val newBoard = SolitaireEngine.applyMove(board, from, to)
            clearSelection()
            pushBoard(newBoard)
            if (SolitaireEngine.isSolved(newBoard)) onGameWon(newBoard)
            true
        } else {
            clearSelection()
            false
        }
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    fun undo() {
        val state = _uiState.value
        if (state.boardHistory.size <= 1) return
        val newHistory = state.boardHistory.dropLast(1)
        _uiState.value = state.copy(
            board            = newHistory.last(),
            boardHistory     = newHistory,
            selectedLocation = null,
            selectedCards    = emptyList()
        )
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedLocation = null,
            selectedCards    = emptyList()
        )
    }

    private fun pushBoard(newBoard: SolitaireBoard) {
        val current = _uiState.value
        val newHistory = (current.boardHistory + newBoard).takeLast(50)
        _uiState.value = current.copy(
            board               = newBoard,
            boardHistory        = newHistory,
            isAutoCompletable   = SolitaireEngine.isAutoCompletable(newBoard)
        )
    }

    // ── Auto-complete ─────────────────────────────────────────────────────────

    /**
     * Starts the auto-complete animation: repeatedly finds the next valid foundation
     * move and applies it with a short visual delay until the board is solved.
     */
    fun autoComplete() {
        val state = _uiState.value
        if (!state.isAutoCompletable || state.isAutoCompleting || state.isComplete) return
        autoCompleteJob?.cancel()
        _uiState.value = state.copy(isAutoCompleting = true, selectedLocation = null, selectedCards = emptyList())
        autoCompleteJob = viewModelScope.launch {
            var board = _uiState.value.board ?: return@launch
            while (isActive && !SolitaireEngine.isSolved(board)) {
                val move = SolitaireEngine.findAutoCompleteMove(board) ?: break
                board = SolitaireEngine.applyMove(board, move.first, move.second)
                _uiState.value = _uiState.value.copy(board = board)
                delay(80) // short delay for visual effect
            }
            if (SolitaireEngine.isSolved(board)) {
                onGameWon(board)
            }
            _uiState.value = _uiState.value.copy(isAutoCompleting = false)
        }
    }

    private fun onGameWon(board: SolitaireBoard) {        timerJob?.cancel()
        val elapsed = _uiState.value.elapsedSeconds
        _uiState.value = _uiState.value.copy(isComplete = true, board = board)
        viewModelScope.launch {
            statsRepo.recordSolitaireWin(elapsed)
        }
    }
}
