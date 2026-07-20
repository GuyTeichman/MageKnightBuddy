package com.guyteichman.mageknightbuddy.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoringSessionTest {

    @Test
    fun `create computes score and outcome from the input via SoloConquestScoring`() {
        val input = SoloConquestScoringInput(
            fame = 50,
            standardAchievements = StandardAchievements(
                spellsInDeck = 0,
                advancedActionsInDeck = 0,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            citiesConquered = 2,
            roundsFinishedEarly = 0,
            cardsRemainingInDummyDeck = 0,
            endOfRoundAnnounced = true,
            questPoints = 0,
        )
        val playedAt = Instant.parse("2026-07-18T12:00:00Z")

        val session = ScoringSession.create(
            scenario = Scenario.SoloConquest,
            knight = Knight.TOVAK,
            playerName = "Guy",
            input = input,
            playedAt = playedAt,
        )

        // 50 fame + 2*10 cities + 15 all-cities bonus = 85
        assertEquals(85, session.score)
        assertEquals(Outcome.WON, session.outcome)
        assertEquals(Scenario.SoloConquest, session.scenario)
        assertEquals(Knight.TOVAK, session.knight)
        assertEquals("Guy", session.playerName)
        assertEquals(playedAt, session.playedAt)
        assertEquals(input, session.input)
    }

    @Test
    fun `create dispatches to the matching scenario's Scoring object regardless of which input type is supplied`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 12,
            reputationTrackSpace = ReputationTrackSpace.PLUS_3,
        )
        val playedAt = Instant.parse("2026-07-18T12:00:00Z")

        val session = ScoringSession.create(
            scenario = Scenario.ForTheCouncil,
            knight = Knight.NOROWAS,
            playerName = null,
            input = input,
            playedAt = playedAt,
        )

        // 12 quest points + 2 reputation modifier (Reputation +3 prints a +2 modifier) = 14
        assertEquals(14, session.score)
        assertEquals(Outcome.WON, session.outcome)
        assertEquals(Scenario.ForTheCouncil, session.scenario)
        assertEquals(input, session.input)
    }
}
