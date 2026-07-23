package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DummyPlayerSessionMapperTest {

    @Test
    fun `toEntity then toDomain round-trips a session covering all four event log types`() {
        // RoundStarted (from start), TurnPlayed (first playTurn), EndOfRoundAnnounced (second
        // playTurn on an empty deck), RoundEnded (endRound) - exercises every DummyPlayerEvent case.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.RED, CardColor.GREEN).map { CardIdentity.SingleColor(it) },
        ).playTurn().playTurn().endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        val roundTripped = session.toEntity().toDomain()

        // `assertEquals(session, roundTripped)` alone only checks that serialization preserves
        // whatever the domain code produced - bug or not (see issue #150: this is exactly why this
        // test round-tripped the pre-fix, discard-pile-dropping endRound() output for that method's
        // whole life without ever flagging it). The assertions below are independently derived by
        // hand-tracing the two playTurn() calls and the endRound() call above, not copied from
        // `session` or a debugger, so they'd actually catch a regression in that logic:
        // - playTurn() #1: deckOrder=[Red,Green] (only 2 cards, so the "flip 3" take(3) flips both).
        //   Coral has 0 Green crystals, so the last-card color (Green) chains nothing further.
        //   -> discardPile becomes [Red, Green], deckOrder becomes [].
        // - playTurn() #2: deckOrder is already empty, so this announces End of Round instead -
        //   discardPile/deckOrder are untouched, staying [Red, Green] / [].
        // - endRound(White, Blue): per the #148 fix, the new deck is
        //   deckOrder([]) + discardPile([Red, Green]) + the offer card (White), reshuffled - and
        //   discardPile empties out. So the reshuffled deck holds exactly 1 Red + 1 Green + 1 White.
        assertEquals(emptyList(), roundTripped.discardPile)
        assertEquals(
            mapOf(CardColor.RED to 1, CardColor.GREEN to 1, CardColor.BLUE to 0, CardColor.WHITE to 1),
            roundTripped.remainingByColor,
        )
        assertEquals(3, roundTripped.deckOrder.size)
        assertEquals(2, roundTripped.round)
        assertEquals(mapOf(CardColor.RED to 1, CardColor.GREEN to 0, CardColor.BLUE to 1, CardColor.WHITE to 2), roundTripped.crystals)
        assertEquals(session, roundTripped)
    }

    @Test
    fun `toEntity stamps the given updatedAt onto the entity`() {
        val session = DummyPlayerSession.start(
            Knight.GOLDYX,
            deckOrder = listOf(CardIdentity.SingleColor(CardColor.RED)),
        )

        val entity = session.toEntity(updatedAt = 99L)

        assertEquals(99L, entity.updatedAt)
    }

    @Test
    fun `toEntity defaults updatedAt to roughly the current time when not given explicitly`() {
        val session = DummyPlayerSession.start(
            Knight.GOLDYX,
            deckOrder = listOf(CardIdentity.SingleColor(CardColor.RED)),
        )
        val before = System.currentTimeMillis()

        val entity = session.toEntity()

        val after = System.currentTimeMillis()
        assertTrue(entity.updatedAt in before..after)
    }

    @Test
    fun `toEntity and toDomain round-trip a deck containing a Dual-Color Advanced Action card`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                CardIdentity.SingleColor(CardColor.RED),
                CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            ),
        ).endRound(
            advancedActionOfferColor = CardIdentity.DualColor(CardColor.WHITE, CardColor.RED),
            spellOfferColor = CardColor.BLUE,
        )

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }
}
