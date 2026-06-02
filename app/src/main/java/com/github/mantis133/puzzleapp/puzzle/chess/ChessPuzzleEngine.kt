package com.github.mantis133.puzzleapp.puzzle.chess

/**
 * Wraps [ChessEngine] with the Lichess puzzle flow.
 *
 * Lichess puzzle CSV format:
 *   FEN  = position BEFORE the opponent's "setup" move.
 *   Moves = space-separated UCI sequence:
 *     moves[0] = opponent's setup move  (played automatically)
 *     moves[1] = player's first move to find
 *     moves[2] = opponent's response    (played automatically)
 *     moves[3] = player's second move   … and so on.
 *
 * Platform-agnostic — no Android imports.
 */
class ChessPuzzleEngine(fen: String, movesString: String) {

    val moves: List<String> = movesString.trim().split(" ").filter { it.isNotBlank() }

    /** The color the player is solving as. */
    val playerColor: PieceColor

    /** Immutable history of positions, starting with [initialPosition]. */
    private val positionHistory = mutableListOf<ChessPosition>()

    /** Index into [moves] for the next expected player move. Starts at 1. */
    var solutionIndex: Int = 1
        private set

    val initialPosition: ChessPosition = ChessFen.parse(fen)

    /** Current board position (after all moves applied so far). */
    val currentPosition: ChessPosition get() = positionHistory.last()

    /** True once all solution moves have been played. */
    val isSolved: Boolean get() = solutionIndex >= moves.size

    init {
        positionHistory.add(initialPosition)
        // The opponent's color is the side to move in the FEN (they make moves[0]).
        playerColor = initialPosition.sideToMove.opposite()
        // Auto-apply the opponent's setup move (moves[0]).
        if (moves.isNotEmpty()) {
            val setupMove = ChessEngine.parseUci(initialPosition, moves[0])
            if (setupMove != null) positionHistory.add(ChessEngine.applyMove(initialPosition, setupMove))
        }
    }

    /**
     * Attempt to play [uciMove] as the player.
     * Returns [MoveResult] indicating success, wrong move, or completion.
     */
    fun playMove(uciMove: String): MoveResult {
        if (isSolved) return MoveResult.AlreadySolved

        val expected = moves.getOrNull(solutionIndex) ?: return MoveResult.AlreadySolved
        if (uciMove != expected) return MoveResult.WrongMove

        // Apply player's move
        val playerMove = ChessEngine.parseUci(currentPosition, uciMove) ?: return MoveResult.WrongMove
        positionHistory.add(ChessEngine.applyMove(currentPosition, playerMove))
        solutionIndex++

        if (isSolved) return MoveResult.Solved

        // Auto-apply opponent response (if any)
        val opponentUci = moves.getOrNull(solutionIndex)
        if (opponentUci != null) {
            val opponentMove = ChessEngine.parseUci(currentPosition, opponentUci)
            if (opponentMove != null) {
                positionHistory.add(ChessEngine.applyMove(currentPosition, opponentMove))
                solutionIndex++
            }
        }

        return if (isSolved) MoveResult.Solved else MoveResult.CorrectContinue
    }

    /** Legal destinations for the piece at [square] in the current position. */
    fun legalDestinations(square: Int): List<Int> =
        ChessEngine.legalMovesFrom(currentPosition, square).map { it.to }

    /** The piece at [square] in the current position. */
    fun pieceAt(square: Int): Piece? = currentPosition.board[square]

    /** The square the player's king occupies (for check highlighting). */
    fun playerKingSquare(): Int =
        currentPosition.board.indexOfFirst { it?.type == PieceType.KING && it.color == playerColor }

    /** True if the player's king is currently in check. */
    fun playerInCheck(): Boolean = ChessEngine.isInCheck(currentPosition, playerColor)

    /** The last move played (for "last move" highlighting), or null. */
    fun lastMoveSquares(): Pair<Int, Int>? {
        val lastUci = moves.getOrNull(solutionIndex - 1) ?: return null
        if (lastUci.length < 4) return null
        return squareIndex(lastUci.substring(0, 2)) to squareIndex(lastUci.substring(2, 4))
    }
}

sealed class MoveResult {
    data object CorrectContinue : MoveResult()
    data object Solved          : MoveResult()
    data object WrongMove       : MoveResult()
    data object AlreadySolved   : MoveResult()
}

