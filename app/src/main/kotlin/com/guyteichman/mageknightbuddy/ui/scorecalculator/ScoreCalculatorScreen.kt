package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.BRAEVALAR_FINAL_SPACE_MOVE_COST_RANGE
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CombatLevel
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.ui.components.CardColorDot
import com.guyteichman.mageknightbuddy.ui.components.CrystalIcon
import com.guyteichman.mageknightbuddy.ui.components.LabelPillPicker
import com.guyteichman.mageknightbuddy.ui.components.LabeledCheckbox
import com.guyteichman.mageknightbuddy.ui.components.LabeledDropdown
import com.guyteichman.mageknightbuddy.ui.components.LabeledSwitch
import com.guyteichman.mageknightbuddy.ui.components.NumberField
import com.guyteichman.mageknightbuddy.ui.components.NumberPillPicker
import com.guyteichman.mageknightbuddy.ui.components.ReputationTrackPicker
import com.guyteichman.mageknightbuddy.ui.components.UnitLevelRow
import com.guyteichman.mageknightbuddy.ui.components.label
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
    HEADS_DEFEATED("Heads Defeated", listOf("Heads Defeated", "All Heads Defeated")),
    HORSEMEN_DEFEATED("Horsemen Defeated", listOf("Horsemen Defeated", "All Horsemen Defeated")),
    HORSEMEN_AND_HEADS_DEFEATED(
        "Horsemen & Heads Defeated",
        listOf("Apocalypse is Here: Horsemen Defeated", "Heads Defeated", "All Heads Defeated"),
    ),
    AVATARS_DEFEATED("Avatars of Tezla Defeated", listOf("Avatars Defeated", "Both Avatars Defeated")),
    RELIC_PIECES_FOUND("Relic Pieces Found", listOf("Relic Pieces Found", "All Relic Pieces Found")),
    DESTROYED_SITE_TOKENS("Destroyed Site Tokens", listOf("Destroyed Site Tokens")),
    ZIGGURAT_PYRAMID_CONQUERED("Ziggurat & Pyramid", listOf("Ziggurat/Pyramid Floors Conquered")),
    ARYTHEA_CHALLENGE("Arythea's Challenge", listOf("Wound Cards on Units")),
    GOLDYX_CHALLENGE("Goldyx's Challenge", listOf("Crystal Colors in Inventory")),
    KRANG_CHALLENGE("Krang's Challenge", listOf("Puppet Master")),
    BRAEVALAR_CHALLENGE(
        "Braevalar's Challenge",
        listOf("All Basic Actions in Deck", "Advanced Action Colors in Deck", "Final Space Move Cost"),
    ),
    VOLKARE_DIFFICULTY("Volkare Difficulty", listOf("Combat Level", "Race Level")),
    VOLKARE_CITIES_CONQUERED("Cities Conquered", listOf("Volkare's Quest: Cities Conquered")),
    VOLKARE_COMBAT("Volkare Combat", listOf("Volkare Defeated", "Volkare Combat Bonus")),
    CITY_AND_VOLKARE("City & Volkare", listOf("City Conquered (Volkare's Return)", "Volkare Defeated", "Volkare Combat Bonus")),
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
 * The Knight-specific "Challenge" page for Solo Conquest Challenge - only Arythea, Goldyx, Krang,
 * and Braevalar need one (their Knight override adds brand-new input fields no other Knight's
 * override needs); the other four Knights' overrides only change the *formula* applied to data
 * already collected on the shared Standard Achievement pages, so they get no extra page at all.
 */
private fun knightChallengePage(knight: Knight): List<WizardPage> = when (knight) {
    Knight.ARYTHEA -> listOf(WizardPage.ARYTHEA_CHALLENGE)
    Knight.GOLDYX -> listOf(WizardPage.GOLDYX_CHALLENGE)
    Knight.KRANG -> listOf(WizardPage.KRANG_CHALLENGE)
    Knight.BRAEVALAR -> listOf(WizardPage.BRAEVALAR_CHALLENGE)
    Knight.TOVAK, Knight.NOROWAS, Knight.WOLFHAWK, Knight.CORAL -> emptyList()
}

/**
 * The ordered page sequence for [scenario]'s wizard, built from its `*ScoringInput`'s actual
 * field shape (see the matching domain data class) rather than one fixed sequence for every
 * scenario. Exhaustive `when` over the sealed [Scenario] - the compiler flags it if a new
 * Scenario is added without a page sequence for it. [knight] only matters for Solo Conquest
 * Challenge, whose page list depends on which Knight is selected as well as the scenario - every
 * other scenario's sequence is fixed regardless of [knight].
 */
private fun wizardPagesFor(scenario: Scenario, knight: Knight): List<WizardPage> = when (scenario) {
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
    Scenario.AgainstTheDragon -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.HEADS_DEFEATED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    Scenario.AgainstTheHorsemen -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.HORSEMEN_DEFEATED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    Scenario.ApocalypseIsHere -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.HORSEMEN_AND_HEADS_DEFEATED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    // The Fractured Lands has no rounds/dummy-deck/end-of-round bonuses at all (docs/rules/
    // the-fractured-lands.md, Scoring > Solo) - it reuses the Greatest Quester page as-is, no
    // scenario-specific page needed.
    Scenario.FracturedLands -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(WizardPage.GREATEST_QUESTER, WizardPage.RESULT)
    Scenario.LifeAndDeath -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.AVATARS_DEFEATED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    // Lost Relic has no roundsFinishedEarly field (docs/rules/lost-relic.md, Scoring > Solo).
    Scenario.LostRelic -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.RELIC_PIECES_FOUND,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    Scenario.AgainstTheApocalypse -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(
            WizardPage.DESTROYED_SITE_TOKENS,
            WizardPage.ZIGGURAT_PYRAMID_CONQUERED,
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    // Same shape as Solo Conquest (Cities Conquered reuses that exact page - same 2-city setup),
    // plus one Knight-conditional "Challenge" page for the 4 Knights that need extra fields.
    Scenario.SoloConquestChallenge -> listOf(WizardPage.SETUP, WizardPage.FAME) + standardAchievementPages +
        listOf(WizardPage.GREATEST_QUESTER, WizardPage.CITIES_CONQUERED) +
        knightChallengePage(knight) +
        listOf(
            WizardPage.ROUNDS_FINISHED_EARLY,
            WizardPage.DUMMY_PLAYER_STATUS,
            WizardPage.RESULT,
        )
    // Combat/Race Level are "chosen before setup" per docs/rules/volkares-quest.md, but still get
    // their own dedicated page (VOLKARE_DIFFICULTY) rather than living on the scenario-agnostic
    // SETUP page - shared by both Volkare scenarios since it's identical in both.
    Scenario.VolkaresQuest -> listOf(WizardPage.SETUP, WizardPage.VOLKARE_DIFFICULTY, WizardPage.FAME) +
        standardAchievementPages +
        listOf(
            WizardPage.VOLKARE_CITIES_CONQUERED,
            WizardPage.VOLKARE_COMBAT,
            WizardPage.RESULT,
        )
    Scenario.VolkaresReturn -> listOf(WizardPage.SETUP, WizardPage.VOLKARE_DIFFICULTY, WizardPage.FAME) +
        standardAchievementPages +
        listOf(
            WizardPage.CITY_AND_VOLKARE,
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
    // Scenario or Knight dropdown on the SETUP page immediately reflects the new page sequence.
    val wizardPages = wizardPagesFor(viewModel.scenario, viewModel.knight)
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
                        // Sorted alphabetically by displayName for the picker only (issue #110) -
                        // Scenario.entries itself stays in rulebook/release order, since other code
                        // (e.g. wizardPagesFor's exhaustive `when`) has no reason to care about
                        // dropdown ordering.
                        options = Scenario.entries.sortedBy { it.displayName },
                        selected = viewModel.scenario,
                        displayName = { it.displayName },
                        onSelected = { viewModel.scenarioId = it.id },
                    )
                    LabeledDropdown(
                        label = "Knight",
                        options = Knight.entries.sortedBy { it.displayName },
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
                        label = "First city conquered",
                        checked = viewModel.city1Conquered,
                        onCheckedChange = { viewModel.city1Conquered = it },
                    )
                    LabeledCheckbox(
                        label = "Second city conquered",
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
                    NumberPillPicker(
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
                WizardPage.HEADS_DEFEATED -> NumberPillPicker(
                    label = "Dragon heads defeated (excluding the Control head)",
                    range = 0..4,
                    selected = viewModel.headsDefeated,
                    onSelect = { viewModel.headsDefeated = it },
                )
                WizardPage.HORSEMEN_DEFEATED -> NumberPillPicker(
                    label = "Horsemen defeated",
                    range = 0..4,
                    selected = viewModel.horsemenDefeated,
                    onSelect = { viewModel.horsemenDefeated = it },
                )
                WizardPage.HORSEMEN_AND_HEADS_DEFEATED -> {
                    NumberPillPicker(
                        label = "Horsemen defeated",
                        range = 0..4,
                        selected = viewModel.horsemenDefeated,
                        onSelect = { viewModel.horsemenDefeated = it },
                    )
                    NumberPillPicker(
                        label = "Dragon heads defeated (excluding the Control head)",
                        range = 0..4,
                        selected = viewModel.headsDefeated,
                        onSelect = { viewModel.headsDefeated = it },
                    )
                }
                WizardPage.AVATARS_DEFEATED -> {
                    LabeledSwitch(
                        label = "Tezla's Spirit (Elementalists) defeated",
                        checked = viewModel.tezlaSpiritDefeated,
                        onCheckedChange = { viewModel.tezlaSpiritDefeated = it },
                    )
                    LabeledSwitch(
                        label = "Dark Tezla (Dark Crusaders) defeated",
                        checked = viewModel.darkTezlaDefeated,
                        onCheckedChange = { viewModel.darkTezlaDefeated = it },
                    )
                }
                WizardPage.RELIC_PIECES_FOUND -> NumberPillPicker(
                    label = "Relic pieces found",
                    range = 0..2,
                    selected = viewModel.relicPiecesFound,
                    onSelect = { viewModel.relicPiecesFound = it },
                )
                WizardPage.DESTROYED_SITE_TOKENS -> NumberField(
                    label = "Destroyed Site tokens in your Inventory",
                    value = viewModel.destroyedSiteTokens,
                    onValueChange = { viewModel.destroyedSiteTokens = it },
                )
                WizardPage.ZIGGURAT_PYRAMID_CONQUERED -> {
                    LabeledSwitch(
                        label = "Conquered a floor of the ziggurat",
                        checked = viewModel.zigguratFloorConquered,
                        onCheckedChange = { viewModel.zigguratFloorConquered = it },
                    )
                    LabeledSwitch(
                        label = "Conquered a floor of the pyramid",
                        checked = viewModel.pyramidFloorConquered,
                        onCheckedChange = { viewModel.pyramidFloorConquered = it },
                    )
                }
                WizardPage.ARYTHEA_CHALLENGE -> NumberField(
                    label = "Wound cards on Units (on top of Wounds in deck)",
                    value = viewModel.woundCardsOnUnits,
                    onValueChange = { viewModel.woundCardsOnUnits = it },
                )
                WizardPage.GOLDYX_CHALLENGE -> {
                    Text("Crystal colors in Inventory", style = MaterialTheme.typography.labelLarge)
                    ColorCheckbox(
                        icon = { CrystalIcon(CardColor.RED) },
                        label = "${CardColor.RED.label} crystal",
                        checked = viewModel.goldyxRedCrystal,
                        onCheckedChange = { viewModel.goldyxRedCrystal = it },
                    )
                    ColorCheckbox(
                        icon = { CrystalIcon(CardColor.GREEN) },
                        label = "${CardColor.GREEN.label} crystal",
                        checked = viewModel.goldyxGreenCrystal,
                        onCheckedChange = { viewModel.goldyxGreenCrystal = it },
                    )
                    ColorCheckbox(
                        icon = { CrystalIcon(CardColor.BLUE) },
                        label = "${CardColor.BLUE.label} crystal",
                        checked = viewModel.goldyxBlueCrystal,
                        onCheckedChange = { viewModel.goldyxBlueCrystal = it },
                    )
                    ColorCheckbox(
                        icon = { CrystalIcon(CardColor.WHITE) },
                        label = "${CardColor.WHITE.label} crystal",
                        checked = viewModel.goldyxWhiteCrystal,
                        onCheckedChange = { viewModel.goldyxWhiteCrystal = it },
                    )
                }
                WizardPage.KRANG_CHALLENGE -> {
                    NumberField(
                        label = "Highest Fame value among held Enemy tokens",
                        value = viewModel.puppetMasterHighestFameValue,
                        onValueChange = { viewModel.puppetMasterHighestFameValue = it },
                    )
                    NumberField(
                        label = "Distinct Fame values among held Enemy tokens",
                        value = viewModel.puppetMasterDistinctFameValues,
                        onValueChange = { viewModel.puppetMasterDistinctFameValues = it },
                    )
                }
                WizardPage.BRAEVALAR_CHALLENGE -> {
                    LabeledSwitch(
                        label = "All Basic Action cards still in deck",
                        checked = viewModel.allBasicActionsInDeck,
                        onCheckedChange = { viewModel.allBasicActionsInDeck = it },
                    )
                    Text("Advanced Action colors in deck", style = MaterialTheme.typography.labelLarge)
                    ColorCheckbox(
                        icon = { CardColorDot(CardColor.RED) },
                        label = "${CardColor.RED.label} Advanced Action",
                        checked = viewModel.braevalarRedAdvancedAction,
                        onCheckedChange = { viewModel.braevalarRedAdvancedAction = it },
                    )
                    ColorCheckbox(
                        icon = { CardColorDot(CardColor.GREEN) },
                        label = "${CardColor.GREEN.label} Advanced Action",
                        checked = viewModel.braevalarGreenAdvancedAction,
                        onCheckedChange = { viewModel.braevalarGreenAdvancedAction = it },
                    )
                    ColorCheckbox(
                        icon = { CardColorDot(CardColor.BLUE) },
                        label = "${CardColor.BLUE.label} Advanced Action",
                        checked = viewModel.braevalarBlueAdvancedAction,
                        onCheckedChange = { viewModel.braevalarBlueAdvancedAction = it },
                    )
                    ColorCheckbox(
                        icon = { CardColorDot(CardColor.WHITE) },
                        label = "${CardColor.WHITE.label} Advanced Action",
                        checked = viewModel.braevalarWhiteAdvancedAction,
                        onCheckedChange = { viewModel.braevalarWhiteAdvancedAction = it },
                    )
                    NumberPillPicker(
                        label = "Final space's normal Move cost at Night",
                        range = BRAEVALAR_FINAL_SPACE_MOVE_COST_RANGE,
                        selected = viewModel.finalSpaceMoveCostAtNight,
                        onSelect = { viewModel.finalSpaceMoveCostAtNight = it },
                    )
                }
                WizardPage.VOLKARE_DIFFICULTY -> {
                    LabelPillPicker(
                        label = "Combat Level",
                        options = CombatLevel.entries,
                        selected = viewModel.combatLevel,
                        displayName = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                        color = { difficultyPillColor(it.ordinal, CombatLevel.entries.size) },
                        onSelect = { viewModel.combatLevel = it },
                    )
                    LabelPillPicker(
                        label = "Race Level",
                        options = RaceLevel.entries,
                        selected = viewModel.raceLevel,
                        displayName = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                        color = { difficultyPillColor(it.ordinal, RaceLevel.entries.size) },
                        onSelect = { viewModel.raceLevel = it },
                    )
                }
                WizardPage.VOLKARE_CITIES_CONQUERED -> NumberPillPicker(
                    label = "Cities conquered",
                    range = 0..2,
                    selected = viewModel.volkareCitiesConquered,
                    onSelect = { viewModel.volkareCitiesConquered = it },
                )
                WizardPage.VOLKARE_COMBAT -> {
                    LabeledSwitch(
                        label = "Volkare defeated",
                        checked = viewModel.volkareDefeated,
                        onCheckedChange = { viewModel.volkareDefeated = it },
                    )
                    NumberField(
                        label = "Cards remaining in Volkare's deck",
                        value = viewModel.cardsRemainingInVolkaresDeck,
                        onValueChange = { viewModel.cardsRemainingInVolkaresDeck = it },
                    )
                }
                WizardPage.CITY_AND_VOLKARE -> {
                    LabeledSwitch(
                        label = "City conquered",
                        checked = viewModel.cityConquered,
                        onCheckedChange = { viewModel.cityConquered = it },
                    )
                    LabeledSwitch(
                        label = "Volkare defeated",
                        checked = viewModel.volkareDefeated,
                        onCheckedChange = { viewModel.volkareDefeated = it },
                    )
                    NumberField(
                        label = "Cards remaining in Volkare's deck",
                        value = viewModel.cardsRemainingInVolkaresDeck,
                        onValueChange = { viewModel.cardsRemainingInVolkaresDeck = it },
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
 * A checkbox with a colored crystal/card-color icon and label, used by Goldyx's and Braevalar's
 * "which colors do you have at least one of" fields - one row per [CardColor], with the
 * ViewModel deriving the actual `distinctXColors` Int count from however many are checked
 * (same pattern Solo Conquest's two named-city checkboxes already use for `citiesConquered`).
 */
@Composable
private fun ColorCheckbox(icon: @Composable () -> Unit, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        icon()
        Text(label)
    }
}

// White (easiest) to gold (hardest) - the same gold used by ReputationTrackPicker's positive
// end, for visual consistency across the app's two difficulty-flavored gradients.
private val DifficultyPillStart = Color(0xFFFFFFFF)
private val DifficultyPillEnd = Color(0xFFC9A227)

// Shared by both CombatLevel and RaceLevel's LabelPillPicker on the VOLKARE_DIFFICULTY page -
// [ordinal]/[total] position along the 3-step gradient, not tied to either enum type specifically.
private fun difficultyPillColor(ordinal: Int, total: Int): Color =
    lerp(DifficultyPillStart, DifficultyPillEnd, ordinal / (total - 1).toFloat())

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
