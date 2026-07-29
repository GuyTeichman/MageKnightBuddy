package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.ScoreCalculatorDraftRepository
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.testsupport.parcelRoundTrip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Saved-state parcelability guard for [ScoreCalculatorViewModel], which owns 50+ `saveable`-backed
 * wizard fields - by far the most likely place a future field of a non-parcelable type slips in.
 * It already dodges the issue-#208 trap the right way (it stores the scenario as its String
 * [ScoreCalculatorViewModel.scenarioId], never the `Scenario` `data object`); this test pins that
 * down and fails loudly if any field it stores ever becomes something Android can't parcel. See
 * [parcelRoundTrip] for why plain-JVM `SavedStateHandle` tests can't catch this.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScoreCalculatorViewModelSavedStateTest {

    // The ViewModel's init block launches a one-shot draft restore on viewModelScope (Dispatchers.Main),
    // so Main needs a test dispatcher installed for construction to succeed.
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(handle: SavedStateHandle) = ScoreCalculatorViewModel(
        handle,
        ScoringSessionRepository(FakeScoringSessionDao()),
        ScoreCalculatorDraftRepository(FakeScoreCalculatorDraftDao()),
    )

    @Test
    fun `wizard state survives a Parcel round-trip and restores across all field types`() {
        val handle = SavedStateHandle()
        val first = buildViewModel(handle)
        // Touch one field of each stored type: String id, enum, free-text String, Int, Boolean.
        first.scenarioId = Scenario.VolkaresReturn.id
        first.knight = Knight.BRAEVALAR
        first.fame = "37"
        first.headsDefeated = 3
        first.city1Conquered = true

        val restored = buildViewModel(SavedStateHandle.createHandle(parcelRoundTrip(handle), null))

        assertEquals(Scenario.VolkaresReturn, restored.scenario) // derived from the restored id
        assertEquals(Knight.BRAEVALAR, restored.knight)
        assertEquals("37", restored.fame)
        assertEquals(3, restored.headsDefeated)
        assertEquals(true, restored.city1Conquered)
    }
}
