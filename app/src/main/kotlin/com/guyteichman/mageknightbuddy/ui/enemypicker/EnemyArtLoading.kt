package com.guyteichman.mageknightbuddy.ui.enemypicker

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Shared asset-bitmap loader for the enemy picker's token art (enemy/ruin/faction-reward/possessed
 * faces and pile backs, see ADR-0007). Tries each of [paths] in order under Android's `assets/` tree
 * and returns the first one that opens and decodes successfully, or null if none do - the signal each
 * face composable's `remember(...)` block uses to fall back to its text/shape stand-in until that
 * asset is bundled.
 */
internal fun loadAssetBitmap(context: Context, vararg paths: String): ImageBitmap? {
    for (path in paths) {
        try {
            // assets.open throws when the file is absent - that's the "try the next path" signal,
            // caught below rather than checked up front (there's no cheap "does this asset exist" call).
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { return it }
            }
        } catch (_: Exception) {
            // Fall through to the next path, if any; once the loop ends without returning, null wins.
        }
    }
    return null
}
