package com.github.mantis133.puzzleapp.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.core.PuzzleTypeInfo
import com.github.mantis133.puzzleapp.puzzle.core.PuzzleTypes

// Sentinel used to track that the user selected the Custom slot in the segmented button.
private val CUSTOM_SENTINEL: Difficulty = Difficulty.Custom(6, 6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToShikaku: (Difficulty) -> Unit,
    onOpenDrawer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Puzzle Collection") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text       = "Choose a Puzzle",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text  = "Tap Play to start a game",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { PuzzleCard(info = PuzzleTypes.SHIKAKU, onPlay = onNavigateToShikaku) }
        }
    }
}

@Composable
private fun PuzzleCard(
    info: PuzzleTypeInfo,
    onPlay: (Difficulty) -> Unit
) {
    // Which segment is highlighted — one of the three presets or the Custom sentinel.
    var selectedSlot by remember { mutableStateOf<Difficulty>(Difficulty.Easy) }
    // Mutable dimensions only used when Custom is selected.
    var customRows by remember { mutableIntStateOf(6) }
    var customCols by remember { mutableIntStateOf(6) }

    val isCustom = selectedSlot is Difficulty.Custom

    // The difficulty that will actually be passed to the game.
    val effectiveDifficulty: Difficulty =
        if (isCustom) Difficulty.Custom(customRows, customCols) else selectedSlot

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header ──────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = info.emoji, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text       = info.displayName,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = info.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Difficulty segmented button ──────────────────────────────────
            Text(
                text  = "Difficulty",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            val slots: List<Difficulty> = Difficulty.presets + CUSTOM_SENTINEL
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                slots.forEachIndexed { index, slot ->
                    SegmentedButton(
                        selected = selectedSlot::class == slot::class,
                        onClick  = { selectedSlot = slot },
                        shape    = SegmentedButtonDefaults.itemShape(index, slots.size)
                    ) {
                        Text(slot.displayName)
                    }
                }
            }

            // ── Custom dimension pickers (only visible when Custom selected) ─
            AnimatedVisibility(visible = isCustom) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        DimensionStepper(
                            label    = "Rows",
                            value    = customRows,
                            min      = 4,
                            max      = 12,
                            onChange = { customRows = it },
                            modifier = Modifier.weight(1f)
                        )
                        DimensionStepper(
                            label    = "Cols",
                            value    = customCols,
                            min      = 4,
                            max      = 12,
                            onChange = { customCols = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Grid size hint ───────────────────────────────────────────────
            Text(
                text  = "${effectiveDifficulty.rows} × ${effectiveDifficulty.cols} grid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick  = { onPlay(effectiveDifficulty) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Play")
            }
        }
    }
}

/** A compact +/− stepper for a single integer dimension. */
@Composable
private fun DimensionStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick  = { if (value > min) onChange(value - 1) },
                enabled  = value > min,
                modifier = Modifier.size(36.dp)
            ) { Text("−") }

            Text(
                text      = value.toString(),
                style     = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier  = Modifier.width(40.dp)
            )

            FilledTonalIconButton(
                onClick  = { if (value < max) onChange(value + 1) },
                enabled  = value < max,
                modifier = Modifier.size(36.dp)
            ) { Text("+") }
        }
    }
}
