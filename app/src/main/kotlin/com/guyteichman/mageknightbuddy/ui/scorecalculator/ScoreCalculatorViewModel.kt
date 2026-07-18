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
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoring
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import java.time.Instant

private fun String.toIntOrZero() = toIntOrNull() ?: 0

@OptIn(SavedStateHandleSaveableApi::class)
class ScoreCalculatorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ScoringSessionRepository,
) : ViewModel() {

    var pageIndex: Int by savedStateHandle.saveable("pageIndex") { mutableStateOf(0) }

    var scenarioId: String by savedStateHandle.saveable("scenarioId") { mutableStateOf(Scenario.SoloConquest.id) }
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

    val input: SoloConquestScoringInput
        get() = SoloConquestScoringInput(
            fame = fame.toIntOrZero(),
            standardAchievements = StandardAchievements(
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
            ),
            citiesConquered = listOf(city1Conquered, city2Conquered).count { it },
            roundsFinishedEarly = roundsFinishedEarly.toIntOrZero(),
            cardsRemainingInDummyDeck = cardsRemainingInDummyDeck.toIntOrZero(),
            endOfRoundAnnounced = endOfRoundAnnounced,
            questPoints = questPoints.toIntOrZero(),
        )

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
    }

    val score: Int get() = SoloConquestScoring.score(input)
    val outcome: Outcome get() = SoloConquestScoring.outcome(input)

    suspend fun save() {
        val session = ScoringSession.create(
            scenario = scenario,
            knight = knight,
            playerName = playerName.ifBlank { null },
            input = input,
            playedAt = Instant.now(),
        )
        repository.save(session)
    }

    companion object {
        fun factory(repository: ScoringSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ScoreCalculatorViewModel(createSavedStateHandle(), repository)
            }
        }
    }
}
