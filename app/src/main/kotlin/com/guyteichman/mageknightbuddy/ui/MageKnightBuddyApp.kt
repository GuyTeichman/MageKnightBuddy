package com.guyteichman.mageknightbuddy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guyteichman.mageknightbuddy.R

private sealed class Tab(val route: String, val labelRes: Int) {
    data object ScoreCalculator : Tab("score_calculator", R.string.tab_score_calculator)
    data object DummyPlayer : Tab("dummy_player", R.string.tab_dummy_player)
}

private val tabs = listOf(Tab.ScoreCalculator, Tab.DummyPlayer)

@Composable
fun MageKnightBuddyApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (tab == Tab.ScoreCalculator) Icons.Filled.Scoreboard else Icons.Filled.Groups,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.ScoreCalculator.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Tab.ScoreCalculator.route) { PlaceholderScreen("Score Calculator — coming soon") }
            composable(Tab.DummyPlayer.route) { PlaceholderScreen("Dummy Player — coming soon") }
        }
    }
}

@Composable
private fun PlaceholderScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message)
    }
}
