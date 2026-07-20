package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ScoringSessionDaoTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-scoring-sessions", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `getAll returns inserted sessions ordered by most recently played first`() = runTest {
        val dao = database.scoringSessionDao()
        val older = testEntity(playedAtEpochMillis = 1000L)
        val newer = testEntity(playedAtEpochMillis = 2000L)

        dao.insert(older)
        dao.insert(newer)

        val result = dao.getAll().first()

        assertEquals(listOf(2000L, 1000L), result.map { it.playedAtEpochMillis })
    }

    private fun testEntity(playedAtEpochMillis: Long) = ScoringSessionEntity(
        scenario = "solo_conquest",
        knight = "TOVAK",
        playerName = null,
        inputJson = """{"type":"solo_conquest","fame":0,"standardAchievements":""" +
            """{"spellsInDeck":0,"advancedActionsInDeck":0,"units":[],"shieldsOnAdventureSites":0,""" +
            """"artifacts":0,"crystalsInInventory":0,"shieldsOnConquerSites":0,"woundsInDeck":0},""" +
            """"citiesConquered":0,"roundsFinishedEarly":0,"cardsRemainingInDummyDeck":0,""" +
            """"endOfRoundAnnounced":true,"questPoints":0}""",
        score = 0,
        outcome = "LOST",
        playedAtEpochMillis = playedAtEpochMillis,
    )
}
