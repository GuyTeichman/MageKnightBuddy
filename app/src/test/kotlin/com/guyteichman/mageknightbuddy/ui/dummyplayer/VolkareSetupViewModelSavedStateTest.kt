package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.RaceLevel
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
 * Regression tests for the "app crashes every time you background the Dummy Player screen" bug
 * (issue #208): [VolkareSetupViewModel] used to store the [Scenario] object itself in
 * [SavedStateHandle], but Scenario's members are `data object`s - neither Parcelable nor
 * Serializable - so Android threw `IllegalArgumentException: Parcel: unknown type for value
 * VolkaresReturn` the instant it parceled saved state on background.
 *
 * These run under Robolectric (not the plain-JVM setup the other ViewModel tests use) precisely
 * because a plain-JVM `SavedStateHandle()` holds any object in memory and never parcels it - which
 * is exactly why every existing test sailed past this bug. Here we drive the real Android save path
 * (`savedStateProvider().saveState()` -> a Bundle) through an actual [Parcel] round-trip, the same
 * thing `onSaveInstanceState` does, so a non-parcelable value fails the test instead of the phone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Pin an SDK Robolectric ships shadows for, independent of the app's targetSdk.
class VolkareSetupViewModelSavedStateTest {

    // The ViewModel's init block launches a one-shot restore check on viewModelScope (Dispatchers.Main),
    // so Main needs a test dispatcher installed - same as the plain VolkareSetupViewModelTest.
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `default saved state survives a Parcel round-trip without crashing`() {
        val handle = SavedStateHandle()
        VolkareSetupViewModel(handle, VolkareSessionRepository(FakeVolkareSessionDao()))

        // Before the fix this threw IllegalArgumentException: unknown type for value VolkaresReturn.
        parcelRoundTrip(handle)
    }

    @Test
    fun `a picked scenario is restored after a Parcel round-trip (process death)`() {
        val handle = SavedStateHandle()
        val first = VolkareSetupViewModel(handle, VolkareSessionRepository(FakeVolkareSessionDao()))
        first.pickScenario(Scenario.VolkaresQuest)
        first.pickRaceLevel(RaceLevel.TIGHT)

        val restoredBundle = parcelRoundTrip(handle)
        val restored = VolkareSetupViewModel(
            SavedStateHandle.createHandle(restoredBundle, null),
            VolkareSessionRepository(FakeVolkareSessionDao()),
        )

        // Independently expected values (not read off the first VM): Quest + Tight, whose table
        // Wound count is 16 (docs/rules/volkares-quest.md), proving the id-based storage both
        // survives the round-trip and rebuilds the right Scenario.
        assertEquals(Scenario.VolkaresQuest, restored.scenario)
        assertEquals(RaceLevel.TIGHT, restored.raceLevel)
        assertEquals(16, restored.woundCount)
    }
}
