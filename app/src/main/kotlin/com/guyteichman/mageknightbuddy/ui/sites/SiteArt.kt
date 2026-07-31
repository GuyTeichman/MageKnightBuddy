package com.guyteichman.mageknightbuddy.ui.sites

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.Site
import com.guyteichman.mageknightbuddy.domain.SiteCategory

/**
 * Draws a [Site]'s art, with a graceful placeholder while the art isn't bundled yet (issue #235
 * fills it in). Same asset-backed, degrade-to-a-stand-in approach as the Enemy Picker's
 * [com.guyteichman.mageknightbuddy.ui.enemypicker.EnemyTokenFace] - so the whole Sites tab (issue
 * #234) is usable and reviewable before a single image lands. Sites are rectangular tiles/cards, so
 * these clip to a rounded rectangle rather than the Enemy Picker's circle.
 *
 * Art lives as Android *assets* (`app/src/main/assets/site-art/<filename>`) keyed by each site's
 * [Site.art] filename, not `res/drawable`, because it's a large id-referenced set that grows
 * site-by-site (ADR-0007) - a new image needs a file, not a code change.
 */

private val THUMB_SHAPE = RoundedCornerShape(8.dp)

/** A small square thumbnail for a list row: the site's art, or a category-hinting placeholder. */
@Composable
internal fun SiteThumbnail(site: Site, size: Dp = 56.dp) {
    val bitmap = rememberSiteBitmap(site)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = site.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(THUMB_SHAPE),
        )
    } else {
        SiteArtPlaceholder(
            category = site.category,
            iconSize = size * 0.5f,
            modifier = Modifier.size(size).clip(THUMB_SHAPE),
        )
    }
}

/** A header image for the detail screen: the site's art framed on a tinted panel, or a placeholder. */
@Composable
internal fun SiteArtHeader(site: Site, modifier: Modifier = Modifier) {
    val bitmap = rememberSiteBitmap(site)
    val panel = modifier.fillMaxWidth().height(220.dp).clip(THUMB_SHAPE)
    if (bitmap != null) {
        Box(
            modifier = panel.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = site.name,
                // Fit (not Crop): the card illustrations vary in aspect - wide (Monastery), tall
                // (Mage Tower), square (the Shades hex tiles) - so Fit shows each one whole,
                // centered on the panel, rather than cropping its edges.
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(8.dp),
            )
        }
    } else {
        SiteArtPlaceholder(category = site.category, iconSize = 72.dp, modifier = panel)
    }
}

/**
 * Decodes a site's art bitmap once per [Site.art] filename and caches it across recompositions.
 * Returns null when the site has no art yet (`art == null`) or the asset is missing - either way the
 * caller shows the placeholder. `remember(key)` re-runs only when the key changes, so scrolling the
 * list doesn't re-decode.
 */
@Composable
private fun rememberSiteBitmap(site: Site): ImageBitmap? {
    val context = LocalContext.current
    return remember(site.art) { site.art?.let { loadSiteBitmap(context, it) } }
}

/** Loads `site-art/<filename>` from assets, or null if that asset isn't bundled yet. */
private fun loadSiteBitmap(context: Context, filename: String): ImageBitmap? = try {
    // assets.open throws if the file is absent - that's the "no art yet" signal, caught below.
    context.assets.open("site-art/$filename").use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}

/** The stand-in shown until a site's real art exists: a tinted box with a category-hinting icon. */
@Composable
private fun SiteArtPlaceholder(category: SiteCategory, iconSize: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Maps a [SiteCategory] to a rough icon, so the placeholder list reads as more than a wall of
 * identical grey squares before art lands. Purely cosmetic - the category is an app-side grouping
 * (see [SiteCategory]), and these icons carry no rules meaning.
 */
private fun categoryIcon(category: SiteCategory): ImageVector = when (category) {
    SiteCategory.RAMPAGING_ENEMY -> Icons.Filled.Whatshot
    SiteCategory.FORTIFIED_SITE -> Icons.Filled.Castle
    SiteCategory.ADVENTURE_SITE -> Icons.Filled.Explore
    SiteCategory.SETTLEMENT -> Icons.Filled.Cottage
    SiteCategory.RESOURCE_SITE -> Icons.Filled.Diamond
    SiteCategory.SPECIAL_TILE -> Icons.Filled.Terrain
    SiteCategory.TERRAIN_FEATURE -> Icons.Filled.Fence
}
