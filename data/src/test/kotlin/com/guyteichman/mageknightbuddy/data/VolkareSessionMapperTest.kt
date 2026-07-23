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

        // Independently-derived expected values (see issue #150), not just self-equality against
        // `session`: playTurn() reveals the sole Wound card, moving it from deckOrder to
        // discardPile and logging a CardRevealed with that mana roll attached.
        assertEquals(emptyList(), roundTripped.deckOrder)
        assertEquals(listOf(VolkareCard.Wound), roundTripped.discardPile)
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

        // Independently-derived expected values (see issue #150), hand-traced through each chained
        // call rather than copied from `session`:
        // - playTurn() #1 reveals BasicAction(Red) (cityRevealed still false at that moment) ->
        //   deckOrder=[CompetitiveSpell(Blue)], discardPile=[BasicAction(Red)].
        // - toggleCityRevealed() flips cityRevealed true; doesn't touch deck/discard/log.
        // - playTurn() #2 reveals CompetitiveSpell(Blue), now with cityRevealed=true baked into the
        //   logged event -> deckOrder=[], discardPile=[BasicAction(Red), CompetitiveSpell(Blue)].
        // - playTurn() #3 finds an empty deck; Volkare's Return logs Frenzy instead of reshuffling -
        //   deck/discard untouched.
        // - endRound() increments round and logs RoundEnded.
        assertEquals(emptyList(), roundTripped.deckOrder)
        assertEquals(
            listOf(VolkareCard.BasicAction(CardColor.RED), VolkareCard.CompetitiveSpell(CardColor.BLUE)),
            roundTripped.discardPile,
        )
        assertEquals(2, roundTripped.round)
        assertEquals(true, roundTripped.cityRevealed)
        assertEquals(false, roundTripped.lost)
        assertEquals(
            listOf(
                VolkareEvent.RoundStarted(round = 1),
                VolkareEvent.CardRevealed(
                    round = 1,
                    card = VolkareCard.BasicAction(CardColor.RED),
                    cityRevealed = false,
                    manaRoll = null,
                ),
                VolkareEvent.CardRevealed(
                    round = 1,
                    card = VolkareCard.CompetitiveSpell(CardColor.BLUE),
                    cityRevealed = true,
                    manaRoll = null,
                ),
                VolkareEvent.Frenzy(round = 1),
                VolkareEvent.RoundEnded(round = 1),
            ),
            roundTripped.log,
        )
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
