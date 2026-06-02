package com.github.mantis133.puzzleapp.ui.screens.shikaku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.github.mantis133.puzzleapp.puzzle.shikaku.ShikakuBoard
import com.github.mantis133.puzzleapp.puzzle.shikaku.ShikakuRectangle

// 12-colour palette — enough to visually separate adjacent rectangles.
private val PALETTE = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0),
    Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFF795548), Color(0xFF8BC34A),
    Color(0xFF3F51B5), Color(0xFFFF5722), Color(0xFF009688), Color(0xFFFFC107)
)

/**
 * Renders a Shikaku grid on a [Canvas] and handles drag-to-draw input.
 *
 * Interaction model:
 *  - Drag across cells → preview rectangle drawn in blue.
 *  - Lift finger / release mouse → rectangle placed if valid.
 *  - Tap a single cell (no drag) → remove whatever rectangle covers that cell.
 */
@Composable
fun ShikakuBoardView(
    board: ShikakuBoard,
    placedRectangles: List<PlacedRectangle>,
    onRectanglePlaced: (ShikakuRectangle) -> Unit,
    onCellTapped: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Local drag state — kept in the composable, not the ViewModel.
    var dragStartCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragCurrentCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Canvas(
        modifier = modifier
            .pointerInput(board) {
                awaitEachGesture {
                    // ── Wait for initial touch ──────────────────────────────
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startCell = pixelToCell(
                        down.position, size.width.toFloat(), size.height.toFloat(), board
                    )
                    dragStartCell   = startCell
                    dragCurrentCell = startCell

                    // ── Track pointer until release ─────────────────────────
                    while (true) {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        val currentCell = pixelToCell(
                            change.position, size.width.toFloat(), size.height.toFloat(), board
                        )

                        if (!change.pressed) {
                            // Finger lifted — decide tap vs drag
                            val end = dragCurrentCell
                            if (startCell != null && end != null) {
                                if (startCell == end) {
                                    // Same cell → tap → remove rectangle covering it
                                    onCellTapped(startCell.first, startCell.second)
                                } else {
                                    // Different cell → place rectangle
                                    onRectanglePlaced(
                                        ShikakuRectangle(
                                            top    = minOf(startCell.first,  end.first),
                                            left   = minOf(startCell.second, end.second),
                                            bottom = maxOf(startCell.first,  end.first),
                                            right  = maxOf(startCell.second, end.second)
                                        )
                                    )
                                }
                            }
                            dragStartCell   = null
                            dragCurrentCell = null
                            break
                        }

                        // Still dragging — update preview
                        if (currentCell != dragCurrentCell) {
                            dragCurrentCell = currentCell
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val cellW = size.width  / board.cols
        val cellH = size.height / board.rows
        val cellSize = minOf(cellW, cellH)
        val offsetX  = (size.width  - cellSize * board.cols) / 2f
        val offsetY  = (size.height - cellSize * board.rows) / 2f

        // ── Background ──────────────────────────────────────────────────────
        drawRect(
            color    = Color(0xFF1E1E2E),
            topLeft  = Offset(offsetX, offsetY),
            size     = Size(cellSize * board.cols, cellSize * board.rows)
        )

        // ── Placed rectangles ───────────────────────────────────────────────
        for (pr in placedRectangles) {
            val color = PALETTE[pr.colorIndex % PALETTE.size]
            drawGridRect(pr.rect, cellSize, offsetX, offsetY, color.copy(alpha = 0.28f), color, 4f)
        }

        // ── Drag preview ────────────────────────────────────────────────────
        val ds = dragStartCell; val dc = dragCurrentCell
        if (ds != null && dc != null && ds != dc) {
            val preview = ShikakuRectangle(
                top    = minOf(ds.first,  dc.first),
                left   = minOf(ds.second, dc.second),
                bottom = maxOf(ds.first,  dc.first),
                right  = maxOf(ds.second, dc.second)
            )
            drawGridRect(preview, cellSize, offsetX, offsetY,
                Color(0x331565C0), Color(0xFF1565C0), 2.5f)
        }

        // ── Grid lines ──────────────────────────────────────────────────────
        val inner = Color(0xFF3A3A4A)
        val outer = Color(0xFFCCCCDD)
        for (r in 0..board.rows) {
            val y = offsetY + r * cellSize
            val isEdge = r == 0 || r == board.rows
            drawLine(if (isEdge) outer else inner,
                Offset(offsetX, y), Offset(offsetX + board.cols * cellSize, y),
                strokeWidth = if (isEdge) 3f else 1f)
        }
        for (c in 0..board.cols) {
            val x = offsetX + c * cellSize
            val isEdge = c == 0 || c == board.cols
            drawLine(if (isEdge) outer else inner,
                Offset(x, offsetY), Offset(x, offsetY + board.rows * cellSize),
                strokeWidth = if (isEdge) 3f else 1f)
        }

        // ── Clue numbers ────────────────────────────────────────────────────
        val fontSize = (cellSize * 0.42f).coerceIn(10f, 32f).sp
        for (clue in board.clues) {
            val cx = offsetX + clue.col * cellSize + cellSize / 2f
            val cy = offsetY + clue.row * cellSize + cellSize / 2f
            val measured = textMeasurer.measure(
                text  = clue.value.toString(),
                style = TextStyle(
                    fontSize   = fontSize,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFEEEEFF)
                )
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    cx - measured.size.width  / 2f,
                    cy - measured.size.height / 2f
                )
            )
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawGridRect(
    rect: ShikakuRectangle,
    cellSize: Float,
    offsetX: Float,
    offsetY: Float,
    fill: Color,
    stroke: Color,
    strokeWidth: Float
) {
    val l = offsetX + rect.left   * cellSize
    val t = offsetY + rect.top    * cellSize
    val w = rect.width  * cellSize
    val h = rect.height * cellSize
    drawRect(fill,   topLeft = Offset(l, t), size = Size(w, h))
    drawRect(stroke, topLeft = Offset(l, t), size = Size(w, h), style = Stroke(strokeWidth))
}

/** Converts a screen [pixel] offset to (row, col), clamped to board bounds. */
private fun pixelToCell(
    pixel: Offset,
    canvasW: Float,
    canvasH: Float,
    board: ShikakuBoard
): Pair<Int, Int> {
    val cellSize = minOf(canvasW / board.cols, canvasH / board.rows)
    val offsetX  = (canvasW - cellSize * board.cols) / 2f
    val offsetY  = (canvasH - cellSize * board.rows) / 2f
    val col = ((pixel.x - offsetX) / cellSize).toInt().coerceIn(0, board.cols - 1)
    val row = ((pixel.y - offsetY) / cellSize).toInt().coerceIn(0, board.rows - 1)
    return row to col
}






