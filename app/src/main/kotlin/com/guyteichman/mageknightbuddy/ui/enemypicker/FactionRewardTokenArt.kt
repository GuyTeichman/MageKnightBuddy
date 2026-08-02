package com.guyteichman.mageknightbuddy.ui.enemypicker

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.FactionRewardToken

/**
 * Renders a [FactionRewardToken]'s face at [size] - the faction reward piles' counterpart to
 * [EnemyTokenFace]/[RuinTokenFace]. A reward token's face is a **square icon tile** (its effect drawn
 * pictographically across the whole tile, e.g. Healing Herbs = a hand / pencil / map), so it's drawn
 * as a lightly rounded rectangle with **no circle clip** - clipping to a circle the way enemy faces
 * are would cut the edge icons off. Their round emblem *backs* instead go through [PileBackFace] like
 * every other pile back (see this file's asset README). Until a token's art is bundled at
 * `faction-reward-tokens/<id>.jpg`, a readable name tile stands in, the same graceful-degradation
 * spirit as [EnemyTokenFace]'s fallback.
 */
@Composable
internal fun FactionRewardTokenFace(token: FactionRewardToken, size: Dp = 96.dp) {
    val context = LocalContext.current
    // Decode once per id and cache across recompositions, matching EnemyTokenFace/RuinTokenFace.
    val bitmap = remember(token.id) { loadRewardBitmap(context, token.id) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = token.name,
            // A square tile (lightly rounded), never a circle - the printed art fills the square - with
            // a thin black outline for consistency with the ruin art's own outline.
            modifier = Modifier.size(size).clip(RewardTokenShape).border(RewardTokenBorder, Color.Black, RewardTokenShape),
        )
    } else {
        RewardTextFallback(token = token, size = size)
    }
}

/** Loads `faction-reward-tokens/<id>.jpg` from assets, or null if that token's art isn't bundled yet. */
private fun loadRewardBitmap(context: Context, id: String): ImageBitmap? = try {
    context.assets.open("faction-reward-tokens/$id.jpg").use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}

/** The stand-in shown until a reward token's real art exists: a rounded tile with its name. */
@Composable
private fun RewardTextFallback(token: FactionRewardToken, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RewardTokenShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(RewardTokenBorder, Color.Black, RewardTokenShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = token.name,
            modifier = Modifier.fillMaxSize().padding(6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/**
 * Shape shared by a faction reward token's square face tile *and* its pile back (see [PileBackFace] in
 * EnemyTokenArt.kt): a lightly-rounded **square**, never a circle, so the tile reads square like the
 * printed art. `internal` so the pile-back renderer in the same package can reuse it for consistency.
 */
internal val RewardTokenShape = RoundedCornerShape(8.dp)

/** Width of the thin black outline drawn around reward token art (face + back), matching the ruin art's outline. */
internal val RewardTokenBorder = 1.dp
