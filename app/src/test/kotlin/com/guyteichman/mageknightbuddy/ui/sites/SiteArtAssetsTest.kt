package com.guyteichman.mageknightbuddy.ui.sites

import com.guyteichman.mageknightbuddy.domain.SiteCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Keeps the Sites tab's art assets and the catalogue's `art` references in sync (issue #235).
 *
 * The domain-side `SiteCatalogueTest` checks that each `art` filename is unique and non-blank, but it
 * runs in a module with no assets and so *cannot* verify the files actually exist. This closes that
 * gap by opening each referenced file from the real, merged asset manager - which needs Robolectric
 * and `unitTests.isIncludeAndroidResources = true` (see `app/build.gradle.kts`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SiteArtAssetsTest {

    private val assets = RuntimeEnvironment.getApplication().assets
    private val artDir = "site-art"

    @Test
    fun `every site with a non-null art references a bundled asset`() {
        // mapNotNull drops sites whose art is still null - art lands incrementally (issue #235), so a
        // still-unillustrated site (it shows a placeholder) is fine; a dangling reference is not.
        val missing = SiteCatalogue.sites.mapNotNull { it.art }.filterNot { assetExists("$artDir/$it") }
        assertTrue("sites.json references site-art files that aren't bundled: $missing", missing.isEmpty())
    }

    @Test
    fun `every bundled site-art image is referenced by some site`() {
        val referenced = SiteCatalogue.sites.mapNotNull { it.art }.toSet()
        // Only the .jpg images are art; a README.md alongside them is documentation, not an orphan.
        val orphaned = (assets.list(artDir) ?: emptyArray())
            .filter { it.endsWith(".jpg") && it !in referenced }
            .sorted()
        assertEquals(emptyList<String>(), orphaned)
    }

    // assets.open throws if the file is absent - that's the "missing" signal; close() frees the handle.
    private fun assetExists(path: String): Boolean = try {
        assets.open(path).close()
        true
    } catch (_: Exception) {
        false
    }
}
