package com.github.mantis133.puzzleapp.ui.screens.wires

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mantis133.puzzleapp.data.StatsRepository
import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.wires.WireColor
import com.github.mantis133.puzzleapp.puzzle.wires.WiresBoard
import com.github.mantis133.puzzleapp.puzzle.wires.WiresEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WiresUiState(
    val board:           WiresBoard?             = null,
    /** Ordered cell-index lists for each colour's drawn path. */
    val paths:           Map<WireColor, List<Int>> = emptyMap(),
    /** Flat row-major grid: which colour (if any) occupies each cell. */
    val cellColors:      List<WireColor?>          = emptyList(),
    /** Colours whose paths connect both terminals. */
    val completedColors: Set<WireColor>            = emptySet(),
    val isComplete:      Boolean                   = false,
    val isLoading:       Boolean                   = false,
    val elapsedSeconds:  Long                      = 0L,
    val difficulty:      Difficulty                = Difficulty.Easy
)

class WiresViewModel(application: Application) : AndroidViewModel(application) {

    private val engine    = WiresEngine()
    private val statsRepo = StatsRepository(application)

    private val _uiState = MutableStateFlow(WiresUiState())
    val uiState: StateFlow<WiresUiState> = _uiState.asStateFlow()

    private var timerJob:        Job?       = null
    /** Colour currently being drawn by an in-progress drag gesture. */
    private var activeDragColor: WireColor? = null

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    fun generatePuzzle(difficulty: Difficulty) {
        timerJob?.cancel()
        activeDragColor = null
        _uiState.update { WiresUiState(isLoading = true, difficulty = difficulty) }

        viewModelScope.launch(Dispatchers.Default) {
            val board      = engine.generate(difficulty)
            val cellColors = board?.let { List<WireColor?>(it.rows * it.cols) { null } } ?: emptyList()
            _uiState.update {
                WiresUiState(board = board, cellColors = cellColors, difficulty = difficulty)
            }
            if (board != null) startTimer()
        }
    }

    /**
     * Called when the player's finger first touches the board at [cellIndex].
     *
     * - If the cell contains a terminal, clear that colour's path and begin a fresh draw.
     * - If the cell is occupied by a wire, truncate that wire at this cell and continue
     *   drawing from here.
     * - Otherwise, the touch is ignored.
     */
    fun startDrag(cellIndex: Int) {
        val state = _uiState.value
        val board = state.board ?: return
        if (state.isComplete) return

        val terminalHere   = board.terminals.find { it.row * board.cols + it.col == cellIndex }
        val occupyingColor = state.cellColors.getOrNull(cellIndex)

        val color:         WireColor
        val truncatedPath: List<Int>

        when {
            terminalHere != null -> {
                // Begin fresh from this terminal
                color         = terminalHere.color
                truncatedPath = listOf(cellIndex)
            }
            occupyingColor != null -> {
                // Truncate the existing path at this cell
                color = occupyingColor
                val existing = state.paths[color] ?: return
                val idx      = existing.indexOf(cellIndex)
                if (idx < 0) return
                truncatedPath = existing.subList(0, idx + 1).toList()
            }
            else -> return
        }

        activeDragColor = color

        val newCellColors = state.cellColors.toMutableList()
        state.paths[color]?.forEach { idx -> newCellColors[idx] = null }
        truncatedPath.forEach    { idx -> newCellColors[idx] = color }

        val newPaths = state.paths.toMutableMap()
        newPaths[color] = truncatedPath

        _uiState.update {
            it.copy(
                paths           = newPaths,
                cellColors      = newCellColors,
                completedColors = computeCompletedColors(board, newPaths)
            )
        }
    }

    /**
     * Called whenever the dragging finger moves into a new cell ([cellIndex]).
     *
     * - If the new cell is already part of the current path, the path is truncated
     *   (backtracking erases the wire).
     * - Otherwise the path is extended to the new cell, provided it is orthogonally
     *   adjacent, unoccupied (or occupied by the same colour), and not another colour's
     *   terminal.
     */
    fun continueDrag(cellIndex: Int) {
        val color       = activeDragColor ?: return
        val state       = _uiState.value
        val board       = state.board ?: return
        val currentPath = state.paths[color] ?: return

        if (currentPath.isEmpty() || cellIndex == currentPath.last()) return

        // ── Backtrack: finger re-entered a cell already in the path ──────────
        val backtrackIdx = currentPath.indexOf(cellIndex)
        if (backtrackIdx >= 0) {
            val newPath       = currentPath.subList(0, backtrackIdx + 1).toList()
            val newCellColors = state.cellColors.toMutableList()
            for (i in backtrackIdx + 1 until currentPath.size) {
                newCellColors[currentPath[i]] = null
            }
            val newPaths = state.paths.toMutableMap()
            newPaths[color] = newPath
            _uiState.update {
                it.copy(
                    paths           = newPaths,
                    cellColors      = newCellColors,
                    completedColors = computeCompletedColors(board, newPaths)
                )
            }
            return
        }

        // ── Extension: don't extend a wire that already connects both terminals
        if (color in state.completedColors) return

        // ── Check orthogonal adjacency ────────────────────────────────────────
        val lastCell  = currentPath.last()
        val lastRow   = lastCell / board.cols;   val lastCol   = lastCell % board.cols
        val targetRow = cellIndex / board.cols;  val targetCol = cellIndex % board.cols
        if (kotlin.math.abs(targetRow - lastRow) + kotlin.math.abs(targetCol - lastCol) != 1) return

        // ── Check cell availability ───────────────────────────────────────────
        val existing = state.cellColors.getOrNull(cellIndex)
        if (existing != null && existing != color) return       // Occupied by another colour

        val isOtherTerminal = board.terminals.any { t ->
            t.row * board.cols + t.col == cellIndex && t.color != color
        }
        if (isOtherTerminal) return                             // Can't overwrite another terminal

        // ── Extend the path ───────────────────────────────────────────────────
        val newPath       = currentPath + cellIndex
        val newPaths      = state.paths.toMutableMap()
        newPaths[color]   = newPath

        val newCellColors = state.cellColors.toMutableList()
        newCellColors[cellIndex] = color

        _uiState.update {
            it.copy(
                paths           = newPaths,
                cellColors      = newCellColors,
                completedColors = computeCompletedColors(board, newPaths)
            )
        }
    }

    /** Called when the player's finger is lifted.  Checks for puzzle completion. */
    fun endDrag() {
        activeDragColor = null
        val state = _uiState.value
        val board = state.board ?: return

        val numPairs       = board.terminals.distinctBy { it.color }.size
        val allFilled      = state.cellColors.count { it != null } == board.rows * board.cols
        val allConnected   = state.completedColors.size == numPairs

        if (allFilled && allConnected && !state.isComplete) {
            timerJob?.cancel()
            _uiState.update { it.copy(isComplete = true) }
            viewModelScope.launch {
                statsRepo.recordWiresCompletion(state.difficulty, state.elapsedSeconds)
            }
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * A colour is "completed" when its drawn path has the correct colour's two terminals
     * as its first and last cells (in either order).
     */
    private fun computeCompletedColors(
        board: WiresBoard,
        paths: Map<WireColor, List<Int>>
    ): Set<WireColor> {
        val completed         = mutableSetOf<WireColor>()
        val terminalsByColor  = board.terminals.groupBy { it.color }
        for ((color, terminals) in terminalsByColor) {
            if (terminals.size != 2) continue
            val path = paths[color] ?: continue
            if (path.size < 2) continue
            val t1    = terminals[0].row * board.cols + terminals[0].col
            val t2    = terminals[1].row * board.cols + terminals[1].col
            val first = path.first(); val last = path.last()
            if ((first == t1 && last == t2) || (first == t2 && last == t1)) {
                completed.add(color)
            }
        }
        return completed
    }

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
