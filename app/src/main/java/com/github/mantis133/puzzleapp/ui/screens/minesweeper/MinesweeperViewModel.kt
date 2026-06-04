package com.github.mantis133.puzzleapp.ui.screens.minesweeper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mantis133.puzzleapp.data.StatsRepository
import com.github.mantis133.puzzleapp.puzzle.minesweeper.CellState
import com.github.mantis133.puzzleapp.puzzle.minesweeper.GameState
import com.github.mantis133.puzzleapp.puzzle.minesweeper.MinesweeperCell
import com.github.mantis133.puzzleapp.puzzle.minesweeper.MinesweeperDifficulty
import com.github.mantis133.puzzleapp.puzzle.minesweeper.MinesweeperEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MinesweeperUiState(
    val difficulty:    MinesweeperDifficulty = MinesweeperDifficulty.BEGINNER,
    /** Flat cell list, row-major: index = row * cols + col. */
    val cells:         List<MinesweeperCell> = emptyList(),
    val gameState:     GameState             = GameState.IDLE,
    /** Index of the cell whose action popup is currently shown (-1 = none). */
    val selectedCell:  Int                   = -1,
    val elapsedSeconds: Long                 = 0L
)

class MinesweeperViewModel(application: Application) : AndroidViewModel(application) {

    private val statsRepo = StatsRepository(application)

    private val _uiState = MutableStateFlow(MinesweeperUiState())
    val uiState: StateFlow<MinesweeperUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /** Reset to a blank board for [difficulty] (mines are placed on first dig). */
    fun newGame(difficulty: MinesweeperDifficulty) {
        timerJob?.cancel()
        _uiState.update {
            MinesweeperUiState(
                difficulty = difficulty,
                cells      = List(difficulty.rows * difficulty.cols) { MinesweeperCell() },
                gameState  = GameState.IDLE
            )
        }
    }

    /**
     * Tap a cell to open (or close) the action popup.
     *
     * - Tapping the already-selected cell dismisses the popup.
     * - Revealed cells with 0 adjacent mines (blank) skip the popup.
     * - Tap is ignored when the game is over.
     */
    fun selectCell(pos: Int) {
        val state = _uiState.value
        if (state.gameState == GameState.WON || state.gameState == GameState.LOST) return
        val cell = state.cells.getOrNull(pos) ?: return
        // Blank revealed cells have no action — dismiss any open popup
        if (cell.state == CellState.REVEALED && cell.adjacentMines == 0) {
            _uiState.update { it.copy(selectedCell = -1) }
            return
        }
        _uiState.update { it.copy(selectedCell = if (it.selectedCell == pos) -1 else pos) }
    }

    fun dismissAction() {
        _uiState.update { it.copy(selectedCell = -1) }
    }

    /**
     * "Dig" the cell at [pos].
     *
     * - On the very first dig the board is generated (safe-zone around [pos]),
     *   the timer starts, and [pos] is immediately revealed.
     * - On a revealed numbered cell this performs a **chord** (auto-reveal
     *   neighbours if the correct number of flags surround the cell).
     * - On a hidden cell this reveals normally (with flood-fill).
     */
    fun dig(pos: Int) {
        val state = _uiState.value
        if (state.gameState == GameState.WON || state.gameState == GameState.LOST) return

        _uiState.update { it.copy(selectedCell = -1) }

        val rows = state.difficulty.rows
        val cols = state.difficulty.cols

        val (cells, hitMine) = when {
            state.gameState == GameState.IDLE -> {
                // First click — generate board now
                val generated = MinesweeperEngine.generateBoard(state.difficulty, pos)
                startTimer()
                MinesweeperEngine.reveal(generated, pos, rows, cols)
            }
            state.cells.getOrNull(pos)?.state == CellState.REVEALED -> {
                // Chord
                MinesweeperEngine.chord(state.cells, pos, rows, cols)
            }
            else -> {
                MinesweeperEngine.reveal(state.cells, pos, rows, cols)
            }
        }

        val newGameState = when {
            hitMine                          -> GameState.LOST
            MinesweeperEngine.isWon(cells)   -> GameState.WON
            else                             -> GameState.PLAYING
        }

        _uiState.update { it.copy(cells = cells, gameState = newGameState) }

        if (newGameState == GameState.WON || newGameState == GameState.LOST) {
            timerJob?.cancel()
            if (newGameState == GameState.WON) {
                val elapsed = _uiState.value.elapsedSeconds
                viewModelScope.launch {
                    statsRepo.recordMinesweeperWin(state.difficulty, elapsed)
                }
            }
        }
    }

    /** Toggle the flag on [pos]. Only works during an active game. */
    fun flag(pos: Int) {
        val state = _uiState.value
        if (state.gameState == GameState.IDLE ||
            state.gameState == GameState.WON  ||
            state.gameState == GameState.LOST) return
        val newCells = MinesweeperEngine.toggleFlag(state.cells, pos)
        _uiState.update { it.copy(cells = newCells, selectedCell = -1) }
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

