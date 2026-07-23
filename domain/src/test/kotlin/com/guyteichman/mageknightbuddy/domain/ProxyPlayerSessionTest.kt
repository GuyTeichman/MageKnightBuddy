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
    fun `remainingByColor counts UniqueAction and BasicAction cards under their own color`() {
        // Tovak's 16-card deck: 4 Blue (3 generic + 1 UniqueAction) and 4 Red (3 generic + 1
        // UniqueAction), 4 Green, 4 White generic - remainingByColor doesn't distinguish card kind.
        val session = ProxyPlayerSession.start(Knight.TOVAK)

        assertEquals(
            mapOf(CardColor.RED to 4, CardColor.GREEN to 4, CardColor.BLUE to 4, CardColor.WHITE to 4),
            session.remainingByColor,
        )
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

    @Test
    fun `playTurn on an empty deck announces End of Round instead of flipping`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.playTurn()

        assertEquals(true, next.roundEnded)
        assertEquals(ProxyPlayerEvent.EndOfRoundAnnounced(round = 1), next.log.last())
    }

    @Test
    fun `playTurn is a no-op once roundEnded is already true`() {
        val ended = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = ended.playTurn()

        assertEquals(ended, next)
    }

    @Test
    fun `playTurn with no objective card draws the first flipped card as the new objective`() {
        // Coral has no Green crystals, so the flip stops at the mandatory 3 (last card Green, no match).
        val session = ProxyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
            ),
        )

        val next = session.playTurn()

        assertEquals(ProxyPlayerCard.UniqueAction(CardColor.WHITE), next.objectiveCard)
        assertEquals(0, next.objectiveShields)
        assertEquals(
            listOf(ProxyPlayerCard.BasicAction(CardColor.RED), ProxyPlayerCard.BasicAction(CardColor.GREEN)),
            next.discardPile,
        )
        assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.BLUE)), next.deckOrder)
        assertEquals(
            ProxyPlayerEvent.NewObjectiveDrawn(
                round = 1,
                objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                discarded = listOf(ProxyPlayerCard.BasicAction(CardColor.RED), ProxyPlayerCard.BasicAction(CardColor.GREEN)),
            ),
            next.log.last(),
        )
    }

    @Test
    fun `playTurn with no objective card chains extra flips off the 3rd card's matching crystals`() {
        // Coral holds 2 White crystals - a White 3rd card should chain 2 additional reveals.
        val session = ProxyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.WHITE),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
            ),
        )

        val next = session.playTurn()

        assertEquals(ProxyPlayerCard.BasicAction(CardColor.RED), next.objectiveCard)
        assertEquals(
            listOf(
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.WHITE),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
            ),
            next.discardPile,
        )
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn with an existing objective card adds a Shield token and flips 3 cards to discard`() {
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = listOf(
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
            ),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
            objectiveShields = 1,
            log = emptyList(),
        )

        val next = session.playTurn()

        assertEquals(ProxyPlayerCard.UniqueAction(CardColor.WHITE), next.objectiveCard)
        assertEquals(2, next.objectiveShields)
        assertEquals(
            listOf(
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
            ),
            next.discardPile,
        )
        assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.GREEN)), next.deckOrder)
        assertEquals(
            ProxyPlayerEvent.TurnContinued(
                round = 1,
                objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                shieldsNow = 2,
                revealed = listOf(
                    ProxyPlayerCard.BasicAction(CardColor.RED),
                    ProxyPlayerCard.BasicAction(CardColor.GREEN),
                    ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ),
            ),
            next.log.last(),
        )
    }

    @Test
    fun `resolveObjective discards the objective card and clears its Shields, regardless of resolution`() {
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            objectiveCard = ProxyPlayerCard.BasicAction(CardColor.GREEN),
            objectiveShields = 2,
            log = emptyList(),
        )

        val explored = session.resolveObjective(ProxyPlayerObjectiveResolution.EXPLORED)
        val completed = session.resolveObjective(ProxyPlayerObjectiveResolution.COMPLETED)

        for (next in listOf(explored, completed)) {
            assertEquals(null, next.objectiveCard)
            assertEquals(0, next.objectiveShields)
            assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.GREEN)), next.discardPile)
        }
        assertEquals(
            ProxyPlayerEvent.ObjectiveResolved(1, ProxyPlayerCard.BasicAction(CardColor.GREEN), ProxyPlayerObjectiveResolution.EXPLORED),
            explored.log.last(),
        )
        assertEquals(
            ProxyPlayerEvent.ObjectiveResolved(1, ProxyPlayerCard.BasicAction(CardColor.GREEN), ProxyPlayerObjectiveResolution.COMPLETED),
            completed.log.last(),
        )
    }

    @Test
    fun `resolveObjective is a no-op if there's no current objective card`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        val next = session.resolveObjective(ProxyPlayerObjectiveResolution.EXPLORED)

        assertEquals(session, next)
    }

    @Test
    fun `endRound appends the Advanced Action offer card to the deck and grants a Spell-color crystal`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(listOf(ProxyPlayerCard.AdvancedAction(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE))), next.deckOrder)
        assertEquals(startingCrystals(Knight.CORAL).getValue(CardColor.WHITE) + 1, next.crystals.getValue(CardColor.WHITE))
    }

    @Test
    fun `endRound discards a lingering objective card and its Shields first`() {
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            objectiveCard = ProxyPlayerCard.BasicAction(CardColor.RED),
            objectiveShields = 3,
            log = emptyList(),
        )

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(null, next.objectiveCard)
        assertEquals(0, next.objectiveShields)
        assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.RED)), next.discardPile)
        assertEquals(
            ProxyPlayerEvent.RoundEnded(1, CardIdentity.SingleColor(CardColor.WHITE), CardColor.BLUE, ProxyPlayerCard.BasicAction(CardColor.RED)),
            next.log.last(),
        )
    }

    @Test
    fun `endRound with no lingering objective logs a null discardedObjective`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(
            ProxyPlayerEvent.RoundEnded(1, CardIdentity.SingleColor(CardColor.WHITE), CardColor.BLUE, null),
            next.log.last(),
        )
    }

    @Test
    fun `endRound increments the round and resets roundEnded`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, next.round)
        assertEquals(false, next.roundEnded)
    }
}
