package com.guyteichman.mageknightbuddy.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.Knight

/**
 * The assets path of a [Knight]'s cropped face, keyed by the lower-cased enum name (e.g.
 * `knight-faces/arythea.jpg`). Exposed (not `private`) so the composable and its assets-honesty
 * test (`KnightFaceAssetsTest`) derive the path from one place - the test's whole job is to prove
 * every one of these files is actually bundled, so it must ask the same question the UI asks.
 */
internal val Knight.faceAsset: String
    get() = "knight-faces/${name.lowercase()}.jpg"

/**
 * A [Knight]'s face as a circular avatar at [size], cropped tight from their identity-card art
 * (`app/src/main/assets/knight-faces/<knight>.jpg`, derived from `knight-cards/` per issue #285).
 *
 * Loads the image from Android *assets* the same way the Sites tab's `SiteThumbnail` does
 * (`BitmapFactory` + `remember(key)` so scrolling a list doesn't re-decode), and clips it to a
 * circle. Degrades to a neutral person glyph for any future [Knight] whose face hasn't been cropped
 * yet - the same asset-backed, degrade-to-a-stand-in posture as [KnightShieldIcon] (all 8 current
 * Knights do have a face, so the glyph is defensive only).
 */
@Composable
internal fun KnightFace(knight: Knight, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val bitmap = rememberKnightFace(knight)
    // Shared shaping for both the real avatar and the fallback, so they occupy the same footprint.
    val shaped = modifier.size(size).clip(CircleShape)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = knight.displayName,
            // Crop (not Fit) so the square crop fills the circle edge-to-edge with no letterboxing.
            contentScale = ContentScale.Crop,
            modifier = shaped,
        )
    } else {
        Box(
            modifier = shaped.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = knight.displayName,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

/**
 * Decodes a knight's face bitmap once per [Knight] and caches it across recompositions. Returns null
 * when the asset is missing, which routes the caller to the fallback glyph. `remember(knight)`
 * re-runs only when the knight changes, so recomposition alone doesn't re-decode.
 */
@Composable
private fun rememberKnightFace(knight: Knight): ImageBitmap? {
    val context = LocalContext.current
    return remember(knight) { loadKnightFace(context, knight.faceAsset) }
}

/** Loads a `knight-faces/<file>` asset, or null if it isn't bundled (caught as the "missing" signal). */
private fun loadKnightFace(context: Context, path: String): ImageBitmap? = try {
    // assets.open throws if the file is absent; .use closes the stream once decoded either way.
    context.assets.open(path).use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}
