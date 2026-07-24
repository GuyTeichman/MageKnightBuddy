package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class VolkareSessionTest {

    @Test
    fun `start builds a deck of 16 Basic Actions, 4 competitive Spells, and the default Fair Wound count for Volkares Return`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR)

        val basicActions = session.deckOrder.filterIsInstance<VolkareCard.BasicAction>()
        val spells = session.deckOrder.filterIsInstance<VolkareCard.CompetitiveSpell>()
        val wounds = session.deckOrder.filterIsInstance<VolkareCard.Wound>()

        assertEquals(16, basicActions.size)
        assertEquals(
            mapOf(
                CardColor.RED to 4,
                CardColor.GREEN to 4,
                CardColor.BLUE to 4,
                CardColor.WHITE to 4,
            ),
            CardColor.entries.associateWith { color -> basicActions.count { it.color == color } },
        )
        assertEquals(4, spells.size)
        assertEquals(
            setOf(CardColor.RED, CardColor.GREEN, CardColor.BLUE, CardColor.WHITE),
            spells.map { it.color }.toSet(),
        )
        assertEquals(18, wounds.size) // Fair Race Level, Volkare's Return - see docs/rules/volkares-return.md.
        assertEquals(16 + 4 + 18, session.deckOrder.size)
    }

    @Test
    fun `start uses the Tight and Thrilling default Wound counts for Volkares Return`() {
        assertEquals(
            15,
            VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.TIGHT)
                .deckOrder.count { it is VolkareCard.Wound },
        )
        assertEquals(
            12,
            VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.THRILLING)
                .deckOrder.count { it is VolkareCard.Wound },
        )
    }

    @Test
    fun `start uses Volkares Quest's own default Wound counts, distinct from Volkares Return`() {
        assertEquals(
            20,
            VolkareSession.start(Scenario.VolkaresQuest, RaceLevel.FAIR)
                .deckOrder.count { it is VolkareCard.Wound },
        )
        assertEquals(
            16,
            VolkareSession.start(Scenario.VolkaresQuest, RaceLevel.TIGHT)
                .deckOrder.count { it is VolkareCard.Wound },
        )
        assertEquals(
            12,
            VolkareSession.start(Scenario.VolkaresQuest, RaceLevel.THRILLING)
                .deckOrder.count { it is VolkareCard.Wound },
        )
    }

    @Test
    fun `start accepts a custom Wound count override instead of the Race Level default`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR, woundCount = 3)

        assertEquals(3, session.deckOrder.count { it is VolkareCard.Wound })
        assertEquals(16 + 4 + 3, session.deckOrder.size)
    }

    @Test
    fun `start begins on Round 1, unrevealed city, not lost, with a RoundStarted log entry`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR)

        assertEquals(1, session.round)
        assertEquals(false, session.cityRevealed)
        assertEquals(false, session.lost)
        assertEquals(listOf(VolkareEvent.RoundStarted(round = 1)), session.log)
    }

    @Test
    fun `playTurn reveals the top card of the deck onto the discard pile and logs it`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED), VolkareCard.Wound),
        )

        val next = session.playTurn()

        assertEquals(listOf(VolkareCard.Wound), next.deckOrder)
        assertEquals(listOf(VolkareCard.BasicAction(CardColor.RED)), next.discardPile)
        assertEquals(
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.RED), cityRevealed = false),
            next.log.last(),
        )
    }

    @Test
    fun `playTurn's logged CardRevealed captures cityRevealed as it was at reveal time, not live`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.Wound, VolkareCard.Wound),
        ).toggleCityRevealed() // City revealed = true before the first card is played.

        val afterFirstReveal = session.playTurn(manaRoll = ManaColor.RED)
        val afterToggleBack = afterFirstReveal.toggleCityRevealed() // Flip back to false...
        val afterSecondReveal = afterToggleBack.playTurn(manaRoll = ManaColor.WHITE)

        // ...the first reveal's logged event must still say true, unaffected by the later toggle.
        assertEquals(
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.Wound, cityRevealed = true, manaRoll = ManaColor.RED),
            afterFirstReveal.log.last(),
        )
        assertEquals(
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.Wound, cityRevealed = false, manaRoll = ManaColor.WHITE),
            afterSecondReveal.log.last(),
        )
    }

    @Test
    fun `playTurn on a Wound rolls the given mana die color and logs it`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 1,
            deckOrder = listOf(VolkareCard.Wound),
        )

        val next = session.playTurn(manaRoll = ManaColor.GOLD)

        assertEquals(
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.Wound, cityRevealed = false, manaRoll = ManaColor.GOLD),
            next.log.last(),
        )
    }

    @Test
    fun `playTurn on a non-Wound card never logs a mana roll, even if one is passed`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED)),
        )

        val next = session.playTurn(manaRoll = ManaColor.BLACK)

        assertEquals(
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.RED), cityRevealed = false, manaRoll = null),
            next.log.last(),
        )
    }

    @Test
    fun `playTurn on an empty deck in Volkares Return logs Frenzy and stays playable`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR, woundCount = 0, deckOrder = emptyList())

        val next = session.playTurn()

        assertEquals(VolkareEvent.Frenzy(round = 1), next.log.last())
        assertEquals(emptyList(), next.deckOrder)
        assertEquals(false, next.lost)
    }

    @Test
    fun `Frenzy never blocks further turns - every subsequent playTurn keeps logging Frenzy`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR, woundCount = 0, deckOrder = emptyList())

        val afterMany = session.playTurn().playTurn().playTurn()

        assertEquals(
            listOf(VolkareEvent.Frenzy(round = 1), VolkareEvent.Frenzy(round = 1), VolkareEvent.Frenzy(round = 1)),
            afterMany.log.drop(1), // Drop the initial RoundStarted entry.
        )
        assertEquals(emptyList(), afterMany.deckOrder)
    }

    @Test
    fun `playTurn on an empty deck in Volkares Quest sets lost and logs QuestLost`() {
        val session = VolkareSession.start(Scenario.VolkaresQuest, RaceLevel.FAIR, woundCount = 0, deckOrder = emptyList())

        val next = session.playTurn()

        assertEquals(true, next.lost)
        assertEquals(VolkareEvent.QuestLost(round = 1), next.log.last())
    }

    @Test
    fun `playTurn in Volkares Quest sets lost the moment the last green,blue,white card is drawn, even with Wounds still undrawn`() {
        val session = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.GREEN), VolkareCard.Wound, VolkareCard.Wound),
        )

        val next = session.playTurn(manaRoll = ManaColor.RED)

        assertEquals(true, next.lost)
        assertEquals(
            listOf(
                VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.GREEN), cityRevealed = false),
                VolkareEvent.QuestLost(round = 1),
            ),
            next.log.drop(1), // Drop the initial RoundStarted entry.
        )
        // The two trailing Wounds are never drawn - the game already ended on the card before them.
        assertEquals(listOf(VolkareCard.Wound, VolkareCard.Wound), next.deckOrder)
        assertEquals(listOf(VolkareCard.BasicAction(CardColor.GREEN)), next.discardPile)
    }

    @Test
    fun `playTurn in Volkares Quest does not set lost while another green,blue,white card still remains`() {
        val session = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.GREEN), VolkareCard.BasicAction(CardColor.BLUE)),
        )

        val next = session.playTurn()

        assertEquals(false, next.lost)
        assertEquals(
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.GREEN), cityRevealed = false),
            next.log.last(),
        )
    }

    @Test
    fun `playTurn in Volkares Quest does not set lost when revealing a Wound, even if it's the deck's last card`() {
        val session = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.FAIR,
            woundCount = 1,
            deckOrder = listOf(VolkareCard.Wound),
        )

        val next = session.playTurn(manaRoll = ManaColor.WHITE)

        assertEquals(false, next.lost)
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn in Volkares Quest does not set lost on a red Basic Action, even with nothing but Wounds behind it`() {
        // Red never moves Volkare in Quest (docs/rules/volkares-quest.md's "Course of the Game":
        // "there is no later phase where red cards start moving him - this rule holds for the
        // entire scenario"), so it never counts as his "last move toward the portal".
        val session = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.FAIR,
            woundCount = 1,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED), VolkareCard.Wound),
        )

        val next = session.playTurn()

        assertEquals(false, next.lost)
        assertEquals(
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.RED), cityRevealed = false),
            next.log.last(),
        )
    }

    @Test
    fun `playTurn in Volkares Quest does not set lost on a red Competitive Spell either`() {
        val session = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.CompetitiveSpell(CardColor.RED)),
        )

        val next = session.playTurn()

        assertEquals(false, next.lost)
    }

    @Test
    fun `playTurn in Volkares Quest ignores a trailing red card - the green,blue,white card before it still triggers the loss`() {
        val session = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.GREEN), VolkareCard.BasicAction(CardColor.RED)),
        )

        val next = session.playTurn()

        assertEquals(true, next.lost)
        assertEquals(VolkareEvent.QuestLost(round = 1), next.log.last())
        // The trailing red card is never drawn - the game already ended on the card before it.
        assertEquals(listOf(VolkareCard.BasicAction(CardColor.RED)), next.deckOrder)
    }

    @Test
    fun `playTurn in Volkares Quest does not lose on a red card when a green,blue,white card still follows it`() {
        val session = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED), VolkareCard.BasicAction(CardColor.BLUE)),
        )

        val next = session.playTurn()

        assertEquals(false, next.lost)
        assertEquals(listOf(VolkareCard.BasicAction(CardColor.BLUE)), next.deckOrder)
    }

    @Test
    fun `playTurn in Volkares Quest with only red cards and Wounds left falls back to the empty-deck loss once truly exhausted`() {
        val afterRed = VolkareSession.start(
            Scenario.VolkaresQuest,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED)),
        ).playTurn()
        assertEquals(false, afterRed.lost) // The lone red card alone never loses it...

        val afterEmpty = afterRed.playTurn()

        assertEquals(true, afterEmpty.lost) // ...but the truly-empty deck fallback still catches it.
        assertEquals(VolkareEvent.QuestLost(round = 1), afterEmpty.log.last())
    }

    @Test
    fun `playTurn in Volkares Return never sets lost from the last-non-Wound-card rule - only Frenzy on a truly empty deck applies`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.GREEN), VolkareCard.Wound),
        )

        val next = session.playTurn()

        assertEquals(false, next.lost)
        assertEquals(
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.GREEN), cityRevealed = false),
            next.log.last(),
        )
    }

    @Test
    fun `playTurn is a no-op once lost is already true`() {
        val lost = VolkareSession.start(Scenario.VolkaresQuest, RaceLevel.FAIR, woundCount = 0, deckOrder = emptyList()).playTurn()

        val next = lost.playTurn()

        assertEquals(lost.lost, next.lost)
        assertEquals(lost.log, next.log)
        assertEquals(lost.deckOrder, next.deckOrder)
    }

    @Test
    fun `turnInRound is 0 right after start, before any turn is played`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR, woundCount = 0, deckOrder = emptyList())

        assertEquals(0, session.turnInRound)
    }

    @Test
    fun `turnInRound counts one CardRevealed per playTurn call this round`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = List(3) { VolkareCard.BasicAction(CardColor.RED) },
        )

        val afterOneTurn = session.playTurn()
        val afterTwoTurns = afterOneTurn.playTurn()
        val afterThreeTurns = afterTwoTurns.playTurn()

        assertEquals(1, afterOneTurn.turnInRound)
        assertEquals(2, afterTwoTurns.turnInRound)
        assertEquals(3, afterThreeTurns.turnInRound)
    }

    @Test
    fun `turnInRound keeps counting Frenzy turns once the deck empties in Volkares Return`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR, woundCount = 0, deckOrder = emptyList())

        val afterFrenzies = session.playTurn().playTurn()

        assertEquals(2, afterFrenzies.turnInRound)
    }

    @Test
    fun `turnInRound resets to 0 once endRound advances to the next round, not counting the prior round's turns`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = List(2) { VolkareCard.BasicAction(CardColor.RED) },
        ).playTurn().playTurn()

        val next = session.endRound()

        assertEquals(2, session.turnInRound) // sanity: the prior round's own count is unaffected by endRound
        assertEquals(0, next.turnInRound)
    }

    @Test
    fun `endRound only increments round and logs RoundEnded - deck and discard are untouched`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED)),
        ).playTurn()

        val next = session.endRound()

        assertEquals(2, next.round)
        assertEquals(session.deckOrder, next.deckOrder)
        assertEquals(session.discardPile, next.discardPile)
        assertEquals(VolkareEvent.RoundEnded(round = 1), next.log.last())
    }

    @Test
    fun `endRound is always callable, even mid-round with cards still in the deck`() {
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED)),
        )

        val next = session.endRound()

        assertEquals(2, next.round)
        assertEquals(session.deckOrder, next.deckOrder)
    }

    @Test
    fun `toggleCityRevealed flips the boolean`() {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR)

        val toggledOn = session.toggleCityRevealed()
        val toggledOff = toggledOn.toggleCityRevealed()

        assertEquals(false, session.cityRevealed)
        assertEquals(true, toggledOn.cityRevealed)
        assertEquals(false, toggledOff.cityRevealed)
    }

    @Test
    fun `restore reconstructs a session with the exact same state it was given`() {
        val original = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = listOf(VolkareCard.BasicAction(CardColor.RED), VolkareCard.Wound),
        ).playTurn().toggleCityRevealed().endRound()

        val restored = VolkareSession.restore(
            scenario = original.scenario,
            raceLevel = original.raceLevel,
            deckOrder = original.deckOrder,
            discardPile = original.discardPile,
            round = original.round,
            cityRevealed = original.cityRevealed,
            lost = original.lost,
            log = original.log,
        )

        assertEquals(original, restored)
    }

    @Test
    fun `isDay derives from round and startsAtNight via isDayRound, defaulting startsAtNight to false`() {
        val defaultSession = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR, woundCount = 0, deckOrder = emptyList())
        assertEquals(isDayRound(round = 1, startsAtNight = false), defaultSession.isDay)

        val nightStartSession = VolkareSession.restore(
            scenario = Scenario.VolkaresReturn,
            raceLevel = RaceLevel.FAIR,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            round = 2,
            cityRevealed = false,
            lost = false,
            log = emptyList(),
            startsAtNight = true,
        )
        assertEquals(isDayRound(round = 2, startsAtNight = true), nightStartSession.isDay)
    }
}
