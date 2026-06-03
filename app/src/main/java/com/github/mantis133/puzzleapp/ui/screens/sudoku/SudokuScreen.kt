package com.github.mantis133.puzzleapp.ui.screens.sudoku

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mantis133.puzzleapp.puzzle.core.Difficulty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(
    difficulty: Difficulty,
    onNavigateBack: () -> Unit,
    viewModel: SudokuViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Generate on first entry or difficulty change
    LaunchedEffect(difficulty) {
        if (state.board == null || state.difficulty != difficulty) {
            viewModel.generatePuzzle(difficulty)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sudoku — ${difficulty.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text     = formatElapsed(state.elapsedSeconds),
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { viewModel.generatePuzzle(difficulty) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "New game")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier          = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment  = Alignment.TopCenter
        ) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.board == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Failed to generate puzzle. Tap ↺ to try again.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ── Board ─────────────────────────────────────────────
                        SudokuBoardView(
                            board          = state.board!!,
                            playerGrid     = state.playerGrid,
                            errors         = state.errors,
                            selectedCell   = state.selectedCell,
                            onCellSelected = { viewModel.selectCell(it) },
                            modifier       = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(8.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        // ── Number pad ────────────────────────────────────────
                        NumberPad(
                            enabled   = !state.isComplete,
                            onDigit   = { viewModel.enterDigit(it) },
                            onErase   = { viewModel.eraseCell() },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        // ── Undo / New Game ───────────────────────────────────
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick  = { viewModel.undoLast() },
                                enabled  = state.moveHistory.isNotEmpty() && !state.isComplete,
                                modifier = Modifier.weight(1f)
                            ) { Text("Undo") }

                            Button(
                                onClick  = { viewModel.generatePuzzle(difficulty) },
                                modifier = Modifier.weight(1f)
                            ) { Text("New Game") }
                        }

                        // ── Completion banner ─────────────────────────────────
                        AnimatedVisibility(
                            visible = state.isComplete,
                            enter   = fadeIn() + slideInVertically { it }
                        ) {
                            Surface(
                                color    = MaterialTheme.colorScheme.primaryContainer,
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
                                        "🎉 Puzzle Solved!",
                                        style      = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Time: ${formatElapsed(state.elapsedSeconds)}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Number pad ────────────────────────────────────────────────────────────────

@Composable
private fun NumberPad(
    enabled:  Boolean,
    onDigit:  (Int) -> Unit,
    onErase:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Row 1: digits 1–5
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (d in 1..5) {
                DigitButton(digit = d, enabled = enabled, onClick = { onDigit(d) },
                    modifier = Modifier.weight(1f))
            }
        }
        // Row 2: digits 6–9 + erase
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (d in 6..9) {
                DigitButton(digit = d, enabled = enabled, onClick = { onDigit(d) },
                    modifier = Modifier.weight(1f))
            }
            FilledTonalButton(
                onClick  = onErase,
                enabled  = enabled,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text("⌫", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun DigitButton(
    digit:    Int,
    enabled:  Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(52.dp)
    ) {
        Text(
            text      = digit.toString(),
            style     = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatElapsed(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

