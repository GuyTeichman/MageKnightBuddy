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
import kotlin.test.assertIs

class BackupCodecTest {

    // A small For-the-Council session (its input is the shortest of any scenario), used for the
    // byte-exact wire-format assertion below.
    private val forTheCouncilSession = ScoringSession(
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

    // A structurally very different session (Solo Conquest has fame + a full Standard Achievements
    // block), so the round-trip test exercises more than one input shape through the codec.
    private val soloConquestSession = ScoringSession(
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

    @Test
    fun `encode writes the exact documented JSON wire format`() {
        val exportedAt = Instant.parse("2026-08-01T00:00:00Z")

        val json = BackupCodec.encode(listOf(forTheCouncilSession), exportedAt)

        // Hand-derived from the DTO field order (not read off the code's output): the envelope's
        // formatVersion/exportedAt/records, then the record's columns, with `input` nested as a
        // structured object whose kotlinx "type" discriminator comes first. Pinning the byte-exact
        // format is what makes the round-trip test below meaningful rather than self-referential.
        val expected = """{"formatVersion":1,"exportedAtEpochMillis":${exportedAt.toEpochMilli()},""" +
            """"records":[{"scenario":"for_the_council","knight":"NOROWAS","playerName":null,""" +
            """"input":{"type":"for_the_council","questPoints":12,""" +
            """"reputationTrackSpaceName":"NEGATIVE_X"},"score":2,"outcome":"LOST",""" +
            """"playedAtEpochMillis":${forTheCouncilSession.playedAt.toEpochMilli()}}]}"""
        assertEquals(expected, json)
    }

    @Test
    fun `encode writes the exact wire format for a Solo Conquest session, achievements and all`() {
        val exportedAt = Instant.parse("2026-08-01T00:00:00Z")

        val json = BackupCodec.encode(listOf(soloConquestSession), exportedAt)

        // Independently hand-derived (per CLAUDE.md's round-trip warning) so the complex shape - Solo
        // Conquest's fame + nested StandardAchievements + per-level unit list - is pinned by an
        // explicit expected value, not left resting only on the encode-then-decode equality below.
        val expected = """{"formatVersion":1,"exportedAtEpochMillis":${exportedAt.toEpochMilli()},""" +
            """"records":[{"scenario":"solo_conquest","knight":"ARYTHEA","playerName":"Guy",""" +
            """"input":{"type":"solo_conquest","fame":72,"standardAchievements":{""" +
            """"spellsInDeck":3,"advancedActionsInDeck":2,"units":[""" +
            """{"level":1,"healthyCount":1,"woundedCount":2},""" +
            """{"level":4,"healthyCount":2,"woundedCount":2}],""" +
            """"shieldsOnAdventureSites":5,"artifacts":4,"crystalsInInventory":7,""" +
            """"shieldsOnConquerSites":6,"woundsInDeck":3},"citiesConquered":2,""" +
            """"roundsFinishedEarly":1,"cardsRemainingInDummyDeck":8,"endOfRoundAnnounced":false,""" +
            """"questPoints":9},"score":216,"outcome":"WON",""" +
            """"playedAtEpochMillis":${soloConquestSession.playedAt.toEpochMilli()}}]}"""
        assertEquals(expected, json)
    }

    @Test
    fun `encode then decode round-trips every field of structurally different sessions`() {
        val sessions = listOf(soloConquestSession, forTheCouncilSession)

        val decoded = BackupCodec.decode(BackupCodec.encode(sessions, Instant.parse("2026-08-01T00:00:00Z")))

        assertIs<BackupDecodeResult.Success>(decoded)
        assertEquals(sessions, decoded.sessions)
    }

    @Test
    fun `encode of empty history round-trips to an empty session list`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(emptyList(), Instant.parse("2026-08-01T00:00:00Z")))

        assertIs<BackupDecodeResult.Success>(decoded)
        assertEquals(emptyList(), decoded.sessions)
    }

    @Test
    fun `decode returns Malformed for text that is not a backup document`() {
        assertIs<BackupDecodeResult.Malformed>(BackupCodec.decode("this is not json"))
        assertIs<BackupDecodeResult.Malformed>(BackupCodec.decode("""{"unrelated":123}"""))
    }

    @Test
    fun `decode refuses a backup whose formatVersion is newer than this build understands`() {
        // A well-formed envelope, but stamped with a future format version - restoring it could
        // silently mis-parse fields this build doesn't know about, so it must be refused outright.
        val future = """{"formatVersion":999,"exportedAtEpochMillis":0,"records":[]}"""

        val result = BackupCodec.decode(future)

        assertIs<BackupDecodeResult.UnsupportedVersion>(result)
        assertEquals(999, result.version)
    }

    @Test
    fun `decode reports UnsupportedVersion even when a newer file adds unknown fields`() {
        // The realistic newer-format case: a future version bumps formatVersion *and* adds fields.
        // The version gate must fire (UnsupportedVersion) rather than the strict parser choking on
        // the unknown fields first and returning Malformed - otherwise the version stamp would be
        // useless for exactly the situation it exists for. (The records:[] test above can't catch
        // this, since with no records there are no new fields for the strict parse to trip over.)
        val futureWithNewFields = """{"formatVersion":2,"exportedAtEpochMillis":0,"newTopLevelField":true,"records":[""" +
            """{"scenario":"for_the_council","knight":"NOROWAS","playerName":null,""" +
            """"input":{"type":"for_the_council","questPoints":1,"reputationTrackSpaceName":"NEGATIVE_X"},""" +
            """"score":0,"outcome":"LOST","playedAtEpochMillis":0,"newRecordField":42}]}"""

        val result = BackupCodec.decode(futureWithNewFields)

        assertIs<BackupDecodeResult.UnsupportedVersion>(result)
        assertEquals(2, result.version)
    }

    @Test
    fun `decode returns Malformed when a record references an unknown knight`() {
        // Structurally valid JSON, but "NONEXISTENT" is not a Knight - mapping it back to the domain
        // would throw, so the whole file is rejected rather than crashing (or partially restoring).
        val badKnight = """{"formatVersion":1,"exportedAtEpochMillis":0,"records":[""" +
            """{"scenario":"for_the_council","knight":"NONEXISTENT","playerName":null,""" +
            """"input":{"type":"for_the_council","questPoints":1,"reputationTrackSpaceName":"NEGATIVE_X"},""" +
            """"score":0,"outcome":"LOST","playedAtEpochMillis":0}]}"""

        assertIs<BackupDecodeResult.Malformed>(BackupCodec.decode(badKnight))
    }

    @Test
    fun `decode returns Malformed when a record references an unknown scenario`() {
        // Same as the unknown-knight case but for the scenario id, which resolves via a different
        // path (Scenario.fromId's entries.first {}, throwing NoSuchElementException not
        // IllegalArgumentException) - so this guards that the codec catches that variant too.
        val badScenario = """{"formatVersion":1,"exportedAtEpochMillis":0,"records":[""" +
            """{"scenario":"not_a_scenario","knight":"NOROWAS","playerName":null,""" +
            """"input":{"type":"for_the_council","questPoints":1,"reputationTrackSpaceName":"NEGATIVE_X"},""" +
            """"score":0,"outcome":"LOST","playedAtEpochMillis":0}]}"""

        assertIs<BackupDecodeResult.Malformed>(BackupCodec.decode(badScenario))
    }
}
