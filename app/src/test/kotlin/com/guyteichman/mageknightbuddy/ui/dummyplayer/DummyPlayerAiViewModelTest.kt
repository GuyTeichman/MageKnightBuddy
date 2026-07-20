package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class DummyPlayerAiViewModelTest {

    // The ViewModel's init block launches a one-shot restore on viewModelScope (which dispatches
    // via Dispatchers.Main), so Main needs a test dispatcher installed for plain JVM unit tests,
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
    fun `session is null until the saved session is restored`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        val viewModel = DummyPlayerAiViewModel(repository)

        assertNull(viewModel.session)
    }

    @Test
    fun `restores the session that was saved by the setup screen`() = runTest {
        val dao = FakeDummyPlayerSessionDao()
        val repository = DummyPlayerSessionRepository(dao)
        repository.save(DummyPlayerSession.start(Knight.GOLDYX, deckOrder = listOf(CardColor.RED)))

        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        assertEquals(Knight.GOLDYX, viewModel.session?.knight)
    }

    @Test
    fun `playTurn advances the in-memory session and autosaves it`() = runTest {
        val dao = FakeDummyPlayerSessionDao()
        val repository = DummyPlayerSessionRepository(dao)
        repository.save(DummyPlayerSession.start(Knight.GOLDYX, deckOrder = listOf(CardColor.RED, CardColor.GREEN, CardColor.BLUE)))
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.playTurn()

        assertEquals(emptyList(), viewModel.session?.deckOrder)
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `endRound resets roundEnded and autosaves, even when called before playTurn`() = runTest {
        val dao = FakeDummyPlayerSessionDao()
        val repository = DummyPlayerSessionRepository(dao)
        repository.save(DummyPlayerSession.start(Knight.GOLDYX, deckOrder = listOf(CardColor.RED)))
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.endRound(advancedActionOfferColor = CardColor.WHITE, spellOfferColor = CardColor.BLUE)

        assertEquals(2, viewModel.session?.round)
        assertEquals(
            DummyPlayerEvent.RoundEnded(round = 1, advancedActionOfferColor = CardColor.WHITE, spellOfferColor = CardColor.BLUE),
            viewModel.session?.log?.last(),
        )
        assertEquals(viewModel.session, repository.restore())
    }
}
