package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.help.HelpButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One entry per wizard page, in the fixed rulebook order the enum declares. Not every scenario
 * visits every page - see [wizardPagesFor], which picks the ordered subset that matches a given
 * [Scenario]'s `*ScoringInput` shape (e.g. CITIES_CONQUERED only applies to Solo Conquest,
 * CITY_REVEALED only to First Reconnaissance). `title` is the page heading; `helpKeys` are the
 * [FieldHelp] map keys whose entries should appear when the page's "?" button is tapped (a page
 * can pull in more than one help entry, or none).
 */
private enum class WizardPage(val title: String, val helpKeys: List<String> = emptyList()) {
    SETUP("Setup"),
    FAME("Fame", listOf("Fame")),
    GREATEST_KNOWLEDGE("Greatest Knowledge", listOf("Greatest Knowledge")),
    GREATEST_LEADER("Greatest Leader", listOf("Greatest Leader")),
    GREATEST_ADVENTURER("Greatest Adventurer", listOf("Greatest Adventurer")),
    GREATEST_LOOT("Greatest Loot", listOf("Greatest Loot")),
    GREATEST_CONQUEROR("Greatest Conqueror", listOf("Greatest Conqueror")),
    GREATEST_BEATING("Greatest Beating", listOf("Greatest Beating")),
    GREATEST_QUESTER("Greatest Quester", listOf("Greatest Quester")),
    CITIES_CONQUERED("Cities Conquered", listOf("Cities Conquered", "All Cities Conquered")),
    CITY_REVEALED("City Revealed", listOf("City Revealed")),
    HIGH_PRIESTESS_DEFEATED("High Priestess Defeated", listOf("High Priestess Defeated")),
    GRAVEYARDS_SEALED("Graveyards Sealed", listOf("Graveyards Sealed", "Necromancer Defeated")),
    ROUNDS_FINISHED_EARLY("Rounds Finished Early", listOf("Rounds Finished Early")),
    DUMMY_PLAYER_STATUS("Dummy Player Status", listOf("Dummy Player's Deck", "End of Round")),
    COUNCIL_QUEST_POINTS("Quest Points", listOf("For the Council: Quest Points")),
    COUNCIL_REPUTATION("Reputation", listOf("For the Council: Reputation")),
    RESULT("Result"),
}

// The 6 Standard Achievements pages, shared by every scenario that has a Standard Achievements
// section (every scenario except For the Council).
private val standardAchievementPages = listOf(
    WizardPage.GREATEST_KNOWLEDGE,
    WizardPage.GREATEST_LEADER,
    WizardPage.GREATEST_ADVENTURER,
    WizardPage.GREATEST_LOOT,
    WizardPage.GREATEST_CONQUEROR,
    WizardPage.GREATEST_BEATING,
)

/**
 * The ordered page sequence for [scenario]'s wizard, built from its `*ScoringInput`'s actual
 * field shape (see the matching domain data class) rather than one fixed sequence for every
 * scenario. Exhaustive `when` over the sealed [Scenario] - the compiler flags it if a new
 * Scenario is added without a page sequence for it.
 */
private fun wizardPagesFor(scenario: Scenario): List<WizardPage> = when (scenario) {
    Scenario.SoloConquest -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.GREATEST_QUESTER,
            WizardPage.CITIES_CONQUERED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    Scenario.FirstReconnaissance -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.CITY_REVEALED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    Scenario.HiddenValley -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.HIGH_PRIESTESS_DEFEATED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    Scenario.RealmOfTheDead -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.GRAVEYARDS_SEALED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    // For the Council has no Fame/Standard Achievements at all (docs/rules/for-the-council.md,
    // Scoring > Solo) - it's Quest-driven, so its sequence is built from scratch.
    Scenario.ForTheCouncil -> listOf(
        WizardPage.SETUP,
        WizardPage.COUNCIL_QUEST_POINTS,
        WizardPage.COUNCIL_REPUTATION,
        WizardPage.RESULT,
    )
}

/**
 * Top-level screen for the Score Calculator tab: the paginated Solo Conquest scoring wizard (see
 * docs/design/architecture.md's "Score Calculator flow"). Hosts the reset FAB and confirmation
 * dialog around [WizardContent], which renders the actual per-page fields.
 *
 * @param repository where a completed [com.guyteichman.mageknightbuddy.domain.ScoringSession] gets saved.
 * @param fieldHelp the bundled "?" help text/citations (see [FieldHelp]), keyed by [WizardPage.helpKeys].
 * @param onDone called after a successful save, to navigate back to the Scoreboard tab.
 */
@Composable
fun ScoreCalculatorScreen(
    repository: ScoringSessionRepository,
    fieldHelp: Map<String, FieldHelp>,
    onDone: () -> Unit,
) {
    // `viewModel(factory = ...)` is the Compose idiom for hoisting an Android ViewModel into a
    // composable: it creates the ViewModel on first composition and hands back the *same*
    // instance across recompositions (and, per ADR-0002, across this tab being navigated away
    // from and back). The factory is needed because this ViewModel's constructor takes a
    // repository the default lookup can't provide on its own.
    val viewModel: ScoreCalculatorViewModel = viewModel(factory = ScoreCalculatorViewModel.factory(repository))
    // A CoroutineScope tied to this composable's lifecycle, used below to launch the suspend
    // `viewModel.save()` call from a plain (non-suspend) button click handler.
    val scope = rememberCoroutineScope()
    // Recomputed on every recomposition (cheap - a handful of enum values), so switching the
    // Scenario dropdown on the SETUP page immediately reflects the new scenario's page sequence.
    val wizardPages = wizardPagesFor(viewModel.scenario)
    // Defensive clamp: pageIndex is restored from SavedStateHandle across process death, and a
    // shorter sequence (e.g. switching from Solo Conquest's 13 pages to For the Council's 4)
    // could otherwise leave it pointing past the end of the new list.
    val currentPage = wizardPages[viewModel.pageIndex.coerceIn(wizardPages.indices)]
    // `remember { mutableStateOf(false) }` is plain Compose state, scoped to this composition -
    // fine here (unlike the wizard fields above) because a dismissed confirmation dialog isn't
    // something that needs to survive a tab switch.
    var showResetConfirmation by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        WizardContent(
            viewModel = viewModel,
            wizardPages = wizardPages,
            currentPage = currentPage,
            fieldHelp = fieldHelp,
            scope = scope,
            onDone = onDone,
        )

        ExtendedFloatingActionButton(
            onClick = { showResetConfirmation = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New scoring session") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 88.dp, end = 16.dp),
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Discard this entry?") },
            text = { Text("Any unsaved progress on this scoring session will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    showResetConfirmation = false
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Renders the header (step counter, "?" help button, progress bar), the current page's fields,
 * and the Previous/Next/Done navigation row. All page state (which page, every field's value)
 * comes from [viewModel]; this composable itself holds no state of its own.
 */
@Composable
private fun WizardContent(
    viewModel: ScoreCalculatorViewModel,
    wizardPages: List<WizardPage>,
    currentPage: WizardPage,
    fieldHelp: Map<String, FieldHelp>,
    scope: CoroutineScope,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Step ${viewModel.pageIndex + 1} of ${wizardPages.size}: ${currentPage.title}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (currentPage.helpKeys.isNotEmpty()) {
                    HelpButton(keys = currentPage.helpKeys, fieldHelp = fieldHelp)
                }
            }
            LinearProgressIndicator(
                progress = { (viewModel.pageIndex + 1) / wizardPages.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // `when` over the WizardPage enum picks which fields to show for the current page.
            // Kotlin can check this `when` is exhaustive (every WizardPage handled) because the
            // enum's full set of constants is known at compile time - the same exhaustiveness
            // check you'd get dispatching over a sealed interface.
            when (currentPage) {
                WizardPage.SETUP -> {
                    LabeledDropdown(
                        label = "Scenario",
                        options = Scenario.entries,
                        selected = viewModel.scenario,
                        displayName = { it.displayName },
                        onSelected = { viewModel.scenarioId = it.id },
                    )
                    LabeledDropdown(
                        label = "Knight",
                        options = Knight.entries,
                        selected = viewModel.knight,
                        displayName = { it.displayName },
                        onSelected = { viewModel.knight = it },
                    )
                    OutlinedTextField(
                        value = viewModel.playerName,
                        onValueChange = { viewModel.playerName = it },
                        label = { Text("Player name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                WizardPage.FAME -> NumberField(label = "Fame", value = viewModel.fame, onValueChange = { viewModel.fame = it })
                WizardPage.GREATEST_KNOWLEDGE -> {
                    NumberField(label = "Spells in deck", value = viewModel.spellsInDeck, onValueChange = { viewModel.spellsInDeck = it })
                    NumberField(
                        label = "Advanced Actions in deck",
                        value = viewModel.advancedActionsInDeck,
                        onValueChange = { viewModel.advancedActionsInDeck = it },
                    )
                }
                WizardPage.GREATEST_LEADER -> {
                    Text("Units (healthy / wounded, per level)", style = MaterialTheme.typography.labelLarge)
                    UnitLevelRow(1, viewModel.unitLevel1Healthy, { viewModel.unitLevel1Healthy = it }, viewModel.unitLevel1Wounded, { viewModel.unitLevel1Wounded = it })
                    UnitLevelRow(2, viewModel.unitLevel2Healthy, { viewModel.unitLevel2Healthy = it }, viewModel.unitLevel2Wounded, { viewModel.unitLevel2Wounded = it })
                    UnitLevelRow(3, viewModel.unitLevel3Healthy, { viewModel.unitLevel3Healthy = it }, viewModel.unitLevel3Wounded, { viewModel.unitLevel3Wounded = it })
                    UnitLevelRow(4, viewModel.unitLevel4Healthy, { viewModel.unitLevel4Healthy = it }, viewModel.unitLevel4Wounded, { viewModel.unitLevel4Wounded = it })
                }
                WizardPage.GREATEST_ADVENTURER -> NumberField(
                    label = "Shields on adventure sites",
                    value = viewModel.shieldsOnAdventureSites,
                    onValueChange = { viewModel.shieldsOnAdventureSites = it },
                )
                WizardPage.GREATEST_LOOT -> {
                    NumberField(label = "Artifacts", value = viewModel.artifacts, onValueChange = { viewModel.artifacts = it })
                    NumberField(
                        label = "Crystals in Inventory",
                        value = viewModel.crystalsInInventory,
                        onValueChange = { viewModel.crystalsInInventory = it },
                    )
                }
                WizardPage.GREATEST_CONQUEROR -> NumberField(
                    label = "Shields on keeps/mage towers/monasteries",
                    value = viewModel.shieldsOnConquerSites,
                    onValueChange = { viewModel.shieldsOnConquerSites = it },
                )
                WizardPage.GREATEST_BEATING -> NumberField(
                    label = "Wounds in deck",
                    value = viewModel.woundsInDeck,
                    onValueChange = { viewModel.woundsInDeck = it },
                )
                WizardPage.GREATEST_QUESTER -> NumberField(
                    label = "Quest Points scored",
                    value = viewModel.questPoints,
                    onValueChange = { viewModel.questPoints = it },
                )
                WizardPage.CITIES_CONQUERED -> {
                    LabeledCheckbox(
                        label = "City 1 (Level 5) conquered",
                        checked = viewModel.city1Conquered,
                        onCheckedChange = { viewModel.city1Conquered = it },
                    )
                    LabeledCheckbox(
                        label = "City 2 (Level 8) conquered",
                        checked = viewModel.city2Conquered,
                        onCheckedChange = { viewModel.city2Conquered = it },
                    )
                }
                WizardPage.CITY_REVEALED -> LabeledSwitch(
                    label = "Capital city revealed",
                    checked = viewModel.cityRevealed,
                    onCheckedChange = { viewModel.cityRevealed = it },
                )
                WizardPage.HIGH_PRIESTESS_DEFEATED -> LabeledSwitch(
                    label = "High Priestess defeated",
                    checked = viewModel.highPriestessDefeated,
                    onCheckedChange = { viewModel.highPriestessDefeated = it },
                )
                WizardPage.GRAVEYARDS_SEALED -> {
                    BoundedIntPicker(
                        label = "Graveyards sealed",
                        range = 0..2,
                        selected = viewModel.graveyardsSealed,
                        onSelect = { viewModel.graveyardsSealed = it },
                    )
                    LabeledSwitch(
                        label = "Necromancer defeated",
                        checked = viewModel.necromancerDefeated,
                        onCheckedChange = { viewModel.necromancerDefeated = it },
                    )
                }
                WizardPage.COUNCIL_QUEST_POINTS -> NumberField(
                    label = "Quest Points scored",
                    value = viewModel.questPoints,
                    onValueChange = { viewModel.questPoints = it },
                )
                WizardPage.COUNCIL_REPUTATION -> ReputationTrackPicker(
                    selected = ReputationTrackSpace.valueOf(viewModel.reputationTrackSpaceName),
                    onSelect = { viewModel.reputationTrackSpaceName = it.name },
                )
                WizardPage.ROUNDS_FINISHED_EARLY -> NumberField(
                    label = "Rounds finished before the Round 6 limit",
                    value = viewModel.roundsFinishedEarly,
                    onValueChange = { viewModel.roundsFinishedEarly = it },
                )
                WizardPage.DUMMY_PLAYER_STATUS -> {
                    NumberField(
                        label = "Cards remaining in Dummy Player's deck",
                        value = viewModel.cardsRemainingInDummyDeck,
                        onValueChange = { viewModel.cardsRemainingInDummyDeck = it },
                    )
                    LabeledSwitch(
                        label = "\"End of the Round\" already announced this Round",
                        checked = viewModel.endOfRoundAnnounced,
                        onCheckedChange = { viewModel.endOfRoundAnnounced = it },
                    )
                }
                WizardPage.RESULT -> ResultCard(score = viewModel.score, outcome = viewModel.outcome)
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Page navigation is just moving viewModel.pageIndex up/down within wizardPages;
            // Previous is disabled on the first page since there's nowhere to go back to.
            OutlinedButton(onClick = { viewModel.pageIndex-- }, enabled = viewModel.pageIndex > 0) {
                Text("Previous")
            }
            val isLastPage = viewModel.pageIndex >= wizardPages.lastIndex
            Button(onClick = {
                if (isLastPage) {
                    // Button click handlers aren't suspend functions, so `scope.launch` starts a
                    // coroutine to call the suspend `save()` and then navigate away once it
                    // finishes.
                    scope.launch {
                        viewModel.save()
                        onDone()
                    }
                } else {
                    viewModel.pageIndex++
                }
            }) {
                Text(if (isLastPage) "Done" else "Next")
            }
        }
    }
}

/**
 * A single-line numeric input field, backed by a `String` (the ViewModel stores everything as
 * strings - see [ScoreCalculatorViewModel]) so it can stay blank/partial while typing rather than
 * forcing a valid Int at every keystroke.
 */
@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        // `Char::isDigit` is a function reference (a callable pointer to that method), used here
        // as shorthand for `{ c -> c.isDigit() }`: rejects the edit entirely if any character
        // typed isn't a digit, so the field can never hold non-numeric text.
        onValueChange = { new -> if (new.all(Char::isDigit)) onValueChange(new) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Which physical space on the Reputation track the player's Shield token sits on, shown as a
 * vertical list of all 10 spaces - a phone screen is much taller than it is wide, and a vertical
 * list is a reasonable stand-in for the physical track's curved fan shape. Negative spaces are
 * tinted red, positive spaces gold, deepening toward each end, mirroring the real board's own
 * coloring. Each row shows the one number the board actually prints there (see
 * [ReputationTrackSpace] - there's no second "position" number to show, since the game never
 * tracks one), so the player just taps where their token is.
 */
@Composable
private fun ReputationTrackPicker(selected: ReputationTrackSpace, onSelect: (ReputationTrackSpace) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Reputation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ReputationTrackSpace.entries.forEach { space ->
                ReputationTrackSpaceRow(space = space, selected = space == selected, onClick = { onSelect(space) })
            }
        }
    }
}

@Composable
private fun ReputationTrackSpaceRow(space: ReputationTrackSpace, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = space.trackColor(),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                space.modifierLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
    }
}

// Deep red toward the negative end, gold toward the positive end - the real board's own Reputation
// track coloring - fading to a neutral surface at the center. Capped well under full saturation
// (maxTint) so body text stays legible on top without per-cell contrast calculation.
private val ReputationNegativeHue = Color(0xFFB3261E)
private val ReputationPositiveHue = Color(0xFFC9A227)
private const val REPUTATION_TRACK_MAX_TINT = 0.35f

@Composable
private fun ReputationTrackSpace.trackColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    // The X space has no modifier - it's the track's most-negative space, so it's tinted as
    // fully as the negative end gets.
    val modifierValue = modifier ?: -5
    val fraction = (kotlin.math.abs(modifierValue) / 5f) * REPUTATION_TRACK_MAX_TINT
    return when {
        modifierValue < 0 -> lerp(surface, ReputationNegativeHue, fraction)
        modifierValue > 0 -> lerp(surface, ReputationPositiveHue, fraction)
        else -> surface
    }
}

// "X" for the track's one end space (matching what's actually printed on the board there - see
// ReputationTrackSpace), otherwise the signed modifier. Explicit "+" for positive values, since a
// plain Int.toString() has no sign and would read as ambiguous next to this same row's negative
// cells.
private val ReputationTrackSpace.modifierLabel: String
    get() = modifier?.let { if (it > 0) "+$it" else it.toString() } ?: "X"

/**
 * A horizontal row of tappable "pill" options for an Int field with a small, fixed valid range
 * (e.g. Graveyards sealed, 0-2) - shows every legal value up front instead of a free-text
 * NumberField whose min/max only lives in the label text and isn't actually enforced by the
 * widget. Reusable for any future bounded field, not just this one.
 */
@Composable
private fun BoundedIntPicker(label: String, range: IntRange, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            range.forEach { value ->
                BoundedIntPickerOption(
                    value = value,
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BoundedIntPickerOption(value: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A checkbox with its label as a clickable row, for simple yes/no wizard fields (e.g. a city being conquered). */
@Composable
private fun LabeledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

/** A toggle switch with its label, used for boolean fields phrased as a stateful condition (e.g. whether "End of the Round" was already announced) rather than a plain checkbox. */
@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * A read-only text field that opens a dropdown of `options` when tapped (used for the Setup
 * page's Scenario and Knight pickers). Generic over `T` so it works for any enum-like option
 * type; `displayName` supplies the human-readable label for each option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LabeledDropdown(
    label: String,
    options: List<T>,
    selected: T,
    displayName: (T) -> String,
    onSelected: (T) -> Unit,
) {
    // Whether the dropdown is currently open - local UI state, not a wizard field, so plain
    // `remember` (not the ViewModel) is the right home for it.
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayName(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayName(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * One row of the Greatest Leader page: a Unit level's healthy/wounded counts side by side. The
 * ViewModel stores these as four separate per-level properties rather than a list, so each level
 * gets its own row composable call (see `WizardPage.GREATEST_LEADER` in [WizardContent]).
 */
@Composable
private fun UnitLevelRow(
    level: Int,
    healthy: String,
    onHealthyChange: (String) -> Unit,
    wounded: String,
    onWoundedChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Level $level", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = healthy,
                onValueChange = { new -> if (new.all(Char::isDigit)) onHealthyChange(new) },
                label = { Text("Healthy") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = wounded,
                onValueChange = { new -> if (new.all(Char::isDigit)) onWoundedChange(new) },
                label = { Text("Wounded") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The final Result page's summary card: the computed total score and derived Won/Lost outcome. */
@Composable
private fun ResultCard(score: Int, outcome: Outcome) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Total score: $score", style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (outcome == Outcome.WON) "Outcome: Won" else "Outcome: Lost",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
