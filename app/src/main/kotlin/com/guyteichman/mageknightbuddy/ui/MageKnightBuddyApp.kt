package com.guyteichman.mageknightbuddy.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
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
import com.guyteichman.mageknightbuddy.data.EnemyPickerSessionRepository
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.ScoreCalculatorDraftRepository
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.ui.dummyplayer.DummyPlayerTab
import com.guyteichman.mageknightbuddy.ui.enemypicker.EnemyPickerTab
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.scoreboard.ScoreboardTab
import com.guyteichman.mageknightbuddy.ui.sites.SitesTab

/**
 * One entry in the bottom navigation bar. `sealed class` restricts every possible
 * Tab to the `data object`s declared inside it, so a `when` elsewhere that handles
 * them all is guaranteed by the compiler to be exhaustive.
 */
private sealed class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    // Scoreboard hosts the Score Calculator wizard inside its own nested NavHost (reached via
    // the "Score new scenario" FAB), so scoring is no longer a separate bottom-nav tab.
    data object Scoreboard : Tab("scoreboard", R.string.tab_scoreboard, Icons.Filled.Scoreboard)
    data object DummyPlayer : Tab("dummy_player", R.string.tab_dummy_player, Icons.Filled.Groups)
    data object EnemyPicker : Tab("enemy_picker", R.string.tab_enemy_picker, Icons.Filled.Casino)
    data object Sites : Tab("sites", R.string.tab_sites, Icons.Filled.Place)
}

private val tabs = listOf(Tab.Scoreboard, Tab.DummyPlayer, Tab.EnemyPicker, Tab.Sites)

/**
 * Top-level app composable: builds the nav graph for the four bottom-nav tabs
 * (Scoreboard, Dummy Player, Enemies, Sites - see docs/design/architecture.md's
 * "Tab roadmap") and wires the bottom navigation bar to it. Scoring lives inside the
 * Scoreboard tab now, not as its own tab. This is the root of the whole UI tree, set as
 * the content of MainActivity.
 */
@Composable
fun MageKnightBuddyApp(
    repository: ScoringSessionRepository,
    draftRepository: ScoreCalculatorDraftRepository,
    dummyPlayerRepository: DummyPlayerSessionRepository,
    volkareRepository: VolkareSessionRepository,
    proxyPlayerRepository: ProxyPlayerSessionRepository,
    enemyPickerRepository: EnemyPickerSessionRepository,
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
            // padding(innerPadding) keeps content clear of the bottom bar; consumeWindowInsets
            // then tells any inset-aware modifier *inside* a screen (e.g. the score wizard's
            // Modifier.imePadding()) that this much of the window insets is already accounted
            // for, so it doesn't add the bottom bar's height a second time when the soft
            // keyboard opens (issue #173).
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Tab.Scoreboard.route) {
                ScoreboardTab(
                    repository = repository,
                    draftRepository = draftRepository,
                    fieldHelp = fieldHelp,
                )
            }
            composable(Tab.DummyPlayer.route) {
                DummyPlayerTab(
                    repository = dummyPlayerRepository,
                    volkareRepository = volkareRepository,
                    proxyPlayerRepository = proxyPlayerRepository,
                    fieldHelp = fieldHelp,
                )
            }
            composable(Tab.EnemyPicker.route) {
                EnemyPickerTab(repository = enemyPickerRepository)
            }
            composable(Tab.Sites.route) {
                SitesTab()
            }
        }
    }
}
