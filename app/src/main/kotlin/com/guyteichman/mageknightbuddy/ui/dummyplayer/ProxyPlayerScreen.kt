package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.objectiveLabel
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import com.guyteichman.mageknightbuddy.ui.components.CardColorDot
import com.guyteichman.mageknightbuddy.ui.components.CrystalIcon
import com.guyteichman.mageknightbuddy.ui.components.KnightShieldIcon
import com.guyteichman.mageknightbuddy.ui.components.label
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.help.HelpButton
import kotlinx.coroutines.launch

/**
 * Human-readable label for a [ProxyPlayerCard], distinguishing the 3 kinds that matter for the
 * movement-point bonus - a generic Basic Action, a Knight's own Unique Basic Action Card, or an
 * Advanced Action (single- or dual-color) - see [ProxyPlayerCard.movementBonus].
 */
private fun ProxyPlayerCard.displayText(): String = when (this) {
    is ProxyPlayerCard.BasicAction -> "${color.label} (Basic Action)"
    is ProxyPlayerCard.UniqueAction -> "${color.label} (Unique)"
    // CardIdentity.displayLabel already joins a Dual-Color card's 2 colors with "/" - reused here
    // instead of re-branching on SingleColor/DualColor a second time.
    is ProxyPlayerCard.AdvancedAction -> "${identity.displayLabel} (Advanced Action)"
}

/** The color(s) [this] card renders/matches as - see [CardIdentity.colors], which [AdvancedAction] delegates to. */
private fun ProxyPlayerCard.colors(): List<CardColor> = when (this) {
    is ProxyPlayerCard.BasicAction -> listOf(color)
    is ProxyPlayerCard.UniqueAction -> listOf(color)
    is ProxyPlayerCard.AdvancedAction -> identity.colors()
}

/** Sort key for laying out a deck of [ProxyPlayerCard]s in a stable, color-grouped order (see [CardColor]'s declaration order). */
private fun ProxyPlayerCard.sortKey(): Int = colors().minOf { it.ordinal }

/** Whether [this] card is Advanced Action/Unique (gets [MiniCard]'s star badge) rather than a plain Basic Action. */
private fun ProxyPlayerCard.isNonBasic(): Boolean = when (this) {
    is ProxyPlayerCard.BasicAction -> false
    is ProxyPlayerCard.UniqueAction -> true
    is ProxyPlayerCard.AdvancedAction -> true
}

/** The Proxy Player objective label for [this] card - see [CardColor.objectiveLabel]/[CardIdentity.objectiveLabel]. */
private fun ProxyPlayerCard.objectiveLabel(): String = when (this) {
    is ProxyPlayerCard.BasicAction -> color.objectiveLabel
    is ProxyPlayerCard.UniqueAction -> color.objectiveLabel
    is ProxyPlayerCard.AdvancedAction -> identity.objectiveLabel
}

/**
 * Whether the player has confirmed a matching-color (or Gold, by day) mana die is currently in
 * the Source this turn - a 3rd state ([UNANSWERED]) instead of a plain [Boolean] so the UI can
 * tell "not yet checked this turn" apart from "checked and it's No", which a reset-to-false
 * checkbox couldn't distinguish (a player could easily mistake a silently-reset unchecked box for
 * their own already-recorded "No").
 */
private enum class ManaDieAnswer { UNANSWERED, YES, NO }

/**
 * Proxy Player mode's AI (turn/round) screen: shows the Proxy Player's deck/crystal state (mirrors
 * `DummyPlayerScreen.kt`'s `TableauCard`), the current Objective Card (or a prompt to play a turn,
 * before the first one) via [displayText], its Shield count, the computed movement-point total
 * (see [ProxyPlayerSession.movementPoints]), the event log, and the mutating actions - Play Turn,
 * End Round, and (only once there's a current objective) Explored/Completed to resolve it. Per
 * docs/rules/proxy-player.md's app-scope note: this screen never decides *where* the Hero moves or
 * *what* gets conquered - only the movement-point number and Objective Card/Shield bookkeeping.
 * Not `private`: called from `DummyPlayerScreen.kt`'s `DummyPlayerTab`, the same cross-file
 * relationship `VolkareScreen.kt`'s `VolkareAiScreen` has with it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProxyPlayerAiScreen(
    repository: ProxyPlayerSessionRepository,
    fieldHelp: Map<String, FieldHelp>,
    onBack: () -> Unit
) {
    val viewModel: ProxyPlayerAiViewModel = viewModel(factory = ProxyPlayerAiViewModel.factory(repository))
    val scope = rememberCoroutineScope()
    val session = viewModel.session
    // The player's per-turn report of whether a matching-color (or gold, by day) mana die
    // currently sits in the Source - see docs/rules/proxy-player.md's "Movement points": that die
    // is immediately rerolled once used, so this fact never carries over to the next turn. A
    // plain per-composition remember (not saved across process death) is fine for the same
    // reason - it's explicitly reset to UNANSWERED in Play Turn's onClick below rather than left
    // to persist, so nothing here needs to survive beyond a single turn.
    var manaDieAnswer by remember { mutableStateOf(ManaDieAnswer.UNANSWERED) }
    var showEndRoundDialog by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proxy Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (session != null) {
                        RoundChip(round = session.round)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
            )
        },
        // Bottom action row, mirroring DummyPlayerScreen.kt's DummyPlayerAiScreen - Play Turn and
        // End Round are always available, unlike Explored/Completed below which only make sense
        // once there's a current objective to resolve.
        bottomBar = {
            if (session != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.playTurn()
                                // manaDieAnswer is "is a matching die in the Source *this* turn" -
                                // docs/rules/proxy-player.md has that die immediately rerolled once
                                // used, so last turn's report says nothing about the turn that just
                                // started. Reset it here rather than leaving it answered, which
                                // would silently keep applying last turn's answer to the displayed
                                // movement points every subsequent turn.
                                manaDieAnswer = ManaDieAnswer.UNANSWERED
                            }
                        },
                        enabled = !session.roundEnded && !viewModel.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Play Turn")
                    }
                    OutlinedButton(
                        onClick = { showEndRoundDialog = true },
                        enabled = !viewModel.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("End Round")
                    }
                }
            }
        },
    ) { padding ->
        if (session == null) {
            // Restoring from Room is asynchronous (see ProxyPlayerAiViewModel's init block); this
            // only shows for the brief window before that first restore completes.
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // A local val, not repeated session.objectiveCard reads: ProxyPlayerCard is declared
            // in the domain module, so Kotlin can't smart-cast a nullable property read from a
            // different module across two statements - capturing it once here gives the compiler
            // (and this code) one non-null local to work with instead.
            val objectiveCard = session.objectiveCard

            // LazyColumn, not a plain Column: the deck tableau, objective details, and event log
            // together can easily outgrow one screen (mirrors DummyPlayerScreen.kt's
            // DummyPlayerAiScreen, which has the same shape of content).
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ProxyPlayerHeroRow(session = session) }
                // The toggle button lives inside ProxyPlayerDeckPanel's own header instead of the
                // top app bar, so it reads as attached to the panel it controls - matches
                // DummyPlayerScreen.kt's DeckPanel. The body still swaps mutually exclusively.
                item {
                    ProxyPlayerDeckPanel(showSummary = showSummary, onToggleSummary = { showSummary = !showSummary }) {
                        if (showSummary) ProxyPlayerStatGridBody(session = session) else ProxyPlayerTableauBody(session = session)
                    }
                }
                item {
                    if (objectiveCard == null) {
                        Text("No current objective - tap Play Turn to draw one.")
                    } else {
                        // Computed up front (not inline where each was previously read) since the
                        // mana-die question now renders in the header Row, above the Shields/
                        // Movement row that used to precede it.
                        //
                        // Names the actual color(s) to check for, dropping the vague "matching" -
                        // and the "or a Gold die" clause only appears on day Rounds, since Gold
                        // can't grant the bonus at night (docs/rules/proxy-player.md's "Movement
                        // points"). A dual-color objective's colors are joined with "or" - either
                        // counts (this app's 3rd dual-color ruling, alongside crystal-chain and
                        // targeting - see docs/rules/proxy-player.md).
                        val dieColors = objectiveCard.colors().joinToString(" or ") { it.label }
                        val dieQuestion = if (session.isDay) "$dieColors die (or Gold)?" else "$dieColors die?"
                        // headlineLarge - one tier above the deck tracker's headlineMedium card
                        // count - ranks this as the 2nd most important number on the screen, after
                        // the objective itself. Unanswered shows the equation ("3(+1)?") rather
                        // than a bare number, so the player can see both the guaranteed base value
                        // and the still-uncertain potential bonus.
                        val basePoints = session.movementPoints(hasMatchingManaDie = false)
                        val pointsText = when (manaDieAnswer) {
                            ManaDieAnswer.UNANSWERED -> "$basePoints(+1)?"
                            ManaDieAnswer.YES -> session.movementPoints(hasMatchingManaDie = true).toString()
                            ManaDieAnswer.NO -> basePoints.toString()
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 36x50.4.dp (36 x the deck tray's 20x28.dp height/width ratio) as
                                // the section's visual anchor - same split-swatch/star-badge
                                // rendering as the deck tracker's MiniCards, just bigger, rather
                                // than a second way of drawing card color on this screen.
                                MiniCard(
colors = objectiveCard.colors(),
isNonBasic = objectiveCard.isNonBasic(),
width = 36.dp,
height = 36.dp * 1.4f
)
                                Column(
modifier = Modifier.weight(1f),
verticalArrangement = Arrangement.spacedBy(2.dp)
) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(objectiveCard.displayText(), style = MaterialTheme.typography.titleMedium)
                                        HelpButton(keys = listOf("Proxy Player Objective"), fieldHelp = fieldHelp)
                                    }
                                    // What this color's movement target actually means in-game -
                                    // see CardColor.objectiveLabel's doc comment for why Blue's own
                                    // wording states its distance condition rather than a specific
                                    // action the app can't actually determine.
                                    Text(objectiveCard.objectiveLabel(), style = MaterialTheme.typography.bodyMedium)
                                }
                                // Mana-die question + Yes/No moved up into the header row (was its
                                // own row below Shields/Movement) - issue feedback was to use the
                                // header row's spare width instead.
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(dieQuestion, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Exactly one tap from UNANSWERED to either answer - neither
                                        // chip starts selected, unlike a 2-state toggle that would
                                        // need to cycle.
                                        FilterChip(
                                            selected = manaDieAnswer == ManaDieAnswer.YES,
                                            onClick = { manaDieAnswer = ManaDieAnswer.YES },
                                            label = { Text("Yes") },
                                        )
                                        FilterChip(
                                            selected = manaDieAnswer == ManaDieAnswer.NO,
                                            onClick = { manaDieAnswer = ManaDieAnswer.NO },
                                            label = { Text("No") },
                                        )
                                    }
                                }
                            }

                            // Shields and Movement Points side by side - both are compact,
                            // single-purpose stats, so pairing them frees a full row instead of
                            // stacking every element in this section vertically.
                            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Shields",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Bare repeated icons, no numeral - same pattern as the
                                        // crystal rows, using the Knight's own shield-token art
                                        // (matches ProxyPlayerHeroRow).
                                        repeat(session.objectiveShields) { KnightShieldIcon(knight = session.knight, size = 16.dp) }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    // Subtitle above the number (not below) - the label/value order
                                    // now matches Shields' label-above-icons layout.
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "movement points",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        HelpButton(keys = listOf("Proxy Player Movement"), fieldHelp = fieldHelp)
                                    }
                                    Text(pointsText, style = MaterialTheme.typography.headlineLarge)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { scope.launch { viewModel.resolveObjective(ProxyPlayerObjectiveResolution.COMPLETED) } },
                                    enabled = !viewModel.isBusy,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Explored/Completed Objective")
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
"Log",
style = MaterialTheme.typography.labelLarge,
color = MaterialTheme.colorScheme.onSurfaceVariant
)
                }
                // Most-recent-first, matching DummyPlayerScreen.kt's DummyPlayerAiScreen (issue #35).
                items(session.log.asReversed()) { event ->
                    LogRow(entry = event.describe())
                }
            }
        }
    }

    if (showEndRoundDialog) {
        ProxyPlayerEndRoundDialog(
            fieldHelp = fieldHelp,
            onDismiss = { showEndRoundDialog = false },
            onConfirm = { advancedActionOfferColor, spellOfferColor ->
                scope.launch {
                    viewModel.endRound(advancedActionOfferColor, spellOfferColor)
                    showEndRoundDialog = false
                }
            },
        )
    }
}

/**
 * Knight shield icon, name, and a "Random" badge if the Knight was randomly rolled at setup - a
 * Proxy Player-mode copy of `DummyPlayerScreen.kt`'s file-private `HeroRow` (same duplication
 * rationale as [ProxyPlayerEndRoundDialog]'s doc comment).
 */
@Composable
private fun ProxyPlayerHeroRow(session: ProxyPlayerSession) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KnightShieldIcon(knight = session.knight, size = 32.dp, tint = MaterialTheme.colorScheme.primary)
        Text(session.knight.displayName, style = MaterialTheme.typography.titleMedium)
        if (session.wasRandom) {
            Surface(shape = RoundedCornerShape(percent = 50), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "RANDOM",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/**
 * The deck panel's shared shell: a title, the Summary/Full View toggle button (styled and placed
 * so it reads as part of this panel rather than a stray top-bar label), and [content] (either
 * [ProxyPlayerTableauBody] or [ProxyPlayerStatGridBody]) below - a Proxy Player-mode copy of
 * `DummyPlayerScreen.kt`'s file-private `DeckPanel` (same duplication rationale as
 * [ProxyPlayerEndRoundDialog]'s doc comment).
 */
@Composable
private fun ProxyPlayerDeckPanel(showSummary: Boolean, onToggleSummary: () -> Unit, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Deck", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = onToggleSummary,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        if (showSummary) Icons.Filled.UnfoldMore else Icons.Filled.UnfoldLess,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showSummary) "Full View" else "Summary")
                }
            }
            content()
        }
    }
}

/**
 * The card-tableau body: how many cards are left in the deck, a pile of their colors ([MiniCard]
 * per card, via [ProxyPlayerCard.colors]), a per-color count breakdown
 * ([ProxyPlayerSession.remainingByColor]), and the crystal Inventory - a Proxy Player-mode copy of
 * `DummyPlayerScreen.kt`'s file-private `TableauBody` (same duplication rationale as
 * [ProxyPlayerEndRoundDialog]'s doc comment). Rendered inside [ProxyPlayerDeckPanel]'s Column - no
 * Card or padding of its own.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProxyPlayerTableauBody(session: ProxyPlayerSession) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.deckOrder.size.toString(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "left in deck",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    FlowRow(
        modifier = Modifier.heightIn(min = 61.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        session.deckOrder.sortedBy { it.sortKey() }
            .forEach { card -> MiniCard(colors = card.colors(), isNonBasic = card.isNonBasic()) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Cards",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CardColor.entries.forEach { color ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CardColorDot(color = color)
                        Text(
                            session.remainingByColor.getValue(color).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Crystals",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                CardColor.entries.forEach { color ->
                    repeat(session.crystals.getValue(color)) { CrystalIcon(color = color) }
                }
            }
        }
    }
}

/**
 * The alternate, denser per-color tile grid, replacing [ProxyPlayerTableauBody] when "Summary" is
 * toggled on - a Proxy Player-mode copy of `DummyPlayerScreen.kt`'s file-private `StatGridBody`
 * (same duplication rationale as [ProxyPlayerEndRoundDialog]'s doc comment). Stays pure aggregate
 * counts, no non-basic (star badge) breakdown - that detail is only shown in the full expanded
 * view, where individual cards are drawn. Rendered inside [ProxyPlayerDeckPanel]'s Column - no Card
 * or padding of its own.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProxyPlayerStatGridBody(session: ProxyPlayerSession) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        CardColor.entries.forEach { color ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.widthIn(max = 56.dp),
            ) {
                CardColorDot(color = color)
                Text(session.remainingByColor.getValue(color).toString(), style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.widthIn(max = 56.dp),
                ) {
                    repeat(session.crystals.getValue(color)) { CrystalIcon(color = color) }
                }
            }
        }
    }
}

/** Describes one [ProxyPlayerEvent] for [LogRow] - the Proxy Player counterpart to `DummyPlayerScreen.kt`'s file-private `DummyPlayerEvent.describe()`. */
private fun ProxyPlayerEvent.describe(): LogEntryText = when (this) {
    is ProxyPlayerEvent.RoundStarted -> LogEntryText(
        icon = "◆",
        title = "Round started",
        meta = "Round $round",
        description = listOf(DescriptionSpan.Words("The Proxy Player's deck is shuffled and ready.")),
    )

    is ProxyPlayerEvent.NewObjectiveDrawn -> LogEntryText(
        icon = "▶",
        title = "Objective drawn",
        meta = "Round $round",
        description = buildList {
            add(DescriptionSpan.Words("New objective: "))
            addCardDots(objectiveCard.colors())
            add(DescriptionSpan.Words(" ${objectiveCard.displayText()}."))
            if (discarded.isNotEmpty()) {
                add(DescriptionSpan.Words(" Also discarded: "))
                discarded.forEachIndexed { index, card ->
                    if (index > 0) add(DescriptionSpan.Words(", "))
                    addCardDots(card.colors())
                    add(DescriptionSpan.Words(" ${card.displayText()}"))
                }
                add(DescriptionSpan.Words("."))
            }
        },
    )

    is ProxyPlayerEvent.TurnContinued -> LogEntryText(
        icon = "▶",
        title = "Turn continued",
        meta = "Round $round",
        description = buildList {
            add(DescriptionSpan.Words("Objective "))
            addCardDots(objectiveCard.colors())
            add(DescriptionSpan.Words(" ${objectiveCard.displayText()} now has $shieldsNow Shield(s)."))
            if (revealed.isNotEmpty()) {
                add(DescriptionSpan.Words(" Revealed: "))
                revealed.forEachIndexed { index, card ->
                    if (index > 0) add(DescriptionSpan.Words(", "))
                    addCardDots(card.colors())
                    add(DescriptionSpan.Words(" ${card.displayText()}"))
                }
                add(DescriptionSpan.Words("."))
            }
        },
    )

    is ProxyPlayerEvent.EndOfRoundAnnounced -> LogEntryText(
        icon = "⚑",
        title = "End of Round announced",
        meta = "Round $round",
        description = listOf(DescriptionSpan.Words("The deck ran out - other players get one more turn each, then the Round ends.")),
    )

    is ProxyPlayerEvent.ObjectiveResolved -> LogEntryText(
        icon = "⚑",
        title = "Objective resolved",
        meta = "Round $round",
        description = buildList {
            addCardDots(objectiveCard.colors())
            add(
                DescriptionSpan.Words(
                    " ${objectiveCard.displayText()} - ${
                        resolution.name.lowercase().replaceFirstChar { it.uppercase() }
                    }.",
                ),
            )
        },
    )

    is ProxyPlayerEvent.RoundEnded -> LogEntryText(
        icon = "⚑",
        title = "Round ended",
        meta = "Round $round",
        // Describes only the round-prep offer swap, which happens every Round regardless of why
        // it ended - mirrors DummyPlayerEvent.RoundEnded's equivalent description, plus one extra
        // sentence when a lingering objective was discarded (a case standard Dummy Player has no
        // equivalent of).
        description = buildList {
            add(DescriptionSpan.Words("Advanced Action offer discard ("))
            addCardDots(advancedActionOfferColor.colors())
            add(DescriptionSpan.Words(" ${advancedActionOfferColor.displayLabel}) added to the deck. Spell offer discard ("))
            add(DescriptionSpan.CrystalDot(spellOfferColor))
            add(DescriptionSpan.Words(" ${spellOfferColor.label}) granted +1 crystal."))
            // Local val, not repeated discardedObjective reads: same cross-module smart-cast
            // limitation as ProxyPlayerAiScreen's objectiveCard (see its comment).
            val lingeringObjective = discardedObjective
            if (lingeringObjective != null) {
                add(DescriptionSpan.Words(" Lingering objective "))
                addCardDots(lingeringObjective.colors())
                add(DescriptionSpan.Words(" ${lingeringObjective.displayText()} discarded."))
            }
        },
    )
}

/**
 * The End Round dialog: a Proxy Player-mode copy of `DummyPlayerScreen.kt`'s file-private
 * `EndRoundDialog` (same fields - an Advanced Action offer [CardIdentity] via [IdentityPickerRow],
 * and a Spell offer color - since [ProxyPlayerAiViewModel.endRound] takes the exact same
 * `(CardIdentity, CardColor)` shape as the standard Dummy Player's `endRound` does). Duplicated
 * rather than shared directly because the two AI screens' ViewModels are distinct types with their
 * own `onConfirm` call sites - matching how `VolkareScreen.kt` keeps its own copies of
 * `RoundChip`/`MiniCard`/`LogRow` instead of reaching into `DummyPlayerScreen.kt`'s private ones.
 */
@Composable
private fun ProxyPlayerEndRoundDialog(
    fieldHelp: Map<String, FieldHelp>,
    onDismiss: () -> Unit,
    onConfirm: (advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor) -> Unit,
) {
    var advancedActionIdentity by remember { mutableStateOf<CardIdentity>(CardIdentity.SingleColor(CardColor.entries.first())) }
    var spellColor by remember { mutableStateOf(CardColor.entries.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // usePlatformDefaultWidth = false + an explicit fillMaxWidth lets the dialog use most of
        // the screen's width instead of Material's narrow default - same as the standard Dummy
        // Player's End Round dialog.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(fraction = 0.94f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("End Round", modifier = Modifier.weight(1f))
                HelpButton(keys = listOf("Round-Prep Offers"), fieldHelp = fieldHelp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Pick the color removed from each offer during round-prep.",
                    style = MaterialTheme.typography.bodySmall,
                )
                IdentityPickerRow(
                    label = "Advanced Action offer",
                    selected = advancedActionIdentity,
                    onSelect = { advancedActionIdentity = it },
                )
                ColorPickerRow(
                    label = "Spell offer",
                    selected = spellColor,
                    onSelect = { spellColor = it },
                    // A crystal of this color is what's granted, not a card - CrystalIcon fits better.
                    chipIcon = { color -> CrystalIcon(color = color, size = 12.dp) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(advancedActionIdentity, spellColor) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
