package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
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

// Every page kind used by any scenario's wizard. Not every scenario visits every page - see
// wizardPagesFor(), which picks the ordered subset that matches a given Scenario's *ScoringInput
// shape (e.g. CITIES_CONQUERED only applies to Solo Conquest, CITY_REVEALED only to First
// Reconnaissance).
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

@Composable
fun ScoreCalculatorScreen(
    repository: ScoringSessionRepository,
    fieldHelp: Map<String, FieldHelp>,
    onDone: () -> Unit,
) {
    val viewModel: ScoreCalculatorViewModel = viewModel(factory = ScoreCalculatorViewModel.factory(repository))
    val scope = rememberCoroutineScope()
    // Recomputed on every recomposition (cheap - a handful of enum values), so switching the
    // Scenario dropdown on the SETUP page immediately reflects the new scenario's page sequence.
    val wizardPages = wizardPagesFor(viewModel.scenario)
    // Defensive clamp: pageIndex is restored from SavedStateHandle across process death, and a
    // shorter sequence (e.g. switching from Solo Conquest's 13 pages to For the Council's 4)
    // could otherwise leave it pointing past the end of the new list.
    val currentPage = wizardPages[viewModel.pageIndex.coerceIn(wizardPages.indices)]
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
                    NumberField(
                        label = "Graveyards sealed (0-2)",
                        value = viewModel.graveyardsSealed,
                        onValueChange = { viewModel.graveyardsSealed = it },
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
                WizardPage.COUNCIL_REPUTATION -> {
                    ReputationModifierPicker(
                        selected = viewModel.reputationModifier,
                        onSelect = { viewModel.reputationModifier = it },
                    )
                    LabeledSwitch(
                        label = "Shield token on the Reputation track's X space",
                        checked = viewModel.shieldOnXSpace,
                        onCheckedChange = { viewModel.shieldOnXSpace = it },
                    )
                    SignedNumberField(
                        label = "Final Reputation",
                        value = viewModel.reputation,
                        onValueChange = { viewModel.reputation = it },
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
            OutlinedButton(onClick = { viewModel.pageIndex-- }, enabled = viewModel.pageIndex > 0) {
                Text("Previous")
            }
            val isLastPage = viewModel.pageIndex >= wizardPages.lastIndex
            Button(onClick = {
                if (isLastPage) {
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

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.all(Char::isDigit)) onValueChange(new) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

// A regex, not just Char::isDigit, since this field also has to accept a leading "-" (Final
// Reputation can legitimately be negative, unlike every other NumberField in this wizard). Empty
// string and a lone "-" are both allowed transiently while the player is typing.
private val signedIntegerRegex = Regex("-?\\d*")

@Composable
private fun SignedNumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (signedIntegerRegex.matches(new)) onValueChange(new) },
        label = { Text(label) },
        // KeyboardType.Number's IME usually has no "-" key, so this field asks for the full
        // text keyboard instead - the regex filter above still keeps input digits-and-minus-only.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

// The fixed values printed at each space of the Reputation track (docs/rules/for-the-council.md's
// "Reputation" note; base rulebook p.2's track illustration), excluding the two "X" spaces at
// either end - a Shield token on an X space is its own distinct case, already covered by the
// separate "Shield on X space" switch below, so it isn't one of this picker's choices.
private val REPUTATION_MODIFIER_OPTIONS = listOf(-5, -3, -2, -1, 0, 1, 2, 3, 5)

/**
 * Single-select chip row for [ScoreCalculatorViewModel.reputationModifier] - a plain number field
 * would let the player enter a value the Reputation track can't actually produce, so this only
 * offers the values the track prints. Same FilterChip-row pattern as `DummyPlayerScreen`'s
 * `ColorPickerRow`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReputationModifierPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Reputation modifier", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            REPUTATION_MODIFIER_OPTIONS.forEach { modifier ->
                FilterChip(
                    selected = modifier == selected,
                    onClick = { onSelect(modifier) },
                    // Explicit "+" for positive values - plain OutlinedTextField.toString() on a
                    // positive Int has no sign, which would read as ambiguous next to the negative
                    // chips in this same row.
                    label = { Text(if (modifier > 0) "+$modifier" else modifier.toString()) },
                )
            }
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LabeledDropdown(
    label: String,
    options: List<T>,
    selected: T,
    displayName: (T) -> String,
    onSelected: (T) -> Unit,
) {
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
