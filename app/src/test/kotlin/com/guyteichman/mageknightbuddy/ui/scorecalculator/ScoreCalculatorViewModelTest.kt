package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.data.toDomain
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals
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
    }

    @Test
    fun `save maps a blank Player name to null`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.save()

        assertEquals(null, fakeDao.inserted.single().toDomain().playerName)
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

        viewModel.reset()

        assertEquals(0, viewModel.pageIndex)
        assertEquals(Knight.entries.first(), viewModel.knight)
        assertEquals("", viewModel.playerName)
        assertEquals("0", viewModel.fame)
        assertEquals(false, viewModel.city1Conquered)
        assertEquals(true, viewModel.endOfRoundAnnounced)
    }
}
