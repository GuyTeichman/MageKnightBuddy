package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Picker for a small, named set of options (e.g. a difficulty level) - a horizontal row of
 * tappable pills, one per entry in [options], generic over any option type `T`. Deliberately not
 * sharing an implementation with [NumberPillPicker] (a plain `0..N` count picker) even though the
 * two look similar - each is independently simple, and there was no real need to unify them under
 * one abstraction. [color] lets the caller tint each pill (e.g. a white-to-gold gradient to read
 * as increasing difficulty); pass the same color for every option to leave pills uncolored.
 *
 * Hand-built from plain [Surface]s (same technique [ReputationTrackPicker] already uses, just
 * horizontal instead of vertical) rather than Material3's `SegmentedButton` - `SegmentedButton`
 * blends its own selected-state color/elevation treatment on top of any custom container color
 * passed to it, which muddies a caller-supplied gradient like this one's. A plain `Surface` paints
 * exactly the color it's given, with no hidden state layer to fight.
 */
@Composable
internal fun <T> LabelPillPicker(
    label: String,
    options: List<T>,
    selected: T,
    displayName: (T) -> String,
    color: (T) -> Color,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                // Rounded only on the row's outer edges (first pill's start, last pill's end),
                // straight where pills meet in the middle - the classic segmented-control look.
                val shape = RoundedCornerShape(
                    topStart = if (index == 0) 50.dp else 0.dp,
                    bottomStart = if (index == 0) 50.dp else 0.dp,
                    topEnd = if (index == options.lastIndex) 50.dp else 0.dp,
                    bottomEnd = if (index == options.lastIndex) 50.dp else 0.dp,
                )
                Surface(
                    onClick = { onSelect(option) },
                    shape = shape,
                    color = color(option),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        }
                        Text(displayName(option))
                    }
                }
            }
        }
    }
}
