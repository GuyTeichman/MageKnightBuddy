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
        // Coral's starting crystals are White, White, Red (no Green) - see docs/rules/dummy-player.md's example.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.WHITE, CardColor.RED, CardColor.GREEN, CardColor.BLUE, CardColor.WHITE)
                .map { CardIdentity.SingleColor(it) },
        )

        val next = session.playTurn()

        assertEquals(
            listOf(CardColor.WHITE, CardColor.RED, CardColor.GREEN).map { CardIdentity.SingleColor(it) },
            next.discardPile,
        )
        assertEquals(listOf(CardColor.BLUE, CardColor.WHITE).map { CardIdentity.SingleColor(it) }, next.deckOrder)
    }

    @Test
    fun `playTurn chains one additional reveal per matching crystal of the 3rd card's color`() {
        // Coral holds 2 White crystals - the 3rd card (White) should chain 2 additional reveals.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                CardColor.WHITE, CardColor.RED, CardColor.WHITE,
                CardColor.GREEN, CardColor.WHITE, CardColor.RED,
            ).map { CardIdentity.SingleColor(it) },
        )

        val next = session.playTurn()

        assertEquals(
            listOf(CardColor.WHITE, CardColor.RED, CardColor.WHITE, CardColor.GREEN, CardColor.WHITE)
                .map { CardIdentity.SingleColor(it) },
            next.discardPile,
        )
        assertEquals(listOf(CardIdentity.SingleColor(CardColor.RED)), next.deckOrder)
    }

    @Test
    fun `playTurn's crystal-chain match counts the higher of a Dual-Color card's two colors' crystals`() {
        // Coral holds 2 White, 1 Red crystals, 0 Green/Blue. A Green+Blue dual-color 3rd card
        // matches neither color directly, but Power of Crystals-style cards would; here we use a
        // dual-color card matching Coral's owned colors (White+Red) to assert the chain triggers
        // for the HIGHER of the two matched colors' crystal counts, not their sum
        // (max(2 White, 1 Red) = 2 additional reveals, not 2+1=3).
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                CardIdentity.SingleColor(CardColor.GREEN),
                CardIdentity.SingleColor(CardColor.BLUE),
                CardIdentity.DualColor(CardColor.WHITE, CardColor.RED),
                CardIdentity.SingleColor(CardColor.GREEN),
                CardIdentity.SingleColor(CardColor.BLUE),
                CardIdentity.SingleColor(CardColor.GREEN),
            ),
        )

        val next = session.playTurn()

        assertEquals(
            listOf(
                CardIdentity.SingleColor(CardColor.GREEN),
                CardIdentity.SingleColor(CardColor.BLUE),
                CardIdentity.DualColor(CardColor.WHITE, CardColor.RED),
                CardIdentity.SingleColor(CardColor.GREEN),
                CardIdentity.SingleColor(CardColor.BLUE),
            ),
            next.discardPile,
        )
        assertEquals(listOf(CardIdentity.SingleColor(CardColor.GREEN)), next.deckOrder)
    }

    @Test
    fun `playTurn on a near-empty deck flips only what's available, same logic as a full flip`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.BLUE, CardColor.WHITE).map { CardIdentity.SingleColor(it) },
        )

        val next = session.playTurn()

        assertEquals(listOf(CardColor.BLUE, CardColor.WHITE).map { CardIdentity.SingleColor(it) }, next.discardPile)
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn on a near-empty deck still chains if there's nothing left to chain into`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.GREEN, CardColor.WHITE).map { CardIdentity.SingleColor(it) },
        )

        val next = session.playTurn()

        assertEquals(listOf(CardColor.GREEN, CardColor.WHITE).map { CardIdentity.SingleColor(it) }, next.discardPile)
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
            deckOrder = listOf(CardColor.RED, CardColor.GREEN).map { CardIdentity.SingleColor(it) },
        )

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(
            mapOf(CardColor.RED to 1, CardColor.GREEN to 1, CardColor.BLUE to 0, CardColor.WHITE to 1),
            next.remainingByColor,
        )
    }

    @Test
    fun `endRound shuffles the discard pile back into the deck and clears it`() {
        // 2 cards left in the deck, 3 already discarded from earlier turns this Round.
        val session = DummyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = listOf(CardColor.RED, CardColor.GREEN).map { CardIdentity.SingleColor(it) },
            discardPile = listOf(CardColor.BLUE, CardColor.BLUE, CardColor.WHITE).map { CardIdentity.SingleColor(it) },
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            log = emptyList(),
        )

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(emptyList(), next.discardPile)
        assertEquals(
            mapOf(CardColor.RED to 1, CardColor.GREEN to 1, CardColor.BLUE to 2, CardColor.WHITE to 2),
            next.remainingByColor,
        )
    }

    @Test
    fun `endRound can append a Dual-Color Advanced Action card, counted toward both colors' remainingByColor`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(1, next.remainingByColor.getValue(CardColor.GREEN))
        assertEquals(1, next.remainingByColor.getValue(CardColor.BLUE))
        assertEquals(listOf(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)), next.deckOrder)
    }

    @Test
    fun `endRound grants +1 crystal of the Spell offer color, uncapped`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(3, next.crystals.getValue(CardColor.WHITE))
    }

    @Test
    fun `endRound increments the round, resets roundEnded, and logs the round-ended event`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, next.round)
        assertEquals(false, next.roundEnded)
        assertEquals(
            DummyPlayerEvent.RoundEnded(
                round = 1,
                advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
                spellOfferColor = CardColor.BLUE,
            ),
            next.log.last(),
        )
    }

    @Test
    fun `endRound is always callable, even mid-round with cards still in the deck`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardIdentity.SingleColor(CardColor.RED)),
        )

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, next.round)
    }

    @Test
    fun `endRound folds a discard pile built via playTurn back into the reshuffled deck, and empties it`() {
        // Regression test for issue #148: build the discard pile the real way, via playTurn() calls,
        // instead of a convenient start(deckOrder = ...) shortcut - that shortcut is exactly what let
        // the original bug (discard pile silently dropped at reshuffle) slip through undetected.
        // Coral's starting crystals are White x2, Red x1, Green/Blue x0 (see docs/rules/dummy-player.md).
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                CardColor.RED, CardColor.GREEN, CardColor.BLUE,
                CardColor.WHITE, CardColor.RED, CardColor.GREEN,
            ).map { CardIdentity.SingleColor(it) },
        )
            // Turn 1: flips Red, Green, Blue. 3rd card is Blue - Coral has 0 Blue crystals, no chain.
            // discardPile becomes [Red, Green, Blue]; deckOrder becomes [White, Red, Green].
            .playTurn()
            // Turn 2: flips White, Red, Green. 3rd card is Green - Coral has 0 Green crystals, no chain.
            // discardPile becomes [Red, Green, Blue, White, Red, Green]; deckOrder becomes empty.
            .playTurn()

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        // The discard pile must be empty right after reshuffling - its cards move into deckOrder.
        assertEquals(emptyList(), next.discardPile)
        // The reshuffled deck's color counts must include the discard pile's 6 cards (Red x2, Green x2,
        // Blue x1, White x1) plus the newly-added Advanced Action offer card (Blue), not just the offer
        // card alone - that's the exact count the original bug (deckOrder + offer only) would get wrong.
        assertEquals(
            mapOf(CardColor.RED to 2, CardColor.GREEN to 2, CardColor.BLUE to 2, CardColor.WHITE to 1),
            next.remainingByColor,
        )
        assertEquals(7, next.deckOrder.size)
        // The rest of the fields endRound()'s doc comment says it changes, asserted alongside discardPile
        // rather than trusting a differently-scoped test to cover them.
        assertEquals(3, next.crystals.getValue(CardColor.WHITE)) // Coral's starting 2 White + 1 from the Spell offer.
        assertEquals(2, next.round)
        assertEquals(false, next.roundEnded)
        assertEquals(
            DummyPlayerEvent.RoundEnded(
                round = 1,
                advancedActionOfferColor = CardIdentity.SingleColor(CardColor.BLUE),
                spellOfferColor = CardColor.WHITE,
            ),
            next.log.last(),
        )
    }

    @Test
    fun `restore reconstructs a session with the exact same state it was given`() {
        val original = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.RED, CardColor.GREEN).map { CardIdentity.SingleColor(it) },
        ).playTurn().endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
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

    @Test
    fun `turnInRound is 0 right after start, before any turn is played`() {
        val session = DummyPlayerSession.start(Knight.CORAL)

        assertEquals(0, session.turnInRound)
    }

    @Test
    fun `turnInRound counts one TurnPlayed event per playTurn call this round`() {
        // 3 turns' worth of cards (9), built via real playTurn() calls rather than a hand-picked
        // count, per CLAUDE.md's guidance on exercising a method's own prior methods.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = List(9) { CardIdentity.SingleColor(CardColor.RED) },
        )

        val afterOneTurn = session.playTurn()
        val afterTwoTurns = afterOneTurn.playTurn()
        val afterThreeTurns = afterTwoTurns.playTurn()

        assertEquals(1, afterOneTurn.turnInRound)
        assertEquals(2, afterTwoTurns.turnInRound)
        assertEquals(3, afterThreeTurns.turnInRound)
    }

    @Test
    fun `turnInRound does not count an EndOfRoundAnnounced turn - the deck emptying isn't a played turn`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.playTurn()

        assertEquals(true, next.roundEnded)
        assertEquals(0, next.turnInRound)
    }

    @Test
    fun `turnInRound resets to 0 once endRound advances to the next round, not counting the prior round's turns`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = List(6) { CardIdentity.SingleColor(CardColor.RED) },
        ).playTurn().playTurn()

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, session.turnInRound) // sanity: the prior round's own count is unaffected by endRound
        assertEquals(0, next.turnInRound)
    }

    @Test
    fun `isDay derives from round and startsAtNight via isDayRound, defaulting startsAtNight to false`() {
        val defaultSession = DummyPlayerSession.start(Knight.CORAL)
        assertEquals(isDayRound(round = 1, startsAtNight = false), defaultSession.isDay)

        val nightStartSession = DummyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 2,
            roundEnded = false,
            log = emptyList(),
            startsAtNight = true,
        )
        assertEquals(isDayRound(round = 2, startsAtNight = true), nightStartSession.isDay)
    }
}
