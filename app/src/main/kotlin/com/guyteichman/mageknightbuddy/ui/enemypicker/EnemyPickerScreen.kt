package com.guyteichman.mageknightbuddy.ui.enemypicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.EnemyPickerSessionRepository
import com.guyteichman.mageknightbuddy.domain.DrawLogEntry
import com.guyteichman.mageknightbuddy.domain.EnemyPickerSession
import com.guyteichman.mageknightbuddy.domain.EnemyToken
import com.guyteichman.mageknightbuddy.domain.Expansion
import com.guyteichman.mageknightbuddy.domain.TokenCatalogue
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
    // Tokens currently shown zoomed (a whole draw batch, or one tapped log entry), plus which index
    // of the batch is on screen for the "x of y" swipe.
    var zoom by remember { mutableStateOf<ZoomState?>(null) }
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

    EnemyPickerContent(
        session = session,
        isBusy = viewModel.isBusy,
        onDraw = { pileId, count ->
            scope.launch {
                viewModel.draw(pileId, count)
                // mutate() has published the new session by the time draw() returns, so the last
                // `count` log entries are exactly what was just drawn - open them zoomed.
                val log = viewModel.session?.drawLog ?: return@launch
                zoom = ZoomState(log.takeLast(count).map { it.tokenId }, 0)
            }
        },
        onOpenToken = { tokenId -> zoom = ZoomState(listOf(tokenId), 0) },
        onOpenDefeatDialog = { index -> defeatDialogIndex = index },
        onRequestReset = { pendingReset = { scope.launch { viewModel.reset() } } },
        onRequestApplyConfig = { tokenSet, replacement ->
            pendingReset = { scope.launch { viewModel.applyConfig(tokenSet, replacement) } }
        },
    )

    // --- Dialogs, driven by the state above ---

    zoom?.let { state ->
        TokenZoomDialog(
            state = state,
            onNavigate = { newIndex -> zoom = state.copy(index = newIndex) },
            onShowInfo = { token -> infoToken = token },
            onDismiss = { zoom = null },
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

/** Immutable UI state for the zoom dialog: the batch of token ids being viewed and the current index. */
private data class ZoomState(val tokenIds: List<String>, val index: Int)

/**
 * The scrollable body of the Enemy Picker: a card per [TokenPile][TokenPileId] with a quantity
 * stepper and Draw button, the Draw Log, and the staged config section. Stateless apart from
 * per-card draw quantity and the staged config edits, which it owns because they don't belong in
 * the persisted session.
 */
@Composable
private fun EnemyPickerContent(
    session: EnemyPickerSession,
    isBusy: Boolean,
    onDraw: (TokenPileId, Int) -> Unit,
    onOpenToken: (String) -> Unit,
    onOpenDefeatDialog: (Int) -> Unit,
    onRequestReset: () -> Unit,
    onRequestApplyConfig: (Set<Expansion>, Boolean) -> Unit,
) {
    Scaffold(
        topBar = { EnemyPickerTopBar() },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // One card per pile that exists in this token set, in a stable enum order.
            val pileIds = TokenPileId.entries.filter { it in session.piles }
            items(pileIds, key = { it.name }) { pileId ->
                PileCard(
                    pileId = pileId,
                    pile = session.piles.getValue(pileId),
                    withReplacement = session.drawWithReplacement,
                    isBusy = isBusy,
                    onDraw = { count -> onDraw(pileId, count) },
                )
            }

            item(key = "draw-log") {
                DrawLogSection(
                    log = session.drawLog,
                    onOpenToken = onOpenToken,
                    onOpenDefeatDialog = onOpenDefeatDialog,
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
 * One pile: its name, how many tokens are left / drawn, a 1..N quantity stepper, and a Draw button.
 * The quantity is card-local UI state (it's not part of the game), reset to 1 on each draw isn't
 * necessary - a player often draws the same number repeatedly.
 */
@Composable
private fun PileCard(
    pileId: TokenPileId,
    pile: com.guyteichman.mageknightbuddy.domain.TokenPile,
    withReplacement: Boolean,
    isBusy: Boolean,
    onDraw: (Int) -> Unit,
) {
    var quantity by rememberSaveable(pileId.name) { mutableStateOf(1) }
    val remaining = pile.drawPile.size
    val drawn = pile.discardPile.size

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pileId.displayName(), style = MaterialTheme.typography.titleMedium)
            Text(
                // With replacement, nothing depletes, so "drawn" would always be 0 - hide it.
                text = if (withReplacement) "Draws with replacement (never depletes)"
                else "$remaining in pile · $drawn drawn",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    // Without replacement, can't draw more than the whole pile (draw + discard) at once.
                    max = if (withReplacement) MAX_BATCH else (remaining + drawn).coerceAtMost(MAX_BATCH),
                )
                Button(onClick = { onDraw(quantity) }, enabled = !isBusy) {
                    Text(if (quantity == 1) "Draw" else "Draw $quantity")
                }
            }
        }
    }
}

/** A compact "− N +" stepper clamped to 1..[max]. */
@Composable
private fun QuantityStepper(quantity: Int, onQuantityChange: (Int) -> Unit, max: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onQuantityChange((quantity - 1).coerceAtLeast(1)) }, enabled = quantity > 1) {
            Icon(Icons.Filled.Remove, contentDescription = "One fewer")
        }
        Text("$quantity", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onQuantityChange((quantity + 1).coerceAtMost(max)) }, enabled = quantity < max) {
            Icon(Icons.Filled.Add, contentDescription = "One more")
        }
    }
}

/**
 * The Draw Log (see `CONTEXT.md`'s "Draw Log"): newest-first, split into the enemies still **on the
 * board** (top) and the **defeated** ones (dimmed, below). A freshly drawn enemy starts on the board
 * (D2); tapping a row re-opens that token zoomed, and the trailing icon opens the Defeat dialog.
 */
@Composable
private fun DrawLogSection(
    log: List<DrawLogEntry>,
    onOpenToken: (String) -> Unit,
    onOpenDefeatDialog: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Draw Log", style = MaterialTheme.typography.titleLarge)
        if (log.isEmpty()) {
            Text("No tokens drawn yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // Keep each entry's original (chronological) index for defeating, then show newest-first.
        val indexed = log.withIndex().toList()
        val onBoard = indexed.filter { !it.value.defeated }.asReversed()
        val defeated = indexed.filter { it.value.defeated }.asReversed()

        // Only show the section headers once there's actually a split to label; a fresh log with
        // nothing defeated yet is just a flat newest-first list.
        val showHeaders = onBoard.isNotEmpty() && defeated.isNotEmpty()

        if (onBoard.isNotEmpty()) {
            if (showHeaders) Text("On the board", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            onBoard.forEach { (index, entry) ->
                DrawLogRow(index, entry, onOpenToken, onOpenDefeatDialog)
            }
        }
        if (defeated.isNotEmpty()) {
            Text(
                "Defeated",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            defeated.forEach { (index, entry) ->
                DrawLogRow(index, entry, onOpenToken, onOpenDefeatDialog)
            }
        }
    }
}

@Composable
private fun DrawLogRow(
    index: Int,
    entry: DrawLogEntry,
    onOpenToken: (String) -> Unit,
    onOpenDefeatDialog: (Int) -> Unit,
) {
    val token = TokenCatalogue.byId(entry.tokenId)
    // Defeated rows are de-emphasised (dimmed name), since the on-board enemies are what still needs
    // attention.
    val nameColor = if (entry.defeated) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(token?.name ?: entry.tokenId, style = MaterialTheme.typography.bodyLarge, color = nameColor)
                Text(
                    text = entry.pile.displayName() + (if (entry.note.isNotBlank()) " · ${entry.note}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onOpenToken(entry.tokenId) }) { Text("View") }
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
 * and attacks, a "?" to open the full ability info window, and prev/next with an "x of y" counter
 * when a whole batch was drawn at once.
 */
@Composable
private fun TokenZoomDialog(
    state: ZoomState,
    onNavigate: (Int) -> Unit,
    onShowInfo: (EnemyToken) -> Unit,
    onDismiss: () -> Unit,
) {
    val token = TokenCatalogue.byId(state.tokenIds[state.index])
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(token?.name ?: state.tokenIds[state.index])
                if (token != null) {
                    IconButton(onClick = { onShowInfo(token) }) {
                        Icon(Icons.Filled.QuestionMark, contentDescription = "Abilities")
                    }
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (token != null) {
                    EnemyTokenFace(token = token, size = 140.dp)
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
                if (state.tokenIds.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onNavigate(state.index - 1) }, enabled = state.index > 0) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                        }
                        Text("${state.index + 1} of ${state.tokenIds.size}")
                        IconButton(onClick = { onNavigate(state.index + 1) }, enabled = state.index < state.tokenIds.size - 1) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                }
            }
        },
    )
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
 * saved together. The one-tap Defeat button on the zoom/grid is a follow-up (Issue A); this is the
 * per-log-row entry point. */
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

/**
 * "Armor 3 · Fame 2" summary line for a token. Deliberately *excludes* the attack (D5): the
 * attack(s) are listed in full just below this line in the zoom, so repeating them here was
 * redundant.
 */
private fun EnemyToken.statLine(): String = "Armor $armor · Fame $fame"

/** Player-facing name of a summoned pile, e.g. "Brown enemy" (used in the zoom's "Summons a …" line). */
private fun TokenPileId.summonName(): String = when (this) {
    TokenPileId.BROWN -> "Brown enemy"
    else -> displayName()
}

/** Player-facing pile name. */
private fun TokenPileId.displayName(): String = when (this) {
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
    Expansion.SHADES_OF_TEZLA -> "Shades of Tezla"
    Expansion.APOCALYPSE_DRAGON -> "The Apocalypse Dragon"
}
