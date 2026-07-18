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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private enum class WizardPage(val title: String) {
    SETUP("Setup"),
    FAME("Fame"),
    GREATEST_KNOWLEDGE("Greatest Knowledge"),
    GREATEST_LEADER("Greatest Leader"),
    GREATEST_ADVENTURER("Greatest Adventurer"),
    GREATEST_LOOT("Greatest Loot"),
    GREATEST_CONQUEROR("Greatest Conqueror"),
    GREATEST_BEATING("Greatest Beating"),
    GREATEST_QUESTER("Greatest Quester"),
    CITIES_CONQUERED("Cities Conquered"),
    ROUNDS_FINISHED_EARLY("Rounds Finished Early"),
    DUMMY_PLAYER_STATUS("Dummy Player Status"),
    RESULT("Result"),
}

private val wizardPages = WizardPage.entries

@Composable
fun ScoreCalculatorScreen(
    repository: ScoringSessionRepository,
    onDone: () -> Unit,
) {
    val viewModel: ScoreCalculatorViewModel = viewModel(factory = ScoreCalculatorViewModel.factory(repository))
    val scope = rememberCoroutineScope()
    val currentPage = wizardPages[viewModel.pageIndex]
    var showResetConfirmation by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        WizardContent(viewModel = viewModel, currentPage = currentPage, scope = scope, onDone = onDone)

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
    currentPage: WizardPage,
    scope: CoroutineScope,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Step ${viewModel.pageIndex + 1} of ${wizardPages.size}: ${currentPage.title}",
                style = MaterialTheme.typography.titleMedium,
            )
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
            val isLastPage = viewModel.pageIndex == wizardPages.lastIndex
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
