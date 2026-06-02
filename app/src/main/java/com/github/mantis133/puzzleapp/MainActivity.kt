package com.github.mantis133.puzzleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.mantis133.puzzleapp.ui.navigation.AppNavigation
import com.github.mantis133.puzzleapp.ui.theme.PuzzleAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PuzzleAppTheme {
                AppNavigation()
            }
        }
    }
}
