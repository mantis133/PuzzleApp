package com.github.mantis133.puzzleapp.ui.screens.wires

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.github.mantis133.puzzleapp.puzzle.wires.WireColor
import com.github.mantis133.puzzleapp.puzzle.wires.WiresBoard

/**
 * Canvas-based rendering of a Wires (Flow Free) board.
 *
 * Interaction model:
 *  - Press on a terminal or an existing wire cell → begin drawing that colour.
 *  - Drag through adjacent cells → extend the path in real time.
 *  - Drag back over a cell already in the current path → erase from that point onwards.
 *  - Lift finger → finalize; ViewModel checks for completion.
 */
@Composable
fun WiresBoardView(
    board:           WiresBoard,
    paths:           Map<WireColor, List<Int>>,
    cellColors:      List<WireColor?>,
    completedColors: Set<WireColor>,
    onStartDrag:     (cellIndex: Int) -> Unit,
    onContinueDrag:  (cellIndex: Int) -> Unit,
    onEndDrag:       () -> Unit,
    modifier:        Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(board) {
                // Pre-compute cell layout helpers inside the gesture scope so they update
                // correctly if the composable is remeasured.
                fun cellLayout(): Triple<Float, Float, Float> {
                    val cellW    = size.width.toFloat()  / board.cols
                    val cellH    = size.height.toFloat() / board.rows
                    val cellSize = minOf(cellW, cellH)
                    val offsetX  = (size.width  - cellSize * board.cols) / 2f
                    val offsetY  = (size.height - cellSize * board.rows) / 2f
                    return Triple(cellSize, offsetX, offsetY)
                }

                fun pixelToCell(offset: Offset): Int {
                    val (cellSize, offsetX, offsetY) = cellLayout()
                    val c = ((offset.x - offsetX) / cellSize).toInt().coerceIn(0, board.cols - 1)
                    val r = ((offset.y - offsetY) / cellSize).toInt().coerceIn(0, board.rows - 1)
                    return r * board.cols + c
                }

                awaitEachGesture {
                    val down     = awaitFirstDown(requireUnconsumed = false)
                    var lastCell = pixelToCell(down.position)
                    onStartDrag(lastCell)

                    while (true) {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (!change.pressed) {
                            onEndDrag()
                            break
                        }

                        val cell = pixelToCell(change.position)
                        if (cell != lastCell) {
                            lastCell = cell
                            onContinueDrag(cell)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val cellW    = size.width  / board.cols
        val cellH    = size.height / board.rows
        val cellSize = minOf(cellW, cellH)
        val offsetX  = (size.width  - cellSize * board.cols) / 2f
        val offsetY  = (size.height - cellSize * board.rows) / 2f

        fun cellCenter(idx: Int): Offset {
            val r = idx / board.cols; val c = idx % board.cols
            return Offset(offsetX + c * cellSize + cellSize / 2f,
                          offsetY + r * cellSize + cellSize / 2f)
        }

        // ── Background ──────────────────────────────────────────────────────
        drawRect(
            color   = Color(0xFF1E1E2E),
            topLeft = Offset(offsetX, offsetY),
            size    = Size(cellSize * board.cols, cellSize * board.rows)
        )

        // ── Grid lines (drawn before paths so wires sit on top) ─────────────
        val gridColor = Color(0xFF3A3A4A)
        val edgeColor = Color(0xFF6A6A7A)
        for (r in 0..board.rows) {
            val y      = offsetY + r * cellSize
            val isEdge = r == 0 || r == board.rows
            drawLine(
                color       = if (isEdge) edgeColor else gridColor,
                start       = Offset(offsetX, y),
                end         = Offset(offsetX + board.cols * cellSize, y),
                strokeWidth = if (isEdge) 2f else 1f
            )
        }
        for (c in 0..board.cols) {
            val x      = offsetX + c * cellSize
            val isEdge = c == 0 || c == board.cols
            drawLine(
                color       = if (isEdge) edgeColor else gridColor,
                start       = Offset(x, offsetY),
                end         = Offset(x, offsetY + board.rows * cellSize),
                strokeWidth = if (isEdge) 2f else 1f
            )
        }

        // ── Wire paths ───────────────────────────────────────────────────────
        val wireStroke = cellSize * 0.45f
        for ((color, path) in paths) {
            if (path.isEmpty()) continue
            val wireColor = color.toComposeColor()

            // Lines connecting consecutive path cells
            for (i in 0 until path.size - 1) {
                drawLine(
                    color       = wireColor,
                    start       = cellCenter(path[i]),
                    end         = cellCenter(path[i + 1]),
                    strokeWidth = wireStroke,
                    cap         = StrokeCap.Round
                )
            }

            // A single-cell path (drag started but not yet moved): draw a dot
            if (path.size == 1) {
                drawCircle(
                    color  = wireColor,
                    radius = wireStroke / 2f,
                    center = cellCenter(path[0])
                )
            }
        }

        // ── Terminals (drawn on top so they're always visible) ───────────────
        val outerRadius = cellSize * 0.36f
        val innerRadius = cellSize * 0.16f
        for (terminal in board.terminals) {
            val idx    = terminal.row * board.cols + terminal.col
            val center = cellCenter(idx)
            val color  = terminal.color.toComposeColor()

            // Filled circle
            drawCircle(color = color, radius = outerRadius, center = center)
            // Dark ring to visually distinguish terminals from plain wire segments
            drawCircle(
                color  = Color(0xFF1E1E2E),
                radius = innerRadius,
                center = center
            )
            // Thin coloured outline for extra crispness
            drawCircle(
                color  = color,
                radius = outerRadius,
                center = center,
                style  = Stroke(width = 2f)
            )
        }
    }
}

// ── Colour mapping ───────────────────────────────────────────────────────────

internal fun WireColor.toComposeColor(): Color = when (this) {
    WireColor.RED    -> Color(0xFFE53935)
    WireColor.ORANGE -> Color(0xFFFF6F00)
    WireColor.YELLOW -> Color(0xFFFFD600)
    WireColor.GREEN  -> Color(0xFF43A047)
    WireColor.BLUE   -> Color(0xFF1E88E5)
    WireColor.MAROON -> Color(0xFF880E4F)
    WireColor.PINK   -> Color(0xFFE91E63)
    WireColor.PURPLE -> Color(0xFF7B1FA2)
    WireColor.CYAN   -> Color(0xFF00ACC1)
    WireColor.BROWN  -> Color(0xFF6D4C41)
}
