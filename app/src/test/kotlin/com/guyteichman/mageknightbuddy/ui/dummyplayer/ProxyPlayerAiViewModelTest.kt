package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
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
class ProxyPlayerAiViewModelTest {

    // The ViewModel's init block launches a one-shot restore on viewModelScope (which dispatches
    // via Dispatchers.Main), so Main needs a test dispatcher installed for plain JVM unit tests,
    // same as DummyPlayerAiViewModelTest.
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
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        val viewModel = ProxyPlayerAiViewModel(repository)

        assertNull(viewModel.session)
    }

    @Test
    fun `restores the saved session on creation`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        repository.save(ProxyPlayerSession.start(Knight.CORAL))
        val viewModel = ProxyPlayerAiViewModel(repository)
        advanceUntilIdle()

        assertEquals(Knight.CORAL, viewModel.session?.knight)
    }

    @Test
    fun `playTurn advances the session and autosaves`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        repository.save(ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()))
        val viewModel = ProxyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.playTurn()

        assertEquals(true, viewModel.session?.roundEnded)
        assertEquals(true, repository.restore()?.roundEnded)
    }

    @Test
    fun `resolveObjective discards the current objective and autosaves`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        repository.save(
            ProxyPlayerSession.restore(
                knight = Knight.CORAL,
                wasRandom = false,
                deckOrder = emptyList(),
                discardPile = emptyList(),
                crystals = ProxyPlayerSession.start(Knight.CORAL).crystals,
                round = 1,
                roundEnded = false,
                objectiveCard = ProxyPlayerCard.BasicAction(CardColor.GREEN),
                objectiveShields = 1,
                log = emptyList(),
            ),
        )
        val viewModel = ProxyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.resolveObjective()

        assertNull(viewModel.session?.objectiveCard)
        assertNull(repository.restore()?.objectiveCard)
    }

    @Test
    fun `endRound applies the round-prep offer interactions and autosaves`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        repository.save(ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()))
        val viewModel = ProxyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.endRound(advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE), spellOfferColor = CardColor.BLUE)

        assertEquals(2, viewModel.session?.round)
        assertEquals(2, repository.restore()?.round)
    }
}
