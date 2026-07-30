package com.guyteichman.mageknightbuddy.ui.enemypicker

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.guyteichman.mageknightbuddy.domain.TokenCatalogue
import com.guyteichman.mageknightbuddy.domain.TokenPile
import com.guyteichman.mageknightbuddy.domain.TokenPileId
import com.guyteichman.mageknightbuddy.ui.components.LabeledCheckbox
import com.guyteichman.mageknightbuddy.ui.components.LabeledSwitch
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
fun EnemyPickerTab(repository: EnemyPickerSessionRepository) {
    val viewModel: EnemyPickerViewModel = viewModel(factory = EnemyPickerViewModel.factory(repository))
    val scope = rememberCoroutineScope()

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
    // The Draw Log index whose Defeat dialog is open, or null.
    var defeatDialogIndex by remember { mutableStateOf<Int?>(null) }
    // A pending destructive reset (Reset piles / Apply & Reset), held until the user confirms.
    var pendingReset by remember { mutableStateOf<(() -> Unit)?>(null) }

    if (session == null) {
        // Still restoring / creating the first session.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Shared by the DrawBar's staged multi-pile draw and #198's per-card tap-to-draw-1 shortcut -
    // both just fire a `draws` map through the same viewModel.draw() call and open the same
    // zoom/grid result, so there is exactly one place that decides what a draw's result opens.
    val onDraw: (Map<TokenPileId, Int>) -> Unit = { draws ->
        scope.launch {
            viewModel.draw(draws)
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

    // The current summoned child of a summoner entry (first one, if a token ever has several
    // Summon attacks) - used to superimpose a small thumbnail of it wherever that summoner is shown
    // at a glance (issue #191). Null for an entry that was never summoned from.
    val currentChildOf: (Int) -> DrawLogEntry? = { index ->
        session.currentChildrenOf(index).firstOrNull()?.let { session.drawLog[it] }
    }

    EnemyPickerContent(
        session = session,
        isBusy = viewModel.isBusy,
        onDraw = onDraw,
        onOpenToken = { index -> zoom = ZoomState(listOf(index), 0); summonGrid = null; summonZoom = null },
        // Reopening a batch from the Draw Log (#203/D18) is fully identical to the grid a fresh
        // draw opens - same GridState, same TokenGridDialog, same D8 checkboxes/D10 dismiss - so
        // this just sets the same `gridState` `onDraw`'s N>1 branch does.
        onOpenBatch = { indices -> gridState = GridState(indices); zoom = null; summonGrid = null; summonZoom = null },
        onOpenDefeatDialog = { index -> defeatDialogIndex = index },
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
            onToggleDefeated = { index, defeated -> scope.launch { viewModel.setDefeated(index, defeated) } },
            onDismiss = { gridState = null; zoom = null; summonGrid = null; summonZoom = null },
        )
    }

    zoom?.let { state ->
        TokenZoomDialog(
            state = state,
            log = session.drawLog,
            onNavigate = { newIndex -> zoom = state.copy(index = newIndex); summonGrid = null; summonZoom = null },
            onShowInfo = { token -> infoToken = token },
            onToggleDefeated = { index, defeated -> scope.launch { viewModel.setDefeated(index, defeated) } },
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

    infoToken?.let { token ->
        TokenInfoDialog(token = token, onDismiss = { infoToken = null })
    }

    defeatDialogIndex?.let { index ->
        DefeatDialog(
            entry = session.drawLog[index],
            onSave = { defeated, note ->
                scope.launch { viewModel.setDefeated(index, defeated, note) }
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
    onDraw: (Map<TokenPileId, Int>) -> Unit,
    onOpenToken: (Int) -> Unit,
    onOpenBatch: (List<Int>) -> Unit,
    onOpenDefeatDialog: (Int) -> Unit,
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

    Scaffold(
        topBar = { EnemyPickerTopBar() },
        bottomBar = {
            DrawBar(
                total = totalQuantity,
                enabled = !isBusy && totalQuantity > 0,
                onDraw = {
                    onDraw(quantities.filterValues { it > 0 })
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
                        PileCard(
                            modifier = Modifier.weight(1f),
                            pileId = pileId,
                            pile = pile,
                            withReplacement = session.drawWithReplacement,
                            quantity = quantities[pileId] ?: 0,
                            onQuantityChange = { quantities = quantities + (pileId to it) },
                            // Tap-to-draw-1 (#198): independent of the staged stepper above -
                            // doesn't read or reset `quantities`, just fires its own one-entry draw.
                            // Same "anything left to draw" check the stepper's own `max` already
                            // makes: with replacement a pile never runs out; without, only disabled
                            // once the pile is entirely empty (draw + discard both zero).
                            canDrawOne = !isBusy &&
                                (session.drawWithReplacement || pile.drawPile.size + pile.discardPile.size > 0),
                            onDrawOne = { onDraw(mapOf(pileId to 1)) },
                        )
                    }
                }
            }

            item(key = "draw-log") {
                DrawLogSection(
                    log = session.drawLog,
                    onOpenToken = onOpenToken,
                    onOpenBatch = onOpenBatch,
                    onOpenDefeatDialog = onOpenDefeatDialog,
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
private fun EnemyPickerTopBar() {
    TopAppBar(title = { Text("Enemies") })
}

/**
 * The bottom bar's single global draw action (D13/D15): always visible, disabled while nothing is
 * staged (`total == 0`) rather than hidden, so the bar's presence never jumps around as steppers
 * change.
 */
@Composable
private fun DrawBar(total: Int, enabled: Boolean, onDraw: () -> Unit) {
    BottomAppBar {
        Button(
            onClick = onDraw,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text(if (total == 0) "Draw" else "Draw $total")
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
    withReplacement: Boolean,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    canDrawOne: Boolean,
    onDrawOne: () -> Unit,
) {
    val remaining = pile.drawPile.size
    val drawn = pile.discardPile.size

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
            Text(
                // With replacement, nothing depletes, so "drawn" would always be 0 - hide it.
                text = if (withReplacement) "Draws with replacement (never depletes)"
                else "$remaining in pile · $drawn drawn",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            QuantityStepper(
                quantity = quantity,
                onQuantityChange = onQuantityChange,
                // Without replacement, can't draw more than the whole pile (draw + discard) at once.
                max = if (withReplacement) MAX_BATCH else (remaining + drawn).coerceAtMost(MAX_BATCH),
            )
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
        val onBoard = groups.filter { !it.allDefeated(log) }
        val defeated = groups.filter { it.allDefeated(log) }

        // Only show the section headers once there's actually a split to label; a fresh log with
        // nothing defeated yet is just a flat newest-first list.
        val showHeaders = onBoard.isNotEmpty() && defeated.isNotEmpty()

        if (onBoard.isNotEmpty()) {
            if (showHeaders) Text("On the board", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            onBoard.forEach { group ->
                DrawLogGroupRow(group, log, onOpenToken, onOpenBatch, onOpenDefeatDialog, currentChildOf)
            }
        }
        if (defeated.isNotEmpty()) {
            Text(
                "Defeated",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            defeated.forEach { group ->
                DrawLogGroupRow(group, log, onOpenToken, onOpenBatch, onOpenDefeatDialog, currentChildOf)
            }
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
    val summonedChildToken = summonedChild?.let { TokenCatalogue.byId(it.tokenId) }
    // Defeated rows are de-emphasised (dimmed name), since the on-board enemies are what still needs
    // attention.
    val nameColor = if (entry.defeated) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (summonedChildToken != null) {
                EnemyTokenFace(token = summonedChildToken, size = 32.dp)
            }
            Column(Modifier.weight(1f).padding(start = if (summonedChildToken != null) 8.dp else 0.dp)) {
                Text(token?.name ?: entry.tokenId, style = MaterialTheme.typography.bodyLarge, color = nameColor)
                Text(
                    text = entry.pile.displayName() + (if (entry.note.isNotBlank()) " · ${entry.note}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onOpenToken(index) }) { Text("View") }
            IconButton(onClick = { onOpenDefeatDialog(index) }) {
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
                    TokenCatalogue.byId(entry.tokenId)?.let { token -> EnemyTokenFace(token = token, size = 32.dp) }
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
 * (issue #191), and a Defeat button (D2/D11).
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
                if (token != null) {
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
                    Text(token.statLine(), style = MaterialTheme.typography.titleMedium)
                    token.attacks.forEach { attack ->
                        if (attack.isSummon) {
                            // A summon has no attack value - it draws a replacement from another pile.
                            Text("Summons a ${attack.summons?.summonName() ?: "token"}")
                        } else {
                            Text("Attack ${attack.value} · ${attack.element.displayName()}")
                        }
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
                    Button(onClick = { onToggleDefeated(logIndex, true) }, enabled = !entry.defeated) {
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
                    token.attacks.forEach { attack ->
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
                // The grid/log cell keeps a single small corner badge even for a double-summoner -
                // both children are still reachable via the row's "Summoned: …" line and the zoom.
                EnemyTokenFaceWithSummon(token = token, size = 72.dp, summonedChildren = listOfNotNull(summonedChild))
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

/** The Defeat dialog: a Defeated toggle plus a free-text note (for enemies kept on the board),
 * saved together. This is the per-log-row entry point (note-editing needs the dialog); the zoom
 * and grid views instead use a one-tap Defeat button/checkbox with no note. */
@Composable
private fun DefeatDialog(entry: DrawLogEntry, onSave: (Boolean, String) -> Unit, onDismiss: () -> Unit) {
    var defeated by remember { mutableStateOf(entry.defeated) }
    var note by remember { mutableStateOf(entry.note) }
    val token = TokenCatalogue.byId(entry.tokenId)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(defeated, note) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(token?.name ?: entry.tokenId) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledSwitch(label = "Defeated", checked = defeated, onCheckedChange = { defeated = it })
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
