package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
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
}
