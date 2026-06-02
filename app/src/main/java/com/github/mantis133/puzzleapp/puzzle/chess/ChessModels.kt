package com.github.mantis133.puzzleapp.puzzle.chess

// ── Core types ────────────────────────────────────────────────────────────────

enum class PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
enum class PieceColor { WHITE, BLACK;
    fun opposite() = if (this == WHITE) BLACK else WHITE
}

data class Piece(val type: PieceType, val color: PieceColor)

/**
 * Board: 64 nullable Piece slots.
 * Index = rank * 8 + file.
 * rank 0 = rank-8 (Black's back rank), rank 7 = rank-1 (White's back rank).
 * file 0 = file-a, file 7 = file-h.
 */
typealias Board = Array<Piece?>

fun emptyBoard(): Board = arrayOfNulls(64)

data class CastlingRights(
    val whiteKingside:  Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside:  Boolean = true,
    val blackQueenside: Boolean = true
)

/**
 * Complete chess position — platform-agnostic, no Android imports.
 */
data class ChessPosition(
    val board:          Board,
    val sideToMove:     PieceColor,
    val castling:       CastlingRights = CastlingRights(),
    /** Square index (0-63) of the en-passant target, or -1. */
    val enPassant:      Int            = -1,
    val halfMoveClock:  Int            = 0,
    val fullMoveNumber: Int            = 1
)

/**
 * A single chess move.
 * [from] and [to] are square indices 0-63.
 * [promotion] is set for pawn promotions.
 */
data class Move(
    val from:      Int,
    val to:        Int,
    val promotion: PieceType? = null
) {
    fun toUci(): String {
        val fromStr = squareName(from)
        val toStr   = squareName(to)
        val promoSuffix = when (promotion) {
            PieceType.QUEEN  -> "q"
            PieceType.ROOK   -> "r"
            PieceType.BISHOP -> "b"
            PieceType.KNIGHT -> "n"
            else             -> ""
        }
        return "$fromStr$toStr$promoSuffix"
    }
}

// ── Square helpers ────────────────────────────────────────────────────────────

fun squareName(idx: Int): String {
    val file = idx % 8
    val rank = idx / 8
    return "${'a' + file}${8 - rank}"
}

fun squareIndex(name: String): Int {
    val file = name[0] - 'a'
    val rank = 8 - (name[1] - '0')
    return rank * 8 + file
}

fun rankOf(sq: Int) = sq / 8
fun fileOf(sq: Int) = sq % 8

fun onBoard(rank: Int, file: Int) = rank in 0..7 && file in 0..7
fun sq(rank: Int, file: Int)      = rank * 8 + file

