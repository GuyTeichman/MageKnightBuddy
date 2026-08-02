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
 * dropped onto the [possessed] token, the way the cardboard stacks (rulebook p.7). The possessed
 * token is a square starfield tile carrying the delta icons down its left; the circular enemy - the
 * same diameter as any other enemy face - nests against its right side and **protrudes past its right
 * edge**, so the composite is wider than it is tall ([COMPOSITE_ASPECT]). Because the enemy face is an
 * opaque circle drawn on top, it simply covers the tile where they overlap and the tile's delta icons
 * stay visible on the left - no cut-out needed. The *numbers* shown elsewhere are the summed values
 * ([com.guyteichman.mageknightbuddy.domain.PossessedEnemy.combine]) - this is only the art.
 */
@Composable
internal fun PossessedEnemyFace(circular: EnemyToken, possessed: PossessedToken, size: Dp) {
    // [size] is the tile side and the enemy diameter (matching a normal enemy face). The composite is
    // wider by the enemy's overhang past the tile's right edge.
    val compositeWidth = size * COMPOSITE_ASPECT
    Box(modifier = Modifier.size(width = compositeWidth, height = size)) {
        // Possessed tile, flush to the left. Its cleaned art is a plain rectangle (an earlier
        // rounded-corner clip distorted the delta numbers), so it's drawn as-is.
        PossessedTokenFace(possessed = possessed, size = size, modifier = Modifier.align(Alignment.CenterStart))
        // Circular enemy on top, its centre [ENEMY_CENTER_X] of the way across the tile. Its box is
        // offset from the composite's left by (centre - radius); radius is half the diameter (= size).
        Box(modifier = Modifier.align(Alignment.CenterStart).offset(x = size * (ENEMY_CENTER_X - 0.5f))) {
            EnemyTokenFace(token = circular, size = size)
        }
    }
}

/**
 * The possessed token's own face at [size] - a plain square tile (its cleaned art is a starfield
 * rectangle with the delta icons, no rounding). [modifier] lets a caller position it (e.g.
 * [PossessedEnemyFace] aligns it left). If its art is bundled (an asset at `enemy-tokens/<id>.png`
 * or `.jpg`, per ADR-0007) it's drawn; otherwise a themed "possession" disc with the token's modifier
 * summary stands in - the same graceful-degradation the round [EnemyTokenFace] uses, so the picker
 * works before the possessed art is sourced.
 */
@Composable
internal fun PossessedTokenFace(possessed: PossessedToken, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(possessed.id) { loadPossessedBitmap(context, possessed.id) }

    if (bitmap != null) {
        // No clip: the cleaned art is already a plain rectangle the size of the tile.
        Image(
            bitmap = bitmap,
            contentDescription = "Possessed token",
            modifier = modifier.size(size),
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
    // Possessed tiles are square PNGs (transparent-free), so PNG is tried first; a JPG crop still
    // works as a fallback source. assets.open throws when absent - that's the "no art" signal.
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
 * Enemy-circle centre X as a fraction of the possessed tile's width (> 1.0 would be off the tile;
 * 0.909 sits it near the right, so its left arc clears the tile's left-side delta icons and its right
 * arc overhangs past the tile's right edge). The enemy radius is half the tile side.
 */
private const val ENEMY_CENTER_X = 0.909f

/** Composite width as a multiple of the tile side: the tile (1.0) plus the enemy's right overhang. */
private const val COMPOSITE_ASPECT = ENEMY_CENTER_X + 0.5f
