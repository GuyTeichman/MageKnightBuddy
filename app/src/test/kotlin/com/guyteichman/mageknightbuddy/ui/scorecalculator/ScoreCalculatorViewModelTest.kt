package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.data.toDomain
import com.guyteichman.mageknightbuddy.domain.FirstReconnaissanceScoringInput
import com.guyteichman.mageknightbuddy.domain.ForTheCouncilScoringInput
import com.guyteichman.mageknightbuddy.domain.HiddenValleyScoringInput
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.RealmOfTheDeadScoringInput
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class ScoreCalculatorViewModelTest {

    @Test
    fun `save persists a ScoringSession built from the current field values`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.knight = Knight.WOLFHAWK
        viewModel.playerName = "Guy"
        viewModel.fame = "50"
        viewModel.spellsInDeck = "2"
        viewModel.advancedActionsInDeck = "1"
        viewModel.city1Conquered = true
        viewModel.city2Conquered = true
        viewModel.roundsFinishedEarly = "1"
        viewModel.cardsRemainingInDummyDeck = "4"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.SoloConquest, saved.scenario)
        assertEquals(Knight.WOLFHAWK, saved.knight)
        assertEquals("Guy", saved.playerName)
        // 50 fame + 5 achievements (2*2+1) + (2*10 + 15 all-cities) cities
        // + 1*30 rounds + 4 dummy cards + 5 end-of-round-not-announced = 129
        assertEquals(129, saved.score)
        assertEquals(Outcome.WON, saved.outcome)
        val input = assertIs<SoloConquestScoringInput>(saved.input)
        assertEquals(2, input.citiesConquered)
    }

    @Test
    fun `save maps a blank Player name to null`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.save()

        assertEquals(null, fakeDao.inserted.single().toDomain().playerName)
    }

    @Test
    fun `save builds a FirstReconnaissanceScoringInput when First Reconnaissance is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.FirstReconnaissance.id
        viewModel.fame = "20"
        viewModel.cityRevealed = true
        viewModel.roundsFinishedEarly = "1"
        viewModel.cardsRemainingInDummyDeck = "3"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.FirstReconnaissance, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 20 fame + 10 city revealed + 30 rounds + 3 dummy cards + 5 end-of-round-not-announced = 68
        assertEquals(68, saved.score)
        val input = assertIs<FirstReconnaissanceScoringInput>(saved.input)
        assertEquals(true, input.cityRevealed)
    }

    @Test
    fun `save builds a HiddenValleyScoringInput when The Hidden Valley is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.HiddenValley.id
        viewModel.fame = "5"
        viewModel.highPriestessDefeated = true

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.HiddenValley, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 5 fame + 20 High Priestess defeated + 5 end-of-round-not-announced (default true, so 0) = 25
        assertEquals(25, saved.score)
        val input = assertIs<HiddenValleyScoringInput>(saved.input)
        assertEquals(true, input.highPriestessDefeated)
    }

    @Test
    fun `save builds a RealmOfTheDeadScoringInput when The Realm of the Dead is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.RealmOfTheDead.id
        viewModel.fame = "5"
        viewModel.graveyardsSealed = "2"
        viewModel.necromancerDefeated = true

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.RealmOfTheDead, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 5 fame + 2*5 graveyards + 10 necromancer + 10 both achieved = 35
        assertEquals(35, saved.score)
        val input = assertIs<RealmOfTheDeadScoringInput>(saved.input)
        assertEquals(2, input.graveyardsSealed)
        assertEquals(true, input.necromancerDefeated)
    }

    @Test
    fun `save builds a ForTheCouncilScoringInput from the selected Reputation track space, negative modifier included`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.ForTheCouncil.id
        viewModel.questPoints = "12"
        viewModel.reputationTrackSpaceName = ReputationTrackSpace.MINUS_3.name

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.ForTheCouncil, saved.scenario)
        assertEquals(Outcome.LOST, saved.outcome)
        // 12 quest points - 3 reputation modifier = 9
        assertEquals(9, saved.score)
        val input = assertIs<ForTheCouncilScoringInput>(saved.input)
        assertEquals(ReputationTrackSpace.MINUS_3, input.reputationTrackSpace)
    }

    @Test
    fun `reset clears all fields back to their defaults`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))
        viewModel.pageIndex = 5
        viewModel.knight = Knight.WOLFHAWK
        viewModel.playerName = "Guy"
        viewModel.fame = "50"
        viewModel.city1Conquered = true
        viewModel.endOfRoundAnnounced = false
        viewModel.scenarioId = Scenario.ForTheCouncil.id
        viewModel.cityRevealed = true
        viewModel.highPriestessDefeated = true
        viewModel.graveyardsSealed = "2"
        viewModel.necromancerDefeated = true
        viewModel.reputationTrackSpaceName = ReputationTrackSpace.NEGATIVE_X.name

        viewModel.reset()

        assertEquals(0, viewModel.pageIndex)
        assertEquals(Scenario.SoloConquest, viewModel.scenario)
        assertEquals(Knight.entries.first(), viewModel.knight)
        assertEquals("", viewModel.playerName)
        assertEquals("0", viewModel.fame)
        assertEquals(false, viewModel.city1Conquered)
        assertEquals(true, viewModel.endOfRoundAnnounced)
        assertEquals(false, viewModel.cityRevealed)
        assertEquals(false, viewModel.highPriestessDefeated)
        assertEquals("0", viewModel.graveyardsSealed)
        assertEquals(false, viewModel.necromancerDefeated)
        assertEquals(ReputationTrackSpace.CENTER.name, viewModel.reputationTrackSpaceName)
    }
}
