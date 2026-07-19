package com.guyteichman.mageknightbuddy.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseMigrationsTest {
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-migration-1-2", ".db")
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    @Test
    fun `MIGRATION_1_2 creates a dummy_player_sessions table with the columns DummyPlayerSessionEntity expects`() {
        val connection = BundledSQLiteDriver().open(dbFile.absolutePath)
        try {
            MIGRATION_1_2.migrate(connection)

            connection.execSQL(
                """
                INSERT INTO dummy_player_sessions
                    (id, knight, wasRandom, deckOrderJson, discardPileJson,
                     crystalsRed, crystalsGreen, crystalsBlue, crystalsWhite,
                     round, roundEnded, logJson)
                VALUES (0, 'GOLDYX', 0, '[]', '[]', 0, 2, 1, 0, 1, 0, '[]')
                """.trimIndent(),
            )

            val statement = connection.prepare("SELECT knight, round FROM dummy_player_sessions WHERE id = 0")
            try {
                assertEquals(true, statement.step())
                assertEquals("GOLDYX", statement.getText(0))
                assertEquals(1L, statement.getLong(1))
            } finally {
                statement.close()
            }
        } finally {
            connection.close()
        }
    }
}
