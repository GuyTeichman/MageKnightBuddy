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
    fun `isDay derives from round and startsAtNight via isDayRound, defaulting startsAtNight to false`() {
        val defaultSession = ProxyPlayerSession.start(Knight.CORAL)
        assertEquals(isDayRound(round = 1, startsAtNight = false), defaultSession.isDay)

        val nightStartSession = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 2,
            roundEnded = false,
            objectiveCard = null,
            objectiveShields = 0,
            log = emptyList(),
            startsAtNight = true,
        )
        assertEquals(isDayRound(round = 2, startsAtNight = true), nightStartSession.isDay)
    }

    @Test
    fun `turnInRound is 0 right after start, before any turn is played`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        assertEquals(0, session.turnInRound)
    }

    @Test
    fun `turnInRound counts one event per playTurn call this round, whether it drew a new objective or continued one`() {
        // 3 turns' worth of cards (9), built via real playTurn() calls per CLAUDE.md's guidance.
        val session = ProxyPlayerSession.start(
            Knight.CORAL,
            deckOrder = List(9) { ProxyPlayerCard.BasicAction(CardColor.RED) },
        )

        val afterOneTurn = session.playTurn() // NewObjectiveDrawn
        val afterTwoTurns = afterOneTurn.playTurn() // TurnContinued
        val afterThreeTurns = afterTwoTurns.playTurn() // TurnContinued

        assertEquals(1, afterOneTurn.turnInRound)
        assertEquals(2, afterTwoTurns.turnInRound)
        assertEquals(3, afterThreeTurns.turnInRound)
    }

    @Test
    fun `turnInRound does not count an EndOfRoundAnnounced turn - the deck emptying isn't a played turn`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.playTurn()

        assertEquals(true, next.roundEnded)
        assertEquals(0, next.turnInRound)
    }

    @Test
    fun `turnInRound resets to 0 once endRound advances to the next round, not counting the prior round's turns`() {
        val session = ProxyPlayerSession.start(
            Knight.CORAL,
            deckOrder = List(6) { ProxyPlayerCard.BasicAction(CardColor.RED) },
        ).playTurn().playTurn()

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, session.turnInRound) // sanity: the prior round's own count is unaffected by endRound
        assertEquals(0, next.turnInRound)
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
    fun `playTurn with an existing objective card chains extra flips off the 3rd card's matching crystals`() {
        // Coral holds 2 White crystals (startingCrystals) - a White 3rd card should chain 2
        // additional reveals, exactly like the "no objective card" branch's chaining test above,
        // but here there's already an objective in progress so every revealed card (mandatory
        // + chained) goes to the discard pile instead of one becoming a new objective.
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = listOf(
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.WHITE),
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.RED),
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

        val revealed = listOf(
            ProxyPlayerCard.BasicAction(CardColor.BLUE),
            ProxyPlayerCard.BasicAction(CardColor.GREEN),
            ProxyPlayerCard.BasicAction(CardColor.WHITE),
            ProxyPlayerCard.BasicAction(CardColor.RED),
            ProxyPlayerCard.BasicAction(CardColor.RED),
        )
        assertEquals(ProxyPlayerCard.UniqueAction(CardColor.WHITE), next.objectiveCard)
        assertEquals(2, next.objectiveShields)
        assertEquals(revealed, next.discardPile)
        assertEquals(emptyList(), next.deckOrder)
        assertEquals(
            ProxyPlayerEvent.TurnContinued(
                round = 1,
                objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                shieldsNow = 2,
                revealed = revealed,
            ),
            next.log.last(),
        )
    }

    @Test
    fun `resolveObjective discards the objective card and clears its Shields`() {
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

        val next = session.resolveObjective()

        assertEquals(null, next.objectiveCard)
        assertEquals(0, next.objectiveShields)
        assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.GREEN)), next.discardPile)
        assertEquals(
            ProxyPlayerEvent.ObjectiveResolved(1, ProxyPlayerCard.BasicAction(CardColor.GREEN)),
            next.log.last(),
        )
    }

    @Test
    fun `resolveObjective is a no-op if there's no current objective card`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        val next = session.resolveObjective()

        assertEquals(session, next)
    }

    @Test
    fun `usedDualColorCards is empty for a fresh session with no Advanced Action cards added`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        assertEquals(emptySet(), session.usedDualColorCards)
    }

    @Test
    fun `usedDualColorCards contains a Dual-Color card added via endRound`() {
        val dual = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(advancedActionOfferColor = dual, spellOfferColor = CardColor.WHITE)

        assertEquals(setOf(dual), next.usedDualColorCards)
    }

    @Test
    fun `usedDualColorCards ignores single-color Advanced Action, Basic, and Unique cards - only dual-color ones are singletons`() {
        // A fresh deck already holds Basic/Unique cards; add a single-color Advanced Action on top.
        val session = ProxyPlayerSession.start(Knight.CORAL)

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.RED),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(emptySet(), next.usedDualColorCards)
    }

    @Test
    fun `usedDualColorCards counts a Dual-Color card that became the current objective card`() {
        val dual = CardIdentity.DualColor(CardColor.RED, CardColor.WHITE)
        // A controlled deck so the first playTurn (no objective yet) draws the dual-color card as
        // the new objective card - exercising the objectiveCard path, not just deck/discard.
        val session = ProxyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                ProxyPlayerCard.AdvancedAction(dual),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
            ),
        )

        val next = session.playTurn()

        // sanity: the dual card is the objective now, not in the deck or discard pile
        assertEquals(ProxyPlayerCard.AdvancedAction(dual), next.objectiveCard)
        assertEquals(setOf(dual), next.usedDualColorCards)
    }

    @Test
    fun `usedDualColorCards accumulates every distinct Dual-Color card added across multiple rounds`() {
        val first = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)
        val second = CardIdentity.DualColor(CardColor.RED, CardColor.WHITE)
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session
            .endRound(advancedActionOfferColor = first, spellOfferColor = CardColor.WHITE)
            .endRound(advancedActionOfferColor = second, spellOfferColor = CardColor.WHITE)

        assertEquals(setOf(first, second), next.usedDualColorCards)
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
        // The discarded objective joins the round-prep reshuffle just like any other discarded
        // card - see the "shuffles the discard pile back into the deck" test below - so it ends
        // up in the deck, not lingering in the discard pile.
        assertEquals(emptyList(), next.discardPile)
        assertEquals(
            setOf(
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.AdvancedAction(CardIdentity.SingleColor(CardColor.WHITE)),
            ),
            next.deckOrder.toSet(),
        )
        assertEquals(
            ProxyPlayerEvent.RoundEnded(1, CardIdentity.SingleColor(CardColor.WHITE), CardColor.BLUE, ProxyPlayerCard.BasicAction(CardColor.RED)),
            next.log.last(),
        )
    }

    @Test
    fun `endRound shuffles the discard pile back into the deck and clears it`() {
        // 1 card left in the deck, 2 already discarded from earlier turns this Round.
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = listOf(ProxyPlayerCard.BasicAction(CardColor.RED)),
            discardPile = listOf(ProxyPlayerCard.BasicAction(CardColor.BLUE), ProxyPlayerCard.BasicAction(CardColor.GREEN)),
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            objectiveCard = null,
            objectiveShields = 0,
            log = emptyList(),
        )

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(emptyList(), next.discardPile)
        assertEquals(
            setOf(
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.AdvancedAction(CardIdentity.SingleColor(CardColor.WHITE)),
            ),
            next.deckOrder.toSet(),
        )
        assertEquals(4, next.deckOrder.size)
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

    @Test
    fun `pickPlayerTactic records the player's pick for the active Day-Night pile and logs it`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        val next = session.pickPlayerTactic(card = 3)

        assertEquals(3, next.tacticState.playerPick)
        assertNull(next.tacticState.dummyPick)
        assertEquals(
            ProxyPlayerEvent.TacticPicked(round = 1, isDay = true, card = 3, pickedByPlayer = true),
            next.log.last(),
        )
    }

    @Test
    fun `pickDummyTactic records a random pick for the active Day-Night pile and logs it`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        val next = session.pickDummyTactic(random = Random(0))
        val dummyPick = next.tacticState.dummyPick

        assertEquals(true, dummyPick in 1..6)
        assertNull(next.tacticState.playerPick)
        assertEquals(
            ProxyPlayerEvent.TacticPicked(round = 1, isDay = true, card = requireNotNull(dummyPick), pickedByPlayer = false),
            next.log.last(),
        )
    }

    @Test
    fun `endRound applies solo's EveryRound BOTH removal to tacticState by default`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())
            .pickPlayerTactic(card = 2)
            .pickDummyTactic(random = Random(0))
        val dummyPick = session.tacticState.dummyPick

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(setOf(2, dummyPick), next.tacticState.removedDayCards)
        assertNull(next.tacticState.playerPick)
        assertNull(next.tacticState.dummyPick)
    }

    @Test
    fun `endRound applies a coop scenario's removal rule to tacticState, keyed by isSolo and scenario`() {
        // Realm of the Dead never removes a Tactic card (TacticRules.kt's Never row), so only the
        // picks themselves should clear - removedDayCards must stay empty.
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            objectiveCard = null,
            objectiveShields = 0,
            log = emptyList(),
            isSolo = false,
            scenario = Scenario.RealmOfTheDead,
        ).pickPlayerTactic(card = 2).pickDummyTactic(random = Random(0))

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(emptySet(), next.tacticState.removedDayCards)
        assertNull(next.tacticState.playerPick)
        assertNull(next.tacticState.dummyPick)
    }
}
