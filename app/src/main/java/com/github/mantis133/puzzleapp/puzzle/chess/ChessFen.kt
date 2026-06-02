package com.github.mantis133.puzzleapp.puzzle.chess

/**
 * Parses a FEN string into a [ChessPosition].
 * Platform-agnostic.
 */
object ChessFen {

    fun parse(fen: String): ChessPosition {
        val parts = fen.trim().split(" ")
        require(parts.size >= 4) { "Invalid FEN: $fen" }

        val board = parseBoard(parts[0])
        val side  = if (parts[1] == "w") PieceColor.WHITE else PieceColor.BLACK
        val castling = parseCastling(parts[2])
        val ep = if (parts[3] == "-") -1 else squareIndex(parts[3])
        val halfMove   = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullMove   = parts.getOrNull(5)?.toIntOrNull() ?: 1

        return ChessPosition(board, side, castling, ep, halfMove, fullMove)
    }

    private fun parseBoard(placement: String): Board {
        val board = emptyBoard()
        var rank = 0; var file = 0
        for (ch in placement) {
            when {
                ch == '/' -> { rank++; file = 0 }
                ch.isDigit() -> file += ch.digitToInt()
                else -> {
                    board[sq(rank, file)] = charToPiece(ch)
                    file++
                }
            }
        }
        return board
    }

    private fun charToPiece(ch: Char): Piece {
        val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
        val type = when (ch.lowercaseChar()) {
            'p' -> PieceType.PAWN
            'n' -> PieceType.KNIGHT
            'b' -> PieceType.BISHOP
            'r' -> PieceType.ROOK
            'q' -> PieceType.QUEEN
            'k' -> PieceType.KING
            else -> throw IllegalArgumentException("Unknown piece: $ch")
        }
        return Piece(type, color)
    }

    private fun parseCastling(s: String) = CastlingRights(
        whiteKingside  = 'K' in s,
        whiteQueenside = 'Q' in s,
        blackKingside  = 'k' in s,
        blackQueenside = 'q' in s
    )

    /** Serialise a [ChessPosition] back to FEN (useful for debugging). */
    fun toFen(pos: ChessPosition): String {
        val sb = StringBuilder()
        for (rank in 0..7) {
            var empty = 0
            for (file in 0..7) {
                val piece = pos.board[sq(rank, file)]
                if (piece == null) { empty++ } else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(pieceChar(piece))
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank < 7) sb.append('/')
        }
        val side = if (pos.sideToMove == PieceColor.WHITE) "w" else "b"
        val castling = buildString {
            if (pos.castling.whiteKingside)  append('K')
            if (pos.castling.whiteQueenside) append('Q')
            if (pos.castling.blackKingside)  append('k')
            if (pos.castling.blackQueenside) append('q')
            if (isEmpty()) append('-')
        }
        val ep = if (pos.enPassant < 0) "-" else squareName(pos.enPassant)
        return "$sb $side $castling $ep ${pos.halfMoveClock} ${pos.fullMoveNumber}"
    }

    private fun pieceChar(p: Piece): Char {
        val ch = when (p.type) {
            PieceType.PAWN   -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK   -> 'r'
            PieceType.QUEEN  -> 'q'
            PieceType.KING   -> 'k'
        }
        return if (p.color == PieceColor.WHITE) ch.uppercaseChar() else ch
    }
}

