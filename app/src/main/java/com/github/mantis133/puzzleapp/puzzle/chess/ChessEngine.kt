package com.github.mantis133.puzzleapp.puzzle.chess

/**
 * Complete legal-move generator and position mutator.
 * Platform-agnostic — no Android dependencies.
 *
 * Square indexing:  index = rank*8 + file
 *   rank 0 = rank-8 (Black back rank)   rank 7 = rank-1 (White back rank)
 *   file 0 = file-a                     file 7 = file-h
 */
object ChessEngine {

    // ── Public API ────────────────────────────────────────────────────────────

    /** All legal moves in [pos]. */
    fun legalMoves(pos: ChessPosition): List<Move> =
        pseudoLegal(pos).filter { isLegal(pos, it) }

    /** Legal moves originating from [square]. */
    fun legalMovesFrom(pos: ChessPosition, square: Int): List<Move> =
        legalMoves(pos).filter { it.from == square }

    /** Parse a UCI string ("e2e4", "e7e8q") into a [Move] if it is legal, else null. */
    fun parseUci(pos: ChessPosition, uci: String): Move? {
        if (uci.length < 4) return null
        val from  = squareIndex(uci.substring(0, 2))
        val to    = squareIndex(uci.substring(2, 4))
        val promo = if (uci.length == 5) promoCharToType(uci[4]) else null
        val move  = Move(from, to, promo)
        return if (legalMoves(pos).any { it == move }) move else null
    }

    /** Apply [move] to [pos], returning the new position. Does NOT validate legality. */
    fun applyMove(pos: ChessPosition, move: Move): ChessPosition {
        val board     = pos.board.copyOf()
        val piece     = board[move.from] ?: return pos
        val target    = board[move.to]

        // Move the piece
        board[move.from] = null
        board[move.to]   = if (move.promotion != null) Piece(move.promotion, piece.color) else piece

        var newEp = -1

        when (piece.type) {
            PieceType.PAWN -> {
                // En-passant capture
                if (move.to == pos.enPassant) {
                    val capturedRank = rankOf(move.to) + if (piece.color == PieceColor.WHITE) 1 else -1
                    board[sq(capturedRank, fileOf(move.to))] = null
                }
                // Double push — set en-passant target
                val rankDelta = rankOf(move.from) - rankOf(move.to)
                if (kotlin.math.abs(rankDelta) == 2) {
                    newEp = sq(rankOf(move.from) + if (piece.color == PieceColor.WHITE) -1 else 1, fileOf(move.from))
                }
            }
            PieceType.KING -> {
                // Castling — move the rook
                val fileDelta = fileOf(move.to) - fileOf(move.from)
                if (kotlin.math.abs(fileDelta) == 2) {
                    val rank = rankOf(move.from)
                    if (fileDelta > 0) { // Kingside
                        board[sq(rank, 5)] = board[sq(rank, 7)]
                        board[sq(rank, 7)] = null
                    } else {             // Queenside
                        board[sq(rank, 3)] = board[sq(rank, 0)]
                        board[sq(rank, 0)] = null
                    }
                }
            }
            else -> {}
        }

        // Update castling rights
        val cr = pos.castling
        val newCr = CastlingRights(
            whiteKingside  = cr.whiteKingside  && move.from != sq(7,4) && move.from != sq(7,7) && move.to != sq(7,7),
            whiteQueenside = cr.whiteQueenside && move.from != sq(7,4) && move.from != sq(7,0) && move.to != sq(7,0),
            blackKingside  = cr.blackKingside  && move.from != sq(0,4) && move.from != sq(0,7) && move.to != sq(0,7),
            blackQueenside = cr.blackQueenside && move.from != sq(0,4) && move.from != sq(0,0) && move.to != sq(0,0)
        )

        val halfClock = if (piece.type == PieceType.PAWN || target != null) 0 else pos.halfMoveClock + 1
        val fullMove  = if (pos.sideToMove == PieceColor.BLACK) pos.fullMoveNumber + 1 else pos.fullMoveNumber

        return pos.copy(
            board          = board,
            sideToMove     = pos.sideToMove.opposite(),
            castling       = newCr,
            enPassant      = newEp,
            halfMoveClock  = halfClock,
            fullMoveNumber = fullMove
        )
    }

    /** True if [color]'s king is currently in check. */
    fun isInCheck(pos: ChessPosition, color: PieceColor): Boolean {
        val kingSquare = pos.board.indexOfFirst { it?.type == PieceType.KING && it.color == color }
        if (kingSquare < 0) return false
        return isAttacked(pos, kingSquare, color.opposite())
    }

    /** True if [square] is attacked by any piece of [byColor]. */
    fun isAttacked(pos: ChessPosition, square: Int, byColor: PieceColor): Boolean {
        val r = rankOf(square); val f = fileOf(square)

        // Pawn attacks
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        for (df in listOf(-1, 1)) {
            val pr = r + pawnDir; val pf = f + df
            if (onBoard(pr, pf)) {
                val p = pos.board[sq(pr, pf)]
                if (p?.type == PieceType.PAWN && p.color == byColor) return true
            }
        }
        // Knight attacks
        for ((dr, df) in KNIGHT_DELTAS) {
            val nr = r + dr; val nf = f + df
            if (onBoard(nr, nf)) {
                val p = pos.board[sq(nr, nf)]
                if (p?.type == PieceType.KNIGHT && p.color == byColor) return true
            }
        }
        // Sliding: rook/queen on orthogonals, bishop/queen on diagonals
        for ((dr, df) in ROOK_DIRS) {
            var nr = r + dr; var nf = f + df
            while (onBoard(nr, nf)) {
                val p = pos.board[sq(nr, nf)]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                nr += dr; nf += df
            }
        }
        for ((dr, df) in BISHOP_DIRS) {
            var nr = r + dr; var nf = f + df
            while (onBoard(nr, nf)) {
                val p = pos.board[sq(nr, nf)]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                nr += dr; nf += df
            }
        }
        // King adjacency
        for ((dr, df) in KING_DELTAS) {
            val nr = r + dr; val nf = f + df
            if (onBoard(nr, nf)) {
                val p = pos.board[sq(nr, nf)]
                if (p?.type == PieceType.KING && p.color == byColor) return true
            }
        }
        return false
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun isLegal(pos: ChessPosition, move: Move): Boolean {
        val after = applyMove(pos, move)
        return !isInCheck(after, pos.sideToMove)
    }

    private fun pseudoLegal(pos: ChessPosition): List<Move> {
        val moves = mutableListOf<Move>()
        val side  = pos.sideToMove
        for (sq in 0..63) {
            val piece = pos.board[sq] ?: continue
            if (piece.color != side) continue
            when (piece.type) {
                PieceType.PAWN   -> pawnMoves(pos, sq, piece.color, moves)
                PieceType.KNIGHT -> jumpMoves(pos, sq, piece.color, KNIGHT_DELTAS, moves)
                PieceType.BISHOP -> slideMoves(pos, sq, piece.color, BISHOP_DIRS, moves)
                PieceType.ROOK   -> slideMoves(pos, sq, piece.color, ROOK_DIRS, moves)
                PieceType.QUEEN  -> { slideMoves(pos, sq, piece.color, BISHOP_DIRS, moves); slideMoves(pos, sq, piece.color, ROOK_DIRS, moves) }
                PieceType.KING   -> { jumpMoves(pos, sq, piece.color, KING_DELTAS, moves); castlingMoves(pos, sq, piece.color, moves) }
            }
        }
        return moves
    }

    private fun pawnMoves(pos: ChessPosition, from: Int, color: PieceColor, out: MutableList<Move>) {
        val r = rankOf(from); val f = fileOf(from)
        val dir        = if (color == PieceColor.WHITE) -1 else 1
        val startRank  = if (color == PieceColor.WHITE) 6 else 1
        val promoRank  = if (color == PieceColor.WHITE) 0 else 7

        // One step forward
        val r1 = r + dir
        if (onBoard(r1, f) && pos.board[sq(r1, f)] == null) {
            if (r1 == promoRank) addPromotions(from, sq(r1, f), out)
            else out += Move(from, sq(r1, f))
            // Two steps from starting rank
            if (r == startRank) {
                val r2 = r + 2 * dir
                if (pos.board[sq(r2, f)] == null) out += Move(from, sq(r2, f))
            }
        }
        // Captures
        for (df in listOf(-1, 1)) {
            val cf = f + df
            if (!onBoard(r1, cf)) continue
            val target = pos.board[sq(r1, cf)]
            val isEp   = sq(r1, cf) == pos.enPassant
            if ((target != null && target.color != color) || isEp) {
                if (r1 == promoRank) addPromotions(from, sq(r1, cf), out)
                else out += Move(from, sq(r1, cf))
            }
        }
    }

    private fun addPromotions(from: Int, to: Int, out: MutableList<Move>) {
        out += Move(from, to, PieceType.QUEEN)
        out += Move(from, to, PieceType.ROOK)
        out += Move(from, to, PieceType.BISHOP)
        out += Move(from, to, PieceType.KNIGHT)
    }

    private fun jumpMoves(pos: ChessPosition, from: Int, color: PieceColor, deltas: List<Pair<Int,Int>>, out: MutableList<Move>) {
        val r = rankOf(from); val f = fileOf(from)
        for ((dr, df) in deltas) {
            val nr = r + dr; val nf = f + df
            if (!onBoard(nr, nf)) continue
            val target = pos.board[sq(nr, nf)]
            if (target == null || target.color != color) out += Move(from, sq(nr, nf))
        }
    }

    private fun slideMoves(pos: ChessPosition, from: Int, color: PieceColor, dirs: List<Pair<Int,Int>>, out: MutableList<Move>) {
        val r = rankOf(from); val f = fileOf(from)
        for ((dr, df) in dirs) {
            var nr = r + dr; var nf = f + df
            while (onBoard(nr, nf)) {
                val target = pos.board[sq(nr, nf)]
                if (target != null) {
                    if (target.color != color) out += Move(from, sq(nr, nf))
                    break
                }
                out += Move(from, sq(nr, nf))
                nr += dr; nf += df
            }
        }
    }

    private fun castlingMoves(pos: ChessPosition, from: Int, color: PieceColor, out: MutableList<Move>) {
        if (isInCheck(pos, color)) return
        val rank     = if (color == PieceColor.WHITE) 7 else 0
        val cr       = pos.castling
        val opponent = color.opposite()

        // Kingside
        val ksClear = (pos.castling.whiteKingside && color == PieceColor.WHITE) ||
                      (pos.castling.blackKingside && color == PieceColor.BLACK)
        if (ksClear && from == sq(rank, 4)) {
            if (pos.board[sq(rank, 5)] == null && pos.board[sq(rank, 6)] == null &&
                pos.board[sq(rank, 7)]?.type == PieceType.ROOK &&
                !isAttacked(pos, sq(rank, 5), opponent) && !isAttacked(pos, sq(rank, 6), opponent)
            ) out += Move(from, sq(rank, 6))
        }
        // Queenside
        val qsClear = (pos.castling.whiteQueenside && color == PieceColor.WHITE) ||
                      (pos.castling.blackQueenside && color == PieceColor.BLACK)
        if (qsClear && from == sq(rank, 4)) {
            if (pos.board[sq(rank, 3)] == null && pos.board[sq(rank, 2)] == null && pos.board[sq(rank, 1)] == null &&
                pos.board[sq(rank, 0)]?.type == PieceType.ROOK &&
                !isAttacked(pos, sq(rank, 3), opponent) && !isAttacked(pos, sq(rank, 2), opponent)
            ) out += Move(from, sq(rank, 2))
        }
    }

    private fun promoCharToType(c: Char) = when (c.lowercaseChar()) {
        'q' -> PieceType.QUEEN; 'r' -> PieceType.ROOK; 'b' -> PieceType.BISHOP; else -> PieceType.KNIGHT
    }

    // ── Deltas / directions ───────────────────────────────────────────────────

    private val KNIGHT_DELTAS = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
    private val KING_DELTAS   = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
    private val ROOK_DIRS     = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    private val BISHOP_DIRS   = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
}

