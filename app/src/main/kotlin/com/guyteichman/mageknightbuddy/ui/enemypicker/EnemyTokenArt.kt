package com.guyteichman.mageknightbuddy.ui.enemypicker

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.EnemyToken

/**
 * Renders an [EnemyToken]'s round token face at [size]. Follows the same graceful-degradation
 * pattern as [com.guyteichman.mageknightbuddy.ui.components.KnightShieldIcon]: if the token's art
 * has been sourced (a drawable named after its [EnemyToken.id], per ADR-0007), it's drawn with
 * [Image]; otherwise a readable text fallback stands in (name + Armor/Attack/Fame), so the picker
 * is fully usable before the token art is bundled.
 */
@Composable
internal fun EnemyTokenFace(token: EnemyToken, size: Dp = 96.dp) {
    val resId = enemyTokenDrawableRes(token.id)
    if (resId != null) {
        Image(
            painter = painterResource(resId),
            contentDescription = token.name,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        TokenTextFallback(token = token, size = size)
    }
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
                // Compact stat line: Armor / topmost Attack / Fame.
                text = "A${token.armor} · ${token.attacks.firstOrNull()?.value ?: 0}⚔ · ${token.fame}✦",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Maps a token [id] to its bundled art drawable, or null where the art hasn't been sourced yet -
 * exactly [com.guyteichman.mageknightbuddy.ui.components.Knight.shieldIconRes]'s pattern. Returns
 * null for every token today (token art is a separate follow-up: extracted from the TTS Mage Knight
 * mod for base/expansion tokens, and from the Apocalypse Dragon rulebook for its own). Each `when`
 * arm added here as art arrives points an id at its `R.drawable.enemy_<id>` asset.
 */
@Suppress("UNUSED_PARAMETER")
private fun enemyTokenDrawableRes(id: String): Int? = null
