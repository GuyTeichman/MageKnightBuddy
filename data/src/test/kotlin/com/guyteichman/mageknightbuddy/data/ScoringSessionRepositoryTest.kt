package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import java.io.File
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ScoringSessionRepositoryTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File
    private lateinit var repository: ScoringSessionRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-repository", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = ScoringSessionRepository(database.scoringSessionDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `save then getAll round-trips a ScoringSession through Room`() = runTest {
        val session = ScoringSession.create(
            scenario = Scenario.SoloConquest,
            knight = Knight.GOLDYX,
            playerName = "Guy",
            input = SoloConquestScoringInput(
                fame = 42,
                standardAchievements = StandardAchievements(
                    spellsInDeck = 1,
                    advancedActionsInDeck = 1,
                    units = (1..4).map { level -> UnitTally(level = level, healthyCount = 0, woundedCount = 0) },
                    shieldsOnAdventureSites = 0,
                    artifacts = 0,
                    crystalsInInventory = 0,
                    shieldsOnConquerSites = 0,
                    woundsInDeck = 0,
                ),
                citiesConquered = 1,
                roundsFinishedEarly = 0,
                cardsRemainingInDummyDeck = 0,
                endOfRoundAnnounced = true,
                questPoints = 0,
            ),
            playedAt = Instant.parse("2026-07-18T10:00:00Z"),
        )

        repository.save(session)

        val result = repository.getAll().first()

        assertEquals(1, result.size)
        assertEquals(session, result.single())
        assertEquals(Outcome.LOST, result.single().outcome)
    }
}
