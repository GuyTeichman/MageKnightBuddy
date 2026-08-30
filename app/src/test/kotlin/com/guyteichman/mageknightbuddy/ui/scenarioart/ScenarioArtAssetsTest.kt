package com.guyteichman.mageknightbuddy.ui.scenarioart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Keeps the scenario-art assets and the catalogue's filenames in sync (issue #285; the folder is
 * filled by #288).
 *
 * Empty-safe now - the folder holds only a README, so both checks pass vacuously - it starts
 * guarding the moment images land: no catalogue row may dangle, and no bundled image may be left
 * unreferenced. Runs on Robolectric to reach the merged asset manager, like `SiteArtAssetsTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScenarioArtAssetsTest {

    private val assets = RuntimeEnvironment.getApplication().assets
    private val artDir = "scenario-art"

    @Test
    fun `every catalogue entry references a bundled asset`() {
        val missing = ScenarioArtCatalogue.entries.map { it.filename }
            .filterNot { assetExists("$artDir/$it") }
        assertTrue("scenario-art files referenced by the catalogue but not bundled: $missing", missing.isEmpty())
    }

    @Test
    fun `every bundled scenario-art image is referenced by the catalogue`() {
        val referenced = ScenarioArtCatalogue.entries.map { it.filename }.toSet()
        // Only .jpg files are art; the README.md is documentation, not an orphan.
        val orphaned = (assets.list(artDir) ?: emptyArray())
            .filter { it.endsWith(".jpg") && it !in referenced }
            .sorted()
        assertEquals(emptyList<String>(), orphaned)
    }

    private fun assetExists(path: String): Boolean = try {
        assets.open(path).close()
        true
    } catch (_: Exception) {
        false
    }
}
