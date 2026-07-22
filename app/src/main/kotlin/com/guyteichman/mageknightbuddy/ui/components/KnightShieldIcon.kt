package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.R
import com.guyteichman.mageknightbuddy.domain.Knight

/**
 * A Knight's shield-token art at [size], or a generic shield glyph for any Knight whose art
 * hasn't been sourced yet (currently just Coral - see issue #69). [Image] renders the drawable's
 * actual pixels (the shield icon), unlike [Icon], which is meant for single-color glyphs - so
 * [tint] only affects the fallback glyph, never the real art. Defaults to [LocalContentColor] so
 * the fallback matches whatever color plain [Icon]s already use at its call site (e.g. a picker's
 * leading icons); callers with a different prior tint pass it explicitly.
 *
 * Shared by the Dummy Player tab's Knight picker (issue #69) and the Score Calculator's Setup
 * page Knight picker (issue #111) - moved here once the second consumer needed it, following the
 * same extract-on-second-use pattern as [CardColorDot]/[CrystalIcon].
 */
@Composable
internal fun KnightShieldIcon(knight: Knight, size: Dp = 24.dp, tint: Color = LocalContentColor.current) {
    val resId = knight.shieldIconRes
    if (resId != null) {
        Image(painter = painterResource(resId), contentDescription = null, modifier = Modifier.size(size))
    } else {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}

/** Maps a [Knight] to its shield-token drawable, or null where the art hasn't been sourced yet. */
internal val Knight.shieldIconRes: Int?
    get() = when (this) {
        Knight.ARYTHEA -> R.drawable.arythea_shield
        Knight.TOVAK -> R.drawable.tovak_shield
        Knight.KRANG -> R.drawable.krang_shield
        Knight.BRAEVALAR -> R.drawable.braevalar_shield
        Knight.WOLFHAWK -> R.drawable.wolfhawk_shield
        Knight.GOLDYX -> R.drawable.goldyx_shield
        Knight.NOROWAS -> R.drawable.norowas_shield
        Knight.CORAL -> null
    }
