package com.guyteichman.mageknightbuddy.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AppResetTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-app-reset", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    // A real, file-backed DataStore under a throwaway temp dir (mirrors TutorialProgressRepositoryTest).
    private fun tempDataStore(scope: CoroutineScope): DataStore<Preferences> {
        val dir = Files.createTempDirectory("app_reset_tutorial").toFile().apply { deleteOnExit() }
        val file = File(dir, "test.preferences_pb").apply { deleteOnExit() }
        return PreferenceDataStoreFactory.create(scope = scope) { file }
    }

    @Test
    fun `resetToDefault wipes every Room table and forgets all seen tutorials`() = runTest {
        // Seed independently-managed stores: a Room table (scoring_sessions), a second Room table
        // (favorite_sites), a single-slot autosave table (dummy_player_sessions), and a tutorial flag -
        // enough to show the reset spans the whole database, not just one table.
        val tutorialProgress = DataStoreTutorialProgressRepository(tempDataStore(backgroundScope), backgroundScope)
        database.scoringSessionDao().insert(scoringEntity(playedAtEpochMillis = 1000L))
        database.favoriteSiteDao().add(FavoriteSiteEntity(siteId = "monastery"))
        tutorialProgress.markSeen("enemy_picker")

        AppReset(database, tutorialProgress).resetToDefault()

        // Every persisted store is back to a first-launch state.
        assertEquals(emptyList(), database.scoringSessionDao().getAllOnce())
        assertEquals(emptyList(), database.favoriteSiteDao().getAllOnce())
        assertFalse(tutorialProgress.hasSeen("enemy_picker").first())
    }

    // Minimal valid scoring row (JSON copied from ScoringSessionDaoTest's Solo Conquest shape).
    private fun scoringEntity(playedAtEpochMillis: Long) = ScoringSessionEntity(
        scenario = "solo_conquest",
        knight = "TOVAK",
        playerName = null,
        inputJson = """{"type":"solo_conquest","fame":0,"standardAchievements":""" +
            """{"spellsInDeck":0,"advancedActionsInDeck":0,"units":[],"shieldsOnAdventureSites":0,""" +
            """"artifacts":0,"crystalsInInventory":0,"shieldsOnConquerSites":0,"woundsInDeck":0},""" +
            """"citiesConquered":0,"roundsFinishedEarly":0,"cardsRemainingInDummyDeck":0,""" +
            """"endOfRoundAnnounced":true,"questPoints":0}""",
        score = 0,
        outcome = "LOST",
        playedAtEpochMillis = playedAtEpochMillis,
    )
}
