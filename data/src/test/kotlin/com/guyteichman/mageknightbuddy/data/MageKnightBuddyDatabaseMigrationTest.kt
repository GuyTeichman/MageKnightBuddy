package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * Verifies the hand-written Room migrations in [MIGRATION_10_11] on a real (bundled-driver, on-disk)
 * database - the fix for issue #194, where upgrading a shipped v10 install to v11 crash-looped
 * because the old destructive fallback dropped and recreated every table during open.
 *
 * The key guarantee under test is the one the destructive path violated: a user's existing data
 * survives the upgrade. We seed a v10 database by hand (Room can't build an old schema for us), drop
 * a Scoreboard row into it, then open the real database at v11 and confirm the row is still there and
 * the newly-added Enemy Picker table is usable.
 */
class MageKnightBuddyDatabaseMigrationTest {

    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-migration", ".db")
        dbFile.delete() // want the driver to create it fresh, not open an empty pre-made file
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    @Test
    fun `migrating a v10 database to v11 preserves existing rows and adds the enemy picker table`() = runTest {
        seedV10Database(dbFile)

        // Open at the current version (11) with only the real migration registered - deliberately no
        // destructive fallback here, so a missing or wrong migration fails loudly instead of silently
        // wiping the seeded data and passing.
        val database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_10_11)
            .build()

        try {
            // Data preservation: the v10 Scoreboard row survived the upgrade (the whole point of #194).
            val scores = database.scoringSessionDao().getAll().first()
            assertEquals(1, scores.size)
            assertEquals("solo_conquest", scores.single().scenario)
            assertEquals(137, scores.single().score)

            // The v11-only table exists and is queryable (would throw "no such table" if the migration
            // hadn't created it); nothing has been saved into it yet.
            assertNull(database.enemyPickerSessionDao().get())
        } finally {
            database.close()
        }
    }

    /**
     * Writes a database that looks exactly like a shipped v10 install: the four tables that existed at
     * v10 (identical to v11 - v10 -> v11 only *added* enemy_picker_sessions), a Room identity row, one
     * Scoreboard row, and `user_version = 10` so Room treats the next open as an upgrade. The four
     * CREATE statements are Room's own generated v11 schema for those tables, captured from a one-off
     * `exportSchema = true` build (see MageKnightBuddyDatabase for why export isn't left on).
     */
    private fun seedV10Database(file: File) {
        val connection: SQLiteConnection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.execSQL("CREATE TABLE IF NOT EXISTS `scoring_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scenario` TEXT NOT NULL, `knight` TEXT NOT NULL, `playerName` TEXT, `inputJson` TEXT NOT NULL, `score` INTEGER NOT NULL, `outcome` TEXT NOT NULL, `playedAtEpochMillis` INTEGER NOT NULL)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `dummy_player_sessions` (`id` INTEGER NOT NULL, `knight` TEXT NOT NULL, `wasRandom` INTEGER NOT NULL, `deckOrderJson` TEXT NOT NULL, `discardPileJson` TEXT NOT NULL, `crystalsRed` INTEGER NOT NULL, `crystalsGreen` INTEGER NOT NULL, `crystalsBlue` INTEGER NOT NULL, `crystalsWhite` INTEGER NOT NULL, `round` INTEGER NOT NULL, `roundEnded` INTEGER NOT NULL, `logJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `startsAtNight` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `volkare_sessions` (`id` INTEGER NOT NULL, `scenario` TEXT NOT NULL, `raceLevel` TEXT NOT NULL, `deckOrderJson` TEXT NOT NULL, `discardPileJson` TEXT NOT NULL, `round` INTEGER NOT NULL, `cityRevealed` INTEGER NOT NULL, `lost` INTEGER NOT NULL, `logJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `startsAtNight` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `proxy_player_sessions` (`id` INTEGER NOT NULL, `knight` TEXT NOT NULL, `wasRandom` INTEGER NOT NULL, `deckOrderJson` TEXT NOT NULL, `discardPileJson` TEXT NOT NULL, `crystalsRed` INTEGER NOT NULL, `crystalsGreen` INTEGER NOT NULL, `crystalsBlue` INTEGER NOT NULL, `crystalsWhite` INTEGER NOT NULL, `round` INTEGER NOT NULL, `roundEnded` INTEGER NOT NULL, `objectiveCardJson` TEXT, `objectiveShields` INTEGER NOT NULL, `logJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `startsAtNight` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            // Room's own bookkeeping table + identity row. The hash value is never checked on the
            // upgrade path (Room overwrites it with v11's after migrating), so a placeholder is fine.
            connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            connection.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, 'v10placeholderhash')")

            // One completed Solo Conquest game, as v1.0.0 would have saved it.
            connection.execSQL("INSERT INTO scoring_sessions (scenario, knight, playerName, inputJson, score, outcome, playedAtEpochMillis) VALUES('solo_conquest', 'TOVAK', 'Guy', '{}', 137, 'WON', 1785000000000)")

            connection.execSQL("PRAGMA user_version = 10")
        } finally {
            connection.close()
        }
    }
}
