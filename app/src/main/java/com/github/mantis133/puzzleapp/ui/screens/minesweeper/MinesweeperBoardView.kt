package com.github.mantis133.puzzleapp.ui.screens.minesweeper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.mantis133.puzzleapp.puzzle.minesweeper.CellState
import com.github.mantis133.puzzleapp.puzzle.minesweeper.GameState
import com.github.mantis133.puzzleapp.puzzle.minesweeper.MinesweeperCell

// ── Palette ───────────────────────────────────────────────────────────────────

private val BoardBg      = Color(0xFF1E1E2E)
private val HiddenCell   = Color(0xFF2E2E48)
private val SelectedCell = Color(0xFF3D3D6E)
private val RevealedCell = Color(0xFF18182C)
private val MineBg       = Color(0xFF5A1020)
private val GridThin     = Color(0xFF9090B8)
private val GridOuter    = Color(0xFFD0B9FC)

private val NUMBER_COLORS = intArrayOf(
    android.graphics.Color.rgb(64,  128, 255),
    android.graphics.Color.rgb(0,   160,   0),
    android.graphics.Color.rgb(220,  50,  50),
    android.graphics.Color.rgb(0,     0, 200),
    android.graphics.Color.rgb(160,   0,   0),
    android.graphics.Color.rgb(0,   160, 160),
    android.graphics.Color.rgb(130,   0, 200),
    android.graphics.Color.rgb(160, 160, 160)
)

/**
 * Minesweeper board with built-in pinch-to-zoom and single-finger pan.
 *
 * - Pinch two fingers → zoom (1× – 6×)
 * - Drag one finger   → pan
 * - Single tap        → open action popup for that cell
 * - Double-tap        → reset zoom / pan back to 1×
 *
 * The action popup is rendered inside this composable so it can track the
 * visual (post-transform) cell position correctly.
 */
@Composable
fun MinesweeperBoardView(
    cells:        List<MinesweeperCell>,
    rows:         Int,
    cols:         Int,
    gameState:    GameState,
    selectedCell: Int,
    onCellTapped: (Int) -> Unit,
    onDig:        (Int) -> Unit,
    onFlag:       (Int) -> Unit,
    modifier:     Modifier = Modifier
) {
    var scale     by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Reset to 1× when a new game starts
    LaunchedEffect(gameState) {
        if (gameState == GameState.IDLE) {
            scale     = 1f
            panOffset = Offset.Zero
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val density  = LocalDensity.current
        val viewWPx  = with(density) { maxWidth.toPx() }
        val viewHPx  = with(density) { maxHeight.toPx() }

        // ── Board canvas ──────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Pinch-to-zoom + pan (also handles single-finger pan)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 6f)
                        // Keep the point under the centroid fixed while zooming
                        val newPan = centroid - (centroid - panOffset) * (newScale / scale) + pan
                        scale     = newScale
                        panOffset = clampPan(newPan, newScale, viewWPx, viewHPx)
                    }
                }
                // Tap to select cell; long-press to reset zoom (no double-tap so onTap fires instantly)
                .pointerInput(cells, gameState) {
                    detectTapGestures(
                        onLongPress = {
                            scale     = 1f
                            panOffset = Offset.Zero
                        },
                        onTap = { tap ->
                            val boardX = (tap.x - panOffset.x) / scale
                            val boardY = (tap.y - panOffset.y) / scale
                            val cw = viewWPx / cols
                            val ch = viewHPx / rows
                            val col = (boardX / cw).toInt().coerceIn(0, cols - 1)
                            val row = (boardY / ch).toInt().coerceIn(0, rows - 1)
                            onCellTapped(row * cols + col)
                        }
                    )
                }
        ) {
            // Capture local state before entering the transform block to avoid
            // the `scale` variable shadowing the DrawTransform.scale() function.
            val s   = scale
            val pan = panOffset

            withTransform({
                translate(pan.x, pan.y)
                scale(s, s, Offset(0f, 0f))   // scale around top-left (0, 0)
            }) {
                val cw = size.width  / cols.toFloat()
                val ch = size.height / rows.toFloat()

                drawRect(color = BoardBg, size = size)

                // Cell fills
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val pos  = r * cols + c
                        val cell = cells.getOrNull(pos) ?: continue
                        val bg = when {
                            cell.state == CellState.REVEALED && cell.isMine -> MineBg
                            cell.state == CellState.REVEALED                 -> RevealedCell
                            pos == selectedCell                              -> SelectedCell
                            else                                             -> HiddenCell
                        }
                        drawRect(
                            color   = bg,
                            topLeft = Offset(c * cw + 1f, r * ch + 1f),
                            size    = Size(cw - 2f, ch - 2f)
                        )
                    }
                }

                // Grid
                for (i in 0..cols) drawLine(GridThin, Offset(i * cw, 0f), Offset(i * cw, size.height), 1f)
                for (i in 0..rows) drawLine(GridThin, Offset(0f, i * ch), Offset(size.width, i * ch), 1f)
                drawRect(color = GridOuter, size = size, style = Stroke(2f))

                // Text content (flags, mines, numbers)
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias    = true
                        textAlign      = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val pos  = r * cols + c
                            val cell = cells.getOrNull(pos) ?: continue
                            val cx = (c + 0.5f) * cw
                            val cy = (r + 0.5f) * ch
                            when {
                                cell.state == CellState.FLAGGED -> {
                                    paint.textSize = ch * 0.58f
                                    paint.color    = android.graphics.Color.rgb(255, 80, 80)
                                    canvas.nativeCanvas.drawText("⚑", cx,
                                        cy - (paint.descent() + paint.ascent()) / 2f, paint)
                                }
                                cell.state == CellState.REVEALED && cell.isMine -> {
                                    paint.textSize = ch * 0.60f
                                    paint.color    = android.graphics.Color.WHITE
                                    canvas.nativeCanvas.drawText("✸", cx,
                                        cy - (paint.descent() + paint.ascent()) / 2f, paint)
                                }
                                cell.state == CellState.REVEALED && cell.adjacentMines > 0 -> {
                                    paint.textSize = ch * 0.62f
                                    paint.color    = NUMBER_COLORS.getOrElse(cell.adjacentMines - 1) {
                                        android.graphics.Color.WHITE
                                    }
                                    canvas.nativeCanvas.drawText(cell.adjacentMines.toString(), cx,
                                        cy - (paint.descent() + paint.ascent()) / 2f, paint)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Action popup ──────────────────────────────────────────────────────
        val selIdx  = selectedCell
        val selCell = cells.getOrNull(selIdx)
        if (selIdx >= 0 && selCell != null &&
            gameState != GameState.WON && gameState != GameState.LOST
        ) {
            val selRow = selIdx / cols
            val selCol = selIdx % cols

            val cellWPx = viewWPx / cols
            val cellHPx = viewHPx / rows

            // Visual centre of the selected cell in the viewport
            val visualXPx = (selCol + 0.5f) * cellWPx * scale + panOffset.x
            val visualYPx = (selRow + 0.5f) * cellHPx * scale + panOffset.y

            // Only draw popup when cell centre is within the visible area
            if (visualXPx in -20f..viewWPx + 20f && visualYPx in -20f..viewHPx + 20f) {
                val visualX     = with(density) { visualXPx.toDp() }
                val visualY     = with(density) { visualYPx.toDp() }
                val scaledCellH = with(density) { (cellHPx * scale).toDp() }

                val cardW = 160.dp
                val cardH = 44.dp

                val xOff = (visualX - cardW / 2)
                    .coerceIn(0.dp, (maxWidth - cardW).coerceAtLeast(0.dp))
                val yOff = if (visualY > maxHeight / 2) {
                    (visualY - scaledCellH / 2 - cardH - 4.dp).coerceAtLeast(0.dp)
                } else {
                    (visualY + scaledCellH / 2 + 4.dp).coerceAtMost(maxHeight - cardH)
                }

                Card(
                    modifier  = Modifier.offset(xOff, yOff).width(cardW).height(cardH),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier              = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        when (selCell.state) {
                            CellState.HIDDEN -> {
                                TextButton(
                                    onClick  = { onDig(selIdx) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("⛏ Dig") }
                                VerticalDivider(modifier = Modifier.height(24.dp))
                                TextButton(
                                    onClick  = { onFlag(selIdx) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("🚩 Flag") }
                            }
                            CellState.FLAGGED -> {
                                TextButton(
                                    onClick  = { onFlag(selIdx) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("🚩 Unflag") }
                            }
                            CellState.REVEALED -> {
                                TextButton(
                                    onClick  = { onDig(selIdx) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("⚡ Auto") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Clamps [pan] so the board (which exactly covers viewW × viewH at scale 1)
 * never scrolls fully out of sight.
 *
 * - maxX = 0  (left edge of board can't go right of viewport left edge)
 * - minX = -(scaledBoard - view)  (right edge of board can't go left of right edge)
 */
private fun clampPan(pan: Offset, scale: Float, viewW: Float, viewH: Float): Offset {
    val minX = (-(viewW * scale - viewW)).coerceAtMost(0f)
    val minY = (-(viewH * scale - viewH)).coerceAtMost(0f)
    return Offset(pan.x.coerceIn(minX, 0f), pan.y.coerceIn(minY, 0f))
}
