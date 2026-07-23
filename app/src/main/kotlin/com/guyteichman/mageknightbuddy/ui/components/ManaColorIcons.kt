package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.ManaColor

/** "Red"/"Green"/.../"Gold"/"Black" - [ManaColor]'s enum name, capitalized for display. */
internal val ManaColor.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

/**
 * The app's Red/Green/Blue/White/Gold/Black palette for [ManaColor] - a mana die roll, shown in
 * Volkare mode's log whenever a Wound reveal triggers one. The four colors shared with [CardColor]
 * reuse [CardColor.swatch]'s exact values (see that file) so a rolled Red, say, reads as visually
 * identical to a Red card; Gold and Black are new since no [CardColor] card carries either.
 */
internal val ManaColor.swatch: Color
    get() = when (this) {
        ManaColor.RED -> Color(0xFFB5423A)
        ManaColor.GREEN -> Color(0xFF3E7C4A)
        ManaColor.BLUE -> Color(0xFF3768A6)
        ManaColor.WHITE -> Color(0xFFFBFAFF)
        ManaColor.GOLD -> Color(0xFFD4AF37)
        ManaColor.BLACK -> Color(0xFF262626)
    }

/**
 * A small colored square standing in for one face of Volkare's mana die - same size/shape as
 * [CardColorDot], so a mana roll reads consistently alongside card-color dots in the same log
 * entry. White and Black both get an outline border (not just White, unlike [CardColorDot]): Black
 * is otherwise indistinguishable from a dark theme's own background.
 */
@Composable
internal fun ManaColorDot(color: ManaColor, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(color.swatch)
            .then(
                if (color == ManaColor.WHITE || color == ManaColor.BLACK) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                } else {
                    Modifier
                },
            ),
    )
}
