package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.VolkareCard
import com.guyteichman.mageknightbuddy.domain.VolkareEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the per-round turn numbering the AI-tab logs use (issue #270): each mode's turn-numbering
 * function ([dummyTurnNumbers]/[proxyTurnNumbers]/[volkareTurnNumbers])
 * tags every "turn-start" entry (the one where the AI draws from its deck) with its 1-based turn
 * index *within its own round*, and leaves every other entry null. The expected lists here are
 * hand-derived from the constructed logs, not read back off the implementation, so they can actually
 * disagree with a wrong count. The set of events that count as a turn-start deliberately mirrors each
 * session's own `turnInRound` predicate, so a log's turn numbers always match the header chip.
 */
class LogTurnNumbersTest {

    private fun single(color: CardColor) = CardIdentity.SingleColor(color)

    private fun dummyTurn(round: Int) = DummyPlayerEvent.TurnPlayed(
        round = round,
        initialReveal = listOf(single(CardColor.RED), single(CardColor.BLUE), single(CardColor.GREEN)),
        additionalReveal = emptyList(),
    )

    @Test
    fun `Dummy numbers only TurnPlayed entries, restarting the count each round`() {
        val log = listOf(
            DummyPlayerEvent.RoundStarted(round = 1),
            DummyPlayerEvent.TacticPicked(round = 1, isDay = true, card = 1, pickedByPlayer = true),
            dummyTurn(1),
            dummyTurn(1),
            DummyPlayerEvent.EndOfRoundAnnounced(round = 1),
            DummyPlayerEvent.RoundEnded(round = 1, advancedActionOfferColor = single(CardColor.RED), spellOfferColor = CardColor.BLUE),
            dummyTurn(2),
            dummyTurn(2),
        )

        // Only the 4 TurnPlayed entries are numbered; the count resets from 1 when round 2 begins.
        assertEquals(listOf(null, null, 1, 2, null, null, 1, 2), dummyTurnNumbers(log))
    }

    @Test
    fun `Proxy numbers both draw branches but not the mid-turn objective resolution`() {
        val red = ProxyPlayerCard.BasicAction(CardColor.RED)
        val log = listOf(
            ProxyPlayerEvent.RoundStarted(round = 1),
            // A turn where a fresh objective is drawn...
            ProxyPlayerEvent.NewObjectiveDrawn(round = 1, objectiveCard = red, discarded = emptyList()),
            // ...then the next turn continues that same objective - a second draw, so turn 2.
            ProxyPlayerEvent.TurnContinued(round = 1, objectiveCard = red, shieldsNow = 2, revealed = emptyList()),
            // Resolving the objective is part of the current turn, not a new draw - stays unnumbered.
            ProxyPlayerEvent.ObjectiveResolved(round = 1, objectiveCard = red),
            ProxyPlayerEvent.EndOfRoundAnnounced(round = 1),
            ProxyPlayerEvent.RoundEnded(
                round = 1,
                advancedActionOfferColor = single(CardColor.RED),
                spellOfferColor = CardColor.BLUE,
                discardedObjective = null,
            ),
            ProxyPlayerEvent.NewObjectiveDrawn(round = 2, objectiveCard = red, discarded = emptyList()),
        )

        // NewObjectiveDrawn and TurnContinued each advance the turn (both are draws, per
        // ProxyPlayerSession.turnInRound); ObjectiveResolved and the round boundaries do not.
        assertEquals(listOf(null, 1, 2, null, null, null, 1), proxyTurnNumbers(log))
    }

    @Test
    fun `Volkare counts a Frenzy turn like a reveal, and resets the count each round`() {
        val log = listOf(
            VolkareEvent.RoundStarted(round = 1),
            VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.GREEN), cityRevealed = false),
            // Volkare's Return keeps taking turns on an empty deck (Frenzy) - each is a played turn.
            VolkareEvent.Frenzy(round = 1),
            VolkareEvent.RoundEnded(round = 1),
            VolkareEvent.CardRevealed(round = 2, card = VolkareCard.BasicAction(CardColor.BLUE), cityRevealed = false),
        )

        // CardRevealed and Frenzy both advance the turn (per VolkareSession.turnInRound); round 2 restarts at 1.
        assertEquals(listOf(null, 1, 2, null, 1), volkareTurnNumbers(log))
    }

    @Test
    fun `Volkare does not give QuestLost its own turn - it shares the paired reveal's turn`() {
        val log = listOf(
            VolkareEvent.RoundStarted(round = 3),
            VolkareEvent.CardRevealed(round = 3, card = VolkareCard.BasicAction(CardColor.GREEN), cityRevealed = false),
            // This reveal is the last card that can still move Volkare toward the portal (turn 2)...
            VolkareEvent.CardRevealed(round = 3, card = VolkareCard.BasicAction(CardColor.WHITE), cityRevealed = false),
            // ...and QuestLost is logged alongside it as a marker for that same turn, so it isn't counted again.
            VolkareEvent.QuestLost(round = 3),
        )

        assertEquals(listOf(null, 1, 2, null), volkareTurnNumbers(log))
    }
}
