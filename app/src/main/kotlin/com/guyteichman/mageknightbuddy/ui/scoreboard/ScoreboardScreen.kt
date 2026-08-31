package com.guyteichman.mageknightbuddy.ui.scoreboard

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guyteichman.mageknightbuddy.data.ScoreCalculatorDraftRepository
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.breakdown
import com.guyteichman.mageknightbuddy.ui.components.KnightFace
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.scenarioart.ScenarioArt
import com.guyteichman.mageknightbuddy.ui.scorecalculator.ScoreCalculatorScreen
import com.guyteichman.mageknightbuddy.ui.settings.SettingsAction

private const val SCOREBOARD_LIST_ROUTE = "scoreboard_list"

// "{index}" is a placeholder segment, filled in with the actual list index at navigation
// time (see the "scoreboard_details/$index" call below) and parsed back out via the
// navArgument declaration further down - the same way a path parameter works in a URL.
private const val SCOREBOARD_DETAILS_ROUTE = "scoreboard_details/{index}"

// The Score Calculator wizard, reached from the list screen's "Score new scenario" FAB.
// Scoring used to be its own bottom-nav tab; it now lives inside the Scoreboard tab's nested
// graph so the two form a single tab (issue #248).
private const val SCOREBOARD_SCORE_ROUTE = "scoreboard_score"

/**
 * Root composable for the Scoreboard tab: shows the list of saved [ScoringSession]s and hosts,
 * inside its own nested [NavHost], both the full-screen per-category score breakdown (reached by
 * tapping a card) and the Score Calculator wizard (reached via the "Score new scenario" FAB). The
 * nested [NavHost] is separate from the app's top-level tab navigation, so pushing the breakdown
 * or the wizard only affects this tab's own back stack. That keeps the tab's navigation state
 * independent of, and unaffected by, switching to other tabs and back, rather than everything
 * living in one flat, shared navigation graph.
 *
 * @param repository source of the saved sessions shown in the list and breakdown.
 * @param draftRepository where the wizard autosaves its in-progress fields (see [ScoreCalculatorScreen]).
 * @param fieldHelp the bundled "?" help text/citations passed through to the wizard.
 */
@Composable
fun ScoreboardTab(
    repository: ScoringSessionRepository,
    draftRepository: ScoreCalculatorDraftRepository,
    fieldHelp: Map<String, FieldHelp>,
    onOpenSettings: () -> Unit,
) {
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
                onScoreNewScenario = { nestedNavController.navigate(SCOREBOARD_SCORE_ROUTE) },
                onOpenSettings = onOpenSettings,
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
        composable(SCOREBOARD_SCORE_ROUTE) {
            ScoreCalculatorScreen(
                repository = repository,
                draftRepository = draftRepository,
                fieldHelp = fieldHelp,
                // On a successful save, pop back to the list, where the new card appears at the
                // top. System back also returns here on its own, since the wizard sits on this
                // nested NavHost's back stack above the list.
                onDone = { nestedNavController.popBackStack() },
            )
        }
    }
}

// The list screen: a FAB to start a new scoring session, plus either an empty-state message
// or the list of per-session art cards.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreboardListScreen(
    sessions: List<ScoringSession>,
    onRowClick: (Int) -> Unit,
    onScoreNewScenario: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scoreboard") },
                // The shared settings gear, present on every tab's top bar so Settings is reachable
                // from anywhere without its own bottom-nav tab (see SettingsAction).
                actions = { SettingsAction(onClick = onOpenSettings) },
            )
        },
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
            // LazyColumn only composes/renders the cards currently on screen, unlike Column,
            // which would lay out every card up front - matters once the session list grows.
            // contentPadding + spacedBy give each card breathing room; there's no header row now
            // that each card carries its own scenario/knight/score/outcome (issue #286).
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // itemsIndexed hands back each item together with its position in the list,
                // which is needed here to pass the right index on to onRowClick. key by each
                // session's timestamp (its stable identity - ScoringSession has no id field) so
                // prepending a new session doesn't re-key every card by position and force
                // KnightFace/ScenarioArt to re-decode into shifted slots.
                itemsIndexed(sessions, key = { _, session -> session.playedAt.toEpochMilli() }) { index, session ->
                    ScoreboardCard(session = session, onClick = { onRowClick(index) })
                }
            }
        }
    }
}

// Fixed hex colors, not MaterialTheme.colorScheme ones: Material3 has no built-in "success"/
// "failure" semantic color slot, so win/loss can't be expressed in theme terms. These are the
// same green/red hex values the old row tint (and CardColor.swatch in DummyPlayerScreen.kt) used,
// here at full strength for the Won/Lost pill and as a low-alpha scrim over the art.
private val WON_COLOR = Color(0xFF3E7C4A)
private val LOST_COLOR = Color(0xFFB5423A)

/** The outcome wash laid over a card's art - green for a win, red for a loss, kept faint so the art
 *  still reads through it (the solid [OutcomePill] carries the unambiguous win/loss signal). */
private fun outcomeTint(outcome: Outcome): Color =
    (if (outcome == Outcome.WON) WON_COLOR else LOST_COLOR).copy(alpha = 0.28f)

/** Cream ink for text/icons over the art, legible on any scenario's darkened background. */
private val CARD_INK = Color(0xFFF6ECDC)

private val CARD_SHAPE = RoundedCornerShape(14.dp)

/**
 * One saved session as a full-width art card (issue #286): the scenario's background art with an
 * outcome wash, the knight's face as a circular avatar, the score, and a solid Won/Lost pill.
 * Tapping anywhere opens that session's breakdown (the caller wires [onClick] to navigation).
 */
@Composable
private fun ScoreboardCard(session: ScoringSession, onClick: () -> Unit) {
    ScenarioArt(
        scenario = session.scenario,
        // ScenarioArt layers this tint above the art + its own scrim but below the content below.
        outcomeTint = outcomeTint(session.outcome),
        shape = CARD_SHAPE,
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            // clip before clickable so the tap ripple is bounded to the rounded card shape (the
            // clip inside ScenarioArt only bounds the art, not this externally-supplied ripple).
            .clip(CARD_SHAPE)
            .clickable(onClickLabel = "View breakdown", role = Role.Button, onClick = onClick),
    ) {
        // Knight avatar, top-start, ringed in cream so it reads as an avatar over busy art. The
        // ring is a bordered box with 2.dp inner padding, so the circle sits inside the ring.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .border(2.dp, CARD_INK, CircleShape)
                .padding(2.dp),
        ) {
            KnightFace(knight = session.knight, size = 40.dp)
        }
        // Won/Lost pill, top-end - the graphic markers (avatar + pill) sit along the top edge,
        // the text (names + score) along the bottom, so each row reads as one kind of thing
        // (author review of #286).
        OutcomePill(
            outcome = session.outcome,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
        )
        // Scenario + knight name, bottom-start. end padding leaves room for the score.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 12.dp, end = 72.dp),
        ) {
            Text(
                session.scenario.displayName,
                color = CARD_INK,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                session.knight.displayName,
                color = CARD_INK.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Score, bottom-end, prominent - paired with the names along the bottom text row.
        Text(
            text = session.score.toString(),
            color = CARD_INK,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 12.dp, end = 16.dp),
        )
    }
}

/** A solid Won/Lost chip in the outcome color - the unambiguous win/loss marker over the art wash. */
@Composable
private fun OutcomePill(outcome: Outcome, modifier: Modifier = Modifier) {
    val won = outcome == Outcome.WON
    Surface(
        color = if (won) WON_COLOR else LOST_COLOR,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = if (won) "Won" else "Lost",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

// Full-screen per-category score breakdown for one session, pushed by tapping a card in the
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
        // session.input.breakdown() dispatches to whichever scenario's *Scoring object matches
        // the input's actual runtime type - see ScoringInput.breakdown() in domain.
        val breakdown = session.input.breakdown()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // rememberScrollState() keeps the scroll position across recompositions of
                // this composable (e.g. when the breakdown data changes); without `remember`
                // the scroll position would reset on every recomposition.
                .verticalScroll(rememberScrollState()),
        ) {
            // Art header banner (issue #286): the same scenario art + avatar + score/outcome as the
            // list card, so the breakdown opens with the game it belongs to. The table below it is
            // left untouched. It scrolls with the content rather than pinning under the top bar.
            ScoreboardDetailHeader(session = session)
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

/**
 * The breakdown screen's header banner: a taller version of [ScoreboardCard]'s art treatment
 * (scenario art + outcome wash + knight avatar + scenario name + score + Won/Lost pill), full-bleed
 * across the top of the screen (a rectangular clip, not the card's rounded one). The knight's name
 * isn't repeated here - it's already the top-bar title, plus the avatar.
 */
@Composable
private fun ScoreboardDetailHeader(session: ScoringSession) {
    ScenarioArt(
        scenario = session.scenario,
        outcomeTint = outcomeTint(session.outcome),
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth().height(184.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .border(2.dp, CARD_INK, CircleShape)
                .padding(2.dp),
        ) {
            KnightFace(knight = session.knight, size = 52.dp)
        }
        // Won/Lost pill top-end (paired with the avatar along the top graphic row), the score
        // bottom-end (paired with the scenario name along the bottom text row) - the same
        // top-graphics / bottom-text split as the list card (author review of #286).
        OutcomePill(
            outcome = session.outcome,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 20.dp),
        )
        Text(
            text = session.scenario.displayName,
            color = CARD_INK,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp, end = 88.dp),
        )
        Text(
            text = session.score.toString(),
            color = CARD_INK,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp, end = 20.dp),
        )
    }
}
