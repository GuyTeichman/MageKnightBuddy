package com.guyteichman.mageknightbuddy.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/**
 * The settings gear, meant to sit in a `TopAppBar`'s `actions` slot (or any header row). It's a
 * single shared composable so every tab reaches the Settings screen the same way without a
 * dedicated bottom-nav tab - see docs/design/architecture.md's "Tab roadmap"/Settings note.
 * [onClick] navigates to the Settings destination.
 */
@Composable
fun SettingsAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.Settings, contentDescription = "Settings")
    }
}
