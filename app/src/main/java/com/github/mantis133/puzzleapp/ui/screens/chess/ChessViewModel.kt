package com.github.mantis133.puzzleapp.ui.screens.chess

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mantis133.puzzleapp.data.chess.ChessDownloadManager
import com.github.mantis133.puzzleapp.data.chess.ChessPuzzleData
import com.github.mantis133.puzzleapp.data.chess.ChessRepository
import com.github.mantis133.puzzleapp.puzzle.chess.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PuzzleStatus { IDLE, LOADING, PLAYING, SOLVED, ERROR }
enum class MoveStatus   { NONE, WRONG, ANIMATING }

data class ChessUiState(
    val board:          Array<Piece?>        = arrayOfNulls(64),
    val playerColor:    PieceColor           = PieceColor.WHITE,
    val selectedSq:     Int                  = -1,
    val legalDests:     List<Int>            = emptyList(),
    val lastMoveFrom:   Int                  = -1,
    val lastMoveTo:     Int                  = -1,
    val checkSq:        Int                  = -1,
    val puzzleStatus:   PuzzleStatus         = PuzzleStatus.IDLE,
    val moveStatus:     MoveStatus           = MoveStatus.NONE,
    val puzzleRating:   Int                  = 0,
    val puzzleThemes:   String               = "",
    val downloadState:  ChessDownloadManager.DownloadState = ChessDownloadManager.DownloadState.Idle,
    val dbInstalled:    Boolean              = false,
    val minRating:      Int                  = 600,
    val maxRating:      Int                  = 1800
) {
    // Generated equals/hashCode needed because Array doesn't implement them
    override fun equals(other: Any?) = other is ChessUiState && board.contentEquals(other.board) &&
        playerColor == other.playerColor && selectedSq == other.selectedSq &&
        legalDests == other.legalDests && lastMoveFrom == other.lastMoveFrom &&
        lastMoveTo == other.lastMoveTo && checkSq == other.checkSq &&
        puzzleStatus == other.puzzleStatus && moveStatus == other.moveStatus &&
        puzzleRating == other.puzzleRating && puzzleThemes == other.puzzleThemes &&
        downloadState == other.downloadState && dbInstalled == other.dbInstalled &&
        minRating == other.minRating && maxRating == other.maxRating
    override fun hashCode() = board.contentHashCode()
}

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ChessRepository(application)
    private var puzzleEngine: ChessPuzzleEngine? = null

    private val _state = MutableStateFlow(ChessUiState())
    val state: StateFlow<ChessUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(dbInstalled = repo.isDatabaseInstalled()) }
    }

    // ── Download ──────────────────────────────────────────────────────────────

    fun startDownload() {
        viewModelScope.launch {
            repo.downloadDatabase { progress ->
                _state.update { it.copy(downloadState = progress) }
                if (progress is ChessDownloadManager.DownloadState.Done) {
                    _state.update { it.copy(dbInstalled = true) }
                }
            }
        }
    }

    // ── Puzzle loading ────────────────────────────────────────────────────────

    fun loadNextPuzzle() {
        viewModelScope.launch(Dispatchers.Default) {
            _state.update { it.copy(puzzleStatus = PuzzleStatus.LOADING, selectedSq = -1, legalDests = emptyList()) }
            val entity: ChessPuzzleData? = repo.randomPuzzle(_state.value.minRating, _state.value.maxRating)
            if (entity == null) {
                _state.update { it.copy(puzzleStatus = PuzzleStatus.ERROR) }
                return@launch
            }
            val engine = ChessPuzzleEngine(entity.fen, entity.moves)
            puzzleEngine = engine
            val last = engine.lastMoveSquares()
            _state.update { it.copy(
                board        = engine.currentPosition.board.copyOf(),
                playerColor  = engine.playerColor,
                selectedSq   = -1,
                legalDests   = emptyList(),
                lastMoveFrom = last?.first ?: -1,
                lastMoveTo   = last?.second ?: -1,
                checkSq      = if (engine.playerInCheck()) engine.playerKingSquare() else -1,
                puzzleStatus = PuzzleStatus.PLAYING,
                moveStatus   = MoveStatus.NONE,
                puzzleRating = entity.rating,
                puzzleThemes = entity.themes
            )}
        }
    }

    fun setRatingRange(min: Int, max: Int) = _state.update { it.copy(minRating = min, maxRating = max) }

    // ── Board interaction ─────────────────────────────────────────────────────

    fun onSquareTapped(sq: Int) {
        val engine = puzzleEngine ?: return
        val state  = _state.value
        if (state.puzzleStatus != PuzzleStatus.PLAYING || state.moveStatus == MoveStatus.ANIMATING) return

        val piece = engine.pieceAt(sq)

        // Tap own piece → select it
        if (piece != null && piece.color == engine.playerColor) {
            val dests = engine.legalDestinations(sq)
            _state.update { it.copy(selectedSq = sq, legalDests = dests) }
            return
        }

        // Tap destination (or empty square with no selection) → try move
        val selected = state.selectedSq
        if (selected < 0) return

        val movingPiece = engine.pieceAt(selected) ?: return
        // Determine promotion if pawn reaches back rank
        val promoRank  = if (engine.playerColor == PieceColor.WHITE) 0 else 7
        val isPromo    = movingPiece.type == PieceType.PAWN && sq / 8 == promoRank
        val uci        = "${squareName(selected)}${squareName(sq)}${if (isPromo) "q" else ""}"

        when (engine.playMove(uci)) {
            is MoveResult.CorrectContinue, is MoveResult.Solved -> {
                val last = engine.lastMoveSquares()
                val solved = engine.isSolved
                _state.update { it.copy(
                    board        = engine.currentPosition.board.copyOf(),
                    selectedSq   = -1,
                    legalDests   = emptyList(),
                    lastMoveFrom = last?.first ?: -1,
                    lastMoveTo   = last?.second ?: -1,
                    checkSq      = if (!solved && engine.playerInCheck()) engine.playerKingSquare() else -1,
                    puzzleStatus = if (solved) PuzzleStatus.SOLVED else PuzzleStatus.PLAYING,
                    moveStatus   = MoveStatus.NONE
                )}
            }
            is MoveResult.WrongMove -> {
                viewModelScope.launch {
                    _state.update { it.copy(moveStatus = MoveStatus.WRONG) }
                    delay(700)
                    _state.update { it.copy(moveStatus = MoveStatus.NONE, selectedSq = -1, legalDests = emptyList()) }
                }
            }
            else -> {}
        }
    }
}





