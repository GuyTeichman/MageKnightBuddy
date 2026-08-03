package com.guyteichman.mageknightbuddy.ui.enemypicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.EnemyPickerSessionRepository
import com.guyteichman.mageknightbuddy.domain.DrawLogEntry
import com.guyteichman.mageknightbuddy.domain.EnemyPickerSession
import com.guyteichman.mageknightbuddy.domain.EnemyToken
import com.guyteichman.mageknightbuddy.domain.Expansion
import com.guyteichman.mageknightbuddy.domain.FactionRewardToken
import com.guyteichman.mageknightbuddy.domain.FactionRewardTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.PossessedEnemy
import com.guyteichman.mageknightbuddy.domain.PossessedEnemyStats
import com.guyteichman.mageknightbuddy.domain.PossessedToken
import com.guyteichman.mageknightbuddy.domain.PossessedTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.RuinToken
import com.guyteichman.mageknightbuddy.domain.RuinTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.TokenCatalogue
import com.guyteichman.mageknightbuddy.domain.TokenPile
import com.guyteichman.mageknightbuddy.domain.TokenPileId
import com.guyteichman.mageknightbuddy.data.TutorialProgressRepository
import com.guyteichman.mageknightbuddy.ui.settings.SettingsAction
import com.guyteichman.mageknightbuddy.ui.components.LabeledCheckbox
import com.guyteichman.mageknightbuddy.ui.components.LabeledSwitch
import com.guyteichman.mageknightbuddy.ui.tutorial.Tutorial
import com.guyteichman.mageknightbuddy.ui.tutorial.TutorialAction
import com.guyteichman.mageknightbuddy.ui.tutorial.TutorialDialog
import com.guyteichman.mageknightbuddy.ui.tutorial.TutorialKeys
import com.guyteichman.mageknightbuddy.ui.tutorial.rememberScreenTutorialState
import kotlinx.coroutines.launch

/**
 * The Enemy Picker tab (issue #178): the app-side replacement for the physical face-down token
 * piles. Resolves its [EnemyPickerViewModel] (which auto-starts a default session when nothing is
 * saved), holds all screen-local UI state (which token is zoomed, which log entry's flag dialog is
 * open, the staged config edits), and renders [EnemyPickerContent].
 *
 * A drawn token is shown zoomed immediately, recorded in the Draw Log, and discarded straight away
 * - the picker models no map (ADR-0006), so the log is the memory of what's still on the board.
 */
@Composable
fun EnemyPickerTab(
    repository: EnemyPickerSessionRepository,
    tutorials: Map<String, Tutorial>,
    tutorialProgress: TutorialProgressRepository,
    onOpenSettings: () -> Unit,
) {
    val viewModel: EnemyPickerViewModel = viewModel(factory = EnemyPickerViewModel.factory(repository))
    val scope = rememberCoroutineScope()

    // This tab's tutorial (issue #161): auto-shows on first visit, re-openable via the top-bar
    // graduation-cap action. Created before the session-null early return below so its auto-show
    // effect isn't skipped while the first session is still loading.
    val tutorial = rememberScreenTutorialState(TutorialKeys.ENEMIES, tutorials, tutorialProgress)

    val session = viewModel.session
    // Tokens currently shown zoomed (a whole draw batch, one grid cell, or one tapped log entry),
    // plus which index of the batch is on screen for the "x of y" swipe. Holds Draw Log indices
    // (not token ids) so the zoom dialog's Defeat button can call setDefeated on the right entry -
    // token id alone can't disambiguate two copies of the same token drawn in one batch.
    var zoom by remember { mutableStateOf<ZoomState?>(null) }
    // The grid overview for a multi-token draw (N > 1, D3/D7); zoom above nests inside it when a
    // cell is tapped, so dismissing zoom alone falls back to the grid instead of closing everything.
    var gridState by remember { mutableStateOf<GridState?>(null) }
    // Summon Draw (issue #191): the result of tapping Summon/Re-summon on whichever entry `zoom` is
    // currently showing - always nested one (or two) layers under that `zoom`, never under the
    // top-level `gridState` directly. `summonGrid` holds 2+ children (a token with several Summon
    // attacks, drawn together); `summonZoom` is either the lone child (1-child case) or a cell
    // drilled into from `summonGrid`. Both are reset whenever `zoom` itself changes or closes, since
    // they only make sense relative to whichever entry `zoom` is currently on.
    var summonGrid by remember { mutableStateOf<GridState?>(null) }
    var summonZoom by remember { mutableStateOf<ZoomState?>(null) }
    // A token whose full ability info window ("?") is open.
    var infoToken by remember { mutableStateOf<EnemyToken?>(null) }
    // A ruin whose read-only detail window is open - the RUIN pile's counterpart to `infoToken`,
    // opened by tapping a ruin cell in the pile-contents dialog (issue #231).
    var infoRuin by remember { mutableStateOf<RuinToken?>(null) }
    // A faction reward token whose read-only detail is open, opened by tapping a reward cell in the
    // pile-contents dialog (issue #252) - the reward piles' counterpart to `infoRuin`.
    var infoReward by remember { mutableStateOf<FactionRewardToken?>(null) }
    // The pile whose "view draw pile" contents dialog is open (issue #231), or null.
    var contentsPileId by remember { mutableStateOf<TokenPileId?>(null) }
    // The Draw Log index whose Defeat dialog is open, or null.
    var defeatDialogIndex by remember { mutableStateOf<Int?>(null) }
    // A pending destructive reset (Reset piles / Apply & Reset), held until the user confirms.
    var pendingReset by remember { mutableStateOf<(() -> Unit)?>(null) }
    // Whether the held-faction-rewards grid (issue #252) is open - the pinned Draw Log entry's target.
    var heldRewardsOpen by remember { mutableStateOf(false) }
    // Set when an un-defeat was blocked (issue #251): the token has been reshuffled out of the discard
    // and re-drawn, so it's in neither pile and can't be returned to the board without duplicating it.
    var undefeatBlocked by remember { mutableStateOf(false) }

    if (session == null) {
        // Still restoring / creating the first session.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Every Defeat / Spend toggle funnels through here so the one un-defeat guard lives in a single
    // place (issue #251): un-defeating a token that's been reshuffled out of the discard and re-drawn
    // (so it's in neither pile) would duplicate it, so instead of silently no-opping we warn. Defeats
    // (and any un-defeat that can be honoured) go straight through to the ViewModel.
    val requestSetDefeated: (Int, Boolean, String) -> Unit = { index, defeated, note ->
        if (!defeated && !session.canUndefeat(index)) {
            undefeatBlocked = true
        } else {
            scope.launch { viewModel.setDefeated(index, defeated, note) }
        }
    }

    // Shared by the DrawBar's staged multi-pile draw and #198's per-card tap-to-draw-1 shortcut -
    // both just fire a `draws` map through the same viewModel.draw() call and open the same
    // zoom/grid result, so there is exactly one place that decides what a draw's result opens.
    // [possessed] (the Apocalypse Dragon toggle) pairs each drawn circular with a possessed token;
    // it's false for the per-card tap shortcut, which is always an ordinary single enemy.
    val onDraw: (Map<TokenPileId, Int>, Boolean) -> Unit = { draws, possessed ->
        scope.launch {
            viewModel.draw(draws, possessed)
            // mutate() has published the new session by the time draw() returns, so the last
            // `total` log entries (draws only ever append) are exactly what was just drawn,
            // across every pile in `draws` (D12's TokenPileId.entries order).
            val log = viewModel.session?.drawLog ?: return@launch
            val total = draws.values.sum()
            val newIndices = (log.size - total until log.size).toList()
            // D7: a single draw skips the grid and goes straight to its detail, same as today;
            // only a batch of more than one - whether from one pile or several (D13) - opens
            // the grid overview.
            if (total == 1) {
                zoom = ZoomState(newIndices, 0)
            } else {
                gridState = GridState(newIndices)
            }
        }
    }

    // Where a Summon Draw's children open, nested under whichever `zoom` entry they belong to - a
    // single child goes straight to `summonZoom`; 2+ children (a token with several Summon attacks,
    // drawn together) open `summonGrid` first (Option B), same "N > 1 opens a grid" convention
    // `onDraw` already follows. Shared by [onSummon] (after drawing a fresh set) and [onViewSummoned]
    // (re-opening the *current* set without drawing again).
    val openSummonResult: (List<Int>) -> Unit = { children ->
        if (children.size > 1) {
            summonZoom = null
            summonGrid = GridState(children)
        } else {
            summonGrid = null
            summonZoom = children.singleOrNull()?.let { ZoomState(children, 0) }
        }
    }
    val onSummon: (Int) -> Unit = { parentIndex ->
        scope.launch {
            viewModel.summon(parentIndex)
            val children = viewModel.session?.currentChildrenOf(parentIndex) ?: return@launch
            openSummonResult(children)
        }
    }
    // Re-opens the already-summoned child(ren) of whichever entry `zoom` is on, without drawing
    // again - the "Summoned: X" line's own tap target, distinct from the Summon/Re-summon button
    // (which always draws a fresh set). A no-op if there's nothing to view (shouldn't happen - the
    // line and its tap target only render once `currentChildrenOf` is non-empty).
    val onViewSummoned: (List<Int>) -> Unit = { children -> openSummonResult(children) }

    // An Enemies-With-Treasure ruin's drawn enemies (issue #201) are real enemies fought in full, so
    // - unlike a Summon Draw's children - they open in the *normal* top-level zoom/grid (full stats,
    // per-enemy Defeat), not the reduced summon dialogs. This closes the ruin's own zoom and shows
    // the enemies, exactly as `onDraw` opens a fresh draw's result.
    val openRuinEnemies: (List<Int>) -> Unit = { children ->
        summonGrid = null; summonZoom = null
        if (children.size == 1) {
            gridState = null; zoom = ZoomState(children, 0)
        } else {
            zoom = null; gridState = GridState(children)
        }
    }
    // Draws the ruin's prescribed enemies (attaching them under it via the shared summon machinery),
    // then opens them. Used by the ruin zoom's "Draw its enemies" button.
    val onDrawRuinEnemies: (Int) -> Unit = { parentIndex ->
        scope.launch {
            viewModel.drawRuinEnemies(parentIndex)
            val children = viewModel.session?.currentChildrenOf(parentIndex) ?: return@launch
            if (children.isNotEmpty()) openRuinEnemies(children)
        }
    }

    // The current summoned child of a summoner entry (first one, if a token ever has several
    // Summon attacks) - used to superimpose a small thumbnail of it wherever that summoner is shown
    // at a glance (issue #191). Null for an entry that was never summoned from.
    val currentChildOf: (Int) -> DrawLogEntry? = { index ->
        session.currentChildrenOf(index).firstOrNull()?.let { session.drawLog[it] }
    }

    // Render the tutorial pop-up over the content when open (auto on first visit, or on demand).
    if (tutorial.isVisible) tutorial.tutorial?.let { TutorialDialog(it, onDismiss = tutorial::dismiss) }

    EnemyPickerContent(
        session = session,
        isBusy = viewModel.isBusy,
        onOpenSettings = onOpenSettings,
        onShowTutorial = tutorial::show,
        onDraw = onDraw,
        onOpenToken = { index -> zoom = ZoomState(listOf(index), 0); summonGrid = null; summonZoom = null },
        // Reopening a batch from the Draw Log (#203/D18) is fully identical to the grid a fresh
        // draw opens - same GridState, same TokenGridDialog, same D8 checkboxes/D10 dismiss - so
        // this just sets the same `gridState` `onDraw`'s N>1 branch does.
        onOpenBatch = { indices -> gridState = GridState(indices); zoom = null; summonGrid = null; summonZoom = null },
        onOpenDefeatDialog = { index -> defeatDialogIndex = index },
        onOpenContents = { pileId -> contentsPileId = pileId },
        onOpenHeldRewards = { heldRewardsOpen = true },
        currentChildOf = currentChildOf,
        onRequestReset = { pendingReset = { scope.launch { viewModel.reset() } } },
        onRequestApplyConfig = { tokenSet, replacement ->
            pendingReset = { scope.launch { viewModel.applyConfig(tokenSet, replacement) } }
        },
    )

    // --- Dialogs, driven by the state above ---

    // Composed before `zoom` below so, when a grid cell opens a detail, the zoom dialog's window
    // is added after the grid's and stacks on top of it.
    gridState?.let { grid ->
        TokenGridDialog(
            state = grid,
            log = session.drawLog,
            title = "${grid.logIndices.size} tokens drawn",
            showDefeatToggle = true,
            currentChildOf = currentChildOf,
            onOpenDetail = { position -> zoom = ZoomState(grid.logIndices, position); summonGrid = null; summonZoom = null },
            onToggleDefeated = { index, defeated -> requestSetDefeated(index, defeated, "") },
            onDismiss = { gridState = null; zoom = null; summonGrid = null; summonZoom = null },
        )
    }

    // The held-faction-rewards grid (issue #252), opened from the pinned Draw Log entry. Composed
    // before `zoom` below so a reward's effect zoom (opened by tapping a cell) stacks on top of it,
    // and dismissing that zoom falls back to this grid - the same before/after trick the enemy
    // grid/zoom pair uses. `heldRewardIndices` is derived live from the session, so spending a token
    // (which flips its defeated flag) drops it out of the grid on the next recomposition.
    val heldRewardIndices = session.drawLog.indices.filter {
        session.drawLog[it].pile.isFactionReward && !session.drawLog[it].defeated
    }
    // If the grid is open but the last held token was just spent, close it - as a side effect
    // (LaunchedEffect) rather than a state write during composition.
    LaunchedEffect(heldRewardsOpen, heldRewardIndices.isEmpty()) {
        if (heldRewardsOpen && heldRewardIndices.isEmpty()) heldRewardsOpen = false
    }
    if (heldRewardsOpen && heldRewardIndices.isNotEmpty()) {
        HeldRewardsDialog(
            heldIndices = heldRewardIndices,
            log = session.drawLog,
            onOpenDetail = { index -> zoom = ZoomState(listOf(index), 0); summonGrid = null; summonZoom = null },
            onSpend = { index -> requestSetDefeated(index, true, "") },
            onDismiss = { heldRewardsOpen = false },
        )
    }

    zoom?.let { state ->
        val entry = session.drawLog[state.logIndices[state.index]]
        // A drawn faction reward token gets its own dialog (name + effect text + spend), never the
        // enemy or ruin zoom - it prints no armor/attack/fame and no altar/enemy-draw prompt.
        val reward = FactionRewardTokenCatalogue.byId(entry.tokenId)
        // A drawn RUIN token gets its own dialog (altar prompt / enemy-draw), never the
        // armor/attack/fame enemy zoom - the two components print entirely different things.
        val ruin = RuinTokenCatalogue.byId(entry.tokenId)
        if (reward != null) {
            FactionRewardZoomDialog(
                token = reward,
                entry = entry,
                logIndex = state.logIndices[state.index],
                onToggleSpent = { index, spent -> requestSetDefeated(index, spent, "") },
                onDismiss = { zoom = null; summonGrid = null; summonZoom = null },
            )
        } else if (ruin != null) {
            RuinZoomDialog(
                ruin = ruin,
                entry = entry,
                logIndex = state.logIndices[state.index],
                currentEnemies = session.currentChildrenOf(state.logIndices[state.index]),
                enemyName = { childIndex -> TokenCatalogue.byId(session.drawLog[childIndex].tokenId)?.name ?: session.drawLog[childIndex].tokenId },
                onDrawEnemies = onDrawRuinEnemies,
                onViewEnemies = openRuinEnemies,
                onToggleDefeated = { index, defeated -> requestSetDefeated(index, defeated, "") },
                onDismiss = { zoom = null; summonGrid = null; summonZoom = null },
            )
        } else {
            TokenZoomDialog(
                state = state,
                log = session.drawLog,
                onNavigate = { newIndex -> zoom = state.copy(index = newIndex); summonGrid = null; summonZoom = null },
                onShowInfo = { token -> infoToken = token },
                onToggleDefeated = { index, defeated -> requestSetDefeated(index, defeated, "") },
                onSummon = onSummon,
                onViewSummoned = onViewSummoned,
                currentChildrenOf = { index -> session.currentChildrenOf(index) },
                // Only clears the detail dialog - if it was opened from the grid, the grid (still set
                // above) reappears underneath; a top-level zoom (gridState null) just closes. Also
                // clears any Summon Draw nested under this entry - it only makes sense while this
                // entry's own zoom is open.
                onDismiss = { zoom = null; summonGrid = null; summonZoom = null },
            )
        }
    }

    // Summon Draw result (nested under `zoom` above, not under `gridState`) - same before/after
    // composition-order trick as the top-level grid/zoom pair, so window stacking is correct.
    summonGrid?.let { grid ->
        TokenGridDialog(
            state = grid,
            log = session.drawLog,
            title = "${grid.logIndices.size} tokens summoned",
            // A Summon Draw child is never independently marked defeated - the summoner's own
            // Defeat flag resolves the whole encounter (see `CONTEXT.md`'s "Summon Draw").
            showDefeatToggle = false,
            onOpenDetail = { position -> summonZoom = ZoomState(grid.logIndices, position) },
            onToggleDefeated = { _, _ -> },
            onDismiss = { summonGrid = null; summonZoom = null },
        )
    }

    summonZoom?.let { state ->
        SummonedChildZoomDialog(
            state = state,
            log = session.drawLog,
            onNavigate = { newIndex -> summonZoom = state.copy(index = newIndex) },
            onShowInfo = { token -> infoToken = token },
            onDismiss = { summonZoom = null },
        )
    }

    // Drop a stale open-contents selection if its pile disappears - e.g. an Apply & Reset that
    // changes the Token Set so this pile no longer exists. Without this, `contentsPileId` would
    // linger (the guard below just skips rendering) and could re-open the dialog unbidden if that
    // same pile id later returned.
    LaunchedEffect(session.piles.keys, contentsPileId) {
        if (contentsPileId != null && contentsPileId !in session.piles) contentsPileId = null
    }

    // The "view draw pile" contents dialog (issue #231). Composed before the info dialogs below so
    // that tapping a cell (which opens infoToken/infoRuin) stacks that detail window on top of it.
    contentsPileId?.let { pileId ->
        // The pile may vanish if the token set changes while this is open; guard with a null-safe let.
        session.piles[pileId]?.let { pile ->
            PileContentsDialog(
                pileId = pileId,
                pile = pile,
                withReplacement = session.drawWithReplacement,
                onOpenEnemyInfo = { token -> infoToken = token },
                onOpenRuinInfo = { ruin -> infoRuin = ruin },
                onOpenRewardInfo = { reward -> infoReward = reward },
                onDismiss = { contentsPileId = null },
            )
        }
    }

    infoToken?.let { token ->
        TokenInfoDialog(token = token, onDismiss = { infoToken = null })
    }

    infoRuin?.let { ruin ->
        RuinInfoDialog(ruin = ruin, onDismiss = { infoRuin = null })
    }

    infoReward?.let { reward ->
        RewardInfoDialog(token = reward, onDismiss = { infoReward = null })
    }

    defeatDialogIndex?.let { index ->
        DefeatDialog(
            entry = session.drawLog[index],
            onSave = { defeated, note ->
                requestSetDefeated(index, defeated, note)
                defeatDialogIndex = null
            },
            onDismiss = { defeatDialogIndex = null },
        )
    }

    pendingReset?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingReset = null },
            title = { Text("Reset piles?") },
            text = { Text("This shuffles every pile back to full and clears the Draw Log. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { action(); pendingReset = null }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { pendingReset = null }) { Text("Cancel") } },
        )
    }

    // Shown when an un-defeat was blocked (issue #251) - see [requestSetDefeated]. Rare: the token has
    // already cycled back into the pile and been re-drawn, so it can't be returned to the board.
    if (undefeatBlocked) {
        AlertDialog(
            onDismissRequest = { undefeatBlocked = false },
            title = { Text("Can't undo this defeat") },
            text = { Text("This token has already been reshuffled back into its pile and drawn again, so it can no longer be returned to the board. A pile Reset clears this up.") },
            confirmButton = { TextButton(onClick = { undefeatBlocked = false }) { Text("OK") } },
        )
    }
}

/**
 * Immutable UI state for the zoom dialog: the Draw Log indices of the batch being viewed (chronological
 * indices into [EnemyPickerSession.drawLog], not token ids - two entries can share a token id) and
 * which position in that batch is on screen for the "x of y" swipe.
 */
private data class ZoomState(val logIndices: List<Int>, val index: Int)

/** Immutable UI state for the multi-draw grid overview (D3): the Draw Log indices of one batch. */
private data class GridState(val logIndices: List<Int>)

/**
 * The scrollable body of the Enemy Picker: a card per [TokenPile], two per row (D14), the Draw
 * Log, and the staged config section - plus a bottom [DrawBar] that fires a draw across every pile
 * with a nonzero stepper (D13). Stateless apart from the staged per-pile quantities and the staged
 * config edits, which it owns because neither belongs in the persisted session.
 */
@Composable
private fun EnemyPickerContent(
    session: EnemyPickerSession,
    isBusy: Boolean,
    onOpenSettings: () -> Unit,
    onShowTutorial: () -> Unit,
    onDraw: (Map<TokenPileId, Int>, Boolean) -> Unit,
    onOpenToken: (Int) -> Unit,
    onOpenBatch: (List<Int>) -> Unit,
    onOpenDefeatDialog: (Int) -> Unit,
    onOpenContents: (TokenPileId) -> Unit,
    onOpenHeldRewards: () -> Unit,
    currentChildOf: (Int) -> DrawLogEntry?,
    onRequestReset: () -> Unit,
    onRequestApplyConfig: (Set<Expansion>, Boolean) -> Unit,
) {
    // Staged draw quantities, keyed by pile (D13): 0/absent means "not part of the next draw."
    // Lives here rather than inside each PileCard so the bottom DrawBar can see every pile's total
    // and reset them all at once after firing - a plain `remember`, like ConfigSection's staging
    // below, since losing this on rotation is a minor inconvenience, not lost game state.
    var quantities by remember { mutableStateOf<Map<TokenPileId, Int>>(emptyMap()) }
    val totalQuantity = quantities.values.sum()
    // The Apocalypse Dragon "possess this draw" toggle: only meaningful when the POSSESSED pile
    // exists (AD in the Token Set). Staged like `quantities` - a plain remember, since losing it on
    // rotation is a minor inconvenience, not game state.
    var possess by remember { mutableStateOf(false) }
    val possessedAvailable = TokenPileId.POSSESSED in session.piles
    // Fold availability in here (rather than writing state mid-compose) so a stale `true` left over
    // after AD is removed from the set never actually possesses a draw.
    val effectivePossess = possess && possessedAvailable

    Scaffold(
        topBar = { EnemyPickerTopBar(onOpenSettings = onOpenSettings, onShowTutorial = onShowTutorial) },
        bottomBar = {
            DrawBar(
                total = totalQuantity,
                possessed = effectivePossess,
                enabled = !isBusy && totalQuantity > 0,
                onDraw = {
                    onDraw(quantities.filterValues { it > 0 }, effectivePossess)
                    quantities = emptyMap()
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // One card per pile that exists in this token set, in a stable enum order, two per row.
            val pileIds = TokenPileId.entries.filter { it in session.piles }
            items(pileIds.chunked(2), key = { row -> row.first().name }) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Each card gets equal weight, so a full row (2 cards) splits evenly in half and
                    // a trailing lone card (D14 - today, Ruin) fills the whole row's width instead.
                    row.forEach { pileId ->
                        val pile = session.piles.getValue(pileId)
                        // The POSSESSED pile is never drawn on its own (it's consumed by a possessed
                        // draw of circular enemies), so its card is display-only: no stepper, no
                        // tap-to-draw-1.
                        val displayOnly = pileId == TokenPileId.POSSESSED
                        PileCard(
                            modifier = Modifier.weight(1f),
                            pileId = pileId,
                            pile = pile,
                            // Tokens drawn from this pile that are still on the board (issue #251) -
                            // derived from the Draw Log, shown alongside the remaining-to-draw count.
                            onBoard = session.onBoardCount(pileId),
                            withReplacement = session.drawWithReplacement,
                            quantity = quantities[pileId] ?: 0,
                            onQuantityChange = { quantities = quantities + (pileId to it) },
                            // Tap-to-draw-1 (#198): independent of the staged stepper above -
                            // doesn't read or reset `quantities`, just fires its own one-entry draw.
                            // Same "anything left to draw" check the stepper's own `max` already
                            // makes: with replacement a pile never runs out; without, only disabled
                            // once the pile is entirely empty (draw + discard both zero). Never for
                            // the display-only POSSESSED pile.
                            canDrawOne = !isBusy && !displayOnly &&
                                (session.drawWithReplacement || pile.drawPile.size + pile.discardPile.size > 0),
                            onDrawOne = { onDraw(mapOf(pileId to 1), false) },
                            onViewContents = { onOpenContents(pileId) },
                            // Ruins and faction reward tokens are revealed one at a time (one ruin
                            // site / one earned reward), so both drop the batch stepper (issue #252).
                            tapToDrawOnly = pileId == TokenPileId.RUIN || pileId.isFactionReward,
                            displayOnly = displayOnly,
                        )
                    }
                }
            }

            // The Apocalypse Dragon possess toggle (issue #189): shown only when the POSSESSED pile
            // exists. When on, the next Draw pairs every staged circular enemy with a possessed token
            // to form composite possessed enemies (docs/rules/apocalypse-dragon.md).
            if (possessedAvailable) {
                item(key = "possess-toggle") {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LabeledSwitch(
                                label = "Possess this draw",
                                checked = possess,
                                onCheckedChange = { possess = it },
                            )
                            Text(
                                "Each enemy in the next draw is paired with a possessed token (Apocalypse Dragon).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item(key = "draw-log") {
                DrawLogSection(
                    log = session.drawLog,
                    onOpenToken = onOpenToken,
                    onOpenBatch = onOpenBatch,
                    onOpenDefeatDialog = onOpenDefeatDialog,
                    onOpenHeldRewards = onOpenHeldRewards,
                    currentChildOf = currentChildOf,
                )
            }

            item(key = "config") {
                ConfigSection(
                    currentTokenSet = session.tokenSet,
                    currentReplacement = session.drawWithReplacement,
                    isBusy = isBusy,
                    onRequestReset = onRequestReset,
                    onRequestApplyConfig = onRequestApplyConfig,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnemyPickerTopBar(onOpenSettings: () -> Unit, onShowTutorial: () -> Unit) {
    // The tutorial (graduation-cap) action sits before the shared settings gear, which is present on
    // every tab's top bar so Settings is reachable from anywhere without its own bottom-nav tab.
    TopAppBar(
        title = { Text("Enemies") },
        actions = {
            TutorialAction(onClick = onShowTutorial)
            SettingsAction(onClick = onOpenSettings)
        },
    )
}

/**
 * The bottom bar's single global draw action (D13/D15): always visible, disabled while nothing is
 * staged (`total == 0`) rather than hidden, so the bar's presence never jumps around as steppers
 * change.
 */
@Composable
private fun DrawBar(total: Int, possessed: Boolean, enabled: Boolean, onDraw: () -> Unit) {
    BottomAppBar {
        Button(
            onClick = onDraw,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            // The label reflects the possess toggle so it's clear the whole batch will be possessed.
            Text(
                when {
                    total == 0 -> "Draw"
                    possessed -> "Draw $total possessed"
                    else -> "Draw $total"
                },
            )
        }
    }
}

/**
 * One pile: its face-down back art (#198 - tapping it draws exactly 1, same shortcut as setting
 * the stepper to 1 and firing "Draw"), name, how many tokens are left / drawn, and a 0..N quantity
 * stepper. Fully "controlled" (no local state of its own) - [quantity] and [onQuantityChange] are
 * hoisted to [EnemyPickerContent] so its bottom [DrawBar] can read every card's total and reset
 * them all at once (D13); there is no per-card Draw button any more, only the art's tap shortcut.
 */
@Composable
private fun PileCard(
    modifier: Modifier = Modifier,
    pileId: TokenPileId,
    pile: TokenPile,
    // How many tokens drawn from this pile are still on the board (undefeated); see [onBoardCount].
    onBoard: Int,
    withReplacement: Boolean,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    canDrawOne: Boolean,
    onDrawOne: () -> Unit,
    onViewContents: () -> Unit,
    // The RUIN pile is revealed one token at a time (you enter one ruin site), never batched, so it
    // drops the quantity stepper and keeps only the tap-to-draw-1 shortcut (issue #201).
    tapToDrawOnly: Boolean = false,
    // The POSSESSED pile is never drawn on its own - it's consumed alongside a possessed circular
    // draw (the possess toggle), so its card is a pure display: count + composition, no draw control.
    displayOnly: Boolean = false,
) {
    // Everything drawable now or after a Replenish: the draw pile plus the discard (defeated tokens
    // reshuffle back in). On-board tokens are held out and never re-drawable, so they aren't counted
    // here (issue #251). "Nothing to draw" = both are empty (everything drawn is on the board).
    val toDraw = pile.drawPile.size + pile.discardPile.size

    ElevatedCard(modifier = modifier) {
        Column(
            // fillMaxWidth so horizontalAlignment actually centers each child across the card's
            // full width - without it the Column shrink-wraps to its widest child and everything
            // ends up flush-left instead of centered. Bottom padding trimmed below the stepper
            // (the last child) - it doesn't need as much breathing room as the top/sides.
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // clickable + enabled gates the tap-to-draw-1 shortcut on canDrawOne (busy / pile
            // empty), same conditions the stepper's own max already enforces below.
            Box(modifier = Modifier.clickable(enabled = canDrawOne, onClick = onDrawOne)) {
                PileBackFace(pileId = pileId, size = 72.dp)
            }
            Text(pileId.displayName(), style = MaterialTheme.typography.titleMedium)
            // The count line doubles as the "view draw pile" trigger (issue #231): primary-colored
            // and underlined so it reads as tappable, opening the pile-composition dialog. With
            // replacement nothing depletes, so the counts are meaningless - hide them and say so.
            // Otherwise it shows what's left to draw and, when any, how many are still on the board.
            Text(
                text = if (withReplacement) {
                    "View pile · never depletes"
                } else {
                    buildString {
                        append(if (toDraw > 0) "$toDraw to draw" else "Nothing to draw")
                        if (onBoard > 0) append(" · $onBoard on board")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(onClick = onViewContents),
            )
            if (displayOnly) {
                // POSSESSED: not drawn on its own, so a hint stands in for the missing control.
                Text(
                    "Drawn with enemies",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (tapToDrawOnly) {
                // No stepper for the RUIN pile - a subtle hint that its face is the whole control.
                Text(
                    "Tap to reveal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                QuantityStepper(
                    quantity = quantity,
                    onQuantityChange = onQuantityChange,
                    // Without replacement, can't draw more than what's left to draw (draw + discard).
                    max = if (withReplacement) MAX_BATCH else toDraw.coerceAtMost(MAX_BATCH),
                )
            }
        }
    }
}

/** A compact "− N +" stepper clamped to 0..[max] (D13 - 0 means "not part of the next draw"). */
@Composable
private fun QuantityStepper(quantity: Int, onQuantityChange: (Int) -> Unit, max: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Shrunk below IconButton's default 48.dp touch target - the pile cards are dense,
        // repeated controls rather than a one-off action, so the extra tap padding just added
        // unused height to every card.
        IconButton(
            onClick = { onQuantityChange((quantity - 1).coerceAtLeast(0)) },
            enabled = quantity > 0,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "One fewer")
        }
        Text("$quantity", style = MaterialTheme.typography.titleMedium)
        IconButton(
            onClick = { onQuantityChange((quantity + 1).coerceAtMost(max)) },
            enabled = quantity < max,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "One more")
        }
    }
}

/**
 * The Draw Log (see `CONTEXT.md`'s "Draw Log"): newest-first, split into the batches still **on the
 * board** (top) and the **defeated** ones (dimmed, below). Since #203/D18, one row is one *batch*
 * (a shared [DrawLogEntry.batchId]), not one token - a size-1 batch renders exactly like the old
 * one-row-per-token model ([DrawLogRow], tapping opens that token zoomed), while a size>1 batch
 * collapses into [DrawLogBatchRow] and tapping it reopens the same grid overview a fresh draw would
 * (D18's "fully identical" call - no separate read-only mode). A batch stays "on the board" until
 * every member is defeated (D19), so it moves exactly once rather than flapping between sections as
 * individual members get cleared.
 */
@Composable
private fun DrawLogSection(
    log: List<DrawLogEntry>,
    onOpenToken: (Int) -> Unit,
    onOpenBatch: (List<Int>) -> Unit,
    onOpenDefeatDialog: (Int) -> Unit,
    onOpenHeldRewards: () -> Unit,
    currentChildOf: (Int) -> DrawLogEntry?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Draw Log", style = MaterialTheme.typography.titleLarge)
        if (log.isEmpty()) {
            Text("No tokens drawn yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // groupDrawLog already excludes Summon Draw children (parentIndex != null) and returns
        // newest-batch-first - see its own doc comment for why.
        val groups = groupDrawLog(log)
        // Faction reward groups (issue #252) are always single-token (earned one at a time), and get
        // special treatment: the *held* (un-spent) ones collapse into one pinned entry at the top,
        // while the *spent* ones drop into the Defeated section as ordinary dimmed rows.
        val (rewardGroups, normalGroups) = groups.partition { log[it.logIndices.first()].pile.isFactionReward }
        val heldRewardCount = rewardGroups.count { !log[it.logIndices.first()].defeated }
        val spentRewardGroups = rewardGroups.filter { log[it.logIndices.first()].defeated }

        val onBoard = normalGroups.filter { !it.allDefeated(log) }
        val defeated = normalGroups.filter { it.allDefeated(log) }

        // The single pinned "Faction rewards held: N" entry, always at the very top of the log.
        if (heldRewardCount > 0) {
            PinnedHeldRewardsRow(count = heldRewardCount, onClick = onOpenHeldRewards)
        }

        val hasDefeated = defeated.isNotEmpty() || spentRewardGroups.isNotEmpty()
        // Only show the section headers once there's actually a split to label; a fresh log with
        // nothing defeated/spent yet is just a flat newest-first list.
        val showHeaders = onBoard.isNotEmpty() && hasDefeated

        if (onBoard.isNotEmpty()) {
            if (showHeaders) Text("On the board", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            onBoard.forEach { group ->
                DrawLogGroupRow(group, log, onOpenToken, onOpenBatch, onOpenDefeatDialog, currentChildOf)
            }
        }
        if (hasDefeated) {
            Text(
                "Defeated",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            defeated.forEach { group ->
                DrawLogGroupRow(group, log, onOpenToken, onOpenBatch, onOpenDefeatDialog, currentChildOf)
            }
            // Spent faction reward tokens sit here too, as ordinary dimmed rows (issue #252's
            // "spent -> dimmed history"), newest first like everything else.
            spentRewardGroups.forEach { group ->
                DrawLogGroupRow(group, log, onOpenToken, onOpenBatch, onOpenDefeatDialog, currentChildOf)
            }
        }
    }
}

/**
 * The single pinned Draw Log entry (issue #252) that collapses every currently-held faction reward
 * token into one row at the top of the log - tapping it opens the [HeldRewardsDialog] grid. Styled
 * like a [DrawLogRow] but with a distinct tinted background so it reads as a persistent summary
 * rather than one more drawn token.
 */
@Composable
private fun PinnedHeldRewardsRow(count: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Faction rewards held: $count",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Tap to view or spend",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            TextButton(onClick = onClick) { Text("View") }
        }
    }
}

/** Dispatches one [DrawLogGroup] to [DrawLogRow] (size 1, unchanged single-token row) or
 * [DrawLogBatchRow] (size > 1, collapsed batch row) - the one place that decides which of the two
 * a group renders as. */
@Composable
private fun DrawLogGroupRow(
    group: DrawLogGroup,
    log: List<DrawLogEntry>,
    onOpenToken: (Int) -> Unit,
    onOpenBatch: (List<Int>) -> Unit,
    onOpenDefeatDialog: (Int) -> Unit,
    currentChildOf: (Int) -> DrawLogEntry?,
) {
    if (group.logIndices.size == 1) {
        val index = group.logIndices.single()
        DrawLogRow(index, log[index], onOpenToken, onOpenDefeatDialog, currentChildOf(index))
    } else {
        DrawLogBatchRow(group, log, onOpenBatch)
    }
}

/**
 * One Draw Log row. [summonedChild] (issue #191) is the entry's current Summon Draw child, if any -
 * shown as a small circular thumbnail of its own art before the name, the row's lightweight version
 * of the grid cell's superimposed badge (the row has no full-size art of its own to superimpose onto).
 */
@Composable
private fun DrawLogRow(
    index: Int,
    entry: DrawLogEntry,
    onOpenToken: (Int) -> Unit,
    onOpenDefeatDialog: (Int) -> Unit,
    summonedChild: DrawLogEntry?,
) {
    val token = TokenCatalogue.byId(entry.tokenId)
    // A ruin id resolves in the ruin catalogue instead (a sibling type, not an EnemyToken); a
    // faction reward id in its own catalogue (issue #252) - the three id namespaces are disjoint.
    val ruin = if (token == null) RuinTokenCatalogue.byId(entry.tokenId) else null
    val reward = if (token == null && ruin == null) FactionRewardTokenCatalogue.byId(entry.tokenId) else null
    val summonedChildToken = summonedChild?.let { TokenCatalogue.byId(it.tokenId) }
    // A possessed enemy (Apocalypse Dragon) shows its composite face as the leading thumbnail.
    val possessed = entry.possessedToken()
    // Defeated/spent rows are de-emphasised (dimmed name), since the on-board enemies are what still
    // needs attention.
    val nameColor = if (entry.defeated) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val hasLeadingFace = ruin != null || reward != null || summonedChildToken != null || (possessed != null && token != null)
    // A reward token's "defeated" flag means "spent", so its toggle reads differently.
    val doneDesc = when {
        reward != null -> if (entry.defeated) "Spent" else "Mark spent"
        else -> if (entry.defeated) "Defeated" else "Mark defeated"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A ruin shows its own hexagonal face; a reward shows its square tile face; an enemy row
            // instead shows its current Summon Draw child (if any) as the leading thumbnail.
            if (ruin != null) {
                RuinTokenFace(ruin = ruin, size = 32.dp)
            } else if (reward != null) {
                FactionRewardTokenFace(token = reward, size = 32.dp)
            } else if (possessed != null && token != null) {
                PossessedEnemyFace(circular = token, possessed = possessed, size = 32.dp)
            } else if (summonedChildToken != null) {
                EnemyTokenFace(token = summonedChildToken, size = 32.dp)
            }
            Column(Modifier.weight(1f).padding(start = if (hasLeadingFace) 8.dp else 0.dp)) {
                Text(
                    token?.name ?: ruin?.let { if (it.isAltar) "Ancient Altar" else "Enemies with Treasure" } ?: reward?.name ?: entry.tokenId,
                    style = MaterialTheme.typography.bodyLarge,
                    color = nameColor,
                )
                Text(
                    text = entry.pile.displayName() +
                        (if (possessed != null) " · Possessed" else "") +
                        (if (entry.note.isNotBlank()) " · ${entry.note}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onOpenToken(index) }) { Text("View") }
            IconButton(onClick = { onOpenDefeatDialog(index) }) {
                Icon(
                    imageVector = if (entry.defeated) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = doneDesc,
                    tint = if (entry.defeated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A collapsed Draw Log row for a batch of more than one token (#203/D18/D20): up to
 * [BATCH_ROW_THUMBNAIL_CAP] small art thumbnails (+N for the rest), an "N tokens drawn" title, and a
 * subtitle with the pile name - only when every member shares one, since #192 will eventually allow
 * a batch to span piles, and D16 already decided per-token pile labels aren't needed once art color
 * conveys it - plus on-board/defeated counts. No Defeat control of its own: defeating a batch member
 * only happens through the grid's own checkboxes ([TokenGridDialog]/D8), which [onOpenBatch] reopens
 * (tapping the row or its "View" button both fire it - either the whole card or just the button is a
 * fine tap target here, since there's nothing else on the row to conflict with).
 */
@Composable
private fun DrawLogBatchRow(group: DrawLogGroup, log: List<DrawLogEntry>, onOpenBatch: (List<Int>) -> Unit) {
    val entries = group.logIndices.map { log[it] }
    val onBoardCount = entries.count { !it.defeated }
    val defeatedCount = entries.size - onBoardCount
    val pileLabel = entries.map { it.pile }.distinct().singleOrNull()?.displayName()
    val subtitle = listOfNotNull(pileLabel, "$onBoardCount on board, $defeatedCount defeated").joinToString(" · ")

    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenBatch(group.logIndices) }) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                entries.take(BATCH_ROW_THUMBNAIL_CAP).forEach { entry ->
                    val token = TokenCatalogue.byId(entry.tokenId)
                    val possessed = entry.possessedToken()
                    if (token != null) {
                        // A possessed batch member shows its composite face, matching the grid/zoom.
                        if (possessed != null) PossessedEnemyFace(circular = token, possessed = possessed, size = 32.dp)
                        else EnemyTokenFace(token = token, size = 32.dp)
                    }
                }
                if (entries.size > BATCH_ROW_THUMBNAIL_CAP) {
                    Text(
                        "+${entries.size - BATCH_ROW_THUMBNAIL_CAP}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("${entries.size} tokens drawn", style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onOpenBatch(group.logIndices) }) { Text("View") }
        }
    }
}

/**
 * The config section: staged [Token Set][Expansion] checkboxes and the Draw with Replacement toggle,
 * committed together by "Apply & Reset" (so editing two expansions doesn't prompt twice), plus a
 * standalone "Reset piles" button. Staging is local state, seeded once from the current session.
 */
@Composable
private fun ConfigSection(
    currentTokenSet: Set<Expansion>,
    currentReplacement: Boolean,
    isBusy: Boolean,
    onRequestReset: () -> Unit,
    onRequestApplyConfig: (Set<Expansion>, Boolean) -> Unit,
) {
    // Seeded once from the live session; thereafter the user's edits are independent of draws.
    var stagedSet by remember { mutableStateOf(currentTokenSet) }
    var stagedReplacement by remember { mutableStateOf(currentReplacement) }

    val dirty = stagedSet != currentTokenSet || stagedReplacement != currentReplacement

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Setup", style = MaterialTheme.typography.titleLarge)
        Text("Which expansions' tokens are in this game's piles.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

        Expansion.entries.forEach { expansion ->
            LabeledCheckbox(
                label = expansion.displayName(),
                checked = expansion in stagedSet,
                onCheckedChange = { checked ->
                    stagedSet = if (checked) stagedSet + expansion else stagedSet - expansion
                },
            )
        }

        LabeledSwitch(
            label = "Draw with replacement",
            checked = stagedReplacement,
            onCheckedChange = { stagedReplacement = it },
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRequestReset, enabled = !isBusy) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Reset piles", modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = { onRequestApplyConfig(stagedSet, stagedReplacement) },
                // Apply only makes sense with a non-empty set and pending changes.
                enabled = !isBusy && dirty && stagedSet.isNotEmpty(),
            ) {
                Text("Apply & Reset")
            }
        }
    }
}

/**
 * Zoomed view of a drawn token (or a tapped log entry): its art (or text fallback), name, stat line
 * and attacks, a "?" to open the full ability info window, prev/next with an "x of y" counter when a
 * whole batch was drawn at once, a Summon/Re-summon button when the token has a Summon attack
 * (issue #191), and a Defeat button (D2/D11) that also closes the window (issue #227).
 *
 * [log] is the whole Draw Log so [state]'s indices can be resolved to entries; that lookup also
 * drives the Defeat button's own state (whether *this* entry is already defeated), since the same
 * dialog instance stays open across [onNavigate] calls as the user flips through a batch.
 * [currentChildrenOf] resolves a Summon Draw's current children (see `EnemyPickerSession`), used
 * both to label the button ("Summon" vs "Re-summon") and to list what's currently summoned - tapping
 * that list ([onViewSummoned]) re-opens the existing child(ren) without drawing again, distinct from
 * [onSummon] which always draws a fresh set.
 */
@Composable
private fun TokenZoomDialog(
    state: ZoomState,
    log: List<DrawLogEntry>,
    onNavigate: (Int) -> Unit,
    onShowInfo: (EnemyToken) -> Unit,
    onToggleDefeated: (Int, Boolean) -> Unit,
    onSummon: (Int) -> Unit,
    onViewSummoned: (List<Int>) -> Unit,
    currentChildrenOf: (Int) -> List<Int>,
    onDismiss: () -> Unit,
) {
    val logIndex = state.logIndices[state.index]
    val entry = log[logIndex]
    val token = TokenCatalogue.byId(entry.tokenId)
    val summonPiles = token?.attacks?.filter { it.isSummon } ?: emptyList()
    val currentChildren = if (summonPiles.isEmpty()) emptyList() else currentChildrenOf(logIndex)
    AlertDialog(
        onDismissRequest = onDismiss,
        // Material3's AlertDialog always end-aligns its confirmButton/dismissButton row, with no
        // way to center it - so Close/Defeat are built as an ordinary centered Row inside `text`
        // instead (below), and this slot is left empty to satisfy the required parameter.
        confirmButton = {},
        title = {
            // Box (not a Row) so the name can be centered across the full width via its own
            // fillMaxWidth + TextAlign.Center, with the "?" button floated at the end on top of
            // it - a Row would instead push the name off-center to make room for the button.
            Box(Modifier.fillMaxWidth()) {
                Text(
                    text = token?.name ?: entry.tokenId,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                if (token != null) {
                    IconButton(onClick = { onShowInfo(token) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Filled.QuestionMark, contentDescription = "Abilities")
                    }
                }
            }
        },
        text = {
            // fillMaxWidth so horizontalAlignment actually centers each line across the dialog's
            // full content width - without it the Column shrink-wraps to its widest child and
            // shorter lines (stat line, attacks) end up flush-left instead of centered.
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // A possessed enemy (Apocalypse Dragon): resolve the possessed token so the face
                // superimposes the circular enemy on it and the stats below are the *summed* values
                // (never the deltas). Null for an ordinary draw. See docs/rules/apocalypse-dragon.md.
                val possessed = entry.possessedToken()
                val stats = if (token != null && possessed != null) PossessedEnemy.combine(token, possessed) else null
                if (token != null) {
                    if (possessed != null) {
                        PossessedEnemyFace(circular = token, possessed = possessed, size = 140.dp)
                        Text(
                            "Possessed enemy",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        EnemyTokenFaceWithSummon(
                            token = token,
                            size = 140.dp,
                            // Left side, not the default corner (D-issue #191 follow-up note): that's
                            // where a printed Summon attack shows its pile-color icon, so the current
                            // child's own face standing in that exact spot reads as "this is who's
                            // actually fighting" rather than a decorative badge. All current children are
                            // passed (not just the first) so a double-summoner shows both over its two slots.
                            summonedChildren = currentChildren.mapNotNull { TokenCatalogue.byId(log[it].tokenId) },
                            alignment = Alignment.CenterStart,
                        )
                    }
                    // Stat line and attacks show the summed values when possessed, the token's own otherwise.
                    Text(stats?.statLine() ?: token.statLine(), style = MaterialTheme.typography.titleMedium)
                    (stats?.attacks ?: token.attacks).forEach { attack ->
                        if (attack.isSummon) {
                            // A summon has no attack value - it draws a replacement from another pile.
                            Text("Summons a ${attack.summons?.summonName() ?: "token"}")
                        } else {
                            Text("Attack ${attack.value} · ${attack.element.displayName()}")
                        }
                    }
                    // The possessed token's added Psychic Attack (elementless; ignores offensive abilities).
                    stats?.psychicAttack?.let { psychic ->
                        Text(
                            "Psychic Attack $psychic",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    // Offensive abilities (Brutal, Swift, ...) are whole-token, so they're shown once
                    // beneath the attack(s) rather than tacked onto each. Full text is in the "?" window.
                    if (token.offensiveAbilities.isNotEmpty()) {
                        Text(
                            token.offensiveAbilities.joinToString { it.describe().first },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Every possessed enemy rewards a Faction token when defeated - a faction-agnostic
                    // reminder here (the faction is named by the scenario and tracked by the separate
                    // faction-token feature, not the picker).
                    if (possessed != null) {
                        Text(
                            "Reward: a Faction token",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (summonPiles.isNotEmpty()) {
                    if (currentChildren.isNotEmpty()) {
                        val names = currentChildren.joinToString { TokenCatalogue.byId(log[it].tokenId)?.name ?: log[it].tokenId }
                        // Tapping re-opens the existing child(ren) (their own zoom, read-only) without
                        // drawing again - underlined + primary-colored so it reads as tappable, distinct
                        // from the Summon/Re-summon button below which always draws a fresh set.
                        Text(
                            "Summoned: $names",
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onViewSummoned(currentChildren) },
                        )
                    }
                    Button(onClick = { onSummon(logIndex) }, enabled = !entry.defeated) {
                        Text(if (currentChildren.isEmpty()) "Summon" else "Re-summon")
                    }
                }
                if (state.logIndices.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onNavigate(state.index - 1) }, enabled = state.index > 0) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                        }
                        Text("${state.index + 1} of ${state.logIndices.size}")
                        IconButton(onClick = { onNavigate(state.index + 1) }, enabled = state.index < state.logIndices.size - 1) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                }
                // D11: Defeat is the primary (filled) action; Close stays a plain text button
                // beside it, in that left-to-right order. Built here instead of in AlertDialog's
                // confirmButton/dismissButton slots so the pair can be centered as a group.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    // Defeating also closes the window (issue #227): you almost always want the
                    // just-defeated enemy's zoom gone immediately, so folding the Close tap into
                    // Defeat spares the extra click. If this zoom was drilled into from a grid,
                    // onDismiss falls back to that grid (see onDismiss's own comment) rather than
                    // closing everything, so the next batch member is still one tap away.
                    Button(
                        onClick = { onToggleDefeated(logIndex, true); onDismiss() },
                        enabled = !entry.defeated,
                    ) {
                        Text(if (entry.defeated) "Defeated" else "Defeat")
                    }
                }
            }
        },
    )
}

/**
 * A Summon Draw child's own zoomed view (issue #191): art, attacks and offensive abilities,
 * Close-only - narrower than [TokenZoomDialog] in two ways. First, no Armor/Fame/resistances/
 * defensive abilities: a child is narrated by what it *does* to you (attacks, offensive abilities),
 * not what it withstands - that fuller reference is still one "?" tap away via [TokenInfoDialog].
 * Second, no Defeat or Summon actions - a child is never independently marked Defeated (the
 * summoner's own Defeat flag resolves the whole encounter) and never itself offers a Summon action -
 * so this is a deliberately simpler sibling rather than [TokenZoomDialog] with flags threaded
 * through it for a case that only ever reads, never acts.
 */
@Composable
private fun SummonedChildZoomDialog(
    state: ZoomState,
    log: List<DrawLogEntry>,
    onNavigate: (Int) -> Unit,
    onShowInfo: (EnemyToken) -> Unit,
    onDismiss: () -> Unit,
) {
    val entry = log[state.logIndices[state.index]]
    val token = TokenCatalogue.byId(entry.tokenId)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Box(Modifier.fillMaxWidth()) {
                // "(Summoned)" suffix - unlike TokenZoomDialog's title, this dialog is always a
                // Summon Draw child, so the title itself says so rather than requiring the "Summon"
                // text on the parent's own dialog to be remembered. End padding reserves room for
                // the "?" button below so this longer title doesn't run into it before centering.
                Text(
                    text = "${token?.name ?: entry.tokenId} (Summoned)",
                    modifier = Modifier.fillMaxWidth().padding(end = 40.dp),
                    textAlign = TextAlign.Center,
                )
                if (token != null) {
                    IconButton(onClick = { onShowInfo(token) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Filled.QuestionMark, contentDescription = "Abilities")
                    }
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (token != null) {
                    EnemyTokenFace(token = token, size = 140.dp)
                    // Deliberately no statLine() here (Armor/Fame) and no resistances/defensive
                    // abilities, unlike the summoner's own zoom - a child is narrated by what it
                    // *does* (its attacks, its offensive abilities), not what it withstands; the
                    // full reference (including Armor/Fame/resistances) is still one "?" tap away
                    // via TokenInfoDialog for whoever wants it.
                    // If a *possessed* summoner drew this child, the possessed Attack delta applies to
                    // this token's topmost attack instead of the summoner's (rulebook p.7), so the
                    // number shown is the summed one.
                    // Only the summed number is shown, never the delta (the "no deltas alongside
                    // sums" rule) - the possessed summoner's boost is already baked into the value.
                    val summonDelta = summonDeltaFor(state.logIndices[state.index], log)
                    PossessedEnemy.withTopmostAttackDelta(token.attacks, summonDelta).forEach { attack ->
                        if (attack.isSummon) {
                            Text("Summons a ${attack.summons?.summonName() ?: "token"}")
                        } else {
                            Text("Attack ${attack.value} · ${attack.element.displayName()}")
                        }
                    }
                    if (token.offensiveAbilities.isNotEmpty()) {
                        Text(
                            token.offensiveAbilities.joinToString { it.describe().first },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.logIndices.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onNavigate(state.index - 1) }, enabled = state.index > 0) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                        }
                        Text("${state.index + 1} of ${state.logIndices.size}")
                        IconButton(onClick = { onNavigate(state.index + 1) }, enabled = state.index < state.logIndices.size - 1) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

/**
 * Zoomed view of a drawn Ruin token (issue #201) - the RUIN pile's counterpart to [TokenZoomDialog],
 * since a ruin prints no armor/attack/fame block. An **Ancient Altar** shows its mana-payment prompt
 * and the Fame it grants (derived, never scored - ADR-0006); an **Enemies With Treasure** shows its
 * draw instruction, its printed [RuinToken.reward] (flavour/reference only), and a one-shot "Draw its
 * enemies" button that draws the prescribed enemies via [onDrawEnemies] and attaches them under this
 * ruin. Once drawn, that button is replaced by a tappable "Enemies: …" line ([onViewEnemies]) - a
 * ruin group is drawn once and stays put (no re-draw). Both kinds carry the same Defeat + Close pair
 * as the enemy zoom, marking the whole ruin resolved.
 */
@Composable
private fun RuinZoomDialog(
    ruin: RuinToken,
    entry: DrawLogEntry,
    logIndex: Int,
    currentEnemies: List<Int>,
    enemyName: (Int) -> String,
    onDrawEnemies: (Int) -> Unit,
    onViewEnemies: (List<Int>) -> Unit,
    onToggleDefeated: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = if (ruin.isAltar) "Ancient Altar" else "Enemies with Treasure",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RuinTokenFace(ruin = ruin, size = 140.dp)

                if (ruin.isAltar) {
                    Text(ruin.altarPrompt(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text("Gain ${ruin.altarFame()} Fame", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(ruin.enemyDrawPrompt(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    // Reward is reference text only (ADR-0006 amended to display, not track it).
                    ruin.reward?.let { Text("Reward: $it", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }

                    if (currentEnemies.isEmpty()) {
                        Button(onClick = { onDrawEnemies(logIndex) }, enabled = !entry.defeated) {
                            Text("Draw its enemies")
                        }
                    } else {
                        // A ruin group is one-shot: once its enemies are out, offer to re-open them
                        // (full stats, per-enemy Defeat) rather than draw a fresh set.
                        Text(
                            "Enemies: " + currentEnemies.joinToString { enemyName(it) },
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable { onViewEnemies(currentEnemies) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    // Defeating a ruin (paid the altar / cleared its enemies) closes the window too,
                    // matching the enemy zoom (issue #227).
                    Button(
                        onClick = { onToggleDefeated(logIndex, true); onDismiss() },
                        enabled = !entry.defeated,
                    ) {
                        Text(if (entry.defeated) "Resolved" else "Resolve")
                    }
                }
            }
        },
    )
}

/**
 * Zoomed view of a drawn (or revisited) faction reward token (issue #252) - the reward piles'
 * counterpart to [TokenZoomDialog]/[RuinZoomDialog], since a reward token prints no armor/attack/fame
 * and no altar/enemy-draw prompt, only a one-off effect. Shows its square tile face, its effect text
 * (reference only, never resolved - ADR-0006), the universal discard-for-Fame/Influence footer, and a
 * **Spend** action - the reward's "Defeat", flipping the same [DrawLogEntry.defeated] flag so a spent
 * token drops out of the held pinned entry into the dimmed history (interim behaviour; issue #251
 * makes that flag pile-correct). Spending also closes the window, matching the enemy/ruin zooms.
 */
@Composable
private fun FactionRewardZoomDialog(
    token: FactionRewardToken,
    entry: DrawLogEntry,
    logIndex: Int,
    onToggleSpent: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(token.name, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FactionRewardTokenFace(token = token, size = 140.dp)
                Text(token.effectText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                // The line printed on every reward token, held once here rather than in each token's
                // effectText (see FactionRewardToken's doc) - shown so the player knows the option.
                Text(
                    FACTION_REWARD_DISCARD_FOOTER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Button(
                        onClick = { onToggleSpent(logIndex, true); onDismiss() },
                        enabled = !entry.defeated,
                    ) {
                        Text(if (entry.defeated) "Spent" else "Spend")
                    }
                }
            }
        },
    )
}

/**
 * The held-faction-rewards grid (issue #252): every currently-held reward token, reached from the
 * pinned Draw Log entry. One cell per held token (face + name + a **Spend** checkbox); tapping the
 * cell opens that token's [FactionRewardZoomDialog] detail. Spending a token flips its
 * [DrawLogEntry.defeated] flag, which drops it out of [heldIndices] on the next recomposition, so it
 * leaves this grid and joins the dimmed history. Reuses the same adaptive grid as [TokenGridDialog].
 */
@Composable
private fun HeldRewardsDialog(
    heldIndices: List<Int>,
    log: List<DrawLogEntry>,
    onOpenDetail: (Int) -> Unit,
    onSpend: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Faction rewards held") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
                modifier = Modifier.heightIn(max = GRID_MAX_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(heldIndices, key = { it }) { logIndex ->
                    val entry = log[logIndex]
                    HeldRewardCell(
                        token = FactionRewardTokenCatalogue.byId(entry.tokenId),
                        tokenId = entry.tokenId,
                        onOpen = { onOpenDetail(logIndex) },
                        onSpend = { onSpend(logIndex) },
                    )
                }
            }
        },
    )
}

/** One held-reward grid cell: the token's face, its name, and a Spend checkbox (issue #252). A held
 * token is by definition un-spent, so the checkbox is always the empty/outlined state - tapping it
 * spends the token. */
@Composable
private fun HeldRewardCell(token: FactionRewardToken?, tokenId: String, onOpen: () -> Unit, onSpend: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (token != null) {
            FactionRewardTokenFace(token = token, size = 72.dp)
        }
        Text(
            text = token?.name ?: tokenId,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onSpend) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Spend",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Read-only detail for a faction reward token (issue #252) - the reward piles' counterpart to
 * [RuinInfoDialog], opened by tapping a reward cell in [PileContentsDialog]'s pile survey. Same face
 * + effect + discard footer as [FactionRewardZoomDialog] but with none of its Spend action - this is
 * a preview of what's still in the pile, not a held token.
 */
@Composable
private fun RewardInfoDialog(token: FactionRewardToken, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(token.name) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FactionRewardTokenFace(token = token, size = 140.dp)
                Text(token.effectText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Text(
                    FACTION_REWARD_DISCARD_FOOTER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

/**
 * Grid overview of a multi-token draw batch (D3/D7), possibly spanning several piles at once
 * (D16): one cell per drawn token with art, name and (when [showDefeatToggle]) a Defeat toggle;
 * tapping a cell opens that token's own zoomed detail with prev/next across the same batch. Sized
 * to content with a max height (D10) so a rare large batch scrolls internally instead of pushing
 * the dialog off-screen, rather than always reserving full-screen space for the common small batch.
 *
 * Reused for a Summon Draw's own multi-child result (issue #191) with [showDefeatToggle] = false -
 * a summoned child is never independently marked Defeated (`CONTEXT.md`'s "Summon Draw") - and a
 * caller-supplied [title], since that case reads "N tokens summoned" rather than "drawn". Left at
 * its default [currentChildOf] for that same call, since a child cell never has children of its own.
 */
@Composable
private fun TokenGridDialog(
    state: GridState,
    log: List<DrawLogEntry>,
    title: String,
    showDefeatToggle: Boolean,
    onOpenDetail: (Int) -> Unit,
    onToggleDefeated: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
    currentChildOf: (Int) -> DrawLogEntry? = { null },
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(title) },
        text = {
            // Adaptive columns (not a fixed count) so 2-6 tokens - the common case - render as a
            // comfortably large grid, while a rare large batch just packs more/smaller columns
            // automatically instead of needing separate tuning (D9).
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
                modifier = Modifier.heightIn(max = GRID_MAX_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // position = index within this batch (for prev/next); logIndex = the entry's real
                // Draw Log index (for reading/writing its defeated flag).
                itemsIndexed(state.logIndices) { position, logIndex ->
                    TokenGridCell(
                        entry = log[logIndex],
                        showDefeatToggle = showDefeatToggle,
                        summonedChild = currentChildOf(logIndex)?.let { TokenCatalogue.byId(it.tokenId) },
                        onOpen = { onOpenDetail(position) },
                        onToggleDefeated = { defeated -> onToggleDefeated(logIndex, defeated) },
                    )
                }
            }
        },
    )
}

/** One grid cell: art + name (dimmed once defeated, matching the Draw Log's own treatment) and,
 * when [showDefeatToggle], a Defeat toggle icon matching [DrawLogRow]'s. A non-null [summonedChild]
 * (issue #191) superimposes a small thumbnail of it on the art's corner, the same "which token is
 * actually fighting" cue `CONTEXT.md`'s Possessed Enemy pairing uses. The cell's own
 * [Modifier.clickable] opens the detail view; it sits underneath the toggle's own clickable, which
 * intercepts taps on itself first, so tapping the icon toggles Defeat instead of also opening the
 * detail. */
@Composable
private fun TokenGridCell(
    entry: DrawLogEntry,
    showDefeatToggle: Boolean,
    summonedChild: EnemyToken?,
    onOpen: () -> Unit,
    onToggleDefeated: (Boolean) -> Unit,
) {
    val token = TokenCatalogue.byId(entry.tokenId)
    Column(
        // fillMaxWidth (not a fixed width) so the cell matches whatever slot width Adaptive chose
        // for this row - which can be wider than GRID_CELL_MIN_SIZE once it divides evenly.
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.alpha(if (entry.defeated) 0.5f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (token != null) {
                val possessed = entry.possessedToken()
                if (possessed != null) {
                    // A possessed enemy shows its composite face (circular over the possessed token).
                    PossessedEnemyFace(circular = token, possessed = possessed, size = 72.dp)
                } else {
                    // The grid/log cell keeps a single small corner badge even for a double-summoner -
                    // both children are still reachable via the row's "Summoned: …" line and the zoom.
                    EnemyTokenFaceWithSummon(token = token, size = 72.dp, summonedChildren = listOfNotNull(summonedChild))
                }
            }
            Text(
                text = token?.name ?: entry.tokenId,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDefeatToggle) {
            IconButton(onClick = { onToggleDefeated(!entry.defeated) }) {
                Icon(
                    imageVector = if (entry.defeated) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (entry.defeated) "Defeated" else "Mark defeated",
                    tint = if (entry.defeated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * [EnemyTokenFace] with each of [summonedChildren]'s own art superimposed (issue #191) - the same
 * "which token is actually fighting" cue `CONTEXT.md`'s Possessed Enemy pairing uses, applied to a
 * summoner once it has current Summon Draw children. No overlay when the list is empty (never
 * summoned, or not a summoner at all). [alignment] defaults to a small corner badge (grid cells,
 * Draw Log row); the zoom dialog instead pins it to the art's left side - where a printed summon
 * token shows its pile-color icon - so it reads as "this is what's standing in" rather than a
 * decorative badge.
 *
 * Several children stack in a column along that same edge, so a *double* summoner (the Lost Legion
 * Dragon Summoner, the only token with two Summon attacks) shows both replacements roughly over its
 * two printed summon slots rather than hiding one behind the other. A single child is just a
 * one-item column, identical to the pre-#188 single-badge layout.
 */
@Composable
private fun EnemyTokenFaceWithSummon(
    token: EnemyToken,
    size: Dp,
    summonedChildren: List<EnemyToken>,
    alignment: Alignment = Alignment.BottomEnd,
) {
    Box {
        EnemyTokenFace(token = token, size = size)
        if (summonedChildren.isNotEmpty()) {
            // A column centered on the aligned edge: with two children it splits into an upper and a
            // lower badge, which lands them over the summoner's two stacked summon-token slots.
            Column(
                modifier = Modifier.align(alignment),
                verticalArrangement = Arrangement.spacedBy(size * SUMMON_STACK_GAP),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                summonedChildren.forEach { child ->
                    Box(Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)) {
                        EnemyTokenFace(token = child, size = size * SUMMON_BADGE_SCALE)
                    }
                }
            }
        }
    }
}

/** The "?" info window: every whole-token ability and per-attack modifier, with its rules text. */
@Composable
private fun TokenInfoDialog(token: EnemyToken, onDismiss: () -> Unit) {
    // Gather each ability/resistance on this token, with its description. Resistances and defensive
    // abilities describe how it's attacked; offensive abilities modify its own attacks - all are
    // whole-token (they apply to every attack), so they're listed once each, not per attack.
    val lines = buildList {
        token.resistances.forEach { add(it.displayName() + " Resistance" to "Attacks of this element are inefficient (halved).") }
        token.defensiveAbilities.forEach { add(it.describe()) }
        // Defend is a valued defensive trait (a number, not an enum), so it's described inline here
        // rather than via DefensiveAbility.describe() (Shades of Tezla, "New Enemy Token Abilities").
        token.defend?.let { add("Defend $it" to "The first enemy you attack this combat has its Armor raised by $it until the end of the combat.") }
        token.offensiveAbilities.forEach { add(it.describe()) }
        // Summon isn't an ability - it's a whole different kind of attack - so describe it here.
        token.attacks.filter { it.isSummon }.forEach {
            add("Summon" to "At the start of the Block phase, draws a ${it.summons?.summonName() ?: "token"} token to fight in its place.")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("${token.name} · abilities") },
        text = {
            if (lines.isEmpty()) {
                Text("This enemy has no special abilities.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lines.forEach { (name, desc) ->
                        Column {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
    )
}

/**
 * The "view draw pile" dialog (issue #231): the still-face-down contents of one [pile], grouped by
 * identity (art + a "×N" count badge) and sorted alphabetically by name via [composePile], so a
 * player can survey what they might still face and plan combat. Deliberately shows *unordered*
 * composition - never draw order - so it can't reveal the next token. Tapping an enemy cell opens
 * its ability window ([onOpenEnemyInfo]); a ruin cell opens its read-only detail ([onOpenRuinInfo]).
 * Under [withReplacement] the pile never depletes, so the title frames it as the full pile rather
 * than "remaining".
 */
@Composable
private fun PileContentsDialog(
    pileId: TokenPileId,
    pile: TokenPile,
    withReplacement: Boolean,
    onOpenEnemyInfo: (EnemyToken) -> Unit,
    onOpenRuinInfo: (RuinToken) -> Unit,
    onOpenRewardInfo: (FactionRewardToken) -> Unit,
    onDismiss: () -> Unit,
) {
    val isRuin = pileId == TokenPileId.RUIN
    val isReward = pileId.isFactionReward
    val isPossessed = pileId == TokenPileId.POSSESSED
    // Name resolver for the sort, drawn from whichever catalogue this pile uses. A ruin has no single
    // "name" field (labelled by kind), a reward carries its own name, and a possessed token has none
    // either (labelled by its modifier summary, "+2 Armor · +3 Psychic").
    val nameOf: (String) -> String = { id ->
        when {
            isRuin -> RuinTokenCatalogue.byId(id)?.let { if (it.isAltar) "Ancient Altar" else "Enemies with Treasure" } ?: id
            isReward -> FactionRewardTokenCatalogue.byId(id)?.name ?: id
            isPossessed -> PossessedTokenCatalogue.byId(id)?.summary() ?: id
            else -> TokenCatalogue.byId(id)?.name ?: id
        }
    }
    // What's left to draw is the draw pile *plus* the discard: defeated tokens reshuffle back on a
    // Replenish, so they're still tokens you might face; only on-board tokens (in neither list) are
    // out for good (issue #251). Shown unordered, so combining the two never reveals draw order.
    // remember keyed on both lists so the grouping recomputes when either changes.
    val composition = remember(pile.drawPile, pile.discardPile) {
        composePile(pile.drawPile + pile.discardPile, nameOf)
    }
    val title = if (withReplacement) "${pileId.displayName()} · full pile (${composition.total})"
    else "${pileId.displayName()} · ${composition.total} remaining"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(title) },
        text = {
            if (composition.groups.isEmpty()) {
                // Empty draw pile *and* discard means every token drawn from this pile is still on
                // the board - the pile is genuinely empty until one is defeated back into it (#251).
                Text("Nothing left to draw — every token from this pile is on the board.")
            } else {
                // Same adaptive grid the draw-result overview uses (D9), for a consistent look.
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
                    modifier = Modifier.heightIn(max = GRID_MAX_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(composition.groups, key = { it.tokenId }) { group ->
                        PileContentsCell(
                            group = group,
                            isRuin = isRuin,
                            isReward = isReward,
                            isPossessed = isPossessed,
                            onOpen = {
                                // Possessed tokens have no detail dialog (they're modifiers, not
                                // enemies), so their cells just display.
                                when {
                                    isRuin -> RuinTokenCatalogue.byId(group.tokenId)?.let(onOpenRuinInfo)
                                    isReward -> FactionRewardTokenCatalogue.byId(group.tokenId)?.let(onOpenRewardInfo)
                                    isPossessed -> Unit
                                    else -> TokenCatalogue.byId(group.tokenId)?.let(onOpenEnemyInfo)
                                }
                            },
                        )
                    }
                }
            }
        },
    )
}

/**
 * One cell of the [PileContentsDialog] grid: a token's art with a "×N" badge when more than one
 * copy remains (a lone copy needs no count), its name below, tappable to open detail. Ruin ids
 * render the hexagonal [RuinTokenFace]; everything else the round [EnemyTokenFace].
 */
@Composable
private fun PileContentsCell(group: PileTokenGroup, isRuin: Boolean, isReward: Boolean, isPossessed: Boolean, onOpen: () -> Unit) {
    val token = if (isRuin || isReward || isPossessed) null else TokenCatalogue.byId(group.tokenId)
    val ruin = if (isRuin) RuinTokenCatalogue.byId(group.tokenId) else null
    val reward = if (isReward) FactionRewardTokenCatalogue.byId(group.tokenId) else null
    val possessed = if (isPossessed) PossessedTokenCatalogue.byId(group.tokenId) else null
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Box so the count badge can overlay the art's corner.
        Box(contentAlignment = Alignment.Center) {
            when {
                token != null -> EnemyTokenFace(token = token, size = 72.dp)
                ruin != null -> RuinTokenFace(ruin = ruin, size = 72.dp)
                reward != null -> FactionRewardTokenFace(token = reward, size = 72.dp)
                possessed != null -> PossessedTokenFace(possessed = possessed, size = 72.dp)
                else -> Text(group.tokenId, style = MaterialTheme.typography.labelSmall)
            }
            if (group.count > 1) {
                CountBadge(count = group.count, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
        Text(
            text = group.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small "×N" pill overlaid on a pile-contents cell when several copies of a token remain. */
@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "×$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * Read-only detail for a ruin token (issue #231) - the RUIN pile's counterpart to [TokenInfoDialog],
 * opened by tapping a ruin cell in [PileContentsDialog]. Shows the same face and prompt as
 * [RuinZoomDialog] (altar payment + Fame, or the Enemies-With-Treasure draw line + reward) but with
 * none of its draw/Defeat actions - this is a preview of pile contents, not a drawn ruin.
 */
@Composable
private fun RuinInfoDialog(ruin: RuinToken, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(if (ruin.isAltar) "Ancient Altar" else "Enemies with Treasure") },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RuinTokenFace(ruin = ruin, size = 140.dp)
                if (ruin.isAltar) {
                    Text(ruin.altarPrompt(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text("Gain ${ruin.altarFame()} Fame", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(ruin.enemyDrawPrompt(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    ruin.reward?.let { Text("Reward: $it", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
                }
            }
        },
    )
}

/** The Defeat dialog: a Defeated toggle plus a free-text note (for enemies kept on the board),
 * saved together. This is the per-log-row entry point (note-editing needs the dialog); the zoom
 * and grid views instead use a one-tap Defeat button/checkbox with no note. */
@Composable
private fun DefeatDialog(entry: DrawLogEntry, onSave: (Boolean, String) -> Unit, onDismiss: () -> Unit) {
    var defeated by remember { mutableStateOf(entry.defeated) }
    var note by remember { mutableStateOf(entry.note) }
    val token = TokenCatalogue.byId(entry.tokenId)
    val reward = if (token == null) FactionRewardTokenCatalogue.byId(entry.tokenId) else null
    val ruin = if (token == null && reward == null) RuinTokenCatalogue.byId(entry.tokenId) else null
    val title = token?.name
        ?: reward?.name
        ?: ruin?.let { if (it.isAltar) "Ancient Altar" else "Enemies with Treasure" }
        ?: entry.tokenId
    // A reward token's "defeated" flag means "spent" (issue #252); a ruin's means "resolved".
    val doneLabel = when {
        reward != null -> "Spent"
        ruin != null -> "Resolved"
        else -> "Defeated"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(defeated, note) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledSwitch(label = doneLabel, checked = defeated, onCheckedChange = { defeated = it })
                // The note is for tracking an enemy still on the board ("keep, NE tile"), so it's
                // only editable while the enemy is *not* marked defeated.
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. \"keep, NE tile\")") },
                    enabled = !defeated,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

/** Largest number of tokens a single stepper draw allows - a sanity cap, well above any real need. */
private const val MAX_BATCH = 20

/**
 * The line printed on **every** faction reward token (issue #252): it can always be cashed in instead
 * of used. Held once here rather than in each token's [FactionRewardToken.effectText] (it's identical
 * across all 24 token types / 48 tokens), and shown as a footer under whichever reward is on screen. Not
 * scored - the player takes the Fame/Influence themselves (ADR-0006).
 */
private const val FACTION_REWARD_DISCARD_FOOTER = "Or discard during interactions for 1 Fame or 3 Influence."

/** Grid cell min width (D9): sized so the common 2-6 token batch reads as a comfortably large grid. */
private val GRID_CELL_MIN_SIZE = 96.dp

/** Grid dialog max height (D10): a batch taller than this scrolls internally instead of growing the dialog. */
private val GRID_MAX_HEIGHT = 400.dp

/** Size of a superimposed summoned-child thumbnail (issue #191), relative to the summoner's own art. */
private const val SUMMON_BADGE_SCALE = 0.45f

/** Most thumbnails a collapsed Draw Log batch row shows before summarizing the rest as "+N" (D20). */
private const val BATCH_ROW_THUMBNAIL_CAP = 4

/**
 * Vertical spacing between stacked summoned-child badges (a two-summon token), relative to the art
 * size. **Negative on purpose**: the badges slightly overlap (the lower one drawn on top). A
 * summoned token is narrated only by its attacks/abilities, never its Armor/Fame, so overlapping
 * those printed numbers costs nothing and keeps the pair reading as one clustered "who's fighting" cue.
 */
private const val SUMMON_STACK_GAP = -0.10f

/**
 * "Armor 3 · Fame 2" summary line for a token. Deliberately *excludes* the attack (D5): the
 * attack(s) are listed in full just below this line in the zoom, so repeating them here was
 * redundant. An Elusive token shows both Armor values as "3/6" (lower/higher) - the higher one is
 * what a player faces until they block all its attacks (Lost Legion, "Elusive"), so hiding it would
 * be misleading; the "Elusive" ability entry in the "?" window explains which value applies when.
 * A token with a printed Reputation change (Lost Legion Thugs/Heroes) appends it as "· Reputation
 * +1" / "-1"; the common `reputation == 0` case adds nothing. A Shades of Tezla token with the
 * Defend ability appends "· Defend N" beside its Armor (the value it adds to the first enemy
 * attacked); the "Defend N" entry in the "?" window explains the timing.
 */
private fun EnemyToken.statLine(): String {
    val armorText = if (elusiveArmor != null) "$armor/$elusiveArmor" else "$armor"
    // Defend sits beside Armor since it's an Armor-boosting defensive trait (Shades of Tezla).
    val defendText = if (defend != null) " · Defend $defend" else ""
    // "%+d" formats with an explicit sign (+1 / -1), matching the token's own +/- Reputation icon.
    val reputationText = if (reputation != 0) " · Reputation %+d".format(reputation) else ""
    return "Armor $armorText$defendText · Fame $fame$reputationText"
}

/** The possessed token paired with this entry (an Apocalypse Dragon composite), or null for an ordinary draw. */
private fun DrawLogEntry.possessedToken(): PossessedToken? = possessedTokenId?.let { PossessedTokenCatalogue.byId(it) }

/**
 * "Armor X · Fame Y" summary for a possessed enemy's *summed* stats (docs/rules/apocalypse-dragon.md)
 * - the composite counterpart to [EnemyToken.statLine]. Shows both Armor values as "lower/higher"
 * when the circular enemy is Elusive (its higher value is boosted by the same Armor delta). The
 * attack(s) and Psychic Attack are listed separately in the zoom, so - like [EnemyToken.statLine] -
 * they're left off this line.
 */
private fun PossessedEnemyStats.statLine(): String {
    val armorText = if (elusiveArmor != null) "$armor/$elusiveArmor" else "$armor"
    // Defend / Reputation are carried through from the circular enemy (a possessed Shades or Lost
    // Legion enemy still prints them), mirroring EnemyToken.statLine().
    val defendText = if (defend != null) " · Defend $defend" else ""
    val reputationText = if (reputation != 0) " · Reputation %+d".format(reputation) else ""
    return "Armor $armorText$defendText · Fame $fame$reputationText"
}

/**
 * The possessed Attack delta a summoned child inherits from its summoner, or 0. When a **possessed
 * summoner**'s topmost attack is a Summon, its Attack delta modifies the *summoned* token's topmost
 * attack instead (rulebook p.7); this resolves that delta from the child's parent entry so the child
 * zoom can apply it via [PossessedEnemy.withTopmostAttackDelta]. Returns 0 for any child whose parent
 * isn't a possessed summoner.
 */
private fun summonDeltaFor(entryIndex: Int, log: List<DrawLogEntry>): Int {
    val entry = log[entryIndex]
    val parentIndex = entry.parentIndex ?: return 0
    val parent = log.getOrNull(parentIndex) ?: return 0
    val parentToken = TokenCatalogue.byId(parent.tokenId) ?: return 0
    val possessed = parent.possessedToken() ?: return 0
    // Rulebook p.7: the possessed Attack delta modifies only the *topmost* summon's summoned token.
    // Children of one summon action share a batchId and are appended in summon-slot order, so the
    // topmost is the first (lowest index) among this child's batch siblings; a second Summon slot
    // (the Lost Legion Dragon Summoner) gets nothing.
    val topmostChild = log.indices.firstOrNull { log[it].parentIndex == parentIndex && log[it].batchId == entry.batchId }
    if (topmostChild != entryIndex) return 0
    return PossessedEnemy.combine(parentToken, possessed).summonAttackDelta
}

/** Player-facing name of a summoned pile, e.g. "Brown enemy" (used in the zoom's "Summons a …" line). */
private fun TokenPileId.summonName(): String = when (this) {
    TokenPileId.BROWN -> "Brown enemy"
    else -> displayName()
}

/** Player-facing pile name. */
internal fun TokenPileId.displayName(): String = when (this) {
    TokenPileId.GREEN -> "Green enemies"
    TokenPileId.GREY -> "Grey enemies"
    TokenPileId.VIOLET -> "Violet enemies"
    TokenPileId.BROWN -> "Brown enemies"
    TokenPileId.RED -> "Red enemies"
    TokenPileId.WHITE -> "White enemies"
    TokenPileId.RUIN -> "Ruins"
    TokenPileId.POSSESSED -> "Possessed"
    TokenPileId.ELEMENTALIST_REWARDS -> "Elementalist rewards"
    TokenPileId.DARK_CRUSADER_REWARDS -> "Dark Crusader rewards"
    TokenPileId.APOCALYPSE_CULT_REWARDS -> "Apocalypse Cult rewards"
    TokenPileId.COUNCIL_OF_VOID_REWARDS -> "Council of the Void rewards"
}

/** Player-facing expansion name for the Token Set checkboxes. */
private fun Expansion.displayName(): String = when (this) {
    Expansion.BASE -> "Base game"
    Expansion.LOST_LEGION -> "The Lost Legion"
    // Shades of Tezla's enemies come in two factions; each is a separately-selectable token set
    // that mixes into the piles (see Expansion's doc comment), so it gets its own checkbox.
    Expansion.SHADES_OF_TEZLA_ELEMENTALIST -> "Shades of Tezla: Elementalist"
    Expansion.SHADES_OF_TEZLA_DARK_CRUSADER -> "Shades of Tezla: Dark Crusader"
    Expansion.APOCALYPSE_DRAGON -> "The Apocalypse Dragon"
}
