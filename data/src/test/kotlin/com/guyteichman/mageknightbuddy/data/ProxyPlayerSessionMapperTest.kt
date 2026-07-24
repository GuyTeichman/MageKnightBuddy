package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyPlayerSessionMapperTest {

    @Test
    fun `toEntity and toDomain round-trip a freshly started session`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }

    @Test
    fun `toEntity and toDomain round-trip a session with a current Unique Basic Action objective card`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())
            .let {
                // Force a specific objective via restore, since playTurn() on an empty deck just ends the round.
                ProxyPlayerSession.restore(
                    knight = it.knight,
                    wasRandom = it.wasRandom,
                    deckOrder = listOf(ProxyPlayerCard.BasicAction(CardColor.RED)),
                    discardPile = listOf(ProxyPlayerCard.UniqueAction(CardColor.WHITE)),
                    crystals = it.crystals,
                    round = 1,
                    roundEnded = false,
                    objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                    objectiveShields = 2,
                    log = it.log,
                )
            }

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }

    @Test
    fun `toEntity and toDomain round-trip a session with no current objective card (null)`() {
        val session = ProxyPlayerSession.start(Knight.GOLDYX)

        val entity = session.toEntity(updatedAt = 0L)
        assertEquals(null, entity.objectiveCardJson)

        val restored = entity.toDomain()
        assertEquals(null, restored.objectiveCard)
        assertEquals(session, restored)
    }

    @Test
    fun `toEntity and toDomain round-trip a deck containing a Dual-Color Advanced Action card`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())
            .endRound(
                advancedActionOfferColor = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
                spellOfferColor = CardColor.WHITE,
            )

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }

    @Test
    fun `toEntity and toDomain round-trip a log containing every event type`() {
        // Arythea has 0 Green crystals, so every Green card's matchingCrystalCount is 0 and none
        // of these playTurn() calls chain extra flips - keeps the deck math simple: 5 cards lets
        // the 1st playTurn (no objective yet) consume the mandatory 3 and start an objective, the
        // 2nd playTurn (already has an objective) consume the remaining 2 and continue it, and the
        // 3rd playTurn find an empty deck and announce End of Round - producing all 6
        // ProxyPlayerEvent variants across this one session's log (RoundStarted from start(),
        // NewObjectiveDrawn/TurnContinued/EndOfRoundAnnounced from the 3 playTurn() calls,
        // ObjectiveResolved and RoundEnded from the calls below). The 1-card-deck scenario this
        // test used before could only ever reach NewObjectiveDrawn, never TurnContinued or
        // EndOfRoundAnnounced - see #146.
        val session = ProxyPlayerSession.start(
            Knight.ARYTHEA,
            deckOrder = List(5) { ProxyPlayerCard.BasicAction(CardColor.GREEN) },
        )
            .playTurn() // NewObjectiveDrawn - no objective yet, deck 5 -> 2
            .playTurn() // TurnContinued - objective already set, deck 2 -> 0
            .playTurn() // EndOfRoundAnnounced - deck empty
            .resolveObjective() // ObjectiveResolved
            .endRound(CardIdentity.SingleColor(CardColor.RED), CardColor.BLUE) // RoundEnded

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }

    @Test
    fun `toEntity then toDomain round-trips startsAtNight`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList(), startsAtNight = true)

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
        assertEquals(true, roundTripped.startsAtNight)
    }
}
