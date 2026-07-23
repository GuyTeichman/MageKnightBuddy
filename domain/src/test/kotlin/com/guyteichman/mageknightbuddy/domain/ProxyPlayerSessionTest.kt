package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProxyPlayerSessionTest {

    @Test
    fun `start builds a 16-card deck with Tovak's 2 unique cards replacing one Blue and one Red generic`() {
        val session = ProxyPlayerSession.start(Knight.TOVAK)

        assertEquals(16, session.deckOrder.size)
        assertEquals(1, session.deckOrder.count { it == ProxyPlayerCard.UniqueAction(CardColor.BLUE) })
        assertEquals(1, session.deckOrder.count { it == ProxyPlayerCard.UniqueAction(CardColor.RED) })
        assertEquals(3, session.deckOrder.count { it == ProxyPlayerCard.BasicAction(CardColor.BLUE) })
        assertEquals(3, session.deckOrder.count { it == ProxyPlayerCard.BasicAction(CardColor.RED) })
        assertEquals(4, session.deckOrder.count { it == ProxyPlayerCard.BasicAction(CardColor.GREEN) })
        assertEquals(4, session.deckOrder.count { it == ProxyPlayerCard.BasicAction(CardColor.WHITE) })
    }

    @Test
    fun `start gives Goldyx the same 2 Green, 1 Blue starting crystals as a standard Dummy Player`() {
        val session = ProxyPlayerSession.start(Knight.GOLDYX)

        assertEquals(DummyPlayerSession.start(Knight.GOLDYX).crystals, session.crystals)
    }

    @Test
    fun `start has no objective card, 0 rounds ended, and round 1`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        assertNull(session.objectiveCard)
        assertEquals(0, session.objectiveShields)
        assertEquals(1, session.round)
        assertEquals(false, session.roundEnded)
    }

    @Test
    fun `startRandom sets wasRandom and picks a knight whose crystals match the starting table`() {
        val session = ProxyPlayerSession.startRandom(random = Random(0))

        assertEquals(true, session.wasRandom)
        assertEquals(ProxyPlayerSession.start(session.knight).crystals, session.crystals)
    }

    @Test
    fun `movementPoints with no objective card is 0`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        assertEquals(0, session.movementPoints(hasMatchingManaDie = false))
    }

    @Test
    fun `movementPoints sums the objective card's bonus, its Shields, and a matching mana die`() {
        val session = ProxyPlayerSession.restore(
            knight = Knight.ARYTHEA,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.ARYTHEA),
            round = 1,
            roundEnded = false,
            objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.RED),
            objectiveShields = 1,
            log = emptyList(),
        )

        // +2 (unique card) + 1 (shield) + 1 (matching mana die) = 4.
        assertEquals(4, session.movementPoints(hasMatchingManaDie = true))
        // Without the matching die: +2 + 1 = 3.
        assertEquals(3, session.movementPoints(hasMatchingManaDie = false))
    }

    @Test
    fun `restore reconstructs a session with the exact same state it was given`() {
        val original = ProxyPlayerSession.start(Knight.CORAL)

        val restored = ProxyPlayerSession.restore(
            knight = original.knight,
            wasRandom = original.wasRandom,
            deckOrder = original.deckOrder,
            discardPile = original.discardPile,
            crystals = original.crystals,
            round = original.round,
            roundEnded = original.roundEnded,
            objectiveCard = original.objectiveCard,
            objectiveShields = original.objectiveShields,
            log = original.log,
        )

        assertEquals(original, restored)
    }
}
