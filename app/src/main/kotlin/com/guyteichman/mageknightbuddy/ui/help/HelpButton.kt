package com.guyteichman.mageknightbuddy.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

/**
 * The shared "?" icon button for referencing rulebook content in-app: tapping it opens a dialog
 * listing the beginner-friendly rule text (and rulebook citation) for every entry in [keys] that
 * has a matching [FieldHelp] in [fieldHelp]. Originally a private helper inside the Score
 * Calculator wizard, extracted here (issue #88) so it's a genuinely reusable affordance any screen
 * can wire up instead of growing its own copy of this dialog - see
 * [com.guyteichman.mageknightbuddy.ui.scorecalculator.ScoreCalculatorScreen] (per-wizard-page help)
 * and [com.guyteichman.mageknightbuddy.ui.dummyplayer]'s End Round dialog for the two current call
 * sites.
 */
@Composable
internal fun HelpButton(keys: List<String>, fieldHelp: Map<String, FieldHelp>) {
    var showHelp by remember { mutableStateOf(false) }

    IconButton(onClick = { showHelp = true }) {
        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Rule details")
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Rule details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    keys.forEach { key ->
                        // `map[key]?.let { }` is a safe-call + scope function combo: if the key
                        // has no entry in fieldHelp, `fieldHelp[key]` is null and the `?.` short-
                        // circuits, so nothing renders for that key instead of crashing.
                        fieldHelp[key]?.let { help ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(help.text)
                                Text(
                                    help.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("Close")
                }
            },
        )
    }
}
