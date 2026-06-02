package com.github.mantis133.puzzleapp.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.mantis133.puzzleapp.puzzle.core.Difficulty
import com.github.mantis133.puzzleapp.puzzle.core.difficultyFromNavArg
import com.github.mantis133.puzzleapp.puzzle.core.toNavArg
import com.github.mantis133.puzzleapp.ui.screens.home.HomeScreen
import com.github.mantis133.puzzleapp.ui.screens.shikaku.ShikakuScreen
import com.github.mantis133.puzzleapp.ui.screens.stats.StatsScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Home    : Screen("home")
    data object Stats   : Screen("stats")
    data object Shikaku : Screen("shikaku/{difficulty}") {
        fun createRoute(difficulty: Difficulty) = "shikaku/${difficulty.toNavArg()}"
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    // Disable swipe-to-open on the game screen so it doesn't fight the drag-draw gesture.
    val isGameScreen = currentRoute?.startsWith("shikaku/") == true

    ModalNavigationDrawer(
        drawerState    = drawerState,
        gesturesEnabled = !isGameScreen,
        drawerContent  = {
            ModalDrawerSheet {
                // ── Header ────────────────────────────────────────────────
                Spacer(Modifier.height(24.dp))
                Text(
                    text       = "Puzzle Collection",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    text     = "Navigate",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // ── Puzzles ───────────────────────────────────────────────
                NavigationDrawerItem(
                    label    = { Text("Puzzles") },
                    selected = currentRoute == Screen.Home.route,
                    onClick  = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // ── Stats ─────────────────────────────────────────────────
                NavigationDrawerItem(
                    label    = { Text("Stats") },
                    selected = currentRoute == Screen.Stats.route,
                    onClick  = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Stats.route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = Screen.Home.route) {

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToShikaku = { difficulty ->
                        navController.navigate(Screen.Shikaku.createRoute(difficulty))
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable(
                route     = Screen.Shikaku.route,
                arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
            ) { backEntry ->
                val arg        = backEntry.arguments?.getString("difficulty") ?: "EASY"
                val difficulty = difficultyFromNavArg(arg)
                ShikakuScreen(
                    difficulty     = difficulty,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
