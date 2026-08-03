package com.guyteichman.mageknightbuddy.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TutorialProgressRepositoryTest {

    /**
     * A real, file-backed DataStore under a throwaway temp dir - the meaningful seam here is
     * "does a marked flag actually persist through DataStore", so we exercise the real store
     * rather than a fake. [scope] is the test's `backgroundScope`, auto-cancelled at test end so
     * the DataStore's internal coroutines don't leak.
     */
    private fun tempDataStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) {
            File(Files.createTempDirectory("tutorial_progress").toFile(), "test.preferences_pb")
        }

    @Test
    fun `hasSeen defaults to false and flips to true after markSeen`() = runTest {
        val repo = DataStoreTutorialProgressRepository(tempDataStore(backgroundScope))

        assertFalse(repo.hasSeen("dummy").first())
        repo.markSeen("dummy")
        assertTrue(repo.hasSeen("dummy").first())
    }

    @Test
    fun `markSeen affects only the given tutorial, not others`() = runTest {
        val repo = DataStoreTutorialProgressRepository(tempDataStore(backgroundScope))

        repo.markSeen("dummy")

        assertTrue(repo.hasSeen("dummy").first())
        assertFalse(repo.hasSeen("volkare").first())
    }
}
