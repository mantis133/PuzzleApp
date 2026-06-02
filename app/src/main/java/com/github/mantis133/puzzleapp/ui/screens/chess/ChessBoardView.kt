package com.github.mantis133.puzzleapp.ui.screens.chess
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.github.mantis133.puzzleapp.puzzle.chess.Piece
import com.github.mantis133.puzzleapp.puzzle.chess.PieceColor
import com.github.mantis133.puzzleapp.puzzle.chess.PieceType

// ── Colour constants ──────────────────────────────────────────────────────────
private val LIGHT_SQ    = Color(0xFFF0D9B5)
private val DARK_SQ     = Color(0xFFB58863)
private val HIGHLIGHT   = Color(0x88F6F669)   // selected piece — yellow
private val LAST_MOVE   = Color(0x88CDD16E)   // last move — muted yellow-green
private val LEGAL_DOT   = Color(0x5520201A)   // legal-move dot — dark semi-transparent
private val CHECK_SQ    = Color(0xAACC0000)   // king-in-check — red

// Unicode chess pieces:  White ♔♕♖♗♘♙  Black ♚♛♜♝♞♟
private fun pieceGlyph(piece: Piece): String = when (piece.color) {
    PieceColor.WHITE -> when (piece.type) {
        PieceType.KING   -> "♔"; PieceType.QUEEN  -> "♕"; PieceType.ROOK  -> "♖"
        PieceType.BISHOP -> "♗"; PieceType.KNIGHT -> "♘"; PieceType.PAWN  -> "♙"
    }
    PieceColor.BLACK -> when (piece.type) {
        PieceType.KING   -> "♚"; PieceType.QUEEN  -> "♛"; PieceType.ROOK  -> "♜"
        PieceType.BISHOP -> "♝"; PieceType.KNIGHT -> "♞"; PieceType.PAWN  -> "♟"
    }
}

/**
 * Renders a Shikaku-style canvas chess board with full interactive tap support.
 *
 * @param board        64-element nullable Piece array (index = rank*8 + file, rank 0 = rank-8)
 * @param playerColor  Which side the player is (board is oriented with this side at bottom)
 * @param selectedSq   Currently selected square (-1 = none)
 * @param legalDests   Legal destination squares for the selected piece
 * @param lastMoveFrom Square the last move originated from (-1 = none)
 * @param lastMoveTo   Square the last move landed on (-1 = none)
 * @param checkSq      Square to highlight in red (-1 = none)
 * @param onSquareTapped Callback when the player taps a square
 */
@Composable
fun ChessBoardView(
    board:          Array<Piece?>,
    playerColor:    PieceColor,
    selectedSq:     Int,
    legalDests:     List<Int>,
    lastMoveFrom:   Int,
    lastMoveTo:     Int,
    checkSq:        Int,
    onSquareTapped: (square: Int) -> Unit,
    modifier:       Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier.pointerInput(playerColor) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                awaitPointerEvent()   // consume the up event
                val sq = offsetToSquare(down.position, size.width.toFloat(), size.height.toFloat(), playerColor)
                if (sq in 0..63) onSquareTapped(sq)
            }
        }
    ) {
        val sqSize = size.width / 8f    // board fills width; caller makes it square

        for (rank in 0..7) {
            for (file in 0..7) {
                // Map display (rank, file) to board index based on orientation
                val boardSq = if (playerColor == PieceColor.WHITE) rank * 8 + file
                              else (7 - rank) * 8 + (7 - file)

                val x = file * sqSize
                val y = rank * sqSize

                // ── Square background ──────────────────────────────────────
                val isLight = (rank + file) % 2 == 0
                drawRect(if (isLight) LIGHT_SQ else DARK_SQ, Offset(x, y), Size(sqSize, sqSize))

                // ── Overlays ───────────────────────────────────────────────
                when {
                    boardSq == checkSq                      -> drawRect(CHECK_SQ,   Offset(x, y), Size(sqSize, sqSize))
                    boardSq == selectedSq                   -> drawRect(HIGHLIGHT,  Offset(x, y), Size(sqSize, sqSize))
                    boardSq == lastMoveFrom || boardSq == lastMoveTo -> drawRect(LAST_MOVE, Offset(x, y), Size(sqSize, sqSize))
                }

                // ── Legal-move dot ────────────────────────────────────────
                if (boardSq in legalDests) {
                    val piece = board[boardSq]
                    if (piece != null) {
                        // Ring around occupied square
                        drawCircle(LEGAL_DOT, sqSize * 0.48f, Offset(x + sqSize/2, y + sqSize/2), style = androidx.compose.ui.graphics.drawscope.Stroke(sqSize * 0.08f))
                    } else {
                        drawCircle(LEGAL_DOT, sqSize * 0.18f, Offset(x + sqSize / 2f, y + sqSize / 2f))
                    }
                }

                // ── Piece glyph ───────────────────────────────────────────
                val piece = board[boardSq]
                if (piece != null) {
                    // Measure at a generous size for glyph quality.
                    val fontSize = (sqSize * 0.85f).sp
                    val measured = textMeasurer.measure(
                        text  = pieceGlyph(piece),
                        style = TextStyle(fontSize = fontSize, fontFamily = FontFamily.Default)
                    )
                    // Scale so the measured bounds occupy 78 % of the cell.
                    // Chess Unicode glyphs visually extend ~20-25 % beyond their
                    // reported metrics, so 78 % measured → ~95 % visual → fits cleanly.
                    val available = sqSize * 0.78f
                    val sf = minOf(
                        available / measured.size.width.toFloat(),
                        available / measured.size.height.toFloat(),
                        1.0f
                    )
                    val cx = x + sqSize / 2f
                    val cy = y + sqSize / 2f
                    val tx = cx - measured.size.width / 2f
                    val ty = cy - measured.size.height / 2f

                    withTransform({ this.scale(sf, sf, Offset(cx, cy)) }) {
                        if (piece.color == PieceColor.WHITE) {
                            // Four-corner dark outline then white fill → crisp on any square.
                            val outline = Color(0xDD000000)
                            for (dx in listOf(-1.5f, 1.5f))
                                for (dy in listOf(-1.5f, 1.5f))
                                    drawText(measured, color = outline, topLeft = Offset(tx + dx, ty + dy))
                            drawText(measured, color = Color.White, topLeft = Offset(tx, ty))
                        } else {
                            // White drop-shadow then near-black fill.
                            drawText(measured, color = Color(0x99FFFFFF), topLeft = Offset(tx + 1.5f, ty + 1.5f))
                            drawText(measured, color = Color(0xFF0D0D0D), topLeft = Offset(tx, ty))
                        }
                    }
                }
            }
        }

        // ── Rank / file labels ────────────────────────────────────────────
        for (i in 0..7) {
            val rankLabel = if (playerColor == PieceColor.WHITE) "${8 - i}" else "${i + 1}"
            val fileLabel = if (playerColor == PieceColor.WHITE) "${'a' + i}"  else "${'h' - i}"
            val labelSize = (sqSize * 0.22f).sp
            val rankM = textMeasurer.measure(rankLabel, TextStyle(fontSize = labelSize, color = if (i % 2 == 0) DARK_SQ else LIGHT_SQ))
            val fileM = textMeasurer.measure(fileLabel, TextStyle(fontSize = labelSize, color = if ((i + 1) % 2 == 0) DARK_SQ else LIGHT_SQ))
            drawText(rankM, topLeft = Offset(2f, i * sqSize + 2f))
            drawText(fileM, topLeft = Offset(i * sqSize + sqSize - fileM.size.width - 2f, size.height - fileM.size.height - 2f))
        }
    }
}

private fun offsetToSquare(offset: Offset, w: Float, h: Float, playerColor: PieceColor): Int {
    val sqSize  = w / 8f
    val fileIdx = (offset.x / sqSize).toInt().coerceIn(0, 7)
    val rankIdx = (offset.y / sqSize).toInt().coerceIn(0, 7)
    return if (playerColor == PieceColor.WHITE) rankIdx * 8 + fileIdx
           else (7 - rankIdx) * 8 + (7 - fileIdx)
}

