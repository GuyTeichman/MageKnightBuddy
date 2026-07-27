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

class EnemyPickerSessionDaoTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-enemy-picker-session", ".db")
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
        val dao = database.enemyPickerSessionDao()

        assertNull(dao.get())
    }

    @Test
    fun `upsert then get round-trips the saved session`() = runTest {
        val dao = database.enemyPickerSessionDao()
        val entity = testEntity(drawLogJson = """[{"tokenId":"orc_a","pile":"GREEN","batchId":42}]""")

        dao.upsert(entity)

        assertEquals(entity, dao.get())
    }

    @Test
    fun `upsert replaces the single saved slot instead of adding a second row`() = runTest {
        val dao = database.enemyPickerSessionDao()

        dao.upsert(testEntity(drawLogJson = "[]"))
        dao.upsert(testEntity(drawLogJson = """[{"tokenId":"orc_b","pile":"GREEN","batchId":7}]"""))

        assertEquals("""[{"tokenId":"orc_b","pile":"GREEN","batchId":7}]""", dao.get()?.drawLogJson)
    }

    @Test
    fun `getUpdatedAt returns the saved row's updatedAt without needing the full session`() = runTest {
        val dao = database.enemyPickerSessionDao()

        dao.upsert(testEntity(updatedAt = 12345L))

        assertEquals(12345L, dao.getUpdatedAt())
    }

    private fun testEntity(
        drawLogJson: String = "[]",
        updatedAt: Long = 0L,
    ) = EnemyPickerSessionEntity(
        drawWithReplacement = false,
        tokenSetJson = """["BASE"]""",
        pilesJson = """{"GREEN":{"drawPile":["orc_a","orc_b"],"discardPile":[]}}""",
        drawLogJson = drawLogJson,
        updatedAt = updatedAt,
    )
}
