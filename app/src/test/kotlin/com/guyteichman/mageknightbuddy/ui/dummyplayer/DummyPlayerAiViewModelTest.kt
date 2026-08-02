package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlin.random.Random
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
        repository.save(
            DummyPlayerSession.start(Knight.GOLDYX, deckOrder = listOf(CardIdentity.SingleColor(CardColor.RED))),
        )

        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        assertEquals(Knight.GOLDYX, viewModel.session?.knight)
    }

    @Test
    fun `playTurn advances the in-memory session and autosaves it`() = runTest {
        val dao = FakeDummyPlayerSessionDao()
        val repository = DummyPlayerSessionRepository(dao)
        repository.save(
            DummyPlayerSession.start(
                Knight.GOLDYX,
                deckOrder = listOf(CardColor.RED, CardColor.GREEN, CardColor.BLUE).map { CardIdentity.SingleColor(it) },
            ),
        )
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.playTurn()

        assertEquals(emptyList(), viewModel.session?.deckOrder)
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `a second playTurn call while one is still in flight is ignored, like a double-tap`() = runTest {
        val dao = FakeDummyPlayerSessionDao()
        val repository = DummyPlayerSessionRepository(dao)
        // GOLDYX starts with 0 WHITE/RED crystals (see STARTING_CRYSTAL_DOTS), so a turn's initial
        // 3-card reveal ending on WHITE/RED never pulls the "additional reveal" bonus cards - one
        // playTurn() always consumes exactly 3 of these 5, leaving [BLUE, RED] behind. That gives a
        // deck state a second, wrongly-allowed playTurn() would visibly further drain (to empty),
        // distinguishing "one play happened" from "two plays happened".
        repository.save(
            DummyPlayerSession.start(
                Knight.GOLDYX,
                deckOrder = listOf(CardColor.RED, CardColor.GREEN, CardColor.WHITE, CardColor.BLUE, CardColor.RED)
                    .map { CardIdentity.SingleColor(it) },
            ),
        )
        val viewModel = DummyPlayerAiViewModel(repository)
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

        assertEquals(listOf(CardColor.BLUE, CardColor.RED).map { CardIdentity.SingleColor(it) }, viewModel.session?.deckOrder)
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `endRound resets roundEnded and autosaves, even when called before playTurn`() = runTest {
        val dao = FakeDummyPlayerSessionDao()
        val repository = DummyPlayerSessionRepository(dao)
        repository.save(
            DummyPlayerSession.start(Knight.GOLDYX, deckOrder = listOf(CardIdentity.SingleColor(CardColor.RED))),
        )
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, viewModel.session?.round)
        assertEquals(
            DummyPlayerEvent.RoundEnded(
                round = 1,
                advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
                spellOfferColor = CardColor.BLUE,
            ),
            viewModel.session?.log?.last(),
        )
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `endRound accepts a Dual-Color Advanced Action offer card`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        repository.save(DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()))
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.endRound(
            advancedActionOfferColor = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(
            CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            viewModel.session?.deckOrder?.single(),
        )
    }

    @Test
    fun `pickPlayerTactic sets the player's pick and autosaves, leaving the dummy's pick untouched`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        repository.save(DummyPlayerSession.start(Knight.GOLDYX, deckOrder = emptyList()))
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.pickPlayerTactic(3)

        assertEquals(3, viewModel.session?.tacticState?.playerPick)
        assertNull(viewModel.session?.tacticState?.dummyPick)
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `pickDummyTactic sets the dummy's pick and autosaves, leaving the player's pick untouched`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        repository.save(DummyPlayerSession.start(Knight.GOLDYX, deckOrder = emptyList()))
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.pickDummyTactic(Random(0))

        assertTrue(viewModel.session?.tacticState?.dummyPick in 1..6)
        assertNull(viewModel.session?.tacticState?.playerPick)
        assertEquals(viewModel.session, repository.restore())
    }

    @Test
    fun `undo restores the exact session from before the last action and autosaves it`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        // A 3-card RED deck: GOLDYX holds 0 RED crystals (see STARTING_CRYSTAL_DOTS), so the turn's
        // 3rd card (RED) triggers no chain - one playTurn() drains the deck to empty deterministically.
        val entry = DummyPlayerSession.start(
            Knight.GOLDYX,
            deckOrder = List(3) { CardIdentity.SingleColor(CardColor.RED) },
        )
        repository.save(entry)
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.playTurn()
        // Sanity-check the action actually happened before undoing it, so this test can tell
        // "undo reverted a real change" apart from "nothing ever changed".
        assertEquals(emptyList(), viewModel.session?.deckOrder)

        viewModel.undo()

        // The whole session snapshot is restored - deck, discard, and the appended TurnPlayed log
        // entry all revert together, since undo pops the pre-action object rather than reversing
        // fields one by one. Comparing against `entry` asserts every field at once.
        assertEquals(entry, viewModel.session)
        // Write-through: Room now holds the reverted state too, so navigating away and back would
        // not resurrect the undone turn (issue #62's autosave interaction).
        assertEquals(entry, repository.restore())
    }

    @Test
    fun `undo of endRound restores the exact pre-shuffle deck and discard pile`() = runTest {
        // The reshuffle case issue #62's Notes singled out as the design risk ("whether EndRound's
        // reshuffled deck order can even be reconstructed once undone - the round-prep shuffle is
        // not currently seeded/reversible"). The snapshot approach dissolves it by retaining the
        // pre-shuffle object rather than recomputing it; this test proves that end to end.
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        // GREEN and BLUE (positions 4-5) are never revealed and stay in a distinguishable order, so
        // asserting them back in [GREEN, BLUE] order is a real order check, not a same-card
        // coincidence. The leading 3 REDs are what playTurn() reveals (GOLDYX holds 0 RED crystals,
        // so the 3rd RED triggers no chain - see STARTING_CRYSTAL_DOTS).
        val entry = DummyPlayerSession.start(
            Knight.GOLDYX,
            deckOrder = listOf(CardColor.RED, CardColor.RED, CardColor.RED, CardColor.GREEN, CardColor.BLUE)
                .map { CardIdentity.SingleColor(it) },
        )
        repository.save(entry)
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        // Build the pre-endRound state via the class's own playTurn() (per CLAUDE.md's TDD habit),
        // so endRound actually reshuffles a *non-empty* discard pile rather than a convenient empty
        // one - a shortcut precondition would never exercise the discard-fold that undo must revert.
        viewModel.playTurn()
        val afterPlay = viewModel.session
        // Precondition sanity: [RED,RED,RED] revealed into discard, [GREEN,BLUE] left in the deck.
        assertEquals(
            listOf(CardColor.GREEN, CardColor.BLUE).map { CardIdentity.SingleColor(it) },
            afterPlay?.deckOrder,
        )
        assertEquals(
            List(3) { CardIdentity.SingleColor(CardColor.RED) },
            afterPlay?.discardPile,
        )

        viewModel.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )
        // endRound really ran: round advanced, discard folded into a now-6-card reshuffled deck,
        // discard emptied. (Its order is non-deterministic - deliberately not asserted here.)
        assertEquals(2, viewModel.session?.round)
        assertEquals(6, viewModel.session?.deckOrder?.size)
        assertEquals(emptyList(), viewModel.session?.discardPile)

        viewModel.undo()

        // The pre-shuffle deck comes back in its exact original order, and the discard pile endRound
        // had emptied is restored - neither is recomputed, both are the retained snapshot.
        assertEquals(
            listOf(CardColor.GREEN, CardColor.BLUE).map { CardIdentity.SingleColor(it) },
            viewModel.session?.deckOrder,
        )
        assertEquals(List(3) { CardIdentity.SingleColor(CardColor.RED) }, viewModel.session?.discardPile)
        // And every other field reverts too (round, crystals, the appended RoundEnded log entry).
        assertEquals(afterPlay, viewModel.session)
        assertEquals(afterPlay, repository.restore())
    }

    @Test
    fun `canUndo is false until a mutation happens, then true`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        repository.save(DummyPlayerSession.start(Knight.GOLDYX, deckOrder = listOf(CardIdentity.SingleColor(CardColor.RED))))
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.canUndo)

        viewModel.playTurn()

        assertTrue(viewModel.canUndo)
    }

    @Test
    fun `undo walks back through the full stack of actions to the entry state`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        // 6 RED cards = two no-chain turns (see the single-undo test's note on why RED never chains
        // for GOLDYX): turn 1 leaves 3, turn 2 leaves 0.
        val entry = DummyPlayerSession.start(
            Knight.GOLDYX,
            deckOrder = List(6) { CardIdentity.SingleColor(CardColor.RED) },
        )
        repository.save(entry)
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.playTurn()
        viewModel.playTurn()
        assertEquals(0, viewModel.session?.deckOrder?.size)

        viewModel.undo()
        assertEquals(3, viewModel.session?.deckOrder?.size)

        viewModel.undo()
        assertEquals(entry, viewModel.session)
        // Back at the entry state, there's nothing left to revert.
        assertFalse(viewModel.canUndo)
    }

    @Test
    fun `undo does nothing when there is no prior action to revert`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        val entry = DummyPlayerSession.start(Knight.GOLDYX, deckOrder = listOf(CardIdentity.SingleColor(CardColor.RED)))
        repository.save(entry)
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.undo()

        assertEquals(entry, viewModel.session)
        assertFalse(viewModel.canUndo)
    }
}
