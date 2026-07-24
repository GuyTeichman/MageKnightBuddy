package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
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

        assertEquals(session, roundTripped)

        // Independent ground truth, worked out by hand from docs/rules/dummy-player.md rather than
        // just capturing whatever `session` happened to hold - see issue #150. A self-referential
        // `assertEquals(session, roundTripped)` alone can't catch a domain bug: it only confirms
        // serialization preserved whatever playTurn()/endRound() produced, bug or not.
        //
        // playTurn() #1: deck has only 2 cards, so the mandatory "flip 3" flips both (RED, GREEN)
        // and leaves the deck empty; the 3rd/last card (GREEN) matches 0 crystals (Coral starts
        // with 0 green), so there's no additional flip.
        // playTurn() #2: deck is empty at turn start, so it announces End of Round instead.
        // endRound(): reshuffle = undrawn deck (empty) + entire discard pile (RED, GREEN) + the new
        // Advanced Action offer card (WHITE) => 3 cards, discard pile empties out; the Spell offer
        // color (BLUE) grants the Dummy Player one BLUE crystal, on top of Coral's starting dots
        // (White, White, Red -> RED:1, WHITE:2, GREEN:0, BLUE:0 before this round-prep step).
        assertEquals(2, roundTripped.round)
        assertEquals(false, roundTripped.roundEnded)
        assertEquals(emptyList(), roundTripped.discardPile)
        assertEquals(
            mapOf(CardColor.RED to 1, CardColor.GREEN to 0, CardColor.BLUE to 1, CardColor.WHITE to 2),
            roundTripped.crystals,
        )
        assertEquals(
            listOf(
                DummyPlayerEvent.RoundStarted(round = 1),
                DummyPlayerEvent.TurnPlayed(
                    round = 1,
                    initialReveal = listOf(CardIdentity.SingleColor(CardColor.RED), CardIdentity.SingleColor(CardColor.GREEN)),
                    additionalReveal = emptyList(),
                ),
                DummyPlayerEvent.EndOfRoundAnnounced(round = 1),
                DummyPlayerEvent.RoundEnded(
                    round = 1,
                    advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
                    spellOfferColor = CardColor.BLUE,
                ),
            ),
            roundTripped.log,
        )
        // deckOrder is `.shuffled()` by endRound(), so its exact order is random - assert the
        // multiset of cards it holds instead of a specific order. `groupingBy { it }.eachCount()`
        // turns the list into a Map<CardIdentity, Int> of occurrence counts, which compares equal
        // regardless of list order.
        assertEquals(
            mapOf<CardIdentity, Int>(
                CardIdentity.SingleColor(CardColor.RED) to 1,
                CardIdentity.SingleColor(CardColor.GREEN) to 1,
                CardIdentity.SingleColor(CardColor.WHITE) to 1,
            ),
            roundTripped.deckOrder.groupingBy { it }.eachCount(),
        )
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

        // Independent ground truth (see the first test's comment for why this matters). No
        // playTurn() here - endRound() runs directly on the freshly-started session, so its
        // reshuffle is just the starting deck (RED, DualColor(GREEN, BLUE)) + empty discard pile +
        // the new Advanced Action offer card (DualColor(WHITE, RED)) => 3 cards. Spell offer color
        // BLUE grants one BLUE crystal on top of Coral's starting dots (RED:1, WHITE:2, GREEN:0,
        // BLUE:0).
        assertEquals(2, restored.round)
        assertEquals(false, restored.roundEnded)
        assertEquals(emptyList(), restored.discardPile)
        assertEquals(
            mapOf(CardColor.RED to 1, CardColor.GREEN to 0, CardColor.BLUE to 1, CardColor.WHITE to 2),
            restored.crystals,
        )
        assertEquals(
            listOf(
                DummyPlayerEvent.RoundStarted(round = 1),
                DummyPlayerEvent.RoundEnded(
                    round = 1,
                    advancedActionOfferColor = CardIdentity.DualColor(CardColor.WHITE, CardColor.RED),
                    spellOfferColor = CardColor.BLUE,
                ),
            ),
            restored.log,
        )
        // Multiset comparison again, since endRound() shuffles deckOrder - see the first test.
        assertEquals(
            mapOf(
                CardIdentity.SingleColor(CardColor.RED) to 1,
                CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE) to 1,
                CardIdentity.DualColor(CardColor.WHITE, CardColor.RED) to 1,
            ),
            restored.deckOrder.groupingBy { it }.eachCount(),
        )
    }

    @Test
    fun `toEntity then toDomain round-trips startsAtNight`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = emptyList(),
            startsAtNight = true,
        )

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
        assertEquals(true, roundTripped.startsAtNight)
    }
}
