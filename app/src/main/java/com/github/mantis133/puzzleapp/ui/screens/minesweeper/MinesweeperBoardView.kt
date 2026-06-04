package com.github.mantis133.puzzleapp.ui.screens.minesweeper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.github.mantis133.puzzleapp.puzzle.minesweeper.CellState
import com.github.mantis133.puzzleapp.puzzle.minesweeper.GameState
import com.github.mantis133.puzzleapp.puzzle.minesweeper.MinesweeperCell

// ── Palette (dark-purple theme to match the rest of the app) ─────────────────

private val BoardBg       = Color(0xFF1E1E2E)
private val HiddenCell    = Color(0xFF2E2E48)
private val SelectedCell  = Color(0xFF3D3D6E)
private val RevealedCell  = Color(0xFF18182C)
private val MineBg        = Color(0xFF5A1020)    // red tint on a struck mine
private val GridThin      = Color(0xFF9090B8)
private val GridOuter     = Color(0xFFD0B9FC)

/** Classic Minesweeper digit colours (index 0 → digit "1"). */
private val NUMBER_COLORS = intArrayOf(
    android.graphics.Color.rgb(64,  128, 255),   // 1 blue
    android.graphics.Color.rgb(0,   160,   0),   // 2 green
    android.graphics.Color.rgb(220,  50,  50),   // 3 red
    android.graphics.Color.rgb(0,     0, 200),   // 4 dark blue
    android.graphics.Color.rgb(160,   0,   0),   // 5 dark red
    android.graphics.Color.rgb(0,   160, 160),   // 6 teal
    android.graphics.Color.rgb(130,   0, 200),   // 7 purple
    android.graphics.Color.rgb(160, 160, 160)    // 8 grey
)

/**
 * Canvas-based Minesweeper grid.
 *
 * Every cell tap is forwarded to [onCellTapped]; the caller decides whether
 * to open the action popup or dismiss it.
 */
@Composable
fun MinesweeperBoardView(
    cells:        List<MinesweeperCell>,
    rows:         Int,
    cols:         Int,
    gameState:    GameState,
    selectedCell: Int,
    onCellTapped: (Int) -> Unit,
    modifier:     Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(cells, gameState) {
                detectTapGestures { offset ->
                    val cw = size.width  / cols.toFloat()
                    val ch = size.height / rows.toFloat()
                    val col = (offset.x / cw).toInt().coerceIn(0, cols - 1)
                    val row = (offset.y / ch).toInt().coerceIn(0, rows - 1)
                    onCellTapped(row * cols + col)
                }
            }
    ) {
        val cw = size.width  / cols.toFloat()
        val ch = size.height / rows.toFloat()

        // ── Board background ──────────────────────────────────────────────────
        drawRect(color = BoardBg, size = size)

        // ── Cell fills ────────────────────────────────────────────────────────
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val pos  = row * cols + col
                val cell = cells.getOrNull(pos) ?: continue
                val bg = when {
                    cell.state == CellState.REVEALED && cell.isMine -> MineBg
                    cell.state == CellState.REVEALED                 -> RevealedCell
                    pos == selectedCell                              -> SelectedCell
                    else                                             -> HiddenCell
                }
                drawRect(
                    color   = bg,
                    topLeft = Offset(col * cw + 1f, row * ch + 1f),
                    size    = Size(cw - 2f, ch - 2f)
                )
            }
        }

        // ── Grid lines ────────────────────────────────────────────────────────
        for (i in 0..cols) {
            val x = i * cw
            drawLine(GridThin, Offset(x, 0f), Offset(x, size.height), 1f)
        }
        for (i in 0..rows) {
            val y = i * ch
            drawLine(GridThin, Offset(0f, y), Offset(size.width, y), 1f)
        }

        // ── Outer border ──────────────────────────────────────────────────────
        drawRect(color = GridOuter, size = size, style = Stroke(width = 2f))

        // ── Cell content (emoji / digits) ─────────────────────────────────────
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias   = true
                textAlign     = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val pos  = row * cols + col
                    val cell = cells.getOrNull(pos) ?: continue
                    val cx = (col + 0.5f) * cw
                    val cy = (row + 0.5f) * ch

                    when {
                        // Flag
                        cell.state == CellState.FLAGGED -> {
                            paint.textSize = ch * 0.58f
                            paint.color    = android.graphics.Color.rgb(255, 80, 80)
                            val ty = cy - (paint.descent() + paint.ascent()) / 2f
                            canvas.nativeCanvas.drawText("⚑", cx, ty, paint)
                        }
                        // Revealed mine (lost)
                        cell.state == CellState.REVEALED && cell.isMine -> {
                            paint.textSize = ch * 0.60f
                            paint.color    = android.graphics.Color.WHITE
                            val ty = cy - (paint.descent() + paint.ascent()) / 2f
                            canvas.nativeCanvas.drawText("✸", cx, ty, paint)
                        }
                        // Revealed number
                        cell.state == CellState.REVEALED && cell.adjacentMines > 0 -> {
                            paint.textSize = ch * 0.62f
                            paint.color    = NUMBER_COLORS.getOrElse(cell.adjacentMines - 1) {
                                android.graphics.Color.WHITE
                            }
                            val ty = cy - (paint.descent() + paint.ascent()) / 2f
                            canvas.nativeCanvas.drawText(
                                cell.adjacentMines.toString(), cx, ty, paint
                            )
                        }
                        // Revealed blank — nothing drawn
                    }
                }
            }
        }
    }
}

