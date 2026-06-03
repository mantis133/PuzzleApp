package com.github.mantis133.puzzleapp.ui.screens.sudoku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.github.mantis133.puzzleapp.puzzle.sudoku.SudokuBoard

// ── Colour palette ────────────────────────────────────────────────────────────

private val BoardBg      = Color(0xFF1E1E2E)
private val CellBg       = Color(0xFF252538)
private val PeerBg       = Color(0xFF2E2E48)   // Same row / col / box as selected
private val SelectedBg   = Color(0xFF3D3D6E)
private val SameDigitBg  = Color(0xFF4A3060)   // Same digit as selected non-zero
private val ErrorBg      = Color(0x55FF4444)
private val ThinLine     = Color(0xFF9090B8)   // purple cell borders
private val ThickLine    = Color(0xFFD0B9FC)   // black 3×3 box borders
private val OuterLine    = Color(0xFFD0B9FC)   // black outer border
private val GivenColor   = android.graphics.Color.rgb(238, 238, 255)
private val PlayerColor  = android.graphics.Color.rgb(120, 180, 255)
private val ErrorColor   = android.graphics.Color.rgb(255, 100, 100)

/**
 * Canvas-based 9×9 Sudoku board renderer.
 *
 * - Tap a cell to select it (highlights peers in same row/col/box).
 * - Cells sharing the same digit as the selected cell get a distinct highlight.
 * - Conflicting cells get a red tint.
 * - Given (clue) digits are drawn white/bold; player entries are drawn blue.
 */
@Composable
fun SudokuBoardView(
    board: SudokuBoard,
    playerGrid: List<Int>,
    errors: Set<Int>,
    selectedCell: Int,
    onCellSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selRow    = if (selectedCell >= 0) selectedCell / 9 else -1
    val selCol    = if (selectedCell >= 0) selectedCell % 9 else -1
    val selDigit  = if (selectedCell >= 0) playerGrid[selectedCell] else 0

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellPx = size.width / 9f
                    val col = (offset.x / cellPx).toInt().coerceIn(0, 8)
                    val row = (offset.y / cellPx).toInt().coerceIn(0, 8)
                    onCellSelected(row * 9 + col)
                }
            }
    ) {
        val cellSize = size.width / 9f      // Board is square; use width for cell size

        // ── Cell backgrounds ─────────────────────────────────────────────────
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val pos     = row * 9 + col
                val digit   = playerGrid[pos]
                val isPeer  = selRow >= 0 && (row == selRow || col == selCol ||
                        (row / 3 == selRow / 3 && col / 3 == selCol / 3))
                val isSame  = selDigit > 0 && digit == selDigit

                val bg = when {
                    pos == selectedCell  -> SelectedBg
                    errors.contains(pos) -> ErrorBg
                    isSame               -> SameDigitBg
                    isPeer               -> PeerBg
                    else                 -> CellBg
                }
                drawRect(
                    color    = bg,
                    topLeft  = Offset(col * cellSize, row * cellSize),
                    size     = Size(cellSize, cellSize)
                )
            }
        }

        // ── Thin cell lines ───────────────────────────────────────────────────
        val thinStroke  = 1.dp.toPx()
        val thickStroke = 3.dp.toPx()
        val outerStroke = 3.5f.dp.toPx()

        for (i in 1 until 9) {
            if (i % 3 == 0) continue         // Box borders drawn separately
            val x = i * cellSize
            drawLine(ThinLine, Offset(x, 0f), Offset(x, size.height), thinStroke)
            drawLine(ThinLine, Offset(0f, x), Offset(size.width, x), thinStroke)
        }

        // ── 3×3 box borders ───────────────────────────────────────────────────
        for (i in 0..9 step 3) {
            val isOuter = (i == 0 || i == 9)
            val stroke  = if (isOuter) outerStroke else thickStroke
            val color   = if (isOuter) OuterLine else ThickLine
            val x       = i * cellSize
            drawLine(color, Offset(x, 0f),          Offset(x, size.height),  stroke)
            drawLine(color, Offset(0f, x),           Offset(size.width, x),   stroke)
        }

        // ── Digits ────────────────────────────────────────────────────────────
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign   = android.graphics.Paint.Align.CENTER
                textSize    = cellSize * 0.58f
            }
            val boldPaint = android.graphics.Paint(paint).apply {
                isFakeBoldText = true
            }

            for (row in 0 until 9) {
                for (col in 0 until 9) {
                    val pos   = row * 9 + col
                    val digit = playerGrid[pos]
                    if (digit == 0) continue

                    val isGiven = board.given[pos]
                    val hasErr  = errors.contains(pos)

                    val p = if (isGiven) boldPaint else paint
                    p.color = when {
                        hasErr  -> ErrorColor
                        isGiven -> GivenColor
                        else    -> PlayerColor
                    }

                    val cx = (col + 0.5f) * cellSize
                    // Vertically centre: shift up by half the total text height
                    val cy = (row + 0.5f) * cellSize - (p.descent() + p.ascent()) / 2f

                    canvas.nativeCanvas.drawText(digit.toString(), cx, cy, p)
                }
            }
        }
    }
}




