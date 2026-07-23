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

class VolkareSessionDaoTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-volkare-session", ".db")
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
        val dao = database.volkareSessionDao()

        assertNull(dao.get())
    }

    @Test
    fun `upsert then get round-trips the saved session`() = runTest {
        val dao = database.volkareSessionDao()
        val entity = testEntity(round = 1)

        dao.upsert(entity)

        assertEquals(entity, dao.get())
    }

    @Test
    fun `upsert replaces the single saved slot instead of adding a second row`() = runTest {
        val dao = database.volkareSessionDao()

        dao.upsert(testEntity(round = 1))
        dao.upsert(testEntity(round = 2))

        assertEquals(2, dao.get()?.round)
    }

    @Test
    fun `getUpdatedAt returns null when nothing has been saved yet`() = runTest {
        val dao = database.volkareSessionDao()

        assertNull(dao.getUpdatedAt())
    }

    @Test
    fun `getUpdatedAt returns the saved row's updatedAt without needing the full session`() = runTest {
        val dao = database.volkareSessionDao()

        dao.upsert(testEntity(round = 1, updatedAt = 12345L))

        assertEquals(12345L, dao.getUpdatedAt())
    }

    @Test
    fun `upsert then get round-trips startsAtNight`() = runTest {
        val dao = database.volkareSessionDao()

        dao.upsert(testEntity(round = 1, startsAtNight = true))

        assertEquals(true, dao.get()?.startsAtNight)
    }

    private fun testEntity(round: Int, updatedAt: Long = 0L, startsAtNight: Boolean = false) = VolkareSessionEntity(
        scenario = "volkares_return",
        raceLevel = "FAIR",
        deckOrderJson = "[]",
        discardPileJson = "[]",
        round = round,
        cityRevealed = false,
        lost = false,
        logJson = "[]",
        updatedAt = updatedAt,
        startsAtNight = startsAtNight,
    )
}
