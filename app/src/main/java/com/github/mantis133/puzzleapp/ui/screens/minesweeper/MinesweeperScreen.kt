package com.github.mantis133.puzzleapp.ui.screens.minesweeper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mantis133.puzzleapp.puzzle.minesweeper.CellState
import com.github.mantis133.puzzleapp.puzzle.minesweeper.GameState
import com.github.mantis133.puzzleapp.puzzle.minesweeper.MinesweeperDifficulty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinesweeperScreen(
    difficulty:     MinesweeperDifficulty,
    onNavigateBack: () -> Unit,
    viewModel:      MinesweeperViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Start a fresh game whenever difficulty changes or screen first mounts
    LaunchedEffect(difficulty) {
        if (state.difficulty != difficulty || state.cells.isEmpty()) {
            viewModel.newGame(difficulty)
        }
    }

    val rows = difficulty.rows
    val cols = difficulty.cols

    // How many mines haven't been flagged yet
    val minesRemaining = difficulty.mineCount -
            state.cells.count { it.state == CellState.FLAGGED }

    // Smiley that reacts to game state (classic touch)
    val face = when (state.gameState) {
        GameState.WON  -> "😎"
        GameState.LOST -> "😵"
        else           -> if (state.selectedCell >= 0) "😮" else "🙂"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$face Minesweeper") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Mine counter
                    Text(
                        text     = "🚩 $minesRemaining",
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    // Timer
                    Text(
                        text     = formatElapsed(state.elapsedSeconds),
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    // New game
                    IconButton(onClick = { viewModel.newGame(difficulty) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "New game")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Difficulty subtitle ────────────────────────────────────────────
            Text(
                text     = "${difficulty.displayName}  ·  ${rows}×${cols}  ·  ${difficulty.mineCount} mines",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Board + action popup ───────────────────────────────────────────
            if (state.cells.isNotEmpty()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .aspectRatio(cols.toFloat() / rows.toFloat())
                ) {
                    val boardW: Dp = maxWidth
                    val boardH: Dp = maxHeight   // equals boardW for square boards
                    val cellW: Dp  = boardW / cols
                    val cellH: Dp  = boardH / rows

                    // ── The grid ──────────────────────────────────────────────
                    MinesweeperBoardView(
                        cells        = state.cells,
                        rows         = rows,
                        cols         = cols,
                        gameState    = state.gameState,
                        selectedCell = state.selectedCell,
                        onCellTapped = { viewModel.selectCell(it) },
                        modifier     = Modifier.fillMaxSize()
                    )

                    // ── Action popup (Google-style) ───────────────────────────
                    val selIdx = state.selectedCell
                    if (selIdx >= 0 &&
                        state.gameState != GameState.WON &&
                        state.gameState != GameState.LOST
                    ) {
                        val selCell = state.cells[selIdx]
                        val selRow  = selIdx / cols
                        val selCol  = selIdx % cols

                        // Card dimensions
                        val cardW = 160.dp
                        val cardH = 44.dp

                        // Horizontal: centre on cell, clamp inside board
                        val cellCx: Dp = cellW * (selCol + 0.5f)
                        val xOff: Dp   = (cellCx - cardW / 2)
                            .coerceIn(0.dp, (boardW - cardW).coerceAtLeast(0.dp))

                        // Vertical: above cell in bottom half, below in top half
                        val cellCy: Dp  = cellH * (selRow + 0.5f)
                        val showAbove   = selRow >= rows / 2
                        val yOff: Dp    = if (showAbove) {
                            (cellCy - cellH / 2 - cardH - 4.dp).coerceAtLeast(0.dp)
                        } else {
                            (cellCy + cellH / 2 + 4.dp).coerceAtMost(boardH - cardH)
                        }

                        Card(
                            modifier  = Modifier
                                .offset(xOff, yOff)
                                .width(cardW)
                                .height(cardH),
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
                                            onClick  = { viewModel.dig(selIdx) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("⛏ Dig") }

                                        VerticalDivider(modifier = Modifier.height(24.dp))

                                        TextButton(
                                            onClick  = { viewModel.flag(selIdx) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("🚩 Flag") }
                                    }
                                    CellState.FLAGGED -> {
                                        TextButton(
                                            onClick  = { viewModel.flag(selIdx) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("🚩 Unflag") }
                                    }
                                    CellState.REVEALED -> {
                                        // Chord: auto-reveal neighbours when flag count matches
                                        TextButton(
                                            onClick  = { viewModel.dig(selIdx) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("⚡ Auto") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── New Game button ────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.newGame(difficulty) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) { Text("New Game") }

            // ── Win / Lose banner ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = state.gameState == GameState.WON || state.gameState == GameState.LOST,
                enter   = fadeIn() + slideInVertically { it }
            ) {
                val won = state.gameState == GameState.WON
                Surface(
                    color    = if (won) MaterialTheme.colorScheme.primaryContainer
                               else     MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = if (won) "😎 You swept it!" else "💥 Boom! Better luck next time.",
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (won) {
                            Text(
                                text  = "Time: ${formatElapsed(state.elapsedSeconds)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatElapsed(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

