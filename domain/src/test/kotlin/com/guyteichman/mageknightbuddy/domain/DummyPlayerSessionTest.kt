package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class DummyPlayerSessionTest {

    @Test
    fun `start creates a 16-card deck of 4 Red, 4 Green, 4 Blue, 4 White`() {
        val session = DummyPlayerSession.start(Knight.GOLDYX)

        assertEquals(
            mapOf(
                CardColor.RED to 4,
                CardColor.GREEN to 4,
                CardColor.BLUE to 4,
                CardColor.WHITE to 4,
            ),
            session.remainingByColor,
        )
    }

    @Test
    fun `start gives Goldyx 2 Green and 1 Blue crystal, per the rulebook's own example`() {
        val session = DummyPlayerSession.start(Knight.GOLDYX)

        assertEquals(
            mapOf(
                CardColor.RED to 0,
                CardColor.GREEN to 2,
                CardColor.BLUE to 1,
                CardColor.WHITE to 0,
            ),
            session.crystals,
        )
    }

    @Test
    fun `start gives Arythea 2 Red and 1 White crystal`() {
        val session = DummyPlayerSession.start(Knight.ARYTHEA)

        assertEquals(
            mapOf(
                CardColor.RED to 2,
                CardColor.GREEN to 0,
                CardColor.BLUE to 0,
                CardColor.WHITE to 1,
            ),
            session.crystals,
        )
    }

    @Test
    fun `startRandom sets wasRandom and picks a knight whose crystals match the starting table`() {
        val session = DummyPlayerSession.startRandom(random = Random(0))

        assertEquals(true, session.wasRandom)
        assertEquals(DummyPlayerSession.start(session.knight).crystals, session.crystals)
    }

    @Test
    fun `start sets wasRandom to false for an explicitly chosen knight`() {
        val session = DummyPlayerSession.start(Knight.CORAL)

        assertEquals(false, session.wasRandom)
        assertEquals(Knight.CORAL, session.knight)
    }

    @Test
    fun `playTurn flips 3 cards and ends the turn when the 3rd card's color has no matching crystal`() {
        // Coral's starting crystals are Blue, Blue, Red (no Green) - see docs/rules/dummy-player.md's example.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.WHITE, CardColor.RED, CardColor.GREEN, CardColor.BLUE, CardColor.WHITE),
        )

        val next = session.playTurn()

        assertEquals(listOf(CardColor.WHITE, CardColor.RED, CardColor.GREEN), next.discardPile)
        assertEquals(listOf(CardColor.BLUE, CardColor.WHITE), next.deckOrder)
    }

    @Test
    fun `playTurn chains one additional reveal per matching crystal of the 3rd card's color`() {
        // Coral holds 2 Blue crystals - the 3rd card (Blue) should chain 2 additional reveals.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                CardColor.WHITE, CardColor.RED, CardColor.BLUE,
                CardColor.GREEN, CardColor.WHITE, CardColor.RED,
            ),
        )

        val next = session.playTurn()

        assertEquals(
            listOf(CardColor.WHITE, CardColor.RED, CardColor.BLUE, CardColor.GREEN, CardColor.WHITE),
            next.discardPile,
        )
        assertEquals(listOf(CardColor.RED), next.deckOrder)
    }

    @Test
    fun `playTurn on a near-empty deck flips only what's available, same logic as a full flip`() {
        // Only 2 cards left - the "initial 3" flip is capped at what's there, no special-casing.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.BLUE, CardColor.WHITE),
        )

        val next = session.playTurn()

        assertEquals(listOf(CardColor.BLUE, CardColor.WHITE), next.discardPile)
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn on a near-empty deck still chains if there's nothing left to chain into`() {
        // Last card flipped is Blue (2 matching crystals) but the deck is already empty afterward -
        // additional reveals are bounded by what's left, so no chain happens.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.WHITE, CardColor.BLUE),
        )

        val next = session.playTurn()

        assertEquals(listOf(CardColor.WHITE, CardColor.BLUE), next.discardPile)
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn on an empty deck announces End of Round instead of flipping`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.playTurn()

        assertEquals(true, next.roundEnded)
        assertEquals(emptyList(), next.discardPile)
        assertEquals(DummyPlayerEvent.EndOfRoundAnnounced(round = 1), next.log.last())
    }

    @Test
    fun `playTurn is a no-op once roundEnded is already true`() {
        val ended = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = ended.playTurn()

        assertEquals(ended.roundEnded, next.roundEnded)
        assertEquals(ended.log, next.log)
        assertEquals(ended.deckOrder, next.deckOrder)
    }

    @Test
    fun `endRound appends the Advanced Action offer color to the deck, reshuffled`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.RED, CardColor.GREEN),
        )

        val next = session.endRound(
            advancedActionOfferColor = CardColor.WHITE,
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(
            mapOf(CardColor.RED to 1, CardColor.GREEN to 1, CardColor.BLUE to 0, CardColor.WHITE to 1),
            next.remainingByColor,
        )
    }

    @Test
    fun `endRound grants +1 crystal of the Spell offer color, uncapped`() {
        // Coral starts with 2 Blue crystals - gaining another should reach 3, not cap.
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardColor.WHITE,
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(3, next.crystals.getValue(CardColor.BLUE))
    }

    @Test
    fun `endRound increments the round, resets roundEnded, and logs the round-ended event`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = session.endRound(
            advancedActionOfferColor = CardColor.WHITE,
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, next.round)
        assertEquals(false, next.roundEnded)
        assertEquals(
            DummyPlayerEvent.RoundEnded(round = 1, advancedActionOfferColor = CardColor.WHITE, spellOfferColor = CardColor.BLUE),
            next.log.last(),
        )
    }

    @Test
    fun `endRound is always callable, even mid-round with cards still in the deck`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = listOf(CardColor.RED))

        val next = session.endRound(
            advancedActionOfferColor = CardColor.WHITE,
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, next.round)
    }

    @Test
    fun `restore reconstructs a session with the exact same state it was given`() {
        val original = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.RED, CardColor.GREEN),
        ).playTurn().endRound(
            advancedActionOfferColor = CardColor.WHITE,
            spellOfferColor = CardColor.BLUE,
        )

        val restored = DummyPlayerSession.restore(
            knight = original.knight,
            wasRandom = original.wasRandom,
            deckOrder = original.deckOrder,
            discardPile = original.discardPile,
            crystals = original.crystals,
            round = original.round,
            roundEnded = original.roundEnded,
            log = original.log,
        )

        assertEquals(original, restored)
    }
}
