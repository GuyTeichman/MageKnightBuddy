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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.ui.components.CardColorDot
import com.guyteichman.mageknightbuddy.ui.components.CrystalIcon
import com.guyteichman.mageknightbuddy.ui.components.KnightShieldIcon
import com.guyteichman.mageknightbuddy.ui.components.LabeledCheckbox
import com.guyteichman.mageknightbuddy.ui.components.label
import com.guyteichman.mageknightbuddy.ui.components.swatch
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.help.HelpButton
import kotlinx.coroutines.launch

private const val DUMMY_PLAYER_SETUP_ROUTE = "dummy_player_setup"
private const val DUMMY_PLAYER_AI_ROUTE = "dummy_player_ai"
private const val VOLKARE_AI_ROUTE = "volkare_ai"
private const val PROXY_PLAYER_AI_ROUTE = "proxy_player_ai"

/**
 * Which of the Dummy Player tab's 3 modes is currently selected on the setup screen - see
 * `CONTEXT.md`'s "Dummy Player tab" entry. Replaces the old `volkareSelected: Boolean` flag now
 * that Proxy Player also needs its own selected state alongside Standard and Volkare.
 */
internal enum class DummyPlayerMode { STANDARD, VOLKARE, PROXY_PLAYER }

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
fun DummyPlayerTab(
    repository: DummyPlayerSessionRepository,
    volkareRepository: VolkareSessionRepository,
    proxyPlayerRepository: ProxyPlayerSessionRepository,
    fieldHelp: Map<String, FieldHelp>,
) {
    val nestedNavController = rememberNavController()

    NavHost(navController = nestedNavController, startDestination = DUMMY_PLAYER_SETUP_ROUTE) {
        composable(DUMMY_PLAYER_SETUP_ROUTE) {
            DummyPlayerSetupScreen(
                repository = repository,
                volkareRepository = volkareRepository,
                proxyPlayerRepository = proxyPlayerRepository,
                // Both Start and Restore Game land on the same AI-screen route - once a session
                // exists (freshly started or restored), the AI screen just loads whatever's saved.
                onStart = { nestedNavController.navigate(DUMMY_PLAYER_AI_ROUTE) },
                onRestore = { nestedNavController.navigate(DUMMY_PLAYER_AI_ROUTE) },
                onStartVolkare = { nestedNavController.navigate(VOLKARE_AI_ROUTE) },
                onRestoreVolkare = { nestedNavController.navigate(VOLKARE_AI_ROUTE) },
                onStartProxyPlayer = { nestedNavController.navigate(PROXY_PLAYER_AI_ROUTE) },
                onRestoreProxyPlayer = { nestedNavController.navigate(PROXY_PLAYER_AI_ROUTE) },
            )
        }
        composable(DUMMY_PLAYER_AI_ROUTE) {
            // Back is a plain pop with no confirmation - autosave means nothing is ever unsaved
            // (per #27).
            DummyPlayerAiScreen(repository = repository, fieldHelp = fieldHelp, onBack = { nestedNavController.popBackStack() })
        }
        composable(VOLKARE_AI_ROUTE) {
            VolkareAiScreen(repository = volkareRepository, onBack = { nestedNavController.popBackStack() })
        }
        composable(PROXY_PLAYER_AI_ROUTE) {
            ProxyPlayerAiScreen(repository = proxyPlayerRepository, fieldHelp = fieldHelp, onBack = { nestedNavController.popBackStack() })
        }
    }
}

/**
 * The setup screen: a top-level choice between **Volkare** (no Knight at all - his deck/turn
 * rules don't depend on one) and **a Knight** ([VolkareOrKnightSelector]); only within the Knight
 * branch does [StandardOrProxyPlayerSelector] then offer Standard vs. Proxy Player, since those
 * two are just different depths of the same Knight-backed Dummy Player, not a peer choice to
 * Volkare (see `CONTEXT.md`'s "Dummy Player tab" entry). Either way, Start or Restore Game lands
 * on the matching AI screen. Hosts [DummyPlayerSetupViewModel], [VolkareSetupViewModel], and
 * [ProxyPlayerSetupViewModel] side by side (only the one matching [DummyPlayerMode] drives the
 * visible fields/Start at a time) so switching between modes mid-setup doesn't lose any of their
 * state, and so Restore Game can compare all 3 repositories' recency regardless of which mode is
 * currently selected. Picking a Knight (or Random) while in the Knight branch updates *both*
 * [DummyPlayerSetupViewModel] and [ProxyPlayerSetupViewModel] together (see [onKnightSelected]/
 * [onRandomSelected] below), so toggling Standard/Proxy Player afterward keeps showing the same
 * Knight instead of jumping to whichever ViewModel wasn't just edited.
 */
@Composable
private fun DummyPlayerSetupScreen(
    repository: DummyPlayerSessionRepository,
    volkareRepository: VolkareSessionRepository,
    proxyPlayerRepository: ProxyPlayerSessionRepository,
    onStart: () -> Unit,
    onRestore: () -> Unit,
    onStartVolkare: () -> Unit,
    onRestoreVolkare: () -> Unit,
    onStartProxyPlayer: () -> Unit,
    onRestoreProxyPlayer: () -> Unit,
) {
    val viewModel: DummyPlayerSetupViewModel = viewModel(factory = DummyPlayerSetupViewModel.factory(repository))
    val volkareViewModel: VolkareSetupViewModel = viewModel(factory = VolkareSetupViewModel.factory(volkareRepository))
    val proxyPlayerViewModel: ProxyPlayerSetupViewModel = viewModel(factory = ProxyPlayerSetupViewModel.factory(proxyPlayerRepository))
    val scope = rememberCoroutineScope()
    // rememberSaveable, not plain remember: this drives which of the 3 ViewModels above is
    // "active", so it needs to survive navigating away to the AI screen and back (plain remember
    // state is lost when Navigation Compose disposes this composable while another route is shown).
    var mode by rememberSaveable { mutableStateOf(DummyPlayerMode.STANDARD) }
    // Feeds whichever of the 3 ViewModels' start() ends up called below - shared across all
    // modes (not one per ViewModel) since it's the same physical fact ("did the table start this
    // game at night") regardless of which mode is chosen.
    var startsAtNight by rememberSaveable { mutableStateOf(false) }
    // Plain remember (not rememberSaveable): re-queried fresh via LaunchedEffect below every time
    // this composable enters composition (including navigating back from the AI screen), the same
    // "don't trust a stale cache of on-disk state" reasoning DummyPlayerSetupViewModel.hasSavedSession
    // already uses - RestoreGamePreview isn't Parcelable/Serializable anyway, so it couldn't be
    // saved across process death the way rememberSaveable state can be.
    var restorePreview by remember { mutableStateOf<RestoreGamePreview?>(null) }
    LaunchedEffect(Unit) {
        restorePreview = loadRestoreGamePreview(repository, volkareRepository, proxyPlayerRepository)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Dummy Player")

        // Top-level choice: Volkare has no Knight at all, so it's a peer of "a Knight" as a whole,
        // not of Standard/Proxy Player individually (those are 2 depths of the same Knight-backed
        // opponent - see StandardOrProxyPlayerSelector below).
        VolkareOrKnightSelector(
            isVolkare = mode == DummyPlayerMode.VOLKARE,
            onSelectVolkare = { mode = DummyPlayerMode.VOLKARE },
            // Falls back to Standard, not whatever `mode` held before Volkare was selected -
            // "Knight" isn't itself a mode, Standard is just a sensible default entry point.
            onSelectKnight = { mode = DummyPlayerMode.STANDARD },
        )

        if (mode == DummyPlayerMode.VOLKARE) {
            VolkareSetupFields(
                scenario = volkareViewModel.scenario,
                raceLevel = volkareViewModel.raceLevel,
                woundCount = volkareViewModel.woundCount,
                woundCountIsCustom = volkareViewModel.woundCountIsCustom,
                onScenarioSelected = volkareViewModel::pickScenario,
                onRaceLevelSelected = volkareViewModel::pickRaceLevel,
                onWoundCountChanged = volkareViewModel::changeWoundCount,
            )
        } else {
            // Always reads/writes viewModel's (Standard's) Knight state, not
            // proxyPlayerViewModel's - onKnightSelected/onRandomSelected below keep both in sync,
            // so it doesn't matter which one backs the display as long as it's consistent.
            KnightOnlyPicker(
                knight = viewModel.knight,
                wasRandom = viewModel.wasRandom,
                onKnightSelected = { knight ->
                    viewModel.pickKnight(knight)
                    proxyPlayerViewModel.pickKnight(knight)
                },
                onRandomSelected = {
                    viewModel.pickRandom()
                    // Reuse the same rolled Knight for Proxy Player instead of an independent
                    // re-roll, so toggling Standard/Proxy Player after "Random" never shows a
                    // different Knight than the one just rolled.
                    proxyPlayerViewModel.pickKnight(viewModel.knight)
                },
            )
            // Only offered once a Knight is already selected (i.e. Volkare wasn't) - see this
            // function's doc comment.
            StandardOrProxyPlayerSelector(mode = mode, onModeSelected = { mode = it })
        }

        // Shown regardless of which mode is selected above - most Mage Knight scenarios start at
        // day (Round 1), so this defaults unchecked. Day/night for any later Round is then just
        // derived from the Round number (see isDayRound), not tracked turn-by-turn.
        LabeledCheckbox(
            checked = startsAtNight,
            onCheckedChange = { startsAtNight = it },
            label = "Starts at night?",
        )

        Button(
            onClick = {
                // Building the session and autosaving it is a suspend call (Room I/O), so it
                // needs a coroutine scope; onStart()/onStartVolkare()/onStartProxyPlayer() only
                // run after that save completes.
                scope.launch {
                    when (mode) {
                        DummyPlayerMode.STANDARD -> { viewModel.start(startsAtNight); onStart() }
                        DummyPlayerMode.VOLKARE -> { volkareViewModel.start(startsAtNight); onStartVolkare() }
                        DummyPlayerMode.PROXY_PLAYER -> { proxyPlayerViewModel.start(startsAtNight); onStartProxyPlayer() }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start")
        }

        // Shown above the button (issue #125's 2nd item) so the player knows what they're about
        // to resume before tapping it - only once restorePreview has actually loaded, since
        // LaunchedEffect's load is async and briefly leaves it null right after this screen
        // appears.
        restorePreview?.let { preview -> RestoreGamePreviewRow(preview) }

        OutlinedButton(
            onClick = {
                // Restore Game always resumes whichever of the 3 saved sessions is most recent,
                // regardless of what's currently selected above - see this function's doc comment.
                // Re-loads fresh here (rather than trusting the restorePreview state above) so the
                // navigation decision is always correct as of the actual tap, not a possibly-stale
                // snapshot from whenever this screen last entered composition.
                scope.launch {
                    when (loadRestoreGamePreview(repository, volkareRepository, proxyPlayerRepository)?.mode) {
                        DummyPlayerMode.VOLKARE -> onRestoreVolkare()
                        DummyPlayerMode.PROXY_PLAYER -> onRestoreProxyPlayer()
                        else -> onRestore()
                    }
                }
            },
            enabled = viewModel.hasSavedSession || volkareViewModel.hasSavedSession || proxyPlayerViewModel.hasSavedSession,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Restore Game")
        }
    }
}

/** Human-readable label for a [DummyPlayerMode] - matches the setup screen's own selector chip text. */
private val DummyPlayerMode.label: String
    get() = when (this) {
        DummyPlayerMode.STANDARD -> "Standard"
        DummyPlayerMode.VOLKARE -> "Volkare"
        DummyPlayerMode.PROXY_PLAYER -> "Proxy Player"
    }

/**
 * A compact preview of what "Restore Game" would resume right now (issue #125's 2nd item): the
 * Knight's shield icon (or [VolkareShieldIcon] for Volkare mode, which has no Knight), its name,
 * which of the 3 modes it is, and its round/turn (mirrors the AI screen's own "ROUND N · TURN M"
 * header - issue #125's 1st item). Shown just above the "Restore Game" button so the player can
 * see what they're about to resume before tapping it, rather than only finding out after.
 */
@Composable
private fun RestoreGamePreviewRow(preview: RestoreGamePreview) {
    // Local val, not a repeated preview.knight read: Knight is declared in the domain module, so
    // Kotlin can't smart-cast a nullable property read from a different module across two
    // statements (same cross-module limitation ProxyPlayerScreen.kt's objectiveCard hits).
    val knight = preview.knight
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (knight != null) KnightShieldIcon(knight = knight, size = 28.dp) else VolkareShieldIcon(size = 28.dp)
        Column {
            Text(
                // Volkare mode already names itself in the "Volkare" line below, so its own title
                // line is just "Volkare" rather than repeating the mode label a second time -
                // Standard/Proxy Player instead lead with the actual Knight's name.
                knight?.displayName ?: "Volkare",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                // Volkare's title line above already says "Volkare", so its subtitle only needs
                // Round/Turn - Standard/Proxy Player's subtitle also states the mode, since their
                // title line is the Knight's name instead.
                if (preview.mode == DummyPlayerMode.VOLKARE) {
                    "Round ${preview.round} · Turn ${preview.turn}"
                } else {
                    "${preview.mode.label} · Round ${preview.round} · Turn ${preview.turn}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The top-level Volkare-vs-Knight choice - see [DummyPlayerSetupScreen]'s doc comment for why this
 * is a 2-way choice rather than 3-way alongside Standard/Proxy Player.
 */
@Composable
private fun VolkareOrKnightSelector(isVolkare: Boolean, onSelectVolkare: () -> Unit, onSelectKnight: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !isVolkare, onClick = onSelectKnight, label = { Text("Knight") })
        FilterChip(selected = isVolkare, onClick = onSelectVolkare, label = { Text("Volkare") })
    }
}

/**
 * Once a Knight is selected (i.e. Volkare wasn't), which depth of Dummy Player to play as - see
 * `CONTEXT.md`'s "Proxy Player" entry.
 */
@Composable
private fun StandardOrProxyPlayerSelector(mode: DummyPlayerMode, onModeSelected: (DummyPlayerMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = mode == DummyPlayerMode.STANDARD, onClick = { onModeSelected(DummyPlayerMode.STANDARD) }, label = { Text("Standard") })
        FilterChip(selected = mode == DummyPlayerMode.PROXY_PLAYER, onClick = { onModeSelected(DummyPlayerMode.PROXY_PLAYER) }, label = { Text("Proxy Player") })
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
 *
 * Used by both Standard and Proxy Player mode ([DummyPlayerMode]) - Volkare mode picks no Knight
 * at all (see `CONTEXT.md`'s "Volkare" entry), and mode selection itself now lives one level up
 * in [DummyPlayerModeSelector] rather than as a hidden entry in this dropdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnightOnlyPicker(
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
            // Sorted alphabetically by displayName for the picker only (matches the Score
            // Calculator's Setup page Knight picker, issue #110) - Knight.entries itself stays
            // in rulebook/release order, since other code has no reason to care about it.
            Knight.entries.sortedBy { it.displayName }.forEach { option ->
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
 * The "Volkare" dropdown entry's icon - his official portrait art (a circular crop of his Lost
 * Legion box/rulebook illustration), the antagonist counterpart to [KnightShieldIcon]'s per-Knight
 * shield tokens. Not `private`: also used from `VolkareScreen.kt`'s `VolkareHeaderRow`.
 */
@Composable
internal fun VolkareShieldIcon(size: Dp = 24.dp) {
    Image(
        painter = painterResource(R.drawable.volkare_shield),
        contentDescription = null,
        modifier = Modifier.size(size),
    )
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
                        RoundChip(round = session.round, turn = session.turnInRound)
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
                // The toggle button now lives inside DeckPanel's own header instead of the top
                // app bar, so it reads as attached to the panel it controls; the panel's body
                // still swaps mutually exclusively rather than showing both at once.
                item {
                    DeckPanel(showSummary = showSummary, onToggleSummary = { showSummary = !showSummary }) {
                        if (showSummary) StatGridBody(session = session) else TableauBody(session = session)
                    }
                }
                item {
                    Text("Log", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Most-recent-first, per issue #35's layout spec - the domain log itself is
                // append-only/chronological, so this screen is what reverses it for display.
                items(session.log.asReversed()) { event ->
                    LogRow(entry = event.describe())
                }
            }
        }
    }

    if (showEndRoundDialog) {
        EndRoundDialog(
            fieldHelp = fieldHelp,
            onDismiss = { showEndRoundDialog = false },
            onConfirm = { advancedActionOfferColor, spellColor ->
                scope.launch { viewModel.endRound(advancedActionOfferColor, spellColor) }
                showEndRoundDialog = false
            },
        )
    }
}

/**
 * The pill-shaped "ROUND N · TURN M" indicator in the top bar - [turn] is
 * [DummyPlayerSession.turnInRound]/`ProxyPlayerSession.turnInRound` (issue #125), how many turns
 * have already been played in the current [round], so a player picking the app back up mid-game
 * can immediately see where they left off (e.g. "ROUND 2 · TURN 4").
 *
 * `internal`, not `private`: `ProxyPlayerScreen.kt`'s `ProxyPlayerAiScreen` reuses this directly.
 */
@Composable
internal fun RoundChip(round: Int, turn: Int) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            "ROUND $round · TURN $turn",
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
 * The deck panel's shared shell: a title, the Summary/Full View toggle button (styled and placed
 * so it reads as part of this panel rather than a stray top-bar label - issue feedback was that
 * the old top-app-bar TextButton looked detached from the thing it controlled), and [content]
 * (either [TableauBody] or [StatGridBody]) below. `internal`, not `private`: `ProxyPlayerScreen.kt`
 * has its own copy ([ProxyPlayerDeckPanel]) rather than sharing this one, matching this file's
 * existing duplication rationale for [MiniCard]/[TableauCard]-shaped composables.
 */
@Composable
private fun DeckPanel(showSummary: Boolean, onToggleSummary: () -> Unit, content: @Composable () -> Unit) {
    // fillMaxWidth on the Card itself - without it, a Card sizes to wrap its widest child, which
    // used to be the full-width mini-card row when the deck was full. As the deck (and that row)
    // shrinks, nothing else here forces full width, so the whole card would visibly narrow too.
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Deck", style = MaterialTheme.typography.titleMedium)
                // OutlinedButton (bordered, filled-adjacent) instead of the old TextButton, which
                // read as plain text rather than a tappable control.
                OutlinedButton(onClick = onToggleSummary, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
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
 * The card-tableau body: how many cards are left in the deck, a pile of their colors grouped by
 * [CardColor] (not the actual shuffled deck order - a real player wouldn't know that order ahead
 * of time either, so this only conveys the same per-color counts the tally row below states
 * numerically), a per-color count breakdown, and the crystal Inventory. Mirrors Variant D of the
 * `prototype/dummy-player-ai-screen` mock, the winning layout from issue #28. Rendered inside
 * [DeckPanel]'s Column, so its children lay out as siblings of that Column's header row - no Card
 * or padding of its own.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableauBody(session: DummyPlayerSession) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(session.deckOrder.size.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(
            "left in deck",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // heightIn(min) reserves 2 rows' worth of space always, so the panel shrinks/grows by at most
    // a few dp as the pile empties instead of visibly collapsing row-by-row (a 16-card starting
    // deck wraps to 2 rows on a typical phone width).
    FlowRow(
        modifier = Modifier.heightIn(min = 61.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        session.deckOrder.sortedBy { it.sortKey() }.forEach { identity -> MiniCard(colors = identity.colors()) }
    }

    // Cards and Crystals side by side, in matching label-above-content shape (a "Cards" title
    // mirroring "Crystals", each above their own value row) - issue feedback was that Crystals had
    // a title above its icons but the per-color card breakdown didn't, reading as asymmetric.
    // Matches ProxyPlayerScreen.kt's ProxyPlayerTableauBody.
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Cards",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
 * The alternate, denser per-color tile grid (Variant A of the prototype) body, replacing
 * [TableauBody] when "Summary" is toggled on. Rendered inside [DeckPanel]'s Column - no Card or
 * padding of its own.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatGridBody(session: DummyPlayerSession) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        CardColor.entries.forEach { color ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.widthIn(max = 56.dp),
            ) {
                CardColorDot(color = color)
                Text(session.remainingByColor.getValue(color).toString(), style = MaterialTheme.typography.titleMedium)
                // Crystal icons instead of a "N crystal(s)" caption - matches how TableauBody
                // shows crystals, so the count is read the same way in both views.
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

/**
 * A single small rounded-rectangle standing in for one card in the deck pile. Renders one
 * full-width swatch for [colors] with 1 entry, or 2 half-width swatches side by side for a
 * Dual-Color card's 2 colors ([CardIdentity.colors]) - a minimal, functional placeholder rather
 * than a final design; see ADR-0005's Consequences for why the dual-color visual treatment isn't
 * decided yet.
 *
 * `internal`, not `private`, and taking `colors: List<CardColor>` rather than a `CardIdentity`
 * directly: `ProxyPlayerScreen.kt`'s deck tableau reuses this for `List<ProxyPlayerCard>`, whose
 * colors come from [ProxyPlayerCard.colors] instead, but the swatch-rendering itself is identical.
 */
@Composable
internal fun MiniCard(colors: List<CardColor>, isNonBasic: Boolean = false, width: Dp = 20.dp, height: Dp = 28.dp) {
    // The outer Box lets the star badge (isNonBasic) overlay the swatch's corner without affecting
    // its own layout - the inner Row is what actually draws the swatch(es) and sizes/clips to fill it.
    Box(modifier = Modifier.size(width = width, height = height)) {
        // Row, not a single Box: colors is a 1- or 2-element list, so this naturally draws one
        // full-width swatch or two half-width swatches side by side, without a separate branch for
        // each case.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp)),
        ) {
            // forEachIndexed, not forEach: a White swatch's border shape depends on whether this
            // swatch sits at the left/right edge of the whole MiniCard (see swatchOuterCorners),
            // which needs both the index and the total count.
            colors.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color.swatch)
                        .then(
                            if (color == CardColor.WHITE) {
                                val corners = swatchOuterCorners(index, colors.size)
                                Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(
                                        topStart = if (corners.topStart) 4.dp else 0.dp,
                                        topEnd = if (corners.topEnd) 4.dp else 0.dp,
                                        bottomEnd = if (corners.bottomEnd) 4.dp else 0.dp,
                                        bottomStart = if (corners.bottomStart) 4.dp else 0.dp,
                                    ),
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
        if (isNonBasic) {
            // Advanced Action / Unique cards get a star badge - Basic Action cards get nothing. A
            // dark circular scrim sits behind the star so it stays visible even on a White card's
            // swatch, where a bare white star would nearly disappear; centering (rather than a
            // corner) keeps it clear of the White swatch's own border too. Sized relative to
            // height (not a fixed Dp) so it stays proportional whether this MiniCard is drawn at
            // deck-tray size or enlarged (see the Objective section's use of this same composable).
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(height / 2.2f)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Advanced Action or Unique card",
                    tint = Color.White,
                    modifier = Modifier.size(height / 3.2f),
                )
            }
        }
    }
}

/**
 * Which corners of a [MiniCard] swatch at position [index] (of [count] total swatches) are also
 * outer corners of the whole card - i.e. should stay rounded rather than square. A single-color
 * card ([count] == 1) has its one swatch touch all 4 corners; a dual-color card's left swatch only
 * touches the 2 left corners, and its right swatch only the 2 right corners. Kept as a plain data
 * class (no Compose types) so this corner-picking logic can be unit-tested on the JVM without an
 * emulator, per this project's domain-testability convention.
 */
internal data class SwatchCorners(val topStart: Boolean, val topEnd: Boolean, val bottomStart: Boolean, val bottomEnd: Boolean)

/**
 * Computes [SwatchCorners] for the swatch at [index] out of [count] total swatches in a [MiniCard].
 * Used to fix #147: a White swatch's border used to round all 4 corners unconditionally, which
 * looked fine for a single-color card but drew a visible rounded notch at the seam between two
 * halves of a dual-color card, since the inner corner facing the other half isn't actually an
 * outer corner of the card.
 */
internal fun swatchOuterCorners(index: Int, count: Int): SwatchCorners {
    val isLeftmost = index == 0
    val isRightmost = index == count - 1
    return SwatchCorners(
        topStart = isLeftmost,
        bottomStart = isLeftmost,
        topEnd = isRightmost,
        bottomEnd = isRightmost,
    )
}

/** The color(s) [identity] renders/matches as - one color for [CardIdentity.SingleColor], two for [CardIdentity.DualColor]. */
internal fun CardIdentity.colors(): List<CardColor> = when (this) {
    is CardIdentity.SingleColor -> listOf(color)
    is CardIdentity.DualColor -> listOf(colorA, colorB)
}

/** Sort key for laying out a deck of [CardIdentity]s in a stable, color-grouped order (see [CardColor]'s declaration order). */
private fun CardIdentity.sortKey(): Int = colors().minOf { it.ordinal }

/** Display text for [identity] - a single color's label, or both colors' labels joined with "/" for a dual-color card. */
internal val CardIdentity.displayLabel: String
    get() = colors().joinToString("/") { it.label }

// CardColorDot, CrystalIcon, DiamondShape, and the CardColor.swatch/label mappings moved to
// ui/components/CardColorIcons.kt - the Score Calculator's Solo Conquest Challenge pages need the
// same color icons for its Knight-specific color checkboxes, so this is no longer single-use.

/**
 * One row of the event log: an icon standing in for the event kind, then its title/meta/description.
 *
 * `internal`, and taking a pre-built [LogEntryText] rather than a `DummyPlayerEvent` directly:
 * `ProxyPlayerScreen.kt`'s log reuses this same rendering for `ProxyPlayerEvent`'s own `describe()`.
 */
@Composable
internal fun LogRow(entry: LogEntryText) {
    val (icon, title, meta, description) = entry
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
internal data class LogEntryText(val icon: String, val title: String, val meta: String, val description: List<DescriptionSpan>)

/**
 * One chunk of a [LogEntryText.description]: plain words, an inline [CardColorDot] for a card of
 * that color, or an inline [CrystalIcon] for a crystal of that color - kept distinct so the log
 * always draws the right glyph for what's actually being described (e.g. a Spell offer discard's
 * color denotes the crystal it grants, not a card, so it renders as [CrystalDot]).
 */
internal sealed interface DescriptionSpan {
    data class Words(val text: String) : DescriptionSpan
    data class ColorDot(val color: CardColor) : DescriptionSpan
    data class CrystalDot(val color: CardColor) : DescriptionSpan
}

/**
 * Appends one [DescriptionSpan.ColorDot] per color in [colors] to this list - one dot for a
 * single-color card, two (separated by "/") for a dual-color one.
 * `MutableList<DescriptionSpan>` here is the receiver `buildList { ... }` gives its lambda, so this
 * extension is only usable inside a `buildList` block like the ones in [DummyPlayerEvent.describe].
 */
internal fun MutableList<DescriptionSpan>.addCardDots(colors: List<CardColor>) {
    colors.forEachIndexed { index, color ->
        if (index > 0) add(DescriptionSpan.Words("/"))
        add(DescriptionSpan.ColorDot(color))
    }
}

internal fun colorDotInlineContentId(color: CardColor) = "color_dot_${color.name}"
internal fun crystalDotInlineContentId(color: CardColor) = "crystal_dot_${color.name}"

/**
 * The inline-content maps [LogRow]'s [Text] needs to render [DescriptionSpan.ColorDot]/
 * [DescriptionSpan.CrystalDot] - one entry per [CardColor] each, built once since there are only 4
 * of each and their appearance never changes.
 */
internal val colorDotInlineContent: Map<String, InlineTextContent> = CardColor.entries.associate { color ->
    colorDotInlineContentId(color) to InlineTextContent(
        placeholder = Placeholder(width = 12.sp, height = 12.sp, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
    ) {
        CardColorDot(color = color)
    }
}

internal val crystalDotInlineContent: Map<String, InlineTextContent> = CardColor.entries.associate { color ->
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
        // The 3rd initial-reveal card decides the chain (see DummyPlayerSession.playTurn) - may be
        // a CardIdentity.DualColor card, in which case both its colors' crystals contributed.
        val lastCard = initialReveal.last()
        val description = buildList {
            add(DescriptionSpan.Words("Revealed "))
            allRevealed.forEachIndexed { index, card ->
                if (index > 0) add(DescriptionSpan.Words(" "))
                addCardDots(card.colors())
            }
            add(DescriptionSpan.Words(" — "))
            if (additionalReveal.isEmpty()) {
                add(DescriptionSpan.Words("no crystal match, turn ended."))
            } else {
                // The dot(s) here stand for the matching crystal(s) held in Inventory, not a card,
                // so they're CrystalDots even though lastCard is also a revealed card.
                lastCard.colors().forEach { color -> add(DescriptionSpan.CrystalDot(color)) }
                add(DescriptionSpan.Words(" ${lastCard.displayLabel} crystal matched, +${additionalReveal.size} revealed: "))
                additionalReveal.forEachIndexed { index, card ->
                    if (index > 0) add(DescriptionSpan.Words(", "))
                    addCardDots(card.colors())
                    add(DescriptionSpan.Words(" ${card.displayLabel}"))
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
        description = buildList {
            add(DescriptionSpan.Words("Advanced Action offer discard ("))
            addCardDots(advancedActionOfferColor.colors())
            add(DescriptionSpan.Words(" ${advancedActionOfferColor.displayLabel}) added to the deck. Spell offer discard ("))
            // CrystalDot, not ColorDot - this discard grants a crystal, not a card, so the color
            // it denotes is the crystal added to Inventory. Spells are never dual-color.
            add(DescriptionSpan.CrystalDot(spellOfferColor))
            add(DescriptionSpan.Words(" ${spellOfferColor.label}) granted +1 crystal."))
        },
    )
}

/**
 * Prompts for the round-prep offer-discards before calling [DummyPlayerAiViewModel.endRound]: the
 * Advanced Action offer's card (one of 4 colors or one of the 4 known Dual-Color cards, see
 * [IdentityPickerRow]) and the Spell offer's color. Tapping Cancel (or dismissing the dialog any
 * other way) just closes it - [onConfirm] is the only path that calls
 * [DummyPlayerAiViewModel.endRound], so nothing has mutated yet for Cancel to undo. The title row
 * carries a [HelpButton] (issue #88) so the round-prep rule itself is a rulebook-cited in-app
 * dialog instead of the raw `docs/rules/dummy-player.md` filename this used to print - that file
 * ships with the repo, not the installed app, so referencing it directly was a dead end for a
 * real player mid-game.
 */
@Composable
private fun EndRoundDialog(
    fieldHelp: Map<String, FieldHelp>,
    onDismiss: () -> Unit,
    onConfirm: (advancedActionOfferColor: CardIdentity, spellColor: CardColor) -> Unit,
) {
    var advancedActionIdentity by remember { mutableStateOf<CardIdentity>(CardIdentity.SingleColor(CardColor.entries.first())) }
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

/**
 * One labeled row of [CardColor] choices, rendered as selectable chips - all 4 colors. FlowRow,
 * not a plain Row - a dialog's width is too narrow to fit all 4 labeled chips on one line, and a
 * non-wrapping Row would squeeze the last chip into a near-zero-width column instead. [chipIcon]
 * lets each offer show the glyph for what its color actually denotes (a card added to the deck vs.
 * a crystal granted), rather than a fixed icon regardless of meaning.
 *
 * `internal`, not `private`: `ProxyPlayerScreen.kt`'s `ProxyPlayerEndRoundDialog` reuses this
 * directly rather than duplicating it, since its Spell-offer picker is visually identical to the
 * standard Dummy Player's End Round dialog.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ColorPickerRow(
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

/**
 * One labeled row of [CardIdentity] choices, rendered as selectable chips - the 4 single colors
 * plus the 4 known Dual-Color Advanced Action cards ([CardIdentity.DUAL_COLOR_CARDS]), since
 * that's the complete, closed set an Advanced Action offer card can ever be - see
 * `docs/rules/proxy-player.md`'s dual-color table. Enumerating the 4 real cards directly (instead
 * of a "Dual-color card" checkbox plus a free second-color picker) means there's no way to select
 * a color pair that doesn't correspond to an actual printed card.
 *
 * `internal`, not `private`: `ProxyPlayerScreen.kt`'s `ProxyPlayerEndRoundDialog` reuses this
 * directly, for the same reason it reuses [ColorPickerRow].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun IdentityPickerRow(label: String, selected: CardIdentity, onSelect: (CardIdentity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        // Centered (not left-packed): the 4 single-color chips usually fill their row edge-to-edge,
        // but the 4 dual-color chips below wrap 2-per-row and, left-aligned, left a lopsided gap of
        // empty space on the right - issue feedback was to center those wrapped rows instead.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ADVANCED_ACTION_OFFER_OPTIONS.forEach { identity ->
                FilterChip(
                    selected = identity == selected,
                    onClick = { onSelect(identity) },
                    label = { Text(identity.displayLabel) },
                    // A card of this/these color(s) is what's added to the deck - CardColorDot
                    // fits, one dot per color the card counts as (1 for a single-color chip, 2 for
                    // a dual-color one).
                    leadingIcon = {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            identity.colors().forEach { color -> CardColorDot(color = color, size = 12.dp) }
                        }
                    },
                )
            }
        }
    }
}

/** The 8 choices [IdentityPickerRow] offers: every single color, plus the 4 known dual-color cards. */
private val ADVANCED_ACTION_OFFER_OPTIONS: List<CardIdentity> =
    CardColor.entries.map { CardIdentity.SingleColor(it) } + CardIdentity.DUAL_COLOR_CARDS

