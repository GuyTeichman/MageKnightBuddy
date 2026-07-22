package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareSession
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class VolkareSessionRepositoryTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File
    private lateinit var repository: VolkareSessionRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-volkare-repository", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = VolkareSessionRepository(database.volkareSessionDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `restore returns null when nothing has been saved yet`() = runTest {
        assertNull(repository.restore())
    }

    @Test
    fun `save then restore round-trips a VolkareSession through Room`() = runTest {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR)

        repository.save(session)

        assertEquals(session, repository.restore())
    }

    @Test
    fun `save silently overwrites the previously saved session`() = runTest {
        val first = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR)
        val second = VolkareSession.start(Scenario.VolkaresQuest, RaceLevel.TIGHT)

        repository.save(first)
        repository.save(second)

        assertEquals(second, repository.restore())
    }

    @Test
    fun `updatedAt returns null when nothing has been saved yet`() = runTest {
        assertNull(repository.updatedAt())
    }

    @Test
    fun `updatedAt returns the timestamp save was called with, without needing the full session`() = runTest {
        val session = VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR)

        repository.save(session, updatedAt = 42L)

        assertEquals(42L, repository.updatedAt())
    }
}
