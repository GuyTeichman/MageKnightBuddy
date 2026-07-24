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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.objectiveLabel
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent
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

/**
 * Just the color(s) [this] card renders as (e.g. "Green", or "Blue/White" for a dual-color card) -
 * no Basic/Unique/Advanced Action suffix, unlike [displayText]. Basic-vs-Advanced only matters for
 * the *objective* card (it's what the movement-point bonus depends on) - a merely revealed or
 * discarded card in the log doesn't need that distinction stated.
 */
private fun ProxyPlayerCard.colorLabel(): String = colors().joinToString("/") { it.label }

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
 * Proxy Player mode's AI (turn/round) screen: shows the Proxy Player's deck/crystal state (mirrors
 * `DummyPlayerScreen.kt`'s `TableauCard`), the current Objective Card (or a prompt to play a turn,
 * before the first one) - its Shield count as a small chip hanging off the card, mirroring how the
 * rulebook physically stacks Shield tokens on top of it - the computed movement-point total (see
 * [ProxyPlayerSession.movementPoints]), the event log, and the mutating actions - Play Turn, End
 * Round, and Complete Objective (always present, just disabled until there's a current objective to
 * resolve - a control popping in and out of the layout read as broken during testing). Per
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
                        RoundChip(round = session.round, turn = session.turnInRound)
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
                        onClick = { scope.launch { viewModel.playTurn() } },
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (objectiveCard == null) {
                            Text("No current objective - tap Play Turn to draw one.")
                        } else {
                            // Names the actual color(s) that grant the movement-point bonus,
                            // dropping the vague "matching" - and the "or Gold" clause only
                            // appears on day Rounds, since Gold can't grant the bonus at night
                            // (docs/rules/proxy-player.md's "Movement points"). A dual-color
                            // objective's colors are joined with "or" - either counts (this app's
                            // 3rd dual-color ruling, alongside crystal-chain and targeting - see
                            // docs/rules/proxy-player.md).
                            val dieColors = objectiveCard.colors().joinToString(" or ") { it.label }
                            val manaDieDescription = if (session.isDay) "$dieColors or Gold" else dieColors
                            val basePoints = session.movementPoints(hasMatchingManaDie = false)
                            val bonusPoints = session.movementPoints(hasMatchingManaDie = true)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // The card is the section's visual anchor; its Shield count hangs
                                // off its own bottom edge (see ProxyPlayerObjectiveCard) rather
                                // than sitting in a separate labeled row.
                                ProxyPlayerObjectiveCard(
                                    objectiveCard = objectiveCard,
                                    shields = session.objectiveShields,
                                )
                                // verticalAlignment = Top (not CenterVertically) - Objective and
                                // Movement need to top-align to *each other*, not each independently
                                // center against the taller card beside them, which is what made
                                // this row read as lopsided before.
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Generic "Objective" - not the card's own color/type
                                            // text, which the card's own art (color + star badge)
                                            // already conveys. Matches "Movement" below exactly
                                            // (same style), so the two columns' label lines land
                                            // on the same size/weight instead of one column having
                                            // no label at all.
                                            Text(
                                                "Objective",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            HelpButton(keys = listOf("Proxy Player Objective"), fieldHelp = fieldHelp)
                                        }
                                        // What this color's movement target actually means in-game
                                        // - see CardColor.objectiveLabel's doc comment for why
                                        // Blue's own wording states its distance condition rather
                                        // than a specific action the app can't actually determine.
                                        // Promoted to titleMedium (was the card's color/type text's
                                        // size) - this is what you actually need to read every
                                        // turn, not the color/type the card's own art already shows.
                                        Text(objectiveCard.objectiveLabel(), style = MaterialTheme.typography.titleMedium)
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        // Text+HelpButton, matching Objective's own label row
                                        // exactly (not just a bare Text) - IconButton's 48dp touch
                                        // target otherwise inflates only *this* column's first row,
                                        // which is what was pushing the two labels out of alignment
                                        // even under a shared Alignment.Top.
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Movement",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            HelpButton(keys = listOf("Proxy Player Movement"), fieldHelp = fieldHelp)
                                        }
                                        Text(basePoints.toString(), style = MaterialTheme.typography.headlineLarge)
                                        Surface(
                                            shape = RoundedCornerShape(percent = 50),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Text(
                                                // bonusPoints - basePoints (not a hardcoded "+1"):
                                                // the mana-die bonus size is movementPoints' own
                                                // formula, not an assumption restated here.
                                                "+${bonusPoints - basePoints} if $manaDieDescription",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Always rendered (not only when objectiveCard != null) - a control that
                        // pops in and out of the layout as the objective resolves read as broken
                        // during testing. Disabled instead: same position every turn, whether or
                        // not there's currently an objective to resolve. Sized to its own content
                        // and left-aligned (Column's own default) - directly under the objective
                        // card above, not trailing the numeric Movement column.
                        OutlinedButton(
                            onClick = { scope.launch { viewModel.resolveObjective() } },
                            enabled = objectiveCard != null && !viewModel.isBusy,
                        ) {
                            Text("Complete Objective")
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

/** Fixed gold/bronze tint for the Shield-count chip's glyph - matches Theme.kt's own gold seed
 * color (0xFFE4B25C, tertiary in light theme / primary in dark), chosen as a plain constant
 * rather than a ColorScheme role since dynamic color (Material You) can otherwise replace that
 * role with an arbitrary wallpaper-derived hue - shields should read the same regardless. */
private val ShieldChipGold = Color(0xFFE4B25C)

/**
 * The current Objective Card, with its Shield count as a small chip hanging off the card's bottom
 * edge - mirrors the rulebook physically stacking Shield tokens on top of the objective card
 * (docs/rules/proxy-player.md's "The Proxy Player's turn"), rather than a separate labeled row.
 * The chip is placed at a *negative* y-offset (pulling it up to overlap the card) instead of
 * computing an absolute position: [Modifier.offset] only shifts where a composable is *painted*,
 * not how much space Column reserves for it, so the chip's full height still counts toward this
 * Column's layout size - which is exactly what keeps whatever's below from colliding with it.
 * The glyph itself is the same generic shield shape [KnightShieldIcon] falls back to for Coral
 * (whose own art isn't sourced yet) - not the current Knight's own shield art, since this chip is
 * a plain Shield-count indicator, not a 2nd place to render the Knight's heraldry.
 */
@Composable
private fun ProxyPlayerObjectiveCard(objectiveCard: ProxyPlayerCard, shields: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MiniCard(
            colors = objectiveCard.colors(),
            isNonBasic = objectiveCard.isNonBasic(),
            width = 58.dp,
            height = 58.dp * 1.4f,
        )
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.inverseSurface,
            modifier = Modifier.offset(y = (-10).dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = ShieldChipGold, modifier = Modifier.size(12.dp))
                Text(
                    shields.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
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
                    // colorLabel(), not displayText() - Basic/Advanced only matters for the
                    // objective card, not a merely-discarded one.
                    add(DescriptionSpan.Words(" ${card.colorLabel()}"))
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
                    // colorLabel(), not displayText() - Basic/Advanced only matters for the
                    // objective card, not a merely-revealed one.
                    add(DescriptionSpan.Words(" ${card.colorLabel()}"))
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
            add(DescriptionSpan.Words(" ${objectiveCard.displayText()} - Completed."))
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
