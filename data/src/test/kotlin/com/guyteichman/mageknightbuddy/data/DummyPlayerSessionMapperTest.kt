package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlin.test.Test
import kotlin.test.assertEquals

class DummyPlayerSessionMapperTest {

    @Test
    fun `toEntity then toDomain round-trips a session covering all four event log types`() {
        // RoundStarted (from start), TurnPlayed (first playTurn), EndOfRoundAnnounced (second
        // playTurn on an empty deck), RoundEnded (endRound) - exercises every DummyPlayerEvent case.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.RED, CardColor.GREEN),
        ).playTurn().playTurn().endRound(
            advancedActionOfferColor = CardColor.WHITE,
            spellOfferColor = CardColor.BLUE,
        )

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
    }
}
