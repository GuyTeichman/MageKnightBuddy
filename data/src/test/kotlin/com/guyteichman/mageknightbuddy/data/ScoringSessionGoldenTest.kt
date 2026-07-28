package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden-decode ("tripwire") test for a persisted Scoreboard row - see [EnemyPickerSessionGoldenTest]
 * for the rationale of freezing a shipped payload rather than round-tripping today's code.
 *
 * The Scoreboard matters more than the single-slot session tables: it is the one store with lasting
 * value (a history of completed games), and by design it is *not* auto-discarded on a decode failure
 * the way [SingleSlotAutosaveRepository.restore] discards a stale in-progress session - so this test,
 * plus [PersistenceJson]'s leniency, is the whole of its forward-compatibility safety net. If this
 * ever goes red, a real migration is almost certainly the right answer, not a reset.
 *
 * The `inputJson` here exercises the polymorphic [ScoringInputDto] hierarchy: `"type"` is
 * kotlinx.serialization's default class discriminator, and `"solo_conquest"` its `@SerialName`.
 */
class ScoringSessionGoldenTest {

    @Test
    fun `a shipped Solo Conquest scoreboard row still decodes to the expected session`() {
        // Verbatim columns as a shipped build writes them - do not regenerate (see class doc).
        val shipped = ScoringSessionEntity(
            id = 1L,
            scenario = "solo_conquest",
            knight = "TOVAK",
            playerName = "Guy",
            inputJson = """
                {"type":"solo_conquest","fame":50,"standardAchievements":{"spellsInDeck":2,
                "advancedActionsInDeck":3,"units":[{"level":2,"healthyCount":1,"woundedCount":0}],
                "shieldsOnAdventureSites":4,"artifacts":1,"crystalsInInventory":5,
                "shieldsOnConquerSites":6,"woundsInDeck":2},"citiesConquered":2,"roundsFinishedEarly":1,
                "cardsRemainingInDummyDeck":7,"endOfRoundAnnounced":true,"questPoints":3}
            """.trimIndent().replace("\n", ""),
            score = 137,
            outcome = "WON",
            playedAtEpochMillis = 1_785_000_000_000,
        )

        val session = shipped.toDomain()

        // Expected values derived by hand from the frozen columns above, not read off `session`.
        assertEquals(Scenario.SoloConquest, session.scenario)
        assertEquals(Knight.TOVAK, session.knight)
        assertEquals("Guy", session.playerName)
        assertEquals(137, session.score) // stored, not recomputed by toDomain
        assertEquals(Outcome.WON, session.outcome)
        assertEquals(Instant.ofEpochMilli(1_785_000_000_000), session.playedAt)
        assertEquals(
            SoloConquestScoringInput(
                fame = 50,
                standardAchievements = StandardAchievements(
                    spellsInDeck = 2,
                    advancedActionsInDeck = 3,
                    units = listOf(UnitTally(level = 2, healthyCount = 1, woundedCount = 0)),
                    shieldsOnAdventureSites = 4,
                    artifacts = 1,
                    crystalsInInventory = 5,
                    shieldsOnConquerSites = 6,
                    woundsInDeck = 2,
                ),
                citiesConquered = 2,
                roundsFinishedEarly = 1,
                cardsRemainingInDummyDeck = 7,
                endOfRoundAnnounced = true,
                questPoints = 3,
            ),
            session.input,
        )
    }
}
