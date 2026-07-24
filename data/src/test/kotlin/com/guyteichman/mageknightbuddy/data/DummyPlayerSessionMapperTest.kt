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
