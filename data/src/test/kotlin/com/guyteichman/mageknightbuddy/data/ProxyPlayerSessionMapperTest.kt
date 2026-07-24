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
        val session = ProxyPlayerSession.start(
            Knight.ARYTHEA,
            deckOrder = listOf(ProxyPlayerCard.BasicAction(CardColor.GREEN)),
        )
            .playTurn() // NewObjectiveDrawn (or EndOfRoundAnnounced if deck too short - 1 card here yields NewObjectiveDrawn)
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
