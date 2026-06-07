package com.github.mantis133.puzzleapp.ui.screens.wires

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
fun WiresScreen(
    difficulty:     Difficulty,
    onNavigateBack: () -> Unit,
    viewModel:      WiresViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Generate a puzzle when the screen first appears or the difficulty changes.
    LaunchedEffect(difficulty) {
        if (state.board == null || state.difficulty != difficulty) {
            viewModel.generatePuzzle(difficulty)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wires — ${difficulty.displayName}") },
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
            modifier        = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()

                state.board == null -> {
                    Text(
                        text  = "Failed to generate puzzle. Tap ↺ to try again.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> {
                    Column(
                        modifier              = Modifier.fillMaxSize(),
                        horizontalAlignment   = Alignment.CenterHorizontally
                    ) {
                        // ── Board ──────────────────────────────────────────
                        WiresBoardView(
                            board           = state.board!!,
                            paths           = state.paths,
                            cellColors      = state.cellColors,
                            completedColors = state.completedColors,
                            onStartDrag     = { viewModel.startDrag(it) },
                            onContinueDrag  = { viewModel.continueDrag(it) },
                            onEndDrag       = { viewModel.endDrag() },
                            modifier        = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(12.dp)
                        )

                        // ── Controls ───────────────────────────────────────
                        Button(
                            onClick  = { viewModel.generatePuzzle(difficulty) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) { Text("New Game") }

                        // ── Completion banner ──────────────────────────────
                        AnimatedVisibility(
                            visible = state.isComplete,
                            enter   = fadeIn() + slideInVertically { it }
                        ) {
                            Surface(
                                color    = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier              = Modifier.padding(20.dp),
                                    horizontalAlignment   = Alignment.CenterHorizontally,
                                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text       = "🎉 All Wires Connected!",
                                        style      = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
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
    }
}

private fun formatElapsed(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
