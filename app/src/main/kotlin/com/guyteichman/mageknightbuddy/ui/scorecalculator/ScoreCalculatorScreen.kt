package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One entry per wizard page, in the fixed rulebook order the wizard walks through. `title` is
 * the page heading; `helpKeys` are the [FieldHelp] map keys whose entries should appear when the
 * page's "?" button is tapped (a page can pull in more than one help entry, or none).
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
    ROUNDS_FINISHED_EARLY("Rounds Finished Early", listOf("Rounds Finished Early")),
    DUMMY_PLAYER_STATUS("Dummy Player Status", listOf("Dummy Player's Deck", "End of Round")),
    RESULT("Result"),
}

// `WizardPage.entries` is the Kotlin-generated list of all enum constants in declaration order
// (replaces the older `values()`) - this is what makes "rulebook order" above just "declaration
// order" here. Page navigation below is just moving an index into this list.
private val wizardPages = WizardPage.entries

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
    val currentPage = wizardPages[viewModel.pageIndex]
    // `remember { mutableStateOf(false) }` is plain Compose state, scoped to this composition -
    // fine here (unlike the wizard fields above) because a dismissed confirmation dialog isn't
    // something that needs to survive a tab switch.
    var showResetConfirmation by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        WizardContent(viewModel = viewModel, currentPage = currentPage, fieldHelp = fieldHelp, scope = scope, onDone = onDone)

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
            val isLastPage = viewModel.pageIndex == wizardPages.lastIndex
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

/**
 * The "?" icon button shown next to a wizard page's title. Tapping it opens a dialog with the
 * beginner-friendly rule text (and rulebook citation) for every help key that page declares in
 * [WizardPage.helpKeys], looked up from the bundled [fieldHelp] map (see [FieldHelp]).
 */
@Composable
private fun HelpButton(keys: List<String>, fieldHelp: Map<String, FieldHelp>) {
    var showHelp by remember { mutableStateOf(false) }

    IconButton(onClick = { showHelp = true }) {
        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Rule details")
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Rule details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    keys.forEach { key ->
                        // `map[key]?.let { }` is a safe-call + scope function combo: if the key
                        // has no entry in fieldHelp, `fieldHelp[key]` is null and the `?.` short-
                        // circuits, so nothing renders for that key instead of crashing.
                        fieldHelp[key]?.let { help ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(help.text)
                                Text(
                                    help.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("Close")
                }
            },
        )
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
