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

        // Independent ground truth, worked out by hand from docs/rules/volkares-return.md's
        // "Volkare's turn" (Wound reveal rolls the mana die) rather than just capturing whatever
        // `session` happened to hold - see issue #150. A self-referential
        // assertEquals(session, roundTripped) alone only confirms serialization preserved
        // whatever playTurn() produced, bug or not.
        //
        // The single Wound is revealed and moved to the discard pile; since it's a Wound, the
        // given manaRoll is recorded on the CardRevealed event. Volkare's Return never reshuffles,
        // so the deck stays empty and round/cityRevealed/lost are untouched by a card reveal.
        assertEquals(emptyList(), roundTripped.deckOrder)
        assertEquals(listOf(VolkareCard.Wound), roundTripped.discardPile)
        assertEquals(1, roundTripped.round)
        assertEquals(false, roundTripped.cityRevealed)
        assertEquals(false, roundTripped.lost)
        assertEquals(
            listOf(
                VolkareEvent.RoundStarted(round = 1),
                VolkareEvent.CardRevealed(
                    round = 1,
                    card = VolkareCard.Wound,
                    cityRevealed = false,
                    manaRoll = ManaColor.BLACK,
                ),
            ),
            roundTripped.log,
        )
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

        // Independent ground truth (see the first test's comment for why this matters).
        // playTurn() #1 reveals BasicAction(RED) (not a Wound, so manaRoll stays null) while
        // cityRevealed is still false; toggleCityRevealed() then flips it to true without logging
        // anything. playTurn() #2 reveals the last card, CompetitiveSpell(BLUE), now with
        // cityRevealed = true (the flag is captured at reveal time, per CardRevealed's doc
        // comment - toggling it afterward never rewrites an already-logged reveal). playTurn() #3
        // hits an empty deck: Volkare's Return logs Frenzy instead of reshuffling or losing.
        // endRound() logs RoundEnded for the completed round (1) and increments round to 2 - unlike
        // DummyPlayerSession, it has no reshuffle or crystal/offer interaction to apply (see
        // VolkareSession.kt's endRound() doc comment: "ending round is purely a player convenience
        // for tracking, not a game mechanic" for Volkare - issue #128).
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

        // Independent ground truth (see the first test's comment for why this matters). The deck
        // starts empty, so playTurn() takes the defensive empty-deck branch immediately (see
        // VolkareSession.kt's playTurn() doc comment - the real losing moment per
        // docs/rules/volkares-quest.md's "Frenzy (deck exhausted)" is normally the *last*
        // portal-moving card reveal, not a literally-empty deck, but this fallback branch covers
        // that edge case too). For Volkare's Quest (unlike Volkare's Return's Frenzy) that branch
        // sets lost = true and logs QuestLost instead. deckOrder/discardPile/round/cityRevealed are
        // all untouched since no card was actually revealed.
        assertEquals(emptyList(), roundTripped.deckOrder)
        assertEquals(emptyList(), roundTripped.discardPile)
        assertEquals(1, roundTripped.round)
        assertEquals(false, roundTripped.cityRevealed)
        assertEquals(true, roundTripped.lost)
        assertEquals(
            listOf(
                VolkareEvent.RoundStarted(round = 1),
                VolkareEvent.QuestLost(round = 1),
            ),
            roundTripped.log,
        )
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
