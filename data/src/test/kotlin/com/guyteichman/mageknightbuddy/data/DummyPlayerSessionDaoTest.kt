package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class DummyPlayerSessionDaoTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-dummy-player-session", ".db")
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
    fun `get returns null when nothing has been saved yet`() = runTest {
        val dao = database.dummyPlayerSessionDao()

        assertNull(dao.get())
    }

    @Test
    fun `upsert then get round-trips the saved session`() = runTest {
        val dao = database.dummyPlayerSessionDao()
        val entity = testEntity(round = 1)

        dao.upsert(entity)

        assertEquals(entity, dao.get())
    }

    @Test
    fun `upsert replaces the single saved slot instead of adding a second row`() = runTest {
        val dao = database.dummyPlayerSessionDao()

        dao.upsert(testEntity(round = 1))
        dao.upsert(testEntity(round = 2))

        assertEquals(2, dao.get()?.round)
    }

    private fun testEntity(round: Int) = DummyPlayerSessionEntity(
        knight = "GOLDYX",
        wasRandom = false,
        deckOrderJson = "[]",
        discardPileJson = "[]",
        crystalsRed = 0,
        crystalsGreen = 2,
        crystalsBlue = 1,
        crystalsWhite = 0,
        round = round,
        roundEnded = false,
        logJson = "[]",
    )
}
