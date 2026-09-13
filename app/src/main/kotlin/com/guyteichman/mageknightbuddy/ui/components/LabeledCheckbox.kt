package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A checkbox with its label (and an optional [leadingIcon], e.g. a color swatch) as a single
 * clickable row, for simple yes/no wizard fields (e.g. a city being conquered). `Modifier
 * .toggleable` on the [Row] - not a click handler on the [Checkbox] - owns the tap and merges
 * the row into one accessibility node with a checkbox role, so the inner [Checkbox]'s own
 * `onCheckedChange` is `null`: it only renders the current state, it doesn't handle the tap.
 */
@Composable
internal fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // onCheckedChange = null makes this Checkbox purely visual (non-interactive on its own) -
        // the Row's toggleable modifier above is what actually handles taps for the whole row.
        Checkbox(checked = checked, onCheckedChange = null)
        leadingIcon?.invoke()
        Text(label)
    }
}
