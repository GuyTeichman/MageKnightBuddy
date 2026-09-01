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
     * the DataStore's internal coroutines don't leak. The temp dir/file are marked deleteOnExit so
     * the suite doesn't leave preference files behind across repeated runs.
     */
    private fun tempDataStore(scope: CoroutineScope): DataStore<Preferences> {
        val dir = Files.createTempDirectory("tutorial_progress").toFile().apply { deleteOnExit() }
        val file = File(dir, "test.preferences_pb").apply { deleteOnExit() }
        return PreferenceDataStoreFactory.create(scope = scope) { file }
    }

    @Test
    fun `hasSeen defaults to false and flips to true after markSeen`() = runTest {
        val repo = DataStoreTutorialProgressRepository(tempDataStore(backgroundScope), backgroundScope)

        assertFalse(repo.hasSeen("dummy").first())
        repo.markSeen("dummy")
        assertTrue(repo.hasSeen("dummy").first())
    }

    @Test
    fun `markSeen affects only the given tutorial, not others`() = runTest {
        val repo = DataStoreTutorialProgressRepository(tempDataStore(backgroundScope), backgroundScope)

        repo.markSeen("dummy")

        assertTrue(repo.hasSeen("dummy").first())
        assertFalse(repo.hasSeen("volkare").first())
    }

    @Test
    fun `clear resets every seen flag back to false`() = runTest {
        val repo = DataStoreTutorialProgressRepository(tempDataStore(backgroundScope), backgroundScope)
        repo.markSeen("dummy")
        repo.markSeen("volkare")

        repo.clear()

        // A cleared store behaves like a first launch: previously-seen tutorials auto-show again (issue #304).
        assertFalse(repo.hasSeen("dummy").first())
        assertFalse(repo.hasSeen("volkare").first())
    }

    // markSeenAsync is intentionally not unit-tested: it's a thin `scope.launch { markSeen(id) }`
    // delegate whose only added behavior - surviving the caller being disposed mid-write - is a
    // scope-lifetime property, not something a virtual-time test meaningfully exercises. The
    // persistence itself is covered by the markSeen tests above.
}
