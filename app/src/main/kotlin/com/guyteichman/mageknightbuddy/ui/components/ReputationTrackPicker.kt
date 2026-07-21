package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace

/**
 * Which physical space on the Reputation track the player's Shield token sits on, shown as a
 * vertical list of all 10 spaces - a phone screen is much taller than it is wide, and a vertical
 * list is a reasonable stand-in for the physical track's curved fan shape. Negative spaces are
 * tinted red, positive spaces gold, deepening toward each end, mirroring the real board's own
 * coloring. Each row shows the one number the board actually prints there (see
 * [ReputationTrackSpace] - there's no second "position" number to show, since the game never
 * tracks one), so the player just taps where their token is.
 *
 * Specific to [ReputationTrackSpace] on purpose, not generalized into some generic track-picker -
 * see docs/design/architecture.md's Score Calculator flow notes: if a second scenario ever needs
 * a similarly-shaped picker, generalize then, once there are two real call sites to design
 * against instead of one plus a guess.
 */
@Composable
internal fun ReputationTrackPicker(selected: ReputationTrackSpace, onSelect: (ReputationTrackSpace) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Reputation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ReputationTrackSpace.entries.forEach { space ->
                ReputationTrackSpaceRow(space = space, selected = space == selected, onClick = { onSelect(space) })
            }
        }
    }
}

@Composable
private fun ReputationTrackSpaceRow(space: ReputationTrackSpace, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = space.trackColor(),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                space.modifierLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
    }
}

// Deep red toward the negative end, gold toward the positive end - the real board's own Reputation
// track coloring - fading to a neutral surface at the center. Capped well under full saturation
// (maxTint) so body text stays legible on top without per-cell contrast calculation.
private val ReputationNegativeHue = Color(0xFFB3261E)
private val ReputationPositiveHue = Color(0xFFC9A227)
private const val REPUTATION_TRACK_MAX_TINT = 0.35f

@Composable
private fun ReputationTrackSpace.trackColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    // The X space has no modifier - it's the track's most-negative space, so it's tinted as
    // fully as the negative end gets.
    val modifierValue = modifier ?: -5
    val fraction = (kotlin.math.abs(modifierValue) / 5f) * REPUTATION_TRACK_MAX_TINT
    return when {
        modifierValue < 0 -> lerp(surface, ReputationNegativeHue, fraction)
        modifierValue > 0 -> lerp(surface, ReputationPositiveHue, fraction)
        else -> surface
    }
}

// "X" for the track's one end space (matching what's actually printed on the board there - see
// ReputationTrackSpace), otherwise the signed modifier. Explicit "+" for positive values, since a
// plain Int.toString() has no sign and would read as ambiguous next to this same row's negative
// cells.
private val ReputationTrackSpace.modifierLabel: String
    get() = modifier?.let { if (it > 0) "+$it" else it.toString() } ?: "X"
