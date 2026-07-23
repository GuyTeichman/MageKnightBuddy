package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ProxyPlayerSessionRepositoryTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File
    private lateinit var repository: ProxyPlayerSessionRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-proxy-player-repository", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = ProxyPlayerSessionRepository(database.proxyPlayerSessionDao())
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
    fun `save then restore round-trips a ProxyPlayerSession through Room`() = runTest {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        repository.save(session)

        assertEquals(session, repository.restore())
    }

    @Test
    fun `save silently overwrites the previously saved session`() = runTest {
        val first = ProxyPlayerSession.start(Knight.CORAL)
        val second = ProxyPlayerSession.start(Knight.GOLDYX)

        repository.save(first)
        repository.save(second)

        assertEquals(second, repository.restore())
    }

    @Test
    fun `updatedAt returns the timestamp save was called with, without needing the full session`() = runTest {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        repository.save(session, updatedAt = 42L)

        assertEquals(42L, repository.updatedAt())
    }
}
