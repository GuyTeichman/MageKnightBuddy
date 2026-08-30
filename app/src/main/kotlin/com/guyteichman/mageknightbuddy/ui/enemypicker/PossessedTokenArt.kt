package com.guyteichman.mageknightbuddy.ui.enemypicker

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.EnemyToken
import com.guyteichman.mageknightbuddy.domain.PossessedToken

/**
 * Renders one **possessed enemy** (docs/rules/apocalypse-dragon.md): the [circular] enemy token
 * nested into the [possessed] token, the way the cardboard stacks (rulebook p.7). The possessed token
 * is a **crescent** starfield tile - the delta icons run down its left, and a circular bite on its
 * right is where the enemy drops in. The circular enemy is drawn on top, filling the bite and
 * **protruding past the tile's right edge**, so the composite is wider than it is tall
 * ([COMPOSITE_ASPECT]) and the two read as one interlocked token. The tile aspect, enemy diameter and
 * offsets are all fractions of [size] measured from the token's own 3D mesh silhouette, so the circle
 * seats in the bite instead of overlapping a square. The *numbers* shown elsewhere are the summed values
 * ([com.guyteichman.mageknightbuddy.domain.PossessedEnemy.combine]) - this is only the art.
 */
@Composable
internal fun PossessedEnemyFace(circular: EnemyToken, possessed: PossessedToken, size: Dp) {
    // [size] is the crescent tile's height; every other length below is a fraction of it. The composite
    // is wider than [size] by the enemy's overhang past the bite.
    val compositeWidth = size * COMPOSITE_ASPECT
    Box(modifier = Modifier.size(width = compositeWidth, height = size)) {
        // Crescent possessed tile, flush to the top-left; its transparent bite is where the enemy seats.
        PossessedTokenFace(possessed = possessed, size = size, modifier = Modifier.align(Alignment.TopStart))
        // Circular enemy nested in the bite and overhanging the tile's right edge. The offsets place its
        // box so the circle's centre lands on the bite centre; drawn on top, it covers the tile they share.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = size * ENEMY_OFFSET_X_FRAC, y = size * ENEMY_OFFSET_Y_FRAC),
        ) {
            EnemyTokenFace(token = circular, size = size * ENEMY_DIAMETER_FRAC)
        }
    }
}

/**
 * The possessed token's own face: a **crescent** starfield tile ([size] tall, [TILE_ASPECT] as wide),
 * its delta icons down the left and a transparent circular bite on the right. [modifier] lets a caller
 * position it (e.g. [PossessedEnemyFace] aligns it top-left). If its art is bundled (an asset at
 * `enemy-tokens/<id>.png` or `.jpg`, per ADR-0007) it's drawn; otherwise a themed "possession" disc
 * with the token's modifier summary stands in - the same graceful-degradation the round [EnemyTokenFace]
 * uses, so the picker works before the possessed art is sourced.
 */
@Composable
internal fun PossessedTokenFace(possessed: PossessedToken, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(possessed.id) { loadPossessedBitmap(context, possessed.id) }

    if (bitmap != null) {
        // No clip: the PNG's own alpha is the crescent shape. Height is [size]; width follows the tile's
        // aspect so the bite stays circular.
        Image(
            bitmap = bitmap,
            contentDescription = "Possessed token",
            modifier = modifier.size(width = size * TILE_ASPECT, height = size),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Possessed\n" + possessed.summary(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}

/**
 * A compact, sign-carrying summary of a possessed token's deltas (e.g. "+2 Armor · +3 Psychic"),
 * used for the art fallback and the pile-composition label. Only the modifiers the token actually
 * prints appear; an all-zero token can't occur (the catalogue test forbids it).
 */
internal fun PossessedToken.summary(): String = buildList {
    if (armorDelta != 0) add("%+d Armor".format(armorDelta))
    if (attackDelta != 0) add("%+d Attack".format(attackDelta))
    if (fameDelta != 0) add("%+d Fame".format(fameDelta))
    psychicAttack?.let { add("Psychic $it") }
}.joinToString(" · ")

/** Loads `enemy-tokens/<id>.png` (then `.jpg`) from assets, or null if the possessed art isn't bundled yet. */
private fun loadPossessedBitmap(context: Context, id: String): ImageBitmap? {
    // Possessed tiles are transparent PNGs (a starfield crescent silhouette), so PNG is tried first; a
    // JPG crop still works as a fallback source. assets.open throws when absent - that's the "no art" signal.
    for (ext in listOf("png", "jpg")) {
        try {
            context.assets.open("enemy-tokens/$id.$ext").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { return it }
            }
        } catch (_: Exception) {
            // Try the next extension, then fall through to null (text fallback).
        }
    }
    return null
}

/**
 * Composite geometry, all as fractions of the tile height [size], measured once from the possessed
 * token's own 3D mesh silhouette (the same mesh that shapes each `possessed_0*.png` crescent). Together
 * they seat the circular enemy in the crescent's bite:
 * - [TILE_ASPECT] - the crescent tile's width / height.
 * - [ENEMY_DIAMETER_FRAC] - the enemy circle's diameter (it fills the bite, a touch under a full tile height).
 * - [ENEMY_OFFSET_X_FRAC] / [ENEMY_OFFSET_Y_FRAC] - the enemy box's top-left corner within the composite.
 * - [COMPOSITE_ASPECT] - the whole composite's width / height (the tile plus the enemy's right overhang).
 */
private const val TILE_ASPECT = 0.882f
private const val ENEMY_DIAMETER_FRAC = 0.967f
private const val ENEMY_OFFSET_X_FRAC = 0.398f
private const val ENEMY_OFFSET_Y_FRAC = 0.032f
private const val COMPOSITE_ASPECT = 1.366f
