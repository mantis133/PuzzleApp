package com.github.mantis133.puzzleapp.ui.screens.solitaire

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolitaireScreen(
    onNavigateBack: () -> Unit,
    viewModel: SolitaireViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val timerText = formatTime(state.elapsedSeconds)

    if (state.isComplete) {
        WinDialog(
            timeText   = timerText,
            onNewGame  = { viewModel.newGame() },
            onNavigateBack = onNavigateBack
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(timerText) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isAutoCompletable && !state.isComplete && !state.isAutoCompleting) {
                        TextButton(onClick = { viewModel.autoComplete() }) {
                            Text("Auto")
                        }
                    }
                    TextButton(
                        onClick  = { viewModel.undo() },
                        enabled  = state.boardHistory.size > 1 && !state.isComplete && !state.isAutoCompleting
                    ) {
                        Text("↩ Undo")
                    }
                    IconButton(onClick = { viewModel.newGame() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "New Game")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        val board = state.board
        if (board == null) {
            Box(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            SolitaireBoardView(
                state    = state,
                onStockTap    = { viewModel.onStockTap() },
                onLocationTap = { viewModel.onLocationTap(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun WinDialog(
    timeText: String,
    onNewGame: () -> Unit,
    onNavigateBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text       = "You Win! 🎉",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("All cards are in the foundations.")
                Text(
                    text  = "Time: $timeText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(onClick = onNewGame) { Text("New Game") }
        },
        dismissButton = {
            OutlinedButton(onClick = onNavigateBack) { Text("Back") }
        }
    )
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
