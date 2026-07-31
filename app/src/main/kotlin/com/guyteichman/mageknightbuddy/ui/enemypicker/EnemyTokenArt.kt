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
import com.guyteichman.mageknightbuddy.domain.TokenPileId

/**
 * Renders an [EnemyToken]'s round token face at [size]. If the token's art has been bundled (an
 * asset at `enemy-tokens/<id>.jpg`, per ADR-0007), it's drawn clipped to a circle; otherwise a
 * readable text fallback stands in (name + Armor/Attack/Fame), the same graceful-degradation spirit
 * as [com.guyteichman.mageknightbuddy.ui.components.KnightShieldIcon] - so the picker stays usable
 * for tokens whose art isn't sourced yet.
 *
 * Token art lives as Android *assets* (not `res/drawable`) because it's a large, id-keyed set that
 * grows token-by-token and is referenced by the catalogue's string id, not a generated R id - so a
 * new token needs no code change, just a file. Base/expansion art is cropped from the TTS Mage
 * Knight mod; Apocalypse Dragon's from its rulebook (see this folder's README).
 */
@Composable
internal fun EnemyTokenFace(token: EnemyToken, size: Dp = 96.dp) {
    val context = LocalContext.current
    // Decode once per token id and cache across recompositions. The images are small (512x512),
    // so decoding on the composition thread is cheap enough to not warrant a background loader/Coil.
    val bitmap = remember(token.id) { loadTokenBitmap(context, token.id) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = token.name,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        TokenTextFallback(token = token, size = size)
    }
}

/** Loads `enemy-tokens/<id>.jpg` from assets, or null if that token's art isn't bundled yet. */
private fun loadTokenBitmap(context: Context, id: String): ImageBitmap? = try {
    // assets.open throws if the file is absent - that's the "no art yet" signal, caught below.
    context.assets.open("enemy-tokens/$id.jpg").use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}

/**
 * Renders a [TokenPileId]'s face-down back art at [size] (issue #198). Round enemy piles are clipped
 * to a circle same as [EnemyTokenFace] - their source crop is a circle centered on a square, so
 * clipping just trims the square's corners. The **RUIN pile is the exception**: ruins are hexagonal,
 * so their back is a transparent-PNG hexagon (like [RuinTokenFace]) and gets **no** clip - the alpha
 * is the shape. Falls back to a colored disc with the pile's name if that pile's back art isn't
 * bundled yet, the same graceful-degradation pattern as [EnemyTokenFace]'s [TokenTextFallback].
 */
@Composable
internal fun PileBackFace(pileId: TokenPileId, size: Dp = 96.dp) {
    val context = LocalContext.current
    val bitmap = remember(pileId) { loadPileBackBitmap(context, pileId) }
    // The RUIN back carries its own hexagon alpha; every other pile's back is a round disc.
    val isHex = pileId == TokenPileId.RUIN

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = pileId.displayName(),
            modifier = if (isHex) Modifier.size(size) else Modifier.size(size).clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = pileId.displayName(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}

/**
 * Loads `enemy-tokens/backs/<pileid>.<ext>` from assets, or null if that pile's back art isn't
 * bundled yet. The RUIN back is a PNG (baked hexagon alpha); the round enemy backs are JPG.
 */
private fun loadPileBackBitmap(context: Context, pileId: TokenPileId): ImageBitmap? = try {
    val ext = if (pileId == TokenPileId.RUIN) "png" else "jpg"
    context.assets.open("enemy-tokens/backs/${pileId.name.lowercase()}.$ext").use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}

/** The stand-in shown until a token's real art exists: a colored disc with its name and stat line. */
@Composable
private fun TokenTextFallback(token: EnemyToken, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = token.name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
            Text(
                // Compact stat line: Armor / topmost Attack / Fame. A summon shows "S" (no value).
                text = run {
                    val attack = token.attacks.firstOrNull()
                    val atk = if (attack?.isSummon == true) "S" else "${attack?.value ?: 0}"
                    "A${token.armor} · $atk⚔ · ${token.fame}✦"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
