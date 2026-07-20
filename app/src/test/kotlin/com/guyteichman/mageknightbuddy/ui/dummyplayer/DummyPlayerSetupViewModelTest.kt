package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class DummyPlayerSetupViewModelTest {

    // The ViewModel's init block launches a one-shot check on viewModelScope (which dispatches
    // via Dispatchers.Main), so Main needs a test dispatcher installed for plain JVM unit tests.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start marks hasSavedSession true so Restore Game reflects the just-started session`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        val viewModel = DummyPlayerSetupViewModel(SavedStateHandle(), repository)

        viewModel.start()

        assertTrue(viewModel.hasSavedSession)
    }
}
