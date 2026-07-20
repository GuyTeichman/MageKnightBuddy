package com.guyteichman.mageknightbuddy.ui.scoreboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoring

private const val SCOREBOARD_LIST_ROUTE = "scoreboard_list"

// "{index}" is a placeholder segment, filled in with the actual list index at navigation
// time (see the "scoreboard_details/$index" call below) and parsed back out via the
// navArgument declaration further down - the same way a path parameter works in a URL.
private const val SCOREBOARD_DETAILS_ROUTE = "scoreboard_details/{index}"

/**
 * Root composable for the Scoreboard tab: shows the list of saved [ScoringSession]s and, on
 * tapping a row, a full-screen per-category score breakdown for that session. Runs its own
 * nested [NavHost] - separate from the app's top-level tab navigation - so that pushing the
 * breakdown screen only affects this tab's own back stack. That keeps the tab's navigation
 * state (list vs. breakdown) independent of, and unaffected by, switching to other tabs and
 * back, rather than everything living in one flat, shared navigation graph.
 */
@Composable
fun ScoreboardTab(repository: ScoringSessionRepository, onScoreNewScenario: () -> Unit) {
    // A NavController scoped to this tab's own nested graph - distinct from whatever
    // NavController drives the app's top-level tab switching.
    val nestedNavController = rememberNavController()
    val viewModel: ScoreboardViewModel = viewModel(factory = ScoreboardViewModel.factory(repository))
    // `by` (a property delegate, needs the `getValue` import above) plus `collectAsState`
    // turns the ViewModel's Flow into Compose State: `sessions` reads like a plain value but
    // triggers recomposition whenever the Flow emits a new list. `initial` is what's shown
    // before the Flow's first emission arrives.
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())

    NavHost(navController = nestedNavController, startDestination = SCOREBOARD_LIST_ROUTE) {
        composable(SCOREBOARD_LIST_ROUTE) {
            ScoreboardListScreen(
                sessions = sessions,
                onRowClick = { index -> nestedNavController.navigate("scoreboard_details/$index") },
                onScoreNewScenario = onScoreNewScenario,
            )
        }
        composable(
            SCOREBOARD_DETAILS_ROUTE,
            // Declares the "{index}" placeholder as an Int argument, so it can be read back
            // out of the destination below via backStackEntry.arguments.
            arguments = listOf(navArgument("index") { type = NavType.IntType }),
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            // getOrNull returns null instead of throwing if the index is out of bounds (e.g.
            // a stale index after the session list changes); ?.let only runs the block - i.e.
            // only renders the details screen - when a session actually exists at that index.
            sessions.getOrNull(index)?.let { session ->
                ScoreboardDetailsScreen(session = session, onBack = { nestedNavController.popBackStack() })
            }
        }
    }
}

// The list screen: a FAB to start a new scoring session, plus either an empty-state message
// or the table of saved sessions (header row + one row per session).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreboardListScreen(
    sessions: List<ScoringSession>,
    onRowClick: (Int) -> Unit,
    onScoreNewScenario: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScoreNewScenario,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Score new scenario") },
            )
        },
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No scored games yet")
            }
        } else {
            // LazyColumn only composes/renders the rows currently on screen, unlike Column,
            // which would lay out every row up front - matters once the session list grows.
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item { ScoreboardHeaderRow() }
                // itemsIndexed hands back each item together with its position in the list,
                // which is needed here to pass the right index on to onRowClick.
                itemsIndexed(sessions) { index, session ->
                    ScoreboardRow(session = session, onClick = { onRowClick(index) })
                }
            }
        }
    }
}

// Bold column-title row (Knight / Score / Outcome) drawn above the session list.
@Composable
private fun ScoreboardHeaderRow() {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // weight(1f) on every Text here splits the Row's width evenly across the three
            // columns; it only works because Row is the parent (it's RowScope.weight).
            Text("Knight", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Score", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Outcome", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()
    }
}

// One row per saved session; tapping anywhere in the row triggers onClick (wired up by the
// caller to navigate to that session's breakdown screen).
@Composable
private fun ScoreboardRow(session: ScoringSession, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(session.knight.displayName, modifier = Modifier.weight(1f))
            Text(session.score.toString(), modifier = Modifier.weight(1f))
            Text(if (session.outcome == Outcome.WON) "Won" else "Lost", modifier = Modifier.weight(1f))
        }
        HorizontalDivider()
    }
}

// Full-screen per-category score breakdown for one session, pushed by tapping a row in the
// list screen; the back arrow in the top bar pops this off the nested NavHost's back stack.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreboardDetailsScreen(session: ScoringSession, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session.knight.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        // Re-derives the row-by-row breakdown from the session's raw input, rather than
        // storing it, so the shown breakdown always matches the current scoring rules.
        val breakdown = SoloConquestScoring.breakdown(session.input)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // rememberScrollState() keeps the scroll position across recompositions of
                // this composable (e.g. when the breakdown data changes); without `remember`
                // the scroll position would reset on every recomposition.
                .verticalScroll(rememberScrollState()),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Category", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Score", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            breakdown.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(item.label, modifier = Modifier.weight(1f))
                    Text(item.value.toString())
                }
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(session.score.toString(), fontWeight = FontWeight.Bold)
            }
        }
    }
}
