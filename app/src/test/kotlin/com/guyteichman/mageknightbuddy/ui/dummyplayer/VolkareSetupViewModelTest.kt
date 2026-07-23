package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareCard
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class VolkareSetupViewModelTest {

    // The ViewModel's init block launches a one-shot check on viewModelScope (which dispatches
    // via Dispatchers.Main), so Main needs a test dispatcher installed for plain JVM unit tests -
    // same as DummyPlayerSetupViewModelTest.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts on Volkares Return, Fair, with that scenario-level's default Wound count`() = runTest {
        val viewModel = VolkareSetupViewModel(SavedStateHandle(), VolkareSessionRepository(FakeVolkareSessionDao()))

        assertEquals(Scenario.VolkaresReturn, viewModel.scenario)
        assertEquals(RaceLevel.FAIR, viewModel.raceLevel)
        assertEquals(18, viewModel.woundCount) // Return, Fair - see docs/rules/volkares-return.md.
        assertFalse(viewModel.woundCountIsCustom)
    }

    @Test
    fun `pickScenario resets woundCount to the new scenario's table default for the current Race Level`() = runTest {
        val viewModel = VolkareSetupViewModel(SavedStateHandle(), VolkareSessionRepository(FakeVolkareSessionDao()))

        viewModel.pickScenario(Scenario.VolkaresQuest)

        assertEquals(Scenario.VolkaresQuest, viewModel.scenario)
        assertEquals(20, viewModel.woundCount) // Quest, Fair - see docs/rules/volkares-quest.md.
        assertFalse(viewModel.woundCountIsCustom)
    }

    @Test
    fun `pickRaceLevel resets woundCount to its table default, clearing a previous custom override`() = runTest {
        val viewModel = VolkareSetupViewModel(SavedStateHandle(), VolkareSessionRepository(FakeVolkareSessionDao()))
        viewModel.changeWoundCount(3) // Diverges from Fair's default (18) - marks custom.

        viewModel.pickRaceLevel(RaceLevel.THRILLING)

        assertEquals(RaceLevel.THRILLING, viewModel.raceLevel)
        assertEquals(12, viewModel.woundCount) // Return, Thrilling - see docs/rules/volkares-return.md.
        assertFalse(viewModel.woundCountIsCustom)
    }

    @Test
    fun `changeWoundCount marks the count custom when it diverges from the current table default`() = runTest {
        val viewModel = VolkareSetupViewModel(SavedStateHandle(), VolkareSessionRepository(FakeVolkareSessionDao()))

        viewModel.changeWoundCount(3)

        assertEquals(3, viewModel.woundCount)
        assertTrue(viewModel.woundCountIsCustom)
    }

    @Test
    fun `changeWoundCount does not mark custom when the typed value happens to equal the table default`() = runTest {
        val viewModel = VolkareSetupViewModel(SavedStateHandle(), VolkareSessionRepository(FakeVolkareSessionDao()))

        viewModel.changeWoundCount(18) // Return, Fair's own default value.

        assertFalse(viewModel.woundCountIsCustom)
    }

    @Test
    fun `start saves a session built from the current scenario, raceLevel, and woundCount, and marks hasSavedSession`() = runTest {
        val repository = VolkareSessionRepository(FakeVolkareSessionDao())
        val viewModel = VolkareSetupViewModel(SavedStateHandle(), repository)
        viewModel.pickScenario(Scenario.VolkaresQuest)
        viewModel.pickRaceLevel(RaceLevel.TIGHT)
        viewModel.changeWoundCount(5)

        viewModel.start()

        val saved = repository.restore()
        assertEquals(Scenario.VolkaresQuest, saved?.scenario)
        assertEquals(RaceLevel.TIGHT, saved?.raceLevel)
        assertEquals(5, saved?.deckOrder?.count { it is VolkareCard.Wound })
        assertTrue(viewModel.hasSavedSession)
    }
}
