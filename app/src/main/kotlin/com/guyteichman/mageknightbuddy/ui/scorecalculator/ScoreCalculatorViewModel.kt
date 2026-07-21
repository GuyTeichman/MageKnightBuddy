package com.guyteichman.mageknightbuddy.ui.scorecalculator

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
import com.guyteichman.mageknightbuddy.domain.FirstReconnaissanceScoringInput
import com.guyteichman.mageknightbuddy.domain.ForTheCouncilScoringInput
import com.guyteichman.mageknightbuddy.domain.FracturedLandsScoringInput
import com.guyteichman.mageknightbuddy.domain.HiddenValleyScoringInput
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.LifeAndDeathScoringInput
import com.guyteichman.mageknightbuddy.domain.LostRelicScoringInput
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.RealmOfTheDeadScoringInput
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.ScoringInput
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
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

    // `by savedStateHandle.saveable("key") { mutableStateOf(...) }` is a Kotlin *property
    // delegate*: reading/writing `pageIndex` actually reads/writes a Compose `MutableState` that
    // is mirrored into the ViewModel's SavedStateHandle under "pageIndex", so the value survives
    // both tab switches (ViewModel outlives the composition) and process death (SavedStateHandle
    // is backed by a Bundle). Every property below follows this same pattern, one per wizard
    // field, each with its own string key and default value.
    var pageIndex: Int by savedStateHandle.saveable("pageIndex") { mutableStateOf(0) }

    var scenarioId: String by savedStateHandle.saveable("scenarioId") { mutableStateOf(Scenario.SoloConquest.id) }

    // Computed property (no backing field): re-derives the Scenario object from the stored id
    // every time it's read, instead of storing the Scenario itself.
    val scenario: Scenario get() = Scenario.fromId(scenarioId)

    var knight: Knight by savedStateHandle.saveable("knight") { mutableStateOf(Knight.entries.first()) }
    var playerName: String by savedStateHandle.saveable("playerName") { mutableStateOf("") }

    var fame: String by savedStateHandle.saveable("fame") { mutableStateOf("0") }

    var spellsInDeck: String by savedStateHandle.saveable("spellsInDeck") { mutableStateOf("0") }
    var advancedActionsInDeck: String by savedStateHandle.saveable("advancedActionsInDeck") { mutableStateOf("0") }
    var unitLevel1Healthy: String by savedStateHandle.saveable("unitLevel1Healthy") { mutableStateOf("0") }
    var unitLevel1Wounded: String by savedStateHandle.saveable("unitLevel1Wounded") { mutableStateOf("0") }
    var unitLevel2Healthy: String by savedStateHandle.saveable("unitLevel2Healthy") { mutableStateOf("0") }
    var unitLevel2Wounded: String by savedStateHandle.saveable("unitLevel2Wounded") { mutableStateOf("0") }
    var unitLevel3Healthy: String by savedStateHandle.saveable("unitLevel3Healthy") { mutableStateOf("0") }
    var unitLevel3Wounded: String by savedStateHandle.saveable("unitLevel3Wounded") { mutableStateOf("0") }
    var unitLevel4Healthy: String by savedStateHandle.saveable("unitLevel4Healthy") { mutableStateOf("0") }
    var unitLevel4Wounded: String by savedStateHandle.saveable("unitLevel4Wounded") { mutableStateOf("0") }
    var shieldsOnAdventureSites: String by savedStateHandle.saveable("shieldsOnAdventureSites") { mutableStateOf("0") }
    var artifacts: String by savedStateHandle.saveable("artifacts") { mutableStateOf("0") }
    var crystalsInInventory: String by savedStateHandle.saveable("crystalsInInventory") { mutableStateOf("0") }
    var shieldsOnConquerSites: String by savedStateHandle.saveable("shieldsOnConquerSites") { mutableStateOf("0") }
    var woundsInDeck: String by savedStateHandle.saveable("woundsInDeck") { mutableStateOf("0") }
    var questPoints: String by savedStateHandle.saveable("questPoints") { mutableStateOf("0") }

    var city1Conquered: Boolean by savedStateHandle.saveable("city1Conquered") { mutableStateOf(false) }
    var city2Conquered: Boolean by savedStateHandle.saveable("city2Conquered") { mutableStateOf(false) }
    var roundsFinishedEarly: String by savedStateHandle.saveable("roundsFinishedEarly") { mutableStateOf("0") }
    var cardsRemainingInDummyDeck: String by savedStateHandle.saveable("cardsRemainingInDummyDeck") { mutableStateOf("0") }
    var endOfRoundAnnounced: Boolean by savedStateHandle.saveable("endOfRoundAnnounced") { mutableStateOf(true) }

    // Scenario-specific fields below: each is only read by the input(s) that actually use it
    // (see the `when` in [input]), so leaving the others at their defaults for an unused
    // scenario is harmless.
    var cityRevealed: Boolean by savedStateHandle.saveable("cityRevealed") { mutableStateOf(false) }
    var highPriestessDefeated: Boolean by savedStateHandle.saveable("highPriestessDefeated") { mutableStateOf(false) }
    // An Int (not a String like most other Int-backed fields above), since it's driven entirely
    // by the NumberPillPicker widget (a fixed set of taps, no free-text typing) - same rationale
    // as the Boolean fields below being stored as Boolean rather than "true"/"false" strings.
    var graveyardsSealed: Int by savedStateHandle.saveable("graveyardsSealed") { mutableStateOf(0) }
    var necromancerDefeated: Boolean by savedStateHandle.saveable("necromancerDefeated") { mutableStateOf(false) }

    // Heads Defeated is shared by Against the Dragon and Apocalypse is Here (same field
    // semantics, 0-4 non-Control heads); Horsemen Defeated is shared by Against the Horsemen and
    // Apocalypse is Here - each scenario's `input` branch below just reads the one(s) it needs.
    var headsDefeated: Int by savedStateHandle.saveable("headsDefeated") { mutableStateOf(0) }
    var horsemenDefeated: Int by savedStateHandle.saveable("horsemenDefeated") { mutableStateOf(0) }
    var tezlaSpiritDefeated: Boolean by savedStateHandle.saveable("tezlaSpiritDefeated") { mutableStateOf(false) }
    var darkTezlaDefeated: Boolean by savedStateHandle.saveable("darkTezlaDefeated") { mutableStateOf(false) }
    var relicPiecesFound: Int by savedStateHandle.saveable("relicPiecesFound") { mutableStateOf(0) }
    var destroyedSiteTokens: String by savedStateHandle.saveable("destroyedSiteTokens") { mutableStateOf("0") }
    // Booleans, not the 0-3 floor number domain actually stores - since issue #100's fix, Solo
    // scoring only ever checks whether a floor was conquered, never which one, so the wizard maps
    // true/false to 1/0 in [input] below rather than asking the player to recall an irrelevant
    // floor number.
    var zigguratFloorConquered: Boolean by savedStateHandle.saveable("zigguratFloorConquered") { mutableStateOf(false) }
    var pyramidFloorConquered: Boolean by savedStateHandle.saveable("pyramidFloorConquered") { mutableStateOf(false) }

    // Which ReputationTrackSpace the player's Shield token sits on, stored by enum name rather
    // than an invented numeric index - the physical track only ever shows one number per space
    // (see ReputationTrackSpace), so the player just picks that one space
    // (ScoreCalculatorScreen's ReputationTrackPicker) and everything else For the Council's
    // scoring needs is derived from it.
    var reputationTrackSpaceName: String by savedStateHandle.saveable("reputationTrackSpaceName") { mutableStateOf(ReputationTrackSpace.CENTER.name) }

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
        }

    /**
     * Clears every field back to its default, for the "New scoring session" action - the
     * ViewModel would otherwise keep showing a finished session's data indefinitely, since its
     * whole reason for existing (per ADR-0002) is to *not* reset itself on tab switches.
     */
    fun reset() {
        pageIndex = 0
        scenarioId = Scenario.SoloConquest.id
        knight = Knight.entries.first()
        playerName = ""
        fame = "0"
        spellsInDeck = "0"
        advancedActionsInDeck = "0"
        unitLevel1Healthy = "0"
        unitLevel1Wounded = "0"
        unitLevel2Healthy = "0"
        unitLevel2Wounded = "0"
        unitLevel3Healthy = "0"
        unitLevel3Wounded = "0"
        unitLevel4Healthy = "0"
        unitLevel4Wounded = "0"
        shieldsOnAdventureSites = "0"
        artifacts = "0"
        crystalsInInventory = "0"
        shieldsOnConquerSites = "0"
        woundsInDeck = "0"
        questPoints = "0"
        city1Conquered = false
        city2Conquered = false
        roundsFinishedEarly = "0"
        cardsRemainingInDummyDeck = "0"
        endOfRoundAnnounced = true
        cityRevealed = false
        highPriestessDefeated = false
        graveyardsSealed = 0
        necromancerDefeated = false
        reputationTrackSpaceName = ReputationTrackSpace.CENTER.name
        headsDefeated = 0
        horsemenDefeated = 0
        tezlaSpiritDefeated = false
        darkTezlaDefeated = false
        relicPiecesFound = 0
        destroyedSiteTokens = "0"
        zigguratFloorConquered = false
        pyramidFloorConquered = false
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
