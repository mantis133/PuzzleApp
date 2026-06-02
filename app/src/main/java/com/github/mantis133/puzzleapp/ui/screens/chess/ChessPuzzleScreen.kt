package com.github.mantis133.puzzleapp.ui.screens.chess

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mantis133.puzzleapp.data.chess.ChessDownloadManager
import com.github.mantis133.puzzleapp.puzzle.chess.MoveResult
import com.github.mantis133.puzzleapp.puzzle.chess.PieceColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessPuzzleScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChessViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Auto-load first puzzle once DB is ready
    LaunchedEffect(state.dbInstalled) {
        if (state.dbInstalled && state.puzzleStatus == PuzzleStatus.IDLE) {
            viewModel.loadNextPuzzle()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chess Puzzles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                // ── Not downloaded yet ─────────────────────────────────────
                !state.dbInstalled -> DownloadSection(
                    downloadState = state.downloadState,
                    onDownload    = { viewModel.startDownload() }
                )

                state.puzzleStatus == PuzzleStatus.LOADING -> {
                    Box(Modifier.fillMaxSize().padding(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.puzzleStatus == PuzzleStatus.ERROR -> {
                    Text("Could not load puzzle.", Modifier.padding(32.dp))
                    Button(onClick = { viewModel.loadNextPuzzle() }, Modifier.padding(8.dp)) { Text("Retry") }
                }

                // ── Active / solved puzzle ─────────────────────────────────
                else -> {
                    Spacer(Modifier.height(8.dp))

                    // Rating + themes
                    if (state.puzzleRating > 0) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            SuggestionChip(onClick = {}, label = { Text("Rating: ${state.puzzleRating}") })
                            Spacer(Modifier.width(8.dp))
                            val firstTheme = state.puzzleThemes.split(" ").firstOrNull { it.isNotBlank() } ?: ""
                            if (firstTheme.isNotBlank())
                                SuggestionChip(onClick = {}, label = { Text(firstTheme) })
                        }
                    }

                    // Perspective label
                    Text(
                        text = if (state.playerColor == PieceColor.WHITE) "White to move" else "Black to move",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Board — fills width, kept square
                    val wrongTint = if (state.moveStatus == MoveStatus.WRONG)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface

                    Surface(
                        color    = wrongTint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .aspectRatio(1f)
                    ) {
                        ChessBoardView(
                            board          = state.board,
                            playerColor    = state.playerColor,
                            selectedSq     = state.selectedSq,
                            legalDests     = state.legalDests,
                            lastMoveFrom   = state.lastMoveFrom,
                            lastMoveTo     = state.lastMoveTo,
                            checkSq        = state.checkSq,
                            onSquareTapped = { viewModel.onSquareTapped(it) },
                            modifier       = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Solved banner
                    if (state.puzzleStatus == PuzzleStatus.SOLVED) {
                        Surface(
                            color    = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎉 Puzzle Solved!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { viewModel.loadNextPuzzle() }) { Text("Next Puzzle") }
                            }
                        }
                    } else {
                        Button(
                            onClick  = { viewModel.loadNextPuzzle() },
                            modifier = Modifier.padding(8.dp)
                        ) { Text("Skip") }
                    }

                    // Rating range
                    RatingRangeSection(state.minRating, state.maxRating) { min, max ->
                        viewModel.setRatingRange(min, max)
                    }
                }
            }
        }
    }
}

// ── Download section ──────────────────────────────────────────────────────────

@Composable
private fun DownloadSection(
    downloadState: ChessDownloadManager.DownloadState,
    onDownload: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♟ Chess Puzzles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Millions of free puzzles from the Lichess open database (CC0).\n" +
                "A compressed database (~100 MB) will be downloaded once and stored locally.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            when (downloadState) {
                is ChessDownloadManager.DownloadState.Idle,
                is ChessDownloadManager.DownloadState.Checking -> {
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Text("Download Puzzles")
                    }
                }
                is ChessDownloadManager.DownloadState.Downloading -> {
                    val progress = if (downloadState.total > 0)
                        downloadState.received.toFloat() / downloadState.total else 0f
                    Text("Downloading… ${(downloadState.received / 1_048_576f).let { "%.1f MB".format(it) }}")
                    Spacer(Modifier.height(8.dp))
                    if (downloadState.total > 0) LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
                    else LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is ChessDownloadManager.DownloadState.Decompressing -> {
                    Text("Decompressing…")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is ChessDownloadManager.DownloadState.Done -> {
                    Text("✓ Download complete!", color = MaterialTheme.colorScheme.primary)
                }
                is ChessDownloadManager.DownloadState.Error -> {
                    Text("Error: ${downloadState.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onDownload, Modifier.fillMaxWidth()) { Text("Retry") }
                }
            }
        }
    }
}

// ── Rating range picker ───────────────────────────────────────────────────────

private val RATING_PRESETS = listOf(
    "Beginner"     to (600  to 1000),
    "Intermediate" to (1000 to 1500),
    "Advanced"     to (1500 to 2000),
    "Expert"       to (2000 to 2500),
    "All"          to (600  to 2500)
)

@Composable
private fun RatingRangeSection(currentMin: Int, currentMax: Int, onRange: (Int, Int) -> Unit) {
    Column(Modifier.padding(16.dp).fillMaxWidth()) {
        Text("Difficulty", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            RATING_PRESETS.forEachIndexed { i, (label, range) ->
                val selected = currentMin == range.first && currentMax == range.second
                SegmentedButton(
                    selected = selected,
                    onClick  = { onRange(range.first, range.second) },
                    shape    = SegmentedButtonDefaults.itemShape(i, RATING_PRESETS.size)
                ) { Text(label, maxLines = 1) }
            }
        }
    }
}


