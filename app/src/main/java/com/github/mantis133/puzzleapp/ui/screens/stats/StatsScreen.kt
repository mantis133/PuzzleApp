package com.github.mantis133.puzzleapp.ui.screens.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mantis133.puzzleapp.data.DifficultyStats
import com.github.mantis133.puzzleapp.data.ShikakuStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onOpenDrawer: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val shikakuStats by viewModel.shikakuStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
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
                    text       = "Your Records",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text  = "Fastest times and completions per difficulty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { ShikakuStatsCard(stats = shikakuStats) }
        }
    }
}

@Composable
private fun ShikakuStatsCard(stats: ShikakuStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header ──────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⬜", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(16.dp))
                Text(
                    text       = "Shikaku",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Fastest times ────────────────────────────────────────────────
            StatsSectionHeader(icon = "⚡", title = "Fastest Completion")
            Spacer(Modifier.height(8.dp))
            StatsRow("Easy",   formatTime(stats.easy.fastestSeconds))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant)
            StatsRow("Medium", formatTime(stats.medium.fastestSeconds))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant)
            StatsRow("Hard",   formatTime(stats.hard.fastestSeconds))

            Spacer(Modifier.height(20.dp))

            // ── Completed counts ─────────────────────────────────────────────
            StatsSectionHeader(icon = "✓", title = "Games Completed")
            Spacer(Modifier.height(8.dp))
            StatsRow("Easy",   stats.easy.completedCount.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant)
            StatsRow("Medium", stats.medium.completedCount.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant)
            StatsRow("Hard",   stats.hard.completedCount.toString())
        }
    }
}

@Composable
private fun StatsSectionHeader(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Formats seconds as MM:SS, or "—" if null (never completed). */
private fun formatTime(seconds: Long?): String {
    if (seconds == null) return "—"
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}



