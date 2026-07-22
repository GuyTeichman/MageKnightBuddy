package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * A read-only text field that opens a dropdown of `options` when tapped (used for the Setup
 * page's Scenario and Knight pickers). Generic over `T` so it works for any enum-like option
 * type; `displayName` supplies the human-readable label for each option. `leadingIcon` is
 * optional (defaults to none, e.g. the Scenario picker) - callers that want a per-option glyph
 * (e.g. the Knight picker's shield art, issue #111) pass one and it's rendered both on the
 * closed field (for the currently `selected` option) and on every option row in the open menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> LabeledDropdown(
    label: String,
    options: List<T>,
    selected: T,
    displayName: (T) -> String,
    onSelected: (T) -> Unit,
    leadingIcon: (@Composable (T) -> Unit)? = null,
) {
    // Whether the dropdown is currently open - local UI state, not a wizard field, so plain
    // `remember` (not the ViewModel) is the right home for it.
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayName(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            // `?.let { ... }` here is not a null-guard on leadingIcon's *value* - it's building a
            // new zero-arg composable lambda `{ it(selected) }` (the shape OutlinedTextField's own
            // leadingIcon parameter expects), only when a leadingIcon was actually supplied. When
            // absent, this whole expression is null and OutlinedTextField renders no leading icon.
            leadingIcon = leadingIcon?.let { { it(selected) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayName(option)) },
                    leadingIcon = leadingIcon?.let { { it(option) } },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
