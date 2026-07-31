package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
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
 * Saved-state parcelability guards for the two remaining Dummy Player-tab setup ViewModels
 * ([DummyPlayerSetupViewModel], [ProxyPlayerSetupViewModel]). Neither is broken today - both store
 * only a [Knight] enum (Serializable) and a Boolean - but the whole tab is instantiated at once
 * (see DummyPlayerScreen), so every one of its ViewModels' saved state is parceled together on
 * background. These tests hold the same invariant [VolkareSetupViewModel] violated in issue #212,
 * so a future field of a non-parcelable type (a `data object`, an un-annotated domain class) fails
 * here instead of crashing the app. See [parcelRoundTrip] for why plain-JVM tests miss this.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SetupViewModelSavedStateTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `DummyPlayerSetupViewModel saved state survives a Parcel round-trip and restores the knight`() {
        val handle = SavedStateHandle()
        val first = DummyPlayerSetupViewModel(handle, DummyPlayerSessionRepository(FakeDummyPlayerSessionDao()))
        first.pickKnight(Knight.GOLDYX)

        val restored = DummyPlayerSetupViewModel(
            SavedStateHandle.createHandle(parcelRoundTrip(handle), null),
            DummyPlayerSessionRepository(FakeDummyPlayerSessionDao()),
        )

        assertEquals(Knight.GOLDYX, restored.knight)
    }

    @Test
    fun `ProxyPlayerSetupViewModel saved state survives a Parcel round-trip and restores the knight`() {
        val handle = SavedStateHandle()
        val first = ProxyPlayerSetupViewModel(handle, ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()))
        first.pickKnight(Knight.GOLDYX)

        val restored = ProxyPlayerSetupViewModel(
            SavedStateHandle.createHandle(parcelRoundTrip(handle), null),
            ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()),
        )

        assertEquals(Knight.GOLDYX, restored.knight)
    }

    // The following 2 tests are this file's actual regression guard for issue #220's new fields:
    // DummyPlayerSetupViewModel.scenario/ProxyPlayerSetupViewModel.scenario are stored as a String
    // id (see their scenarioId doc comments), exactly the fix pattern issue #212 established for
    // VolkareSetupViewModel.scenario - a plain-JVM test can't tell a correct id-based store apart
    // from an accidental direct-object store (both just work in memory), so only a real Parcel
    // round-trip like this one actually exercises the thing #212 broke.
    @Test
    fun `DummyPlayerSetupViewModel saved state survives a Parcel round-trip and restores isSolo and scenario`() {
        val handle = SavedStateHandle()
        val first = DummyPlayerSetupViewModel(handle, DummyPlayerSessionRepository(FakeDummyPlayerSessionDao()))
        first.isSolo = false
        first.scenario = Scenario.AgainstTheDragon

        val restored = DummyPlayerSetupViewModel(
            SavedStateHandle.createHandle(parcelRoundTrip(handle), null),
            DummyPlayerSessionRepository(FakeDummyPlayerSessionDao()),
        )

        assertEquals(false, restored.isSolo)
        assertEquals(Scenario.AgainstTheDragon, restored.scenario)
    }

    @Test
    fun `ProxyPlayerSetupViewModel saved state survives a Parcel round-trip and restores isSolo and scenario`() {
        val handle = SavedStateHandle()
        val first = ProxyPlayerSetupViewModel(handle, ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()))
        first.isSolo = false
        first.scenario = Scenario.ForTheCouncil

        val restored = ProxyPlayerSetupViewModel(
            SavedStateHandle.createHandle(parcelRoundTrip(handle), null),
            ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()),
        )

        assertEquals(false, restored.isSolo)
        assertEquals(Scenario.ForTheCouncil, restored.scenario)
    }
}
