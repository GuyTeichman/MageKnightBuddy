package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ForTheCouncilScoringTest {

    @Test
    fun `score adds the Reputation modifier to Quest Points and outcome is Won at Reputation +2 or higher`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 5,
            reputationModifier = 2,
            shieldOnXSpace = false,
            reputation = 3,
        )

        assertEquals(7, ForTheCouncilScoring.score(input))
        assertEquals(Outcome.WON, ForTheCouncilScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost below Reputation plus 2, even when the score itself is positive`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 5,
            reputationModifier = 2,
            shieldOnXSpace = false,
            reputation = 1,
        )

        assertEquals(7, ForTheCouncilScoring.score(input))
        assertEquals(Outcome.LOST, ForTheCouncilScoring.outcome(input))
    }

    @Test
    fun `a Shield token on the Reputation track's X space costs 10 Quest Points regardless of the modifier`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 3,
            reputationModifier = 5,
            shieldOnXSpace = true,
            reputation = -1,
        )

        assertEquals(-7, ForTheCouncilScoring.score(input))
        assertEquals(Outcome.LOST, ForTheCouncilScoring.outcome(input))
    }
}
