package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.ManaColor
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareCard
import com.guyteichman.mageknightbuddy.domain.VolkareEvent
import com.guyteichman.mageknightbuddy.domain.VolkareSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VolkareSessionMapperTest {

    @Test
    fun `toEntity then toDomain round-trips a Wound reveal's mana roll`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 1,
            deckOrder = listOf(VolkareCard.Wound),
        ).playTurn(manaRoll = ManaColor.BLACK)

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
        assertEquals(ManaColor.BLACK, (roundTripped.log.last() as VolkareEvent.CardRevealed).manaRoll)
    }

    @Test
    fun `toEntity then toDomain round-trips a Volkares Return session covering every event log type`() {
        // RoundStarted (from start), CardRevealed (playTurn with a card), Frenzy (playTurn on an
        // empty deck, Volkare's Return only), RoundEnded (endRound) - exercises every VolkareEvent
        // case except QuestLost, which only Volkare's Quest can produce (see the next test).
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED), VolkareCard.CompetitiveSpell(CardColor.BLUE)),
        ).playTurn().toggleCityRevealed().playTurn().playTurn().endRound()

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
    }

    @Test
    fun `toEntity then toDomain round-trips a Volkares Quest session, including QuestLost`() {
        val session = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.THRILLING,
            woundCount = 0,
            deckOrder = emptyList(),
        ).playTurn()

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
    }

    @Test
    fun `toEntity then toDomain round-trips startsAtNight`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR, woundCount = 0, deckOrder = emptyList(), startsAtNight = true)

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
        assertEquals(true, roundTripped.startsAtNight)
    }

    @Test
    fun `toEntity stamps the given updatedAt onto the entity`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR)

        val entity = session.toEntity(updatedAt = 99L)

        assertEquals(99L, entity.updatedAt)
    }

    @Test
    fun `toEntity defaults updatedAt to roughly the current time when not given explicitly`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR)
        val before = System.currentTimeMillis()

        val entity = session.toEntity()

        val after = System.currentTimeMillis()
        assertTrue(entity.updatedAt in before..after)
    }
}
