package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.CardColor

/** "Red"/"Green"/"Blue"/"White" - [CardColor]'s enum name, capitalized for display. */
internal val CardColor.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

/**
 * The app's canonical Red/Green/Blue/White palette for [CardColor], shared by every place a card
 * color needs a swatch (the Dummy Player tab's deck/crystal icons, the Score Calculator's
 * color-coded checkboxes).
 */
internal val CardColor.swatch: Color
    get() = when (this) {
        CardColor.RED -> Color(0xFFB5423A)
        CardColor.GREEN -> Color(0xFF3E7C4A)
        CardColor.BLUE -> Color(0xFF3768A6)
        CardColor.WHITE -> Color(0xFFFBFAFF)
    }

/** A small colored square used as a compact per-color legend marker. [size] is the square's edge. */
@Composable
internal fun CardColorDot(color: CardColor, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(color.swatch)
            .then(if (color == CardColor.WHITE) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp)) else Modifier),
    )
}

/**
 * A small diamond standing in for one crystal in the Inventory, sized to fill exactly a [size] x
 * [size] box - same as [CardColorDot] - so the two read as the same size given the same [size].
 * Drawn as an explicit [DiamondShape] rather than a rotated square: a rotated square's rendered
 * pixels extend past its own layout bounds (rotate() doesn't change reported layout size), which
 * left it up to whatever container this sits in (e.g. FilterChip's icon slot) whether that
 * overflow is visible or clipped - too fragile to guarantee matching [CardColorDot] reliably.
 */
@Composable
internal fun CrystalIcon(color: CardColor, size: Dp = 17.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(DiamondShape)
            .background(color.swatch)
            .then(if (color == CardColor.WHITE) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, DiamondShape) else Modifier),
    )
}

/** A diamond/rhombus shape filling its full bounding box - the [CrystalIcon] glyph. */
internal val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}
