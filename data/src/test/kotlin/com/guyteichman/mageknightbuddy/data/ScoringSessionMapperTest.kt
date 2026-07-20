package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.ForTheCouncilScoringInput
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoringSessionMapperTest {

    @Test
    fun `toEntity then toDomain round-trips every field for a SoloConquestScoringInput session`() {
        val session = ScoringSession(
            scenario = Scenario.SoloConquest,
            knight = Knight.ARYTHEA,
            playerName = "Guy",
            input = SoloConquestScoringInput(
                fame = 72,
                standardAchievements = StandardAchievements(
                    spellsInDeck = 3,
                    advancedActionsInDeck = 2,
                    units = listOf(
                        UnitTally(level = 1, healthyCount = 1, woundedCount = 2),
                        UnitTally(level = 2, healthyCount = 3, woundedCount = 0),
                        UnitTally(level = 3, healthyCount = 0, woundedCount = 1),
                        UnitTally(level = 4, healthyCount = 2, woundedCount = 2),
                    ),
                    shieldsOnAdventureSites = 5,
                    artifacts = 4,
                    crystalsInInventory = 7,
                    shieldsOnConquerSites = 6,
                    woundsInDeck = 3,
                ),
                citiesConquered = 2,
                roundsFinishedEarly = 1,
                cardsRemainingInDummyDeck = 8,
                endOfRoundAnnounced = false,
                questPoints = 9,
            ),
            score = 216,
            outcome = Outcome.WON,
            playedAt = Instant.parse("2026-07-18T12:34:56Z"),
        )

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
    }

    @Test
    fun `toEntity then toDomain round-trips a structurally different ForTheCouncilScoringInput session`() {
        // For the Council has no fame/Standard Achievements at all, unlike the other 4
        // scenarios - this exercises the JSON-column approach's whole reason for existing:
        // one inputJson column has to hold every scenario's differently-shaped input.
        val session = ScoringSession(
            scenario = Scenario.ForTheCouncil,
            knight = Knight.NOROWAS,
            playerName = null,
            input = ForTheCouncilScoringInput(
                questPoints = 12,
                reputationTrackSpace = ReputationTrackSpace.NEGATIVE_X,
            ),
            score = 2,
            outcome = Outcome.LOST,
            playedAt = Instant.parse("2026-07-19T08:00:00Z"),
        )

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
    }
}
