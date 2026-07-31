package com.guyteichman.mageknightbuddy.ui.enemypicker

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.RuinToken

/**
 * Renders a [RuinToken]'s hexagonal face at [size] - the RUIN pile's counterpart to
 * [EnemyTokenFace]. Ruin art (when bundled at `enemy-tokens/<id>.png`, same asset folder and id-key
 * convention as enemy art, ADR-0007) is a *transparent PNG whose hexagon silhouette is baked in* -
 * so it's drawn with **no** clip at all, unlike the circle-clip enemy faces get: a ruin token is
 * physically a hexagon, not a round disc, and the cutout alpha (not a Compose [androidx.compose.ui.graphics.Shape])
 * is what gives it that shape. PNG rather than JPG precisely because JPG has no alpha channel.
 * Until a ruin's art is sourced, a readable text fallback stands in (its altar prompt or its
 * enemy-draw line), the same graceful-degradation spirit as [EnemyTokenFace]'s own fallback.
 */
@Composable
internal fun RuinTokenFace(ruin: RuinToken, size: Dp = 96.dp) {
    val context = LocalContext.current
    // Decode once per id and cache across recompositions, matching EnemyTokenFace.
    val bitmap = remember(ruin.id) { loadRuinBitmap(context, ruin.id) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = ruin.id,
            // No clip: the PNG's own transparent hexagon is the shape.
            modifier = Modifier.size(size),
        )
    } else {
        RuinTextFallback(ruin = ruin, size = size)
    }
}

/** Loads `enemy-tokens/<id>.png` for a ruin id (PNG for its baked hexagon alpha), or null if absent. */
private fun loadRuinBitmap(context: Context, id: String): ImageBitmap? = try {
    context.assets.open("enemy-tokens/$id.png").use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}

/** The stand-in shown until a ruin's real art exists: a rounded tile summarising what it is. */
@Composable
private fun RuinTextFallback(ruin: RuinToken, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(RUIN_CORNER))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (ruin.isAltar) "Ancient\nAltar" else "Enemies\n& Treasure",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 2,
            )
            Text(
                // A compact cue: the altar's colours, or the enemy-draw's pile count.
                text = if (ruin.isAltar) ruin.altarColors!!.joinToString("/") { it.shortLabel() }
                else "${ruin.enemyPiles!!.size} enemies",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Slight corner rounding applied to ruin faces - a nod to the hexagon shape without a full hex clip. */
private val RUIN_CORNER = 12.dp
