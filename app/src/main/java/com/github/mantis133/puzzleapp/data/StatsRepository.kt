package com.github.mantis133.puzzleapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One DataStore instance per application, accessed as a property extension on Context.
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "puzzle_stats")

// ── Keys ─────────────────────────────────────────────────────────────────────

private object Keys {
    // Shikaku — fastest solve times in seconds
    val SHIKAKU_FASTEST_EASY   = longPreferencesKey("shikaku_fastest_easy")
    val SHIKAKU_FASTEST_MEDIUM = longPreferencesKey("shikaku_fastest_medium")
    val SHIKAKU_FASTEST_HARD   = longPreferencesKey("shikaku_fastest_hard")
    // Shikaku — completed game counts
    val SHIKAKU_COMPLETED_EASY   = intPreferencesKey("shikaku_completed_easy")
    val SHIKAKU_COMPLETED_MEDIUM = intPreferencesKey("shikaku_completed_medium")
    val SHIKAKU_COMPLETED_HARD   = intPreferencesKey("shikaku_completed_hard")

    // Sudoku — fastest solve times in seconds
    val SUDOKU_FASTEST_EASY   = longPreferencesKey("sudoku_fastest_easy")
    val SUDOKU_FASTEST_MEDIUM = longPreferencesKey("sudoku_fastest_medium")
    val SUDOKU_FASTEST_HARD   = longPreferencesKey("sudoku_fastest_hard")
    // Sudoku — completed game counts
    val SUDOKU_COMPLETED_EASY   = intPreferencesKey("sudoku_completed_easy")
    val SUDOKU_COMPLETED_MEDIUM = intPreferencesKey("sudoku_completed_medium")
    val SUDOKU_COMPLETED_HARD   = intPreferencesKey("sudoku_completed_hard")
}

// ── Data models ───────────────────────────────────────────────────────────────

data class DifficultyStats(
    /** Fastest solve in seconds, or null if never completed. */
    val fastestSeconds: Long? = null,
    val completedCount: Int   = 0
)

data class ShikakuStats(
    val easy:   DifficultyStats = DifficultyStats(),
    val medium: DifficultyStats = DifficultyStats(),
    val hard:   DifficultyStats = DifficultyStats()
)

data class SudokuStats(
    val easy:   DifficultyStats = DifficultyStats(),
    val medium: DifficultyStats = DifficultyStats(),
    val hard:   DifficultyStats = DifficultyStats()
)

// ── Repository ────────────────────────────────────────────────────────────────

class StatsRepository(private val context: Context) {

    val shikakuStats: Flow<ShikakuStats> = context.appDataStore.data.map { prefs ->
        ShikakuStats(
            easy   = DifficultyStats(
                fastestSeconds = prefs[Keys.SHIKAKU_FASTEST_EASY],
                completedCount = prefs[Keys.SHIKAKU_COMPLETED_EASY] ?: 0
            ),
            medium = DifficultyStats(
                fastestSeconds = prefs[Keys.SHIKAKU_FASTEST_MEDIUM],
                completedCount = prefs[Keys.SHIKAKU_COMPLETED_MEDIUM] ?: 0
            ),
            hard   = DifficultyStats(
                fastestSeconds = prefs[Keys.SHIKAKU_FASTEST_HARD],
                completedCount = prefs[Keys.SHIKAKU_COMPLETED_HARD] ?: 0
            )
        )
    }

    val sudokuStats: Flow<SudokuStats> = context.appDataStore.data.map { prefs ->
        SudokuStats(
            easy   = DifficultyStats(
                fastestSeconds = prefs[Keys.SUDOKU_FASTEST_EASY],
                completedCount = prefs[Keys.SUDOKU_COMPLETED_EASY] ?: 0
            ),
            medium = DifficultyStats(
                fastestSeconds = prefs[Keys.SUDOKU_FASTEST_MEDIUM],
                completedCount = prefs[Keys.SUDOKU_COMPLETED_MEDIUM] ?: 0
            ),
            hard   = DifficultyStats(
                fastestSeconds = prefs[Keys.SUDOKU_FASTEST_HARD],
                completedCount = prefs[Keys.SUDOKU_COMPLETED_HARD] ?: 0
            )
        )
    }

    /**
     * Records a completed Shikaku game. Custom difficulty games are ignored.
     */
    suspend fun recordShikakuCompletion(difficulty: Difficulty, elapsedSeconds: Long) {
        val (fastestKey, completedKey) = when (difficulty) {
            is Difficulty.Easy   -> Keys.SHIKAKU_FASTEST_EASY   to Keys.SHIKAKU_COMPLETED_EASY
            is Difficulty.Medium -> Keys.SHIKAKU_FASTEST_MEDIUM to Keys.SHIKAKU_COMPLETED_MEDIUM
            is Difficulty.Hard   -> Keys.SHIKAKU_FASTEST_HARD   to Keys.SHIKAKU_COMPLETED_HARD
            is Difficulty.Custom -> return
        }
        context.appDataStore.edit { prefs ->
            val current = prefs[fastestKey]
            if (current == null || elapsedSeconds < current) prefs[fastestKey] = elapsedSeconds
            prefs[completedKey] = (prefs[completedKey] ?: 0) + 1
        }
    }

    /**
     * Records a completed Sudoku game. Custom difficulty games are ignored.
     */
    suspend fun recordSudokuCompletion(difficulty: Difficulty, elapsedSeconds: Long) {
        val (fastestKey, completedKey) = when (difficulty) {
            is Difficulty.Easy   -> Keys.SUDOKU_FASTEST_EASY   to Keys.SUDOKU_COMPLETED_EASY
            is Difficulty.Medium -> Keys.SUDOKU_FASTEST_MEDIUM to Keys.SUDOKU_COMPLETED_MEDIUM
            is Difficulty.Hard   -> Keys.SUDOKU_FASTEST_HARD   to Keys.SUDOKU_COMPLETED_HARD
            is Difficulty.Custom -> return
        }
        context.appDataStore.edit { prefs ->
            val current = prefs[fastestKey]
            if (current == null || elapsedSeconds < current) prefs[fastestKey] = elapsedSeconds
            prefs[completedKey] = (prefs[completedKey] ?: 0) + 1
        }
    }
}

