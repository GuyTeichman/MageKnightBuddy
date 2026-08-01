package com.guyteichman.mageknightbuddy.ui.enemypicker

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
 * superimposed on the [possessed] token, the way the cardboard stacks (rulebook p.7). The possessed
 * token is the full-size background disc; the circular enemy sits on top at
 * [CIRCULAR_ON_POSSESSED_SCALE] of that, covering its centre so the possessed token's delta icons
 * peek out around the rim. The *numbers* shown elsewhere are the summed values
 * ([com.guyteichman.mageknightbuddy.domain.PossessedEnemy.combine]) - this is only the art.
 */
@Composable
internal fun PossessedEnemyFace(circular: EnemyToken, possessed: PossessedToken, size: Dp) {
    // contentAlignment centres the smaller circular token over the possessed background disc.
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        PossessedTokenFace(possessed = possessed, size = size)
        EnemyTokenFace(token = circular, size = size * CIRCULAR_ON_POSSESSED_SCALE)
    }
}

/**
 * The possessed token's own face at [size], clipped to a circle. If its art is bundled (an asset at
 * `enemy-tokens/<id>.png` or `.jpg`, per ADR-0007) it's drawn; otherwise a themed "possession" disc
 * with the token's modifier summary stands in - the same graceful-degradation the round
 * [EnemyTokenFace] uses, so the picker works before the possessed art is sourced.
 */
@Composable
internal fun PossessedTokenFace(possessed: PossessedToken, size: Dp) {
    val context = LocalContext.current
    val bitmap = remember(possessed.id) { loadPossessedBitmap(context, possessed.id) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Possessed token",
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
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
    // Possessed faces are cut to a transparent-background circle, so PNG is tried first; a JPG crop
    // still works as a fallback source. assets.open throws when absent - that's the "no art" signal.
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

/** How much of the possessed token's diameter the superimposed circular enemy covers (rulebook p.7 stacking). */
private const val CIRCULAR_ON_POSSESSED_SCALE = 0.78f
