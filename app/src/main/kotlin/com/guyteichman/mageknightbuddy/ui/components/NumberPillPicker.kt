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
import androidx.compose.ui.unit.dp

/**
 * Picker for a small, anonymous, fixed-ceiling count (e.g. "how many of the 4 Horsemen did you
 * defeat?") - a horizontal row of tappable pills, one per value in [range]. Unlike
 * [ReputationTrackPicker], there's no per-value identity or ordering worth reading top-to-bottom
 * here, just "which number", so a compact horizontal row (built on Material3's own
 * `SegmentedButton`) reads better than a vertical list. Not meant for large ranges - if a future
 * field has too many values to fit a phone screen horizontally, that one should get its own
 * picker shape instead of forcing this one wider.
 */
@Composable
internal fun NumberPillPicker(label: String, range: IntRange, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            range.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = range.count()),
                ) {
                    Text(value.toString())
                }
            }
        }
    }
}
