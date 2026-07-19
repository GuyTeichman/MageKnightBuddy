package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class DummyPlayerSessionRepositoryTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File
    private lateinit var repository: DummyPlayerSessionRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-dummy-player-repository", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = DummyPlayerSessionRepository(database.dummyPlayerSessionDao())
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
    fun `save then restore round-trips a DummyPlayerSession through Room`() = runTest {
        val session = DummyPlayerSession.start(Knight.GOLDYX, deckOrder = listOf(CardColor.RED, CardColor.GREEN))

        repository.save(session)

        assertEquals(session, repository.restore())
    }

    @Test
    fun `save silently overwrites the previously saved session`() = runTest {
        val first = DummyPlayerSession.start(Knight.GOLDYX)
        val second = DummyPlayerSession.start(Knight.CORAL)

        repository.save(first)
        repository.save(second)

        assertEquals(second, repository.restore())
    }
}
