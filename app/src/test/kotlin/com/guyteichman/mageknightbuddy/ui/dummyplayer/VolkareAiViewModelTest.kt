package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareCard
import com.guyteichman.mageknightbuddy.domain.VolkareSession
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class VolkareAiViewModelTest {

    // The ViewModel's init block launches a one-shot restore on viewModelScope (which dispatches
    // via Dispatchers.Main), so Main needs a test dispatcher installed for plain JVM unit tests -
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
        val repository = VolkareSessionRepository(FakeVolkareSessionDao())
        val viewModel = VolkareAiViewModel(repository)

        assertNull(viewModel.session)
    }

    @Test
    fun `restores the session that was saved by the setup screen`() = runTest {
        val dao = FakeVolkareSessionDao()
        val repository = VolkareSessionRepository(dao)
        repository.save(
            VolkareSession.start(
                Scenario.VolkaresQuest,
                RaceLevel.TIGHT,
                woundCount = 0,
                deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED)),
            ),
        )

        val viewModel = VolkareAiViewModel(repository)
        advanceUntilIdle()

        assertEquals(Scenario.VolkaresQuest, viewModel.session?.scenario)
        assertEquals(RaceLevel.TIGHT, viewModel.session?.raceLevel)
    }

    @Test
    fun `playTurn advances the in-memory session and autosaves it`() = runTest {
        val dao = FakeVolkareSessionDao()
        val repository = VolkareSessionRepository(dao)
        repository.save(
            VolkareSession.start(
                Scenario.VolkaresReturn,
                RaceLevel.FAIR,
                woundCount = 0,
                deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED), VolkareCard.BasicAction(CardColor.GREEN)),
            ),
        )
        val viewModel = VolkareAiViewModel(repository)
        advanceUntilIdle()

        viewModel.playTurn()

        assertEquals(listOf(VolkareCard.BasicAction(CardColor.GREEN)), viewModel.session?.deckOrder)
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `a second playTurn call while one is still in flight is ignored, like a double-tap`() = runTest {
        val dao = FakeVolkareSessionDao()
        val repository = VolkareSessionRepository(dao)
        repository.save(
            VolkareSession.start(
                Scenario.VolkaresReturn,
                RaceLevel.FAIR,
                woundCount = 0,
                deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED), VolkareCard.BasicAction(CardColor.GREEN)),
            ),
        )
        val viewModel = VolkareAiViewModel(repository)
        advanceUntilIdle()

        // The gate holds the first playTurn() suspended inside its autosave, mid-flight - the same
        // window a real double-tap would land in with Room's actual IO-dispatched upsert.
        val gate = CompletableDeferred<Unit>()
        dao.upsertGate = gate
        launch { viewModel.playTurn() }
        runCurrent()

        launch { viewModel.playTurn() }
        runCurrent()

        gate.complete(Unit)
        advanceUntilIdle()

        // If the second call weren't gated, both deck cards would have been drawn instead of one.
        assertEquals(listOf(VolkareCard.BasicAction(CardColor.GREEN)), viewModel.session?.deckOrder)
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `endRound advances the round and autosaves, without touching the deck`() = runTest {
        val dao = FakeVolkareSessionDao()
        val repository = VolkareSessionRepository(dao)
        repository.save(
            VolkareSession.start(
                Scenario.VolkaresReturn,
                RaceLevel.FAIR,
                woundCount = 0,
                deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED)),
            ),
        )
        val viewModel = VolkareAiViewModel(repository)
        advanceUntilIdle()

        viewModel.endRound()

        assertEquals(2, viewModel.session?.round)
        assertEquals(listOf(VolkareCard.BasicAction(CardColor.RED)), viewModel.session?.deckOrder)
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `toggleCityRevealed flips the flag and autosaves`() = runTest {
        val dao = FakeVolkareSessionDao()
        val repository = VolkareSessionRepository(dao)
        repository.save(VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR))
        val viewModel = VolkareAiViewModel(repository)
        advanceUntilIdle()
        assertFalse(viewModel.session?.cityRevealed ?: true)

        viewModel.toggleCityRevealed()

        assertTrue(viewModel.session?.cityRevealed ?: false)
        assertEquals(viewModel.session, repository.restore())
    }
}
