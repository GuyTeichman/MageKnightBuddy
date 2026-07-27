package com.guyteichman.mageknightbuddy.ui.enemypicker

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.EnemyToken

/**
 * Renders an [EnemyToken]'s round token face at [size]. Follows the same graceful-degradation
 * spirit as [com.guyteichman.mageknightbuddy.ui.components.KnightShieldIcon]: until a token's real
 * art is bundled, a readable text fallback stands in (name + Armor/Attack/Fame), so the picker is
 * fully usable before any art exists.
 *
 * Token art itself is a separate follow-up (per ADR-0007 it will live as Android *assets* in
 * `app/src/main/assets/enemy-tokens/`, named after each [EnemyToken.id], sourced from the TTS Mage
 * Knight mod for base/expansion tokens and the Apocalypse Dragon rulebook for its own). Rendering an
 * asset bitmap in Compose needs a small loader that isn't worth adding until the art lands, so this
 * currently always shows the fallback.
 */
@Composable
internal fun EnemyTokenFace(token: EnemyToken, size: Dp = 96.dp) {
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
