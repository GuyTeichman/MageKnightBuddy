package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * DAO-level behavior for the Sites tab's favorites (issue #236), exercised against a real SQLite
 * database via [BundledSQLiteDriver] (ADR-0003) so the actual table/constraints run - the same
 * setup as [ScoringSessionDaoTest]. Favorites are stored one row per favorited site id, so the set
 * of favorites is just the set of rows in `favorite_sites`.
 */
class FavoriteSiteDaoTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-favorite-sites", ".db")
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
    fun `add then observeAll emits the favorited id`() = runTest {
        val dao = database.favoriteSiteDao()

        dao.add(FavoriteSiteEntity("keep"))

        assertEquals(listOf("keep"), dao.observeAll().first())
    }

    @Test
    fun `add is idempotent - favoriting the same site twice keeps a single row`() = runTest {
        val dao = database.favoriteSiteDao()

        dao.add(FavoriteSiteEntity("keep"))
        dao.add(FavoriteSiteEntity("keep"))

        // INSERT-IGNORE on the primary key: the second favorite is a no-op, not a duplicate row or a crash.
        assertEquals(listOf("keep"), dao.getAllOnce())
    }

    @Test
    fun `remove deletes only that favorite`() = runTest {
        val dao = database.favoriteSiteDao()
        dao.add(FavoriteSiteEntity("keep"))
        dao.add(FavoriteSiteEntity("village"))

        dao.remove("keep")

        assertEquals(listOf("village"), dao.getAllOnce())
    }

    @Test
    fun `getAllOnce returns favorites sorted by id`() = runTest {
        val dao = database.favoriteSiteDao()
        // Inserted out of alphabetical order on purpose - the query's ORDER BY, not insertion order,
        // is what must produce the deterministic snapshot the backup export relies on.
        dao.add(FavoriteSiteEntity("village"))
        dao.add(FavoriteSiteEntity("keep"))
        dao.add(FavoriteSiteEntity("oasis"))

        assertEquals(listOf("keep", "oasis", "village"), dao.getAllOnce())
    }

    @Test
    fun `replaceAll wipes existing favorites and inserts the new set`() = runTest {
        val dao = database.favoriteSiteDao()
        dao.add(FavoriteSiteEntity("keep"))

        dao.replaceAll(listOf(FavoriteSiteEntity("village"), FavoriteSiteEntity("oasis")))

        // "keep" is gone (wiped), and only the restored set remains - the "restore from backup" contract.
        assertEquals(listOf("oasis", "village"), dao.getAllOnce())
    }
}
