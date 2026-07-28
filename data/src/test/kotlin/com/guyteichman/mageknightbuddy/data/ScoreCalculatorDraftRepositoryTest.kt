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

class ScoreCalculatorDraftRepositoryTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File
    private lateinit var repository: ScoreCalculatorDraftRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-score-calculator-draft-repository", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = ScoreCalculatorDraftRepository(database.scoreCalculatorDraftDao())
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
    fun `save then restore round-trips a draft through Room`() = runTest {
        val draft = mapOf("pageIndex" to "3", "fame" to "42")

        repository.save(draft)

        assertEquals(draft, repository.restore())
    }

    @Test
    fun `save silently overwrites the previously saved draft`() = runTest {
        repository.save(mapOf("fame" to "10"))
        repository.save(mapOf("fame" to "20"))

        assertEquals(mapOf("fame" to "20"), repository.restore())
    }

    @Test
    fun `updatedAt returns the timestamp save was called with, without needing the full draft`() = runTest {
        repository.save(mapOf("fame" to "10"), updatedAt = 42L)

        assertEquals(42L, repository.updatedAt())
    }
}
