package com.github.mantis133.puzzleapp.ui.screens.shikaku

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
fun ShikakuScreen(
    difficulty: Difficulty,
    onNavigateBack: () -> Unit,
    viewModel: ShikakuViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Generate puzzle when screen first appears or difficulty changes.
    LaunchedEffect(difficulty) {
        if (state.board == null || state.difficulty != difficulty) {
            viewModel.generatePuzzle(difficulty)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shikaku — ${difficulty.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = formatElapsed(state.elapsedSeconds),
                        style = MaterialTheme.typography.bodyLarge,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()

                state.board == null -> {
                    Text("Failed to generate puzzle. Tap ↺ to try again.",
                        style = MaterialTheme.typography.bodyLarge)
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ── Board ──────────────────────────────────────────
                        ShikakuBoardView(
                            board              = state.board!!,
                            placedRectangles   = state.placedRectangles,
                            onRectanglePlaced  = { viewModel.tryPlaceRectangle(it) },
                            onCellTapped       = { r, c -> viewModel.removeRectangleAt(r, c) },
                            modifier           = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(12.dp)
                        )

                        // ── Controls ───────────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                        ) {
                            OutlinedButton(
                                onClick  = { viewModel.undoLast() },
                                enabled  = state.placedRectangles.isNotEmpty() && !state.isComplete,
                                modifier = Modifier.weight(1f)
                            ) { Text("Undo") }

                            Button(
                                onClick  = { viewModel.generatePuzzle(difficulty) },
                                modifier = Modifier.weight(1f)
                            ) { Text("New Game") }
                        }

                        // ── Completion banner ──────────────────────────────
                        AnimatedVisibility(
                            visible = state.isComplete,
                            enter   = fadeIn() + slideInVertically { it }
                        ) {
                            Surface(
                                color  = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🎉 Puzzle Solved!",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold)
                                    Text("Time: ${formatElapsed(state.elapsedSeconds)}",
                                        style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
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

