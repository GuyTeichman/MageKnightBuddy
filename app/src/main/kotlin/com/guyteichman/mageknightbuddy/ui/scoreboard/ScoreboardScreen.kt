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
import com.guyteichman.mageknightbuddy.domain.breakdown

private const val SCOREBOARD_LIST_ROUTE = "scoreboard_list"
private const val SCOREBOARD_DETAILS_ROUTE = "scoreboard_details/{index}"

@Composable
fun ScoreboardTab(repository: ScoringSessionRepository, onScoreNewScenario: () -> Unit) {
    val nestedNavController = rememberNavController()
    val viewModel: ScoreboardViewModel = viewModel(factory = ScoreboardViewModel.factory(repository))
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
            arguments = listOf(navArgument("index") { type = NavType.IntType }),
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            sessions.getOrNull(index)?.let { session ->
                ScoreboardDetailsScreen(session = session, onBack = { nestedNavController.popBackStack() })
            }
        }
    }
}

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
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item { ScoreboardHeaderRow() }
                itemsIndexed(sessions) { index, session ->
                    ScoreboardRow(session = session, onClick = { onRowClick(index) })
                }
            }
        }
    }
}

@Composable
private fun ScoreboardHeaderRow() {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Knight", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Score", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Outcome", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()
    }
}

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
        // session.input.breakdown() dispatches to whichever scenario's *Scoring object matches
        // the input's actual runtime type - see ScoringInput.breakdown() in domain.
        val breakdown = session.input.breakdown()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
