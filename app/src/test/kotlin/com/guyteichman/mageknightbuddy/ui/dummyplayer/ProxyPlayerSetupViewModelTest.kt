package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ProxyPlayerSetupViewModelTest {

    // The ViewModel's init block launches a one-shot check on viewModelScope (which dispatches
    // via Dispatchers.Main), so Main needs a test dispatcher installed for plain JVM unit tests -
    // same fixture as DummyPlayerSetupViewModelTest.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `defaults to the first Knight, not random, and no saved session`() = runTest {
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()))
        advanceUntilIdle()

        assertEquals(Knight.entries.first(), viewModel.knight)
        assertEquals(false, viewModel.wasRandom)
        assertEquals(false, viewModel.hasSavedSession)
    }

    @Test
    fun `start marks hasSavedSession true so Restore Game reflects the just-started session`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), repository)

        viewModel.start()

        assertTrue(viewModel.hasSavedSession)
    }

    @Test
    fun `pickKnight selects the Knight directly and clears wasRandom`() {
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()))

        viewModel.pickKnight(Knight.CORAL)

        assertEquals(Knight.CORAL, viewModel.knight)
        assertEquals(false, viewModel.wasRandom)
    }

    @Test
    fun `pickRandom rolls a Knight immediately and sets wasRandom`() {
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()))

        viewModel.pickRandom()

        assertTrue(viewModel.wasRandom)
    }

    @Test
    fun `start saves a fresh session for the chosen Knight`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), repository)

        viewModel.pickKnight(Knight.GOLDYX)
        viewModel.start()

        assertEquals(Knight.GOLDYX, repository.restore()?.knight)
    }
}
