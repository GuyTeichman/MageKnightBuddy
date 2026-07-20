package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ForTheCouncilScoringTest {

    @Test
    fun `score adds the Reputation modifier to Quest Points and outcome is Won at Reputation +2 or higher`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 5,
            reputationTrackSpace = ReputationTrackSpace.PLUS_3,
        )

        assertEquals(8, ForTheCouncilScoring.score(input))
        assertEquals(Outcome.WON, ForTheCouncilScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost below Reputation plus 2, even when the score itself is positive`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 5,
            reputationTrackSpace = ReputationTrackSpace.PLUS_1,
        )

        assertEquals(6, ForTheCouncilScoring.score(input))
        assertEquals(Outcome.LOST, ForTheCouncilScoring.outcome(input))
    }

    @Test
    fun `a Shield token on the Reputation track's X space costs 10 Quest Points and always loses`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 3,
            reputationTrackSpace = ReputationTrackSpace.NEGATIVE_X,
        )

        assertEquals(-7, ForTheCouncilScoring.score(input))
        assertEquals(Outcome.LOST, ForTheCouncilScoring.outcome(input))
    }
}
