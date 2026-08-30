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
 * Behavior of [FavoriteSitesRepository] over a real Room database (via [BundledSQLiteDriver],
 * ADR-0003), the same integration style as [ScoringSessionRepositoryTest]. Starting state is built
 * through the repository's own [FavoriteSitesRepository.setFavorite], not by hand-inserting rows.
 */
class FavoriteSitesRepositoryTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File
    private lateinit var repository: FavoriteSitesRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-favorites-repository", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = FavoriteSitesRepository(database.favoriteSiteDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `setFavorite true then observeFavorites emits the id as a set`() = runTest {
        repository.setFavorite("keep", favorite = true)

        assertEquals(setOf("keep"), repository.observeFavorites().first())
    }

    @Test
    fun `setFavorite false unfavorites just that site`() = runTest {
        repository.setFavorite("keep", favorite = true)
        repository.setFavorite("village", favorite = true)

        repository.setFavorite("keep", favorite = false)

        assertEquals(setOf("village"), repository.observeFavorites().first())
    }

    @Test
    fun `setFavorite true is idempotent`() = runTest {
        repository.setFavorite("keep", favorite = true)
        repository.setFavorite("keep", favorite = true)

        assertEquals(setOf("keep"), repository.observeFavorites().first())
        assertEquals(listOf("keep"), repository.exportAll())
    }

    @Test
    fun `exportAll returns a sorted snapshot of the favorites`() = runTest {
        repository.setFavorite("village", favorite = true)
        repository.setFavorite("keep", favorite = true)

        assertEquals(listOf("keep", "village"), repository.exportAll())
    }

    @Test
    fun `replaceAll installs exactly the given set, discarding the rest`() = runTest {
        repository.setFavorite("keep", favorite = true)

        repository.replaceAll(listOf("village", "oasis"))

        assertEquals(setOf("village", "oasis"), repository.observeFavorites().first())
        assertEquals(listOf("oasis", "village"), repository.exportAll())
    }
}
