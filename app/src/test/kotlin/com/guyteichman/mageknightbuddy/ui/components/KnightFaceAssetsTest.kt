package com.guyteichman.mageknightbuddy.ui.components

import com.guyteichman.mageknightbuddy.domain.Knight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Keeps the Knight face avatars and their asset files in sync (issue #285).
 *
 * Runs on Robolectric so it can open files from the real, merged asset manager - which needs
 * `unitTests.isIncludeAndroidResources = true` (see `app/build.gradle.kts`); a plain JVM test has no
 * assets. Mirrors `SiteArtAssetsTest`. Every current Knight ships a face, so unlike the site/scenario
 * art these checks are load-bearing today, not just forward guards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KnightFaceAssetsTest {

    private val assets = RuntimeEnvironment.getApplication().assets
    private val faceDir = "knight-faces"

    @Test
    fun `every knight has a bundled face asset`() {
        // faceAsset is the same path the composable loads, so this proves the UI won't fall back.
        val missing = Knight.entries.filterNot { assetExists(it.faceAsset) }.map { it.name }
        assertTrue("Knights missing a knight-faces/*.jpg asset: $missing", missing.isEmpty())
    }

    @Test
    fun `every bundled face image is referenced by some knight`() {
        val referenced = Knight.entries.map { it.faceAsset.substringAfterLast('/') }.toSet()
        // Only .jpg files are avatars; a README.md alongside them is documentation, not an orphan.
        val orphaned = (assets.list(faceDir) ?: emptyArray())
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
