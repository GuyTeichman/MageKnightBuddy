package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * One row of the Greatest Leader page: a Unit level's healthy/wounded counts side by side. The
 * ViewModel stores these as four separate per-level properties rather than a list, so each level
 * gets its own row composable call.
 *
 * @param woundedImeAction the soft keyboard's action key for the *second* field of the row - the
 *   "Healthy" field always shows Next (its neighbour is right there), while "Wounded" shows Next
 *   on every row but the last one on the page, which shows Done instead (issue #173).
 */
@Composable
internal fun UnitLevelRow(
    level: Int,
    healthy: String,
    onHealthyChange: (String) -> Unit,
    wounded: String,
    onWoundedChange: (String) -> Unit,
    woundedImeAction: ImeAction = ImeAction.Next,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Level $level", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            // Both cells reuse NumberField (same digits-only filtering and IME-action wiring as
            // every other numeric input); `Modifier.weight(1f)` is passed in to override its
            // full-width default so the two share the row evenly.
            NumberField(
                label = "Healthy",
                value = healthy,
                onValueChange = onHealthyChange,
                modifier = Modifier.weight(1f),
                imeAction = ImeAction.Next,
            )
            NumberField(
                label = "Wounded",
                value = wounded,
                onValueChange = onWoundedChange,
                modifier = Modifier.weight(1f),
                imeAction = woundedImeAction,
            )
        }
    }
}
