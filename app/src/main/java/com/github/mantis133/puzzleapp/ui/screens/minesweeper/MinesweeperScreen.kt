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
import androidx.compose.ui.text.style.TextAlign
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

    LaunchedEffect(difficulty) {
        if (state.difficulty != difficulty || state.cells.isEmpty()) {
            viewModel.newGame(difficulty)
        }
    }

    val rows = difficulty.rows
    val cols = difficulty.cols

    val minesRemaining = difficulty.mineCount -
            state.cells.count { it.state == CellState.FLAGGED }

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
                    Text(
                        text     = "🚩 $minesRemaining",
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text     = formatElapsed(state.elapsedSeconds),
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 4.dp)
                    )
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

            // ── Board (zoom/pan + popup built-in) ─────────────────────────────
            if (state.cells.isNotEmpty()) {
                MinesweeperBoardView(
                    cells        = state.cells,
                    rows         = rows,
                    cols         = cols,
                    gameState    = state.gameState,
                    selectedCell = state.selectedCell,
                    onCellTapped = { viewModel.selectCell(it) },
                    onDig        = { viewModel.dig(it) },
                    onFlag       = { viewModel.flag(it) },
                    modifier     = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .aspectRatio(cols.toFloat() / rows.toFloat())
                )
            }

            // ── Zoom hint ──────────────────────────────────────────────────────
            Text(
                text      = "Pinch to zoom · Long-press to reset view",
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )

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
                        if (won) Text(
                            "Time: ${formatElapsed(state.elapsedSeconds)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

private fun formatElapsed(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

