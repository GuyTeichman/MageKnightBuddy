package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = color(option),
                        inactiveContainerColor = color(option).copy(alpha = 0.35f),
                    ),
                ) {
                    Text(displayName(option))
                }
            }
        }
    }
}
