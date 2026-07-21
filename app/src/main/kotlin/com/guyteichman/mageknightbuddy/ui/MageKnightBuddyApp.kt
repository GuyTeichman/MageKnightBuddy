package com.guyteichman.mageknightbuddy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guyteichman.mageknightbuddy.R
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.ui.dummyplayer.DummyPlayerTab
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.scoreboard.ScoreboardTab
import com.guyteichman.mageknightbuddy.ui.scorecalculator.ScoreCalculatorScreen

/**
 * One entry in the bottom navigation bar. `sealed class` restricts every possible
 * Tab to the `data object`s declared inside it, so a `when` elsewhere that handles
 * all three is guaranteed by the compiler to be exhaustive.
 */
private sealed class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Scoreboard : Tab("scoreboard", R.string.tab_scoreboard, Icons.Filled.Scoreboard)
    data object ScoreCalculator : Tab("score_calculator", R.string.tab_score_calculator, Icons.Filled.Calculate)
    data object DummyPlayer : Tab("dummy_player", R.string.tab_dummy_player, Icons.Filled.Groups)
}

private val tabs = listOf(Tab.Scoreboard, Tab.ScoreCalculator, Tab.DummyPlayer)

/**
 * Top-level app composable: builds the nav graph for the three bottom-nav tabs
 * (Scoreboard, Score Calculator, Dummy Player - see docs/design/architecture.md's
 * "Tab roadmap") and wires the bottom navigation bar to it. This is the root of the
 * whole UI tree, set as the content of MainActivity.
 */
@Composable
fun MageKnightBuddyApp(
    repository: ScoringSessionRepository,
    dummyPlayerRepository: DummyPlayerSessionRepository,
    fieldHelp: Map<String, FieldHelp>,
) {
    // rememberNavController creates the NavController once and keeps the same instance
    // across recompositions (Compose's "remember" idiom), so navigation state survives
    // re-renders caused by other state changes.
    val navController = rememberNavController()

    // Switches the visible tab without stacking up duplicate destinations: popUpTo +
    // saveState clears back to the start destination but remembers each tab's scroll/UI
    // state, launchSingleTop avoids pushing a second copy of a tab already on top, and
    // restoreState brings that remembered state back when you return to a tab.
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                // Observed as Compose State so this recomposes whenever the current
                // destination changes, keeping the highlighted tab in sync with navigation.
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        // `hierarchy` walks up through nested nav graphs, so this still
                        // matches correctly if a tab's destination is ever nested deeper.
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = { navigateToTab(tab.route) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        // NavHost + composable(route) { ... } is Compose Navigation's nav graph: each
        // composable(...) block registers one destination, and NavHost swaps the visible
        // one based on navController's current route.
        NavHost(
            navController = navController,
            startDestination = Tab.Scoreboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.Scoreboard.route) {
                ScoreboardTab(
                    repository = repository,
                    onScoreNewScenario = { navigateToTab(Tab.ScoreCalculator.route) },
                )
            }
            composable(Tab.ScoreCalculator.route) {
                ScoreCalculatorScreen(
                    repository = repository,
                    fieldHelp = fieldHelp,
                    onDone = { navigateToTab(Tab.Scoreboard.route) },
                )
            }
            composable(Tab.DummyPlayer.route) { DummyPlayerTab(repository = dummyPlayerRepository, fieldHelp = fieldHelp) }
        }
    }
}
