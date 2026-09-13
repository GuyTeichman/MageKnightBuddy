package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.PickOrder
import com.guyteichman.mageknightbuddy.domain.TacticState
import com.guyteichman.mageknightbuddy.domain.tacticPickOrder
import com.guyteichman.mageknightbuddy.ui.tutorial.ScreenTutorialState
import com.guyteichman.mageknightbuddy.ui.tutorial.TutorialAction

/**
 * Shared Scaffold shell for the three AI (turn/round) screens - `DummyPlayerScreen.kt`'s
 * `DummyPlayerAiScreen`, `ProxyPlayerScreen.kt`'s `ProxyPlayerAiScreen`, and `VolkareScreen.kt`'s
 * `VolkareAiScreen` (issue #319). All 3 used to repeat the same ~80-line skeleton: a top bar (back
 * nav + an optional [RoundChip] + the tutorial action), a bottom action row (Undo / Play Turn / End
 * Round), a loading guard that shows a spinner until the session first restores from Room, and a
 * trailing Tactic-picker dialog slot. This extracts exactly that shell - everything that genuinely
 * differs per screen stays a parameter or lambda instead of being forced uniform: [title]; the
 * enabled predicates [playTurnEnabled]/[endRoundEnabled] (Volkare's Play Turn reads `!session.lost`
 * where Dummy/Proxy Player read `!session.roundEnded`); what End Round actually does ([onEndRound] -
 * opens a confirmation dialog for Dummy/Proxy Player, calls straight through to `endRound()` for
 * Volkare, which has no round-prep offer input to collect); and the body itself ([content]).
 *
 * [roundChip] is a nullable composable slot rather than always rendered: each screen only shows its
 * [RoundChip] once a session has loaded, mirroring the `if (session != null) { RoundChip(...);
 * Spacer(...) }` block every screen used to inline in its top bar's `actions`. [isLoading] is what
 * switches between the spinner and the real bottom bar/[content], the same way each screen's own
 * `session == null` check used to.
 *
 * `internal`, not `private`: shared across 3 files in this package (`DummyPlayerScreen.kt`,
 * `ProxyPlayerScreen.kt`, `VolkareScreen.kt`), matching how issue #318 already exposed
 * `DeckPanel`/`HeroRow`/etc. the same way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiTurnScaffold(
    title: String,
    onBack: () -> Unit,
    roundChip: (@Composable () -> Unit)?,
    tutorial: ScreenTutorialState,
    isLoading: Boolean,
    canUndo: Boolean,
    isBusy: Boolean,
    onUndo: () -> Unit,
    playTurnEnabled: Boolean,
    onPlayTurn: () -> Unit,
    endRoundEnabled: Boolean,
    onEndRound: () -> Unit,
    tacticPicker: @Composable () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Only rendered once a session exists - see this function's doc comment.
                    if (roundChip != null) {
                        roundChip()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TutorialAction(onClick = tutorial::show)
                },
            )
        },
        bottomBar = {
            // Bottom action row instead of a Scaffold-managed bottomBar surface - only these
            // controls need it, and Row + padding is simpler than a full BottomAppBar here. Hidden
            // during the loading guard below, matching each screen's own `if (session != null)`.
            if (!isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Undo: icon-only so the two primary verbs keep their width - it's a
                    // misclick-recovery utility, not a peer game action. Disabled when there's
                    // nothing to revert or a mutation is in flight.
                    IconButton(onClick = onUndo, enabled = canUndo && !isBusy) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    Button(
                        onClick = onPlayTurn,
                        enabled = playTurnEnabled && !isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Play Turn")
                    }
                    OutlinedButton(
                        onClick = onEndRound,
                        enabled = !isBusy && endRoundEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("End Round")
                    }
                }
            }
        },
    ) { padding ->
        if (isLoading) {
            // Restoring from Room is asynchronous (see e.g. DummyPlayerAiViewModel's init block);
            // this only shows for the brief window before that first restore completes.
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }

    // Rendered last, in the same relative position as each screen's own trailing Tactic-picker
    // gate - the caller decides whether this actually shows a dialog (needsTacticPick &&
    // !tutorial.isVisible) or nothing at all.
    tacticPicker()
}

/**
 * The shared Tactic-card auto-pick effect for all 3 AI screens (issue #319): once a Round's Tactic
 * draft still needs a pick and it's the AI's/Volkare's turn to go by [tacticPickOrder]'s rule, this
 * calls [onPick] automatically instead of waiting for the player to trigger it. `LaunchedEffect`
 * keyed on [tacticState] (a data class, compared structurally) restarts this exactly when a pick
 * changes or a new Round clears both - DUMMY_FIRST draws as soon as it's null, PLAYER_FIRST waits
 * for the player's own pick to land first (see `TacticRules.kt`'s [tacticPickOrder] doc comment).
 *
 * [isVolkare] selects which of the two pick-order rules applies: Volkare's is always PLAYER_FIRST
 * regardless of [isSolo], while Standard Dummy Player/Proxy Player's depends on it (DUMMY_FIRST in
 * coop, PLAYER_FIRST in solo).
 */
@Composable
internal fun AutoPickDummyTactic(tacticState: TacticState, isVolkare: Boolean, isSolo: Boolean, onPick: () -> Unit) {
    LaunchedEffect(tacticState) {
        val pickOrder = tacticPickOrder(isVolkare = isVolkare, isSolo = isSolo)
        val dummyShouldGoNow = when (pickOrder) {
            PickOrder.DUMMY_FIRST -> true
            PickOrder.PLAYER_FIRST -> tacticState.playerPick != null
        }
        if (tacticState.dummyPick == null && dummyShouldGoNow) {
            onPick()
        }
    }
}
