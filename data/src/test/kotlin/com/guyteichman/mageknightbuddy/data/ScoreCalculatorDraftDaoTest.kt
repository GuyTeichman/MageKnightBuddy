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

class ScoreCalculatorDraftDaoTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-score-calculator-draft", ".db")
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
        val dao = database.scoreCalculatorDraftDao()

        assertNull(dao.get())
    }

    @Test
    fun `upsert then get round-trips the saved draft`() = runTest {
        val dao = database.scoreCalculatorDraftDao()
        val entity = testEntity(fieldsJson = """{"fame":"50","knight":"WOLFHAWK"}""")

        dao.upsert(entity)

        assertEquals(entity, dao.get())
    }

    @Test
    fun `upsert replaces the single saved slot instead of adding a second row`() = runTest {
        val dao = database.scoreCalculatorDraftDao()

        dao.upsert(testEntity(fieldsJson = """{"fame":"10"}"""))
        dao.upsert(testEntity(fieldsJson = """{"fame":"20"}"""))

        assertEquals("""{"fame":"20"}""", dao.get()?.fieldsJson)
    }

    @Test
    fun `getUpdatedAt returns the saved row's updatedAt without needing the full draft`() = runTest {
        val dao = database.scoreCalculatorDraftDao()

        dao.upsert(testEntity(updatedAt = 12345L))

        assertEquals(12345L, dao.getUpdatedAt())
    }

    private fun testEntity(
        fieldsJson: String = "{}",
        updatedAt: Long = 0L,
    ) = ScoreCalculatorDraftEntity(
        fieldsJson = fieldsJson,
        updatedAt = updatedAt,
    )
}
