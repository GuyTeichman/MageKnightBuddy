package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.ManaColor
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareCard
import com.guyteichman.mageknightbuddy.domain.VolkareEvent
import com.guyteichman.mageknightbuddy.domain.VolkareSession
import com.guyteichman.mageknightbuddy.ui.components.CardColorDot
import com.guyteichman.mageknightbuddy.ui.components.LabelPillPicker
import com.guyteichman.mageknightbuddy.ui.components.LabeledSwitch
import com.guyteichman.mageknightbuddy.ui.components.ManaColorDot
import com.guyteichman.mageknightbuddy.ui.components.NumberField
import com.guyteichman.mageknightbuddy.ui.components.difficultyPillColor
import com.guyteichman.mageknightbuddy.ui.components.label
import com.guyteichman.mageknightbuddy.ui.components.swatch
import kotlinx.coroutines.launch

/**
 * Volkare mode's setup fields: which Volkare scenario, Race Level, and how many Wounds go in his
 * deck - shown on the shared setup screen (`DummyPlayerScreen.kt`'s `DummyPlayerSetupScreen`)
 * instead of nothing further once "Volkare" is picked in place of a Knight. [woundCountIsCustom]
 * comes straight from [VolkareSetupViewModel] - it only affects the field's label, since
 * [LabelPillPicker] itself has no notion of "no pill selected".
 */
@Composable
fun VolkareSetupFields(
    scenario: Scenario,
    raceLevel: RaceLevel,
    woundCount: Int,
    woundCountIsCustom: Boolean,
    onScenarioSelected: (Scenario) -> Unit,
    onRaceLevelSelected: (RaceLevel) -> Unit,
    onWoundCountChanged: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LabelPillPicker(
            label = "Scenario",
            options = listOf(Scenario.VolkaresReturn, Scenario.VolkaresQuest),
            selected = scenario,
            displayName = { it.displayName },
            // Flat, uncolored pills (per LabelPillPicker's own doc comment) - Return vs. Quest
            // isn't an ordinal difficulty axis like Combat/Race Level, so a gradient would
            // misleadingly imply one is "harder" than the other.
            color = { Color(0xFFE0E0E0) },
            onSelect = onScenarioSelected,
        )
        LabelPillPicker(
            label = "Race Level",
            options = RaceLevel.entries,
            selected = raceLevel,
            displayName = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
            color = { difficultyPillColor(it.ordinal, RaceLevel.entries.size) },
            onSelect = onRaceLevelSelected,
        )
        NumberField(
            label = if (woundCountIsCustom) "Wounds in Volkare's deck (custom)" else "Wounds in Volkare's deck",
            value = woundCount.toString(),
            onValueChange = { text -> text.toIntOrNull()?.let(onWoundCountChanged) },
        )
    }
}

/**
 * Volkare mode's AI (turn/round) screen: shows his deck composition and event log, and drives
 * [VolkareAiViewModel.playTurn]/[endRound][VolkareAiViewModel.endRound] -
 * [toggleCityRevealed][VolkareAiViewModel.toggleCityRevealed] - the Volkare counterpart to
 * `DummyPlayerScreen.kt`'s `DummyPlayerAiScreen`, minus any crystal display (Volkare has none) and
 * with an End Round that applies immediately rather than opening a dialog (there's no round-prep
 * offer input to collect - see `CONTEXT.md`'s "Volkare Session" entry, "ending round is purely a
 * player convenience for tracking, not a game mechanic").
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VolkareAiScreen(repository: VolkareSessionRepository, onBack: () -> Unit) {
    val viewModel: VolkareAiViewModel = viewModel(factory = VolkareAiViewModel.factory(repository))
    val scope = rememberCoroutineScope()
    val session = viewModel.session

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volkare") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (session != null) {
                        VolkareRoundChip(round = session.round)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
            )
        },
        bottomBar = {
            if (session != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { scope.launch { viewModel.playTurn() } },
                        // Volkare's Return has no equivalent guard - Frenzy keeps him playable
                        // forever once his deck is empty; only Volkare's Quest can set `lost`.
                        enabled = !session.lost && !viewModel.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Play Turn")
                    }
                    OutlinedButton(
                        onClick = { scope.launch { viewModel.endRound() } },
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
            // Restoring from Room is asynchronous (see VolkareAiViewModel's init block); this only
            // shows for the brief window before that first restore completes.
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { VolkareHeaderRow(session = session) }
                // Only Volkare's Return ever reads cityRevealed - see CONTEXT.md's "City Revealed" entry.
                if (session.scenario == Scenario.VolkaresReturn) {
                    item {
                        LabeledSwitch(
                            label = "City Revealed",
                            checked = session.cityRevealed,
                            onCheckedChange = { scope.launch { viewModel.toggleCityRevealed() } },
                        )
                    }
                }
                if (session.lost) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                "Volkare reached the portal - you lost this scenario.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                item { VolkareTableauCard(session = session) }
                item {
                    Text("Log", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Most-recent-first, matching DummyPlayerAiScreen's log ordering.
                items(session.log.asReversed()) { event ->
                    VolkareLogRow(event = event, scenario = session.scenario)
                }
            }
        }
    }
}

/** The pill-shaped "ROUND N" indicator in the top bar - a Volkare-mode-local copy of `DummyPlayerScreen.kt`'s file-private `RoundChip`. */
@Composable
private fun VolkareRoundChip(round: Int) {
    Surface(shape = RoundedCornerShape(percent = 50), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text("ROUND $round", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

/** Volkare's shield icon, the active scenario's name, and a Race Level badge. The Volkare-mode counterpart to `DummyPlayerScreen.kt`'s `HeroRow`. */
@Composable
private fun VolkareHeaderRow(session: VolkareSession) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VolkareShieldIcon(size = 32.dp)
        Text(session.scenario.displayName, style = MaterialTheme.typography.titleMedium)
        Surface(shape = RoundedCornerShape(percent = 50), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                session.raceLevel.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * How many cards are left in Volkare's deck, grouped by kind/color (not his deck's actual draw
 * order - like `DummyPlayerScreen.kt`'s `TableauCard`, showing the real order would leak which
 * card comes up next, information a real player wouldn't have either). No crystal section - see
 * `CONTEXT.md`'s "Volkare Session" entry: Volkare has none.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolkareTableauCard(session: VolkareSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.deckOrder.size.toString(), style = MaterialTheme.typography.headlineMedium)
                Text("cards left in deck", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(
                modifier = Modifier.heightIn(min = 61.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                session.deckOrder.sortedBy { it.tableauSortKey }.forEach { card -> VolkareMiniCard(card = card) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "${session.deckOrder.count { it is VolkareCard.BasicAction }} Actions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${session.deckOrder.count { it is VolkareCard.CompetitiveSpell }} Spells",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${session.deckOrder.count { it is VolkareCard.Wound }} Wounds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Groups [VolkareTableauCard]'s pile by color, Spell right after that color's 4 Basic Actions -
 * not by [VolkareSession.deckOrder]'s actual (order-revealing) position. Wounds carry no color, so
 * they sort last, after every color's group.
 */
private val VolkareCard.tableauSortKey: Int
    get() = when (this) {
        is VolkareCard.BasicAction -> color.ordinal * 2
        is VolkareCard.CompetitiveSpell -> color.ordinal * 2 + 1
        VolkareCard.Wound -> 100
    }

/** Wound mini-cards' burgundy fill and near-black border - deliberately distinct from every [CardColor] swatch, since a Wound carries no color of its own. */
private val WoundSwatch = Color(0xFF5C1220)
private val WoundBorder = Color(0xFF2A0A10)

/** Competitive Spell mini-cards' trim - a light gold, echoing [ManaColor.GOLD]'s swatch, to read as "magical" against any of the 4 [CardColor] fills a Spell can have. */
private val SpellTrim = Color(0xFFF6D667)

/**
 * One card-shaped tile in [VolkareTableauCard]'s pile - a Volkare-mode copy of `DummyPlayerScreen.kt`'s
 * `MiniCard`, extended for the 3-kind [VolkareCard] shape instead of a plain [CardColor]: a Wound
 * gets its own [WoundSwatch]/[WoundBorder] and a blood-drop glyph, and a Spell gets a gold [SpellTrim]
 * border and a small sparkle glyph (the same "✦" used for Spell reveals in the log) on top of its
 * color's own fill, so the two card kinds read apart from a same-color Basic Action at a glance.
 */
@Composable
private fun VolkareMiniCard(card: VolkareCard) {
    val background = when (card) {
        is VolkareCard.BasicAction -> card.color.swatch
        is VolkareCard.CompetitiveSpell -> card.color.swatch
        VolkareCard.Wound -> WoundSwatch
    }
    val border = when {
        card is VolkareCard.CompetitiveSpell -> Modifier.border(2.dp, SpellTrim, RoundedCornerShape(4.dp))
        card is VolkareCard.Wound -> Modifier.border(1.5.dp, WoundBorder, RoundedCornerShape(4.dp))
        card is VolkareCard.BasicAction && card.color == CardColor.WHITE -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
        else -> Modifier
    }
    Box(
        modifier = Modifier
            .size(width = 20.dp, height = 28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .then(border),
        contentAlignment = Alignment.Center,
    ) {
        when (card) {
            VolkareCard.Wound -> Text("🩸", fontSize = 11.sp)
            is VolkareCard.CompetitiveSpell -> Text("✦", fontSize = 11.sp, color = SpellTrim)
            is VolkareCard.BasicAction -> {}
        }
    }
}

/** One row of the event log - a Volkare-mode copy of `DummyPlayerScreen.kt`'s `LogRow`. */
@Composable
private fun VolkareLogRow(event: VolkareEvent, scenario: Scenario) {
    val (icon, title, meta, description) = event.describe(scenario)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon)
        }
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = buildAnnotatedString {
                    description.forEach { span ->
                        when (span) {
                            is VolkareDescriptionSpan.Words -> append(span.text)
                            is VolkareDescriptionSpan.ColorDot -> appendInlineContent(volkareColorDotInlineContentId(span.color), span.color.label)
                            is VolkareDescriptionSpan.ManaDot -> appendInlineContent(volkareManaDotInlineContentId(span.color), span.color.label)
                        }
                    }
                },
                inlineContent = volkareColorDotInlineContent + volkareManaDotInlineContent,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** The four pieces of text/icon a [VolkareLogRow] needs - a Volkare-mode copy of `DummyPlayerScreen.kt`'s `LogEntryText`. */
private data class VolkareLogEntryText(val icon: String, val title: String, val meta: String, val description: List<VolkareDescriptionSpan>)

/** One chunk of a [VolkareLogEntryText.description] - words, a [CardColor] dot, or a [ManaColor] dot (a Wound reveal's mana roll - see `VolkareSession.playTurn`). */
private sealed interface VolkareDescriptionSpan {
    data class Words(val text: String) : VolkareDescriptionSpan
    data class ColorDot(val color: CardColor) : VolkareDescriptionSpan
    data class ManaDot(val color: ManaColor) : VolkareDescriptionSpan
}

private fun volkareColorDotInlineContentId(color: CardColor) = "volkare_color_dot_${color.name}"
private fun volkareManaDotInlineContentId(color: ManaColor) = "volkare_mana_dot_${color.name}"

private val volkareColorDotInlineContent: Map<String, InlineTextContent> = CardColor.entries.associate { color ->
    volkareColorDotInlineContentId(color) to InlineTextContent(
        placeholder = Placeholder(width = 12.sp, height = 12.sp, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
    ) {
        CardColorDot(color = color)
    }
}

private val volkareManaDotInlineContent: Map<String, InlineTextContent> = ManaColor.entries.associate { color ->
    volkareManaDotInlineContentId(color) to InlineTextContent(
        placeholder = Placeholder(width = 12.sp, height = 12.sp, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
    ) {
        ManaColorDot(color = color)
    }
}

/**
 * Turns one [VolkareEvent] into log text, per the "Course of the Game" procedures in
 * `docs/rules/volkares-return.md`/`volkares-quest.md`. Like `DummyPlayerScreen.kt`'s `describe()`,
 * this only narrates what a card *means procedurally* - see ADR-0004 - it never claims to know
 * Volkare's actual board position, so every "if adjacent"/"if this brings him into the city" clause
 * below is a reminder for the player to check by hand, not a computed condition.
 */
private fun VolkareEvent.describe(scenario: Scenario): VolkareLogEntryText = when (this) {
    is VolkareEvent.RoundStarted -> VolkareLogEntryText(
        icon = "◆",
        title = "Round started",
        meta = "Round $round",
        description = listOf(VolkareDescriptionSpan.Words("Volkare's deck is set - drawn once, never reshuffled.")),
    )
    is VolkareEvent.CardRevealed -> describeCardRevealed(this, scenario)
    is VolkareEvent.Frenzy -> VolkareLogEntryText(
        icon = "⚡",
        title = "Frenzy",
        meta = "Round $round",
        description = listOf(
            VolkareDescriptionSpan.Words(
                "Volkare's deck is empty. He moves/attacks twice as if a blue Spell were revealed, with no Source die reroll.",
            ),
        ),
    )
    is VolkareEvent.RoundEnded -> VolkareLogEntryText(
        icon = "⚑",
        title = "Round ended",
        meta = "Round $round",
        description = listOf(VolkareDescriptionSpan.Words("Tracking convenience only - nothing changes for Volkare.")),
    )
    is VolkareEvent.QuestLost -> VolkareLogEntryText(
        icon = "☠",
        title = "Volkare reached the portal",
        meta = "Round $round",
        description = listOf(
            VolkareDescriptionSpan.Words(
                "That was his last non-Wound card - his final move takes him into the portal. You lost this scenario.",
            ),
        ),
    )
}

private fun describeCardRevealed(event: VolkareEvent.CardRevealed, scenario: Scenario): VolkareLogEntryText = when (val card = event.card) {
    VolkareCard.Wound -> describeWound(event, scenario)
    is VolkareCard.BasicAction -> describeMove(event.round, card.color, isSpell = false, scenario = scenario, cityRevealed = event.cityRevealed)
    is VolkareCard.CompetitiveSpell -> describeMove(event.round, card.color, isSpell = true, scenario = scenario, cityRevealed = event.cityRevealed)
}

private fun describeWound(event: VolkareEvent.CardRevealed, scenario: Scenario): VolkareLogEntryText {
    val manaRoll = event.manaRoll ?: error("A Wound reveal always carries a mana roll - see VolkareSession.playTurn")
    // Return adds a gray army token when the roll finds a Unit; Quest just scares that Unit off -
    // see CONTEXT.md's "Wound"-adjacent rules and docs/rules/volkares-quest.md's "Volkare's turn".
    val verb = if (scenario == Scenario.VolkaresReturn) "recruits" else "scares off"
    return VolkareLogEntryText(
        icon = "✚",
        title = "Wound revealed",
        meta = "Round ${event.round}",
        description = listOf(
            VolkareDescriptionSpan.Words("Volkare rests and $verb the "),
            VolkareDescriptionSpan.ManaDot(manaRoll),
            VolkareDescriptionSpan.Words(" ${manaRoll.label} unit."),
        ),
    )
}

private fun describeMove(round: Int, color: CardColor, isSpell: Boolean, scenario: Scenario, cityRevealed: Boolean): VolkareLogEntryText {
    val title = if (isSpell) "Spell revealed" else "Action revealed"
    val icon = if (isSpell) "✦" else "▶"
    val description = buildList {
        add(VolkareDescriptionSpan.Words("Revealed "))
        add(VolkareDescriptionSpan.ColorDot(color))
        add(VolkareDescriptionSpan.Words(" ${color.label} - "))
        addAll(movementClause(color, isSpell, scenario, cityRevealed))
    }
    return VolkareLogEntryText(icon = icon, title = title, meta = "Round $round", description = description)
}

private fun movementClause(color: CardColor, isSpell: Boolean, scenario: Scenario, cityRevealed: Boolean): List<VolkareDescriptionSpan> {
    // City Revealed collapses the rulebook's separate Race-for-the-City/Battle-for-the-City phases
    // into this one flag (see CONTEXT.md's "City Revealed" entry) - once set, every color (red
    // included) advances him toward the city instead of red triggering an attack.
    if (scenario == Scenario.VolkaresReturn && cityRevealed) {
        return advanceTowardCityClause(isSpell)
    }
    if (color == CardColor.RED) {
        val text = if (scenario == Scenario.VolkaresReturn && isSpell) {
            // Only Volkare's Return's red Spell gets the "two spaces away, then returns" nuance -
            // Volkare's Quest treats red Action/Spell identically (docs/rules/volkares-quest.md).
            "Volkare doesn't move - he attacks the highest-Fame Mage Knight within two spaces, then returns."
        } else {
            "Volkare doesn't move - he attacks the highest-Fame adjacent Mage Knight."
        }
        return listOf(VolkareDescriptionSpan.Words(text))
    }
    return if (scenario == Scenario.VolkaresReturn) exploringClause(isSpell) else questClause(isSpell)
}

private fun advanceTowardCityClause(isSpell: Boolean): List<VolkareDescriptionSpan> {
    val move = if (isSpell) {
        "Volkare advances twice toward the city (one Source die reroll)."
    } else {
        "Volkare advances one space toward the city and rerolls a matching Source die."
    }
    return listOf(VolkareDescriptionSpan.Words("$move If this brings him into the city, resolve the Battle for the City."))
}

private fun exploringClause(isSpell: Boolean): List<VolkareDescriptionSpan> {
    val text = if (isSpell) {
        "Volkare moves twice in this direction (one Source die reroll)."
    } else {
        "Volkare moves one space in this direction and rerolls a matching Source die (gold, if none)."
    }
    return listOf(VolkareDescriptionSpan.Words(text))
}

private fun questClause(isSpell: Boolean): List<VolkareDescriptionSpan> {
    val text = if (isSpell) {
        "Volkare moves twice toward the portal (one Source die reroll) - enters the portal instead if already adjacent."
    } else {
        "Volkare moves one space toward the portal in this direction (or closer, if that leaves the map) - enters the portal instead if already adjacent."
    }
    return listOf(VolkareDescriptionSpan.Words(text))
}
