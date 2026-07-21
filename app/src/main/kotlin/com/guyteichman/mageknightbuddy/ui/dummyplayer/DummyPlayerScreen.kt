package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.guyteichman.mageknightbuddy.R
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.help.HelpButton
import kotlinx.coroutines.launch

private const val DUMMY_PLAYER_SETUP_ROUTE = "dummy_player_setup"
private const val DUMMY_PLAYER_AI_ROUTE = "dummy_player_ai"

/**
 * Root composable for the Dummy Player tab: the Knight-select setup screen is the tab's start
 * destination, with the AI (turn/round) screen pushed on top of it once a session is started or
 * restored. Runs its own nested [NavHost], the same pattern [com.guyteichman.mageknightbuddy.ui.scoreboard.ScoreboardTab]
 * uses, so this tab's own back stack (setup vs. AI screen) is independent of the app's top-level
 * tab switching.
 *
 * @param fieldHelp the bundled "?" help text/citations (see [FieldHelp]), threaded down to the AI
 * screen's End Round dialog so it can show a [HelpButton] instead of a raw rulebook filename
 * reference (issue #88).
 */
@Composable
fun DummyPlayerTab(repository: DummyPlayerSessionRepository, fieldHelp: Map<String, FieldHelp>) {
    val nestedNavController = rememberNavController()

    NavHost(navController = nestedNavController, startDestination = DUMMY_PLAYER_SETUP_ROUTE) {
        composable(DUMMY_PLAYER_SETUP_ROUTE) {
            DummyPlayerSetupScreen(
                repository = repository,
                // Both Start and Restore Game land on the same AI-screen route - once a session
                // exists (freshly started or restored), the AI screen just loads whatever's saved.
                onStart = { nestedNavController.navigate(DUMMY_PLAYER_AI_ROUTE) },
                onRestore = { nestedNavController.navigate(DUMMY_PLAYER_AI_ROUTE) },
            )
        }
        composable(DUMMY_PLAYER_AI_ROUTE) {
            // Back is a plain pop with no confirmation - autosave means nothing is ever unsaved
            // (per #27).
            DummyPlayerAiScreen(repository = repository, fieldHelp = fieldHelp, onBack = { nestedNavController.popBackStack() })
        }
    }
}

/**
 * The Knight-select setup screen: pick a Knight (or "Random"), then either Start a fresh session
 * or Restore a previously saved one.
 */
@Composable
private fun DummyPlayerSetupScreen(
    repository: DummyPlayerSessionRepository,
    onStart: () -> Unit,
    onRestore: () -> Unit,
) {
    val viewModel: DummyPlayerSetupViewModel = viewModel(factory = DummyPlayerSetupViewModel.factory(repository))
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Dummy Player")

        KnightPicker(
            knight = viewModel.knight,
            wasRandom = viewModel.wasRandom,
            onKnightSelected = viewModel::pickKnight,
            onRandomSelected = viewModel::pickRandom,
        )

        Button(
            onClick = {
                // Building the session and autosaving it is a suspend call (Room I/O), so it
                // needs a coroutine scope; onStart() only runs after that save completes.
                scope.launch {
                    viewModel.start()
                    onStart()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start")
        }

        OutlinedButton(
            onClick = onRestore,
            enabled = viewModel.hasSavedSession,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Restore Game")
        }
    }
}

/**
 * The Knight dropdown: "Random" is an entry alongside the 8 Knights, not a separate control.
 * Picking "Random" resolves immediately (see [DummyPlayerSetupViewModel.pickRandom]), so the
 * field then shows the rolled Knight with a "(Random)" suffix rather than staying on "Random".
 *
 * Every entry gets a leading shield icon: real per-Knight art via [KnightShieldIcon] where it's
 * been sourced (issue #69), falling back to a generic shield glyph for Coral until hers lands too.
 * "Random" reuses that same generic glyph with a "?" overlaid (see [RandomShieldIcon]) to mark
 * it as the wildcard choice, distinct from the plain per-Knight entries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnightPicker(
    knight: Knight,
    wasRandom: Boolean,
    onKnightSelected: (Knight) -> Unit,
    onRandomSelected: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (wasRandom) "${knight.displayName} (Random)" else knight.displayName

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Knight") },
            leadingIcon = { KnightShieldIcon(knight = knight) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Random") },
                leadingIcon = { RandomShieldIcon() },
                onClick = {
                    onRandomSelected()
                    expanded = false
                },
            )
            Knight.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    leadingIcon = { KnightShieldIcon(knight = option) },
                    onClick = {
                        onKnightSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * A Knight's shield-token art at [size], or a generic shield glyph for any Knight whose art
 * hasn't been sourced yet (currently just Coral - see issue #69). [Image] renders the drawable's
 * actual pixels (the shield icon), unlike [Icon], which is meant for single-color glyphs - so
 * [tint] only affects the fallback glyph, never the real art. Defaults to [LocalContentColor] so
 * the fallback matches whatever color plain [Icon]s already use in its call site (e.g. the picker's
 * leading icons); callers with a different prior tint (e.g. [HeroRow]) pass it explicitly.
 */
@Composable
private fun KnightShieldIcon(knight: Knight, size: Dp = 24.dp, tint: Color = LocalContentColor.current) {
    val resId = knight.shieldIconRes
    if (resId != null) {
        Image(painter = painterResource(resId), contentDescription = null, modifier = Modifier.size(size))
    } else {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}

/** Maps a [Knight] to its shield-token drawable, or null where the art hasn't been sourced yet. */
private val Knight.shieldIconRes: Int?
    get() = when (this) {
        Knight.ARYTHEA -> R.drawable.arythea_shield
        Knight.TOVAK -> R.drawable.tovak_shield
        Knight.KRANG -> R.drawable.krang_shield
        Knight.BRAEVALAR -> R.drawable.braevalar_shield
        Knight.WOLFHAWK -> R.drawable.wolfhawk_shield
        Knight.GOLDYX -> R.drawable.goldyx_shield
        Knight.NOROWAS -> R.drawable.norowas_shield
        Knight.CORAL -> null
    }

/**
 * The "Random" dropdown entry's icon: the same generic shield glyph [KnightShieldIcon]'s fallback
 * uses, with a "?" overlaid on top to distinguish it as the wildcard/random choice rather than a
 * specific Knight.
 */
@Composable
private fun RandomShieldIcon(size: Dp = 24.dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(size))
        Text(
            "?",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(bottom = size * 0.04f),
        )
    }
}

/**
 * The AI (turn/round) screen: shows the Dummy Player's current deck/crystal state and event log,
 * and drives the two mutating actions - Play Turn and End Round - defined by
 * `docs/rules/dummy-player.md`. Follows the same repository-backed ViewModel pattern as the setup
 * screen, autosaving after every mutation (issue #35).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DummyPlayerAiScreen(repository: DummyPlayerSessionRepository, fieldHelp: Map<String, FieldHelp>, onBack: () -> Unit) {
    val viewModel: DummyPlayerAiViewModel = viewModel(factory = DummyPlayerAiViewModel.factory(repository))
    val scope = rememberCoroutineScope()
    val session = viewModel.session

    var showSummary by remember { mutableStateOf(false) }
    var showEndRoundDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dummy Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (session != null) {
                        TextButton(onClick = { showSummary = !showSummary }) {
                            Text(if (showSummary) "Hide Summary" else "Summary")
                        }
                        RoundChip(round = session.round)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
            )
        },
        bottomBar = {
            // Bottom action row instead of a Scaffold-managed bottomBar surface, since only the
            // two buttons need it and Row + padding is simpler than a full BottomAppBar here.
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
            // Restoring from Room is asynchronous (see DummyPlayerAiViewModel's init block); this
            // only shows for the brief window before that first restore completes.
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { HeroRow(session = session) }
                item { TableauCard(session = session) }
                if (showSummary) {
                    item { StatGridCard(session = session) }
                }
                item {
                    Text("Log", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Most-recent-first, per issue #35's layout spec - the domain log itself is
                // append-only/chronological, so this screen is what reverses it for display.
                items(session.log.asReversed()) { event ->
                    LogRow(event = event)
                }
            }
        }
    }

    if (showEndRoundDialog) {
        EndRoundDialog(
            fieldHelp = fieldHelp,
            onDismiss = { showEndRoundDialog = false },
            onConfirm = { advancedActionColor, spellColor ->
                scope.launch { viewModel.endRound(advancedActionColor, spellColor) }
                showEndRoundDialog = false
            },
        )
    }
}

/** The pill-shaped "ROUND N" indicator in the top bar. */
@Composable
private fun RoundChip(round: Int) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            "ROUND $round",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Knight shield icon, name, and a "Random" badge if the Knight was randomly rolled at setup. */
@Composable
private fun HeroRow(session: DummyPlayerSession) {
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
 * The card-tableau: how many cards are left in the deck, a pile of their colors grouped by
 * [CardColor] (not the actual shuffled deck order - a real player wouldn't know that order ahead
 * of time either, so this only conveys the same per-color counts the tally row below states
 * numerically), a per-color count breakdown, and the crystal Inventory. Mirrors Variant D of the
 * `prototype/dummy-player-ai-screen` mock, the winning layout from issue #28.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableauCard(session: DummyPlayerSession) {
    // fillMaxWidth on the Card itself - without it, a Card sizes to wrap its widest child, which
    // used to be the full-width mini-card row when the deck was full. As the deck (and that row)
    // shrinks, nothing else here forces full width, so the whole card would visibly narrow too.
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.deckOrder.size.toString(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "cards left in deck",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // heightIn(min) reserves 2 rows' worth of space always, so the card shrinks/grows
            // by at most a few dp as the pile empties instead of visibly collapsing row-by-row
            // (a 16-card starting deck wraps to 2 rows on a typical phone width).
            FlowRow(
                modifier = Modifier.heightIn(min = 61.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                session.deckOrder.sortedBy { it.ordinal }.forEach { color -> MiniCard(color = color) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CardColor.entries.forEach { color ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CardColorDot(color = color)
                        Text(
                            session.remainingByColor.getValue(color).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

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

/** The alternate, denser per-color tile grid (Variant A of the prototype), shown when "Summary" is toggled on. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatGridCard(session: DummyPlayerSession) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CardColor.entries.forEach { color ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.widthIn(max = 56.dp),
                    ) {
                        CardColorDot(color = color)
                        Text(session.remainingByColor.getValue(color).toString(), style = MaterialTheme.typography.titleMedium)
                        // Crystal icons instead of a "N crystal(s)" caption - matches how
                        // TableauCard shows crystals, so the count is read the same way in both views.
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
    }
}

/** A single small rounded-rectangle standing in for one card in the deck pile. */
@Composable
private fun MiniCard(color: CardColor) {
    Box(
        modifier = Modifier
            .size(width = 20.dp, height = 28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.swatch)
            .then(if (color == CardColor.WHITE) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)) else Modifier),
    )
}

/** A small colored square used as a compact per-color legend marker. [size] is the square's edge. */
@Composable
private fun CardColorDot(color: CardColor, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(color.swatch)
            .then(if (color == CardColor.WHITE) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp)) else Modifier),
    )
}

/**
 * A small diamond standing in for one crystal in the Inventory, sized to fill exactly a [size] x
 * [size] box - same as [CardColorDot] - so the two read as the same size given the same [size].
 * Drawn as an explicit [DiamondShape] rather than a rotated square: a rotated square's rendered
 * pixels extend past its own layout bounds (rotate() doesn't change reported layout size), which
 * left it up to whatever container this sits in (e.g. FilterChip's icon slot) whether that
 * overflow is visible or clipped - too fragile to guarantee matching [CardColorDot] reliably.
 */
@Composable
private fun CrystalIcon(color: CardColor, size: Dp = 17.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(DiamondShape)
            .background(color.swatch)
            .then(if (color == CardColor.WHITE) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, DiamondShape) else Modifier),
    )
}

/** A diamond/rhombus shape filling its full bounding box - the [CrystalIcon] glyph. */
private val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

/** One row of the event log: an icon standing in for the event kind, then its title/meta/description. */
@Composable
private fun LogRow(event: DummyPlayerEvent) {
    val (icon, title, meta, description) = event.describe()
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
            // buildAnnotatedString + inlineContent renders the CardColorDot spans inline with the
            // surrounding text, wrapping naturally as part of the paragraph rather than as a
            // separate row - the same "color indicators inside the sentence" the prototype used.
            Text(
                text = buildAnnotatedString {
                    description.forEach { span ->
                        when (span) {
                            is DescriptionSpan.Words -> append(span.text)
                            is DescriptionSpan.ColorDot -> appendInlineContent(colorDotInlineContentId(span.color), span.color.label)
                            is DescriptionSpan.CrystalDot -> appendInlineContent(crystalDotInlineContentId(span.color), span.color.label)
                        }
                    }
                },
                inlineContent = colorDotInlineContent + crystalDotInlineContent,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** The four pieces of text/icon a [LogRow] needs - title/meta/description phrased from `docs/rules/dummy-player.md`'s terms. */
private data class LogEntryText(val icon: String, val title: String, val meta: String, val description: List<DescriptionSpan>)

/**
 * One chunk of a [LogEntryText.description]: plain words, an inline [CardColorDot] for a card of
 * that color, or an inline [CrystalIcon] for a crystal of that color - kept distinct so the log
 * always draws the right glyph for what's actually being described (e.g. a Spell offer discard's
 * color denotes the crystal it grants, not a card, so it renders as [CrystalDot]).
 */
private sealed interface DescriptionSpan {
    data class Words(val text: String) : DescriptionSpan
    data class ColorDot(val color: CardColor) : DescriptionSpan
    data class CrystalDot(val color: CardColor) : DescriptionSpan
}

private fun colorDotInlineContentId(color: CardColor) = "color_dot_${color.name}"
private fun crystalDotInlineContentId(color: CardColor) = "crystal_dot_${color.name}"

/**
 * The inline-content maps [LogRow]'s [Text] needs to render [DescriptionSpan.ColorDot]/
 * [DescriptionSpan.CrystalDot] - one entry per [CardColor] each, built once since there are only 4
 * of each and their appearance never changes.
 */
private val colorDotInlineContent: Map<String, InlineTextContent> = CardColor.entries.associate { color ->
    colorDotInlineContentId(color) to InlineTextContent(
        placeholder = Placeholder(width = 12.sp, height = 12.sp, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
    ) {
        CardColorDot(color = color)
    }
}

private val crystalDotInlineContent: Map<String, InlineTextContent> = CardColor.entries.associate { color ->
    crystalDotInlineContentId(color) to InlineTextContent(
        placeholder = Placeholder(width = 12.sp, height = 12.sp, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
    ) {
        CrystalIcon(color = color)
    }
}

private fun DummyPlayerEvent.describe(): LogEntryText = when (this) {
    is DummyPlayerEvent.RoundStarted -> LogEntryText(
        icon = "◆",
        title = "Round started",
        meta = "Round $round",
        description = listOf(DescriptionSpan.Words("The Dummy Player's deck is shuffled and ready.")),
    )
    is DummyPlayerEvent.TurnPlayed -> {
        val allRevealed = initialReveal + additionalReveal
        val description = buildList {
            add(DescriptionSpan.Words("Revealed "))
            allRevealed.forEachIndexed { index, color ->
                if (index > 0) add(DescriptionSpan.Words(" "))
                add(DescriptionSpan.ColorDot(color))
            }
            add(DescriptionSpan.Words(" — "))
            if (additionalReveal.isEmpty()) {
                add(DescriptionSpan.Words("no crystal match, turn ended."))
            } else {
                // The dot here stands for the matching crystal held in Inventory, not a card, so
                // it's a CrystalDot even though initialReveal.last() is also a revealed card's color.
                add(DescriptionSpan.CrystalDot(initialReveal.last()))
                add(DescriptionSpan.Words(" ${initialReveal.last().label} crystal matched, +${additionalReveal.size} revealed: "))
                additionalReveal.forEachIndexed { index, color ->
                    if (index > 0) add(DescriptionSpan.Words(", "))
                    add(DescriptionSpan.ColorDot(color))
                    add(DescriptionSpan.Words(" ${color.label}"))
                }
                add(DescriptionSpan.Words("."))
            }
        }
        LogEntryText(icon = "▶", title = "Turn played", meta = "Round $round", description = description)
    }
    is DummyPlayerEvent.EndOfRoundAnnounced -> LogEntryText(
        icon = "⚑",
        title = "End of Round announced",
        meta = "Round $round",
        description = listOf(DescriptionSpan.Words("The deck ran out - other players get one more turn each, then the Round ends.")),
    )
    is DummyPlayerEvent.RoundEnded -> LogEntryText(
        icon = "⚑",
        title = "Round ended",
        meta = "Round $round",
        // Describes only the round-prep offer swap, which happens every Round regardless of why
        // it ended (per docs/rules/dummy-player.md) - accurate whether or not the deck actually
        // ran out first, so it never claims "the deck ran out" itself.
        description = listOf(
            DescriptionSpan.Words("Advanced Action offer discard ("),
            DescriptionSpan.ColorDot(advancedActionOfferColor),
            DescriptionSpan.Words(" ${advancedActionOfferColor.label}) added to the deck. Spell offer discard ("),
            // CrystalDot, not ColorDot - this discard grants a crystal, not a card, so the color
            // it denotes is the crystal added to Inventory.
            DescriptionSpan.CrystalDot(spellOfferColor),
            DescriptionSpan.Words(" ${spellOfferColor.label}) granted +1 crystal."),
        ),
    )
}

/**
 * Prompts for the two round-prep offer-discard colors before calling [DummyPlayerAiViewModel.endRound].
 * Tapping Cancel (or dismissing the dialog any other way) just closes it - [onConfirm] is the only
 * path that calls [DummyPlayerAiViewModel.endRound], so nothing has mutated yet for Cancel to undo.
 * The title row carries a [HelpButton] (issue #88) so the round-prep rule itself is a rulebook-cited
 * in-app dialog instead of the raw `docs/rules/dummy-player.md` filename this used to print - that
 * file ships with the repo, not the installed app, so referencing it directly was a dead end for a
 * real player mid-game.
 */
@Composable
private fun EndRoundDialog(
    fieldHelp: Map<String, FieldHelp>,
    onDismiss: () -> Unit,
    onConfirm: (advancedActionColor: CardColor, spellColor: CardColor) -> Unit,
) {
    var advancedActionColor by remember { mutableStateOf(CardColor.entries.first()) }
    var spellColor by remember { mutableStateOf(CardColor.entries.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // usePlatformDefaultWidth = false + an explicit fillMaxWidth lets the dialog use most of
        // the screen's width instead of Material's narrow default, which was forcing the color
        // chips (and the intro paragraph) into awkward mid-word line wraps.
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
                ColorPickerRow(
                    label = "Advanced Action offer",
                    selected = advancedActionColor,
                    onSelect = { advancedActionColor = it },
                    // A card of this color is what's added to the deck - CardColorDot fits. Both
                    // chipIcons here pass the same `size` (12.dp) so the square and the diamond
                    // read as the same size in the chip, not just the same nominal Dp value.
                    chipIcon = { color -> CardColorDot(color = color, size = 12.dp) },
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
            TextButton(onClick = { onConfirm(advancedActionColor, spellColor) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * One labeled row of the 4 [CardColor] choices, rendered as selectable chips.
 * FlowRow, not a plain Row - a dialog's width is too narrow to fit all 4 labeled chips on one
 * line, and a non-wrapping Row would squeeze the last chip into a near-zero-width column instead.
 * [chipIcon] lets each offer show the glyph for what its color actually denotes (a card added to
 * the deck vs. a crystal granted), rather than a fixed icon regardless of meaning.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerRow(
    label: String,
    selected: CardColor,
    onSelect: (CardColor) -> Unit,
    chipIcon: @Composable (CardColor) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardColor.entries.forEach { color ->
                FilterChip(
                    selected = color == selected,
                    onClick = { onSelect(color) },
                    label = { Text(color.label) },
                    leadingIcon = { chipIcon(color) },
                )
            }
        }
    }
}

private val CardColor.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

private val CardColor.swatch: Color
    get() = when (this) {
        CardColor.RED -> Color(0xFFB5423A)
        CardColor.GREEN -> Color(0xFF3E7C4A)
        CardColor.BLUE -> Color(0xFF3768A6)
        CardColor.WHITE -> Color(0xFFFBFAFF)
    }
