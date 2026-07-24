package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.AgainstTheApocalypseScoringInput
import com.guyteichman.mageknightbuddy.domain.AgainstTheDragonScoringInput
import com.guyteichman.mageknightbuddy.domain.AgainstTheHorsemenScoringInput
import com.guyteichman.mageknightbuddy.domain.ApocalypseIsHereScoringInput
import com.guyteichman.mageknightbuddy.domain.CombatLevel
import com.guyteichman.mageknightbuddy.domain.FirstReconnaissanceScoringInput
import com.guyteichman.mageknightbuddy.domain.ForTheCouncilScoringInput
import com.guyteichman.mageknightbuddy.domain.FracturedLandsScoringInput
import com.guyteichman.mageknightbuddy.domain.HiddenValleyScoringInput
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.LifeAndDeathScoringInput
import com.guyteichman.mageknightbuddy.domain.LostRelicScoringInput
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.RealmOfTheDeadScoringInput
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.ScoringInput
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.SoloConquestChallengeScoringInput
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import com.guyteichman.mageknightbuddy.domain.VolkaresQuestScoringInput
import com.guyteichman.mageknightbuddy.domain.VolkaresReturnScoringInput
import com.guyteichman.mageknightbuddy.domain.outcome
import com.guyteichman.mageknightbuddy.domain.score
import java.time.Instant

/** Kotlin extension function on `String`: treats a blank/non-numeric text field as 0 rather than crashing or nulling out. */
private fun String.toIntOrZero() = toIntOrNull() ?: 0

/**
 * Owns every field of the Score Calculator wizard (one property per scoring input, plus which
 * page the wizard is on) and knows how to turn them into a saved [ScoringSession].
 *
 * State lives here instead of Compose `remember` specifically so it survives the player switching
 * tabs and back - see docs/adr/0002-viewmodel-backed-wizard-state.md for why a `ViewModel` was
 * chosen over the smaller `rememberSaveable` fix, and why it also owns the final `save()` step.
 */
@OptIn(SavedStateHandleSaveableApi::class) // saveable() is still an experimental Compose+ViewModel API.
class ScoreCalculatorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ScoringSessionRepository,
) : ViewModel() {

    // `by resettable("key", default)` is a Kotlin *property delegate*: reading/writing `pageIndex`
    // actually reads/writes a Compose `MutableState` that is mirrored into the ViewModel's
    // SavedStateHandle under "pageIndex", so the value survives both tab switches (ViewModel
    // outlives the composition) and process death (SavedStateHandle is backed by a Bundle) - see
    // [resettable]'s own doc comment below for how it also wires the field into [reset]. Every
    // property below follows this same pattern, one per wizard field, each with its own string
    // key and default value.

    /**
     * Reset closures, one per field, collected as each field is declared below via [resettable].
     * Must be declared before the first `by resettable(...)` property in this class - property
     * initializers run top-to-bottom during construction, so this list has to already exist by
     * the time the first field's initializer tries to add to it.
     */
    private val resettables = mutableListOf<() -> Unit>()

    /**
     * Declares one wizard field: still backed by [SavedStateHandle] exactly as before (see
     * docs/adr/0002-viewmodel-backed-wizard-state.md) - the only addition is remembering [default]
     * so [reset] can restore it later without a hand-written line. Returns the same
     * `MutableState<T>` `savedStateHandle.saveable` always returned, so `var x by resettable(key,
     * default)` is a drop-in replacement for `var x by savedStateHandle.saveable(key) {
     * mutableStateOf(default) }`.
     */
    private fun <T : Any> resettable(key: String, default: T): MutableState<T> {
        val state = savedStateHandle.saveable(key) { mutableStateOf(default) }
        resettables += { state.value = default }
        return state
    }

    var pageIndex: Int by resettable("pageIndex", 0)

    var scenarioId: String by resettable("scenarioId", Scenario.SoloConquest.id)

    // Computed property (no backing field): re-derives the Scenario object from the stored id
    // every time it's read, instead of storing the Scenario itself.
    val scenario: Scenario get() = Scenario.fromId(scenarioId)

    var knight: Knight by resettable("knight", Knight.entries.first())
    var playerName: String by resettable("playerName", "")

    var fame: String by resettable("fame", "0")

    var spellsInDeck: String by resettable("spellsInDeck", "0")
    var advancedActionsInDeck: String by resettable("advancedActionsInDeck", "0")
    var unitLevel1Healthy: String by resettable("unitLevel1Healthy", "0")
    var unitLevel1Wounded: String by resettable("unitLevel1Wounded", "0")
    var unitLevel2Healthy: String by resettable("unitLevel2Healthy", "0")
    var unitLevel2Wounded: String by resettable("unitLevel2Wounded", "0")
    var unitLevel3Healthy: String by resettable("unitLevel3Healthy", "0")
    var unitLevel3Wounded: String by resettable("unitLevel3Wounded", "0")
    var unitLevel4Healthy: String by resettable("unitLevel4Healthy", "0")
    var unitLevel4Wounded: String by resettable("unitLevel4Wounded", "0")
    var shieldsOnAdventureSites: String by resettable("shieldsOnAdventureSites", "0")
    var artifacts: String by resettable("artifacts", "0")
    var crystalsInInventory: String by resettable("crystalsInInventory", "0")
    var shieldsOnConquerSites: String by resettable("shieldsOnConquerSites", "0")
    var woundsInDeck: String by resettable("woundsInDeck", "0")
    var questPoints: String by resettable("questPoints", "0")

    var city1Conquered: Boolean by resettable("city1Conquered", false)
    var city2Conquered: Boolean by resettable("city2Conquered", false)
    var roundsFinishedEarly: String by resettable("roundsFinishedEarly", "0")
    var cardsRemainingInDummyDeck: String by resettable("cardsRemainingInDummyDeck", "0")
    var endOfRoundAnnounced: Boolean by resettable("endOfRoundAnnounced", true)

    // Scenario-specific fields below: each is only read by the input(s) that actually use it
    // (see the `when` in [input]), so leaving the others at their defaults for an unused
    // scenario is harmless.
    var cityRevealed: Boolean by resettable("cityRevealed", false)
    var highPriestessDefeated: Boolean by resettable("highPriestessDefeated", false)
    // An Int (not a String like most other Int-backed fields above), since it's driven entirely
    // by the NumberPillPicker widget (a fixed set of taps, no free-text typing) - same rationale
    // as the Boolean fields below being stored as Boolean rather than "true"/"false" strings.
    var graveyardsSealed: Int by resettable("graveyardsSealed", 0)
    var necromancerDefeated: Boolean by resettable("necromancerDefeated", false)

    // Heads Defeated is shared by Against the Dragon and Apocalypse is Here (same field
    // semantics, 0-4 non-Control heads); Horsemen Defeated is shared by Against the Horsemen and
    // Apocalypse is Here - each scenario's `input` branch below just reads the one(s) it needs.
    var headsDefeated: Int by resettable("headsDefeated", 0)
    var horsemenDefeated: Int by resettable("horsemenDefeated", 0)
    var tezlaSpiritDefeated: Boolean by resettable("tezlaSpiritDefeated", false)
    var darkTezlaDefeated: Boolean by resettable("darkTezlaDefeated", false)
    var relicPiecesFound: Int by resettable("relicPiecesFound", 0)
    var destroyedSiteTokens: String by resettable("destroyedSiteTokens", "0")
    // Booleans, not the 0-3 floor number domain actually stores - since issue #100's fix, Solo
    // scoring only ever checks whether a floor was conquered, never which one, so the wizard maps
    // true/false to 1/0 in [input] below rather than asking the player to recall an irrelevant
    // floor number.
    var zigguratFloorConquered: Boolean by resettable("zigguratFloorConquered", false)
    var pyramidFloorConquered: Boolean by resettable("pyramidFloorConquered", false)

    // Solo Conquest Challenge's Knight-specific fields below - only one Knight's block is ever
    // read for a given session (see the `when (knight)` in [input]'s SoloConquestChallenge
    // branch), so leaving the others at their defaults for a different Knight is harmless.
    var woundCardsOnUnits: String by resettable("woundCardsOnUnits", "0")
    // Goldyx: one checkbox per CardColor, deriving distinctCrystalColorsInInventory the same way
    // Solo Conquest's city1Conquered/city2Conquered derive citiesConquered.
    var goldyxRedCrystal: Boolean by resettable("goldyxRedCrystal", false)
    var goldyxGreenCrystal: Boolean by resettable("goldyxGreenCrystal", false)
    var goldyxBlueCrystal: Boolean by resettable("goldyxBlueCrystal", false)
    var goldyxWhiteCrystal: Boolean by resettable("goldyxWhiteCrystal", false)
    var puppetMasterHighestFameValue: String by resettable("puppetMasterHighestFameValue", "0")
    var puppetMasterDistinctFameValues: String by resettable("puppetMasterDistinctFameValues", "0")
    var allBasicActionsInDeck: Boolean by resettable("allBasicActionsInDeck", false)
    // Braevalar: one checkbox per CardColor, deriving distinctAdvancedActionColorsInDeck.
    var braevalarRedAdvancedAction: Boolean by resettable("braevalarRedAdvancedAction", false)
    var braevalarGreenAdvancedAction: Boolean by resettable("braevalarGreenAdvancedAction", false)
    var braevalarBlueAdvancedAction: Boolean by resettable("braevalarBlueAdvancedAction", false)
    var braevalarWhiteAdvancedAction: Boolean by resettable("braevalarWhiteAdvancedAction", false)
    // An Int (not String), driven by NumberPillPicker(2..5) - same rationale as graveyardsSealed.
    // Defaults to 2 (the range's minimum), matching the domain field's own non-zero default -
    // see SoloConquestChallengeScoringInput.finalSpaceMoveCostAtNight's KDoc for why 0 can't be
    // used as the usual "not this Knight" sentinel here.
    var finalSpaceMoveCostAtNight: Int by resettable("finalSpaceMoveCostAtNight", 2)

    // Combat/Race Level are shared by both Volkare scenarios (same two enums, same
    // VOLKARE_DIFFICULTY page). cardsRemainingInVolkaresDeck is also shared, even though the two
    // domain input types name it slightly differently (cardsRemainingInVolkaresDeck vs
    // cardsRemainingInVolkareDeck) - see the `when` in [input] for the mapping.
    var combatLevel: CombatLevel by resettable("combatLevel", CombatLevel.entries.first())
    var raceLevel: RaceLevel by resettable("raceLevel", RaceLevel.entries.first())
    var volkareCitiesConquered: Int by resettable("volkareCitiesConquered", 0)
    var volkareDefeated: Boolean by resettable("volkareDefeated", false)
    var cardsRemainingInVolkaresDeck: String by resettable("cardsRemainingInVolkaresDeck", "0")
    var cityConquered: Boolean by resettable("cityConquered", false)

    // Which ReputationTrackSpace the player's Shield token sits on, stored by enum name rather
    // than an invented numeric index - the physical track only ever shows one number per space
    // (see ReputationTrackSpace), so the player just picks that one space
    // (ScoreCalculatorScreen's ReputationTrackPicker) and everything else For the Council's
    // scoring needs is derived from it.
    var reputationTrackSpaceName: String by resettable("reputationTrackSpaceName", ReputationTrackSpace.CENTER.name)

    // Shared by every scenario except For the Council (which has no Standard Achievements at
    // all - see the `when` in [input]), so it's factored out instead of repeated 4 times.
    private val standardAchievements: StandardAchievements
        get() = StandardAchievements(
            spellsInDeck = spellsInDeck.toIntOrZero(),
            advancedActionsInDeck = advancedActionsInDeck.toIntOrZero(),
            units = listOf(
                UnitTally(level = 1, healthyCount = unitLevel1Healthy.toIntOrZero(), woundedCount = unitLevel1Wounded.toIntOrZero()),
                UnitTally(level = 2, healthyCount = unitLevel2Healthy.toIntOrZero(), woundedCount = unitLevel2Wounded.toIntOrZero()),
                UnitTally(level = 3, healthyCount = unitLevel3Healthy.toIntOrZero(), woundedCount = unitLevel3Wounded.toIntOrZero()),
                UnitTally(level = 4, healthyCount = unitLevel4Healthy.toIntOrZero(), woundedCount = unitLevel4Wounded.toIntOrZero()),
            ),
            shieldsOnAdventureSites = shieldsOnAdventureSites.toIntOrZero(),
            artifacts = artifacts.toIntOrZero(),
            crystalsInInventory = crystalsInInventory.toIntOrZero(),
            shieldsOnConquerSites = shieldsOnConquerSites.toIntOrZero(),
            woundsInDeck = woundsInDeck.toIntOrZero(),
        )

    /**
     * Builds whichever `*ScoringInput` variant matches the currently selected [scenario], out of
     * the current field values - a `when` over [scenario] rather than one fixed shape, since each
     * scenario's input has a different set of fields (see each `*ScoringInput` data class in
     * `domain/`).
     */
    val input: ScoringInput
        get() = when (scenario) {
            Scenario.SoloConquest -> SoloConquestScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                citiesConquered = listOf(city1Conquered, city2Conquered).count { it },
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
                questPoints = questPoints.toIntOrZero(),
            )
            Scenario.FirstReconnaissance -> FirstReconnaissanceScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                cityRevealed = cityRevealed,
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.HiddenValley -> HiddenValleyScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                highPriestessDefeated = highPriestessDefeated,
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.RealmOfTheDead -> RealmOfTheDeadScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                graveyardsSealed = graveyardsSealed,
                necromancerDefeated = necromancerDefeated,
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.ForTheCouncil -> ForTheCouncilScoringInput(
                questPoints = questPoints.toIntOrZero(),
                reputationTrackSpace = ReputationTrackSpace.valueOf(reputationTrackSpaceName),
            )
            Scenario.AgainstTheDragon -> AgainstTheDragonScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                headsDefeated = headsDefeated,
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.AgainstTheHorsemen -> AgainstTheHorsemenScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                horsemenDefeated = horsemenDefeated,
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.ApocalypseIsHere -> ApocalypseIsHereScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                horsemenDefeated = horsemenDefeated,
                headsDefeated = headsDefeated,
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.FracturedLands -> FracturedLandsScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                questPoints = questPoints.toIntOrZero(),
            )
            Scenario.LifeAndDeath -> LifeAndDeathScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                tezlaSpiritDefeated = tezlaSpiritDefeated,
                darkTezlaDefeated = darkTezlaDefeated,
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.LostRelic -> LostRelicScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                relicPiecesFound = relicPiecesFound,
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.AgainstTheApocalypse -> AgainstTheApocalypseScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                destroyedSiteTokens = destroyedSiteTokens.toIntOrZero(),
                zigguratFloorsConquered = if (zigguratFloorConquered) 1 else 0,
                pyramidFloorsConquered = if (pyramidFloorConquered) 1 else 0,
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
            )
            Scenario.SoloConquestChallenge -> SoloConquestChallengeScoringInput(
                knight = knight,
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                citiesConquered = listOf(city1Conquered, city2Conquered).count { it },
                roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
                cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
                endOfRoundAnnounced = endOfRoundAnnounced,
                questPoints = questPoints.toIntOrZero(),
                woundCardsOnUnits = woundCardsOnUnits.toIntOrZero(),
                distinctCrystalColorsInInventory = listOf(
                    goldyxRedCrystal,
                    goldyxGreenCrystal,
                    goldyxBlueCrystal,
                    goldyxWhiteCrystal,
                ).count { it },
                puppetMasterHighestFameValue = puppetMasterHighestFameValue.toIntOrZero(),
                puppetMasterDistinctFameValues = puppetMasterDistinctFameValues.toIntOrZero(),
                allBasicActionsInDeck = allBasicActionsInDeck,
                distinctAdvancedActionColorsInDeck = listOf(
                    braevalarRedAdvancedAction,
                    braevalarGreenAdvancedAction,
                    braevalarBlueAdvancedAction,
                    braevalarWhiteAdvancedAction,
                ).count { it },
                finalSpaceMoveCostAtNight = finalSpaceMoveCostAtNight,
            )
            Scenario.VolkaresQuest -> VolkaresQuestScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                citiesConquered = volkareCitiesConquered,
                combatLevel = combatLevel,
                raceLevel = raceLevel,
                volkareDefeated = volkareDefeated,
                cardsRemainingInVolkaresDeck = cardsRemainingInVolkaresDeck.toIntOrZero(),
            )
            Scenario.VolkaresReturn -> VolkaresReturnScoringInput(
                fame = fame.toIntOrZero(),
                standardAchievements = standardAchievements,
                cityConquered = cityConquered,
                volkareDefeated = volkareDefeated,
                combatLevel = combatLevel,
                raceLevel = raceLevel,
                cardsRemainingInVolkareDeck = cardsRemainingInVolkaresDeck.toIntOrZero(),
            )
        }

    /**
     * Clears every field back to its default, for the "New scoring session" action - the
     * ViewModel would otherwise keep showing a finished session's data indefinitely, since its
     * whole reason for existing (per ADR-0002) is to *not* reset itself on tab switches. Each
     * field's default lives next to its declaration (see [resettable]) rather than being
     * hand-listed here, so a new field can't be added without also wiring up its reset.
     */
    fun reset() {
        resettables.forEach { it() }
    }

    /**
     * Live total score for the Result page, computed from the current [input]. `input.score()`
     * dispatches to whichever scenario's `*Scoring` object matches the input's actual runtime
     * type - see the `ScoringInput` dispatcher functions in domain.
     */
    val score: Int get() = input.score()

    /** Live Won/Lost outcome for the Result page - derived from [input], never a separate manual field (see architecture.md). */
    val outcome: Outcome get() = input.outcome()

    /**
     * Builds the final [ScoringSession] from the wizard's current [input] and saves it via the
     * repository, so it shows up on the Scoreboard tab. `suspend` because the repository write
     * goes through Room, which requires calling off the main thread; callers launch this from a
     * coroutine (see `ScoreCalculatorScreen`'s "Done" button).
     *
     * Calls [reset] right after the save succeeds, so the wizard is already back to a blank first
     * page by the time the player returns to this tab, instead of staying parked on the
     * just-submitted Result page (issue #87) - which also let a player re-tap "Done" and submit
     * the same score again. The [ScoringSession] is built and persisted before [reset] runs, so
     * clearing the fields afterward can't affect what was just saved.
     */
    suspend fun save() {
        val session = ScoringSession.create(
            scenario = scenario,
            knight = knight,
            playerName = playerName.ifBlank { null },
            input = input,
            playedAt = Instant.now(),
        )
        repository.save(session)
        reset()
    }

    companion object {
        /**
         * Builds a [ViewModelProvider.Factory] for this ViewModel. A factory is needed because
         * this constructor takes a [ScoringSessionRepository] the default Compose `viewModel()`
         * lookup can't supply on its own; `initializer { }` wires in `createSavedStateHandle()`
         * so the ViewModel still gets its process-death-surviving `SavedStateHandle` as usual.
         */
        fun factory(repository: ScoringSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ScoreCalculatorViewModel(createSavedStateHandle(), repository)
            }
        }
    }
}
