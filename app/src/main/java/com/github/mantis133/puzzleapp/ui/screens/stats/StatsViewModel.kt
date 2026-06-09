package com.github.mantis133.puzzleapp.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.mantis133.puzzleapp.data.MinesweeperStats
import com.github.mantis133.puzzleapp.data.ShikakuStats
import com.github.mantis133.puzzleapp.data.SolitaireStats
import com.github.mantis133.puzzleapp.data.StatsRepository
import com.github.mantis133.puzzleapp.data.SudokuStats
import com.github.mantis133.puzzleapp.data.WiresStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = StatsRepository(application)

    val shikakuStats: StateFlow<ShikakuStats> = repo.shikakuStats.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShikakuStats()
    )

    val sudokuStats: StateFlow<SudokuStats> = repo.sudokuStats.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SudokuStats()
    )

    val minesweeperStats: StateFlow<MinesweeperStats> = repo.minesweeperStats.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = MinesweeperStats()
    )

    val wiresStats: StateFlow<WiresStats> = repo.wiresStats.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = WiresStats()
    )

    val solitaireStats: StateFlow<SolitaireStats> = repo.solitaireStats.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SolitaireStats()
    )
}
