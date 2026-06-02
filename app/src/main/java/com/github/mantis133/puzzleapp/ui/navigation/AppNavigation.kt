package com.github.mantis133.puzzleapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.core.difficultyFromNavArg
import com.github.mantis133.puzzleapp.puzzle.core.toNavArg
import com.github.mantis133.puzzleapp.ui.screens.home.HomeScreen
import com.github.mantis133.puzzleapp.ui.screens.shikaku.ShikakuScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Shikaku : Screen("shikaku/{difficulty}") {
        fun createRoute(difficulty: Difficulty) = "shikaku/${difficulty.toNavArg()}"
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToShikaku = { difficulty ->
                    navController.navigate(Screen.Shikaku.createRoute(difficulty))
                }
            )
        }

        composable(
            route     = Screen.Shikaku.route,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
        ) { backStackEntry ->
            val arg        = backStackEntry.arguments?.getString("difficulty") ?: "EASY"
            val difficulty = difficultyFromNavArg(arg)
            ShikakuScreen(
                difficulty     = difficulty,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
