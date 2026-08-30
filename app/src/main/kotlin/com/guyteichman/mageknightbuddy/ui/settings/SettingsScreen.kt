package com.guyteichman.mageknightbuddy.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import java.time.LocalDate

/** Public GitHub repository, opened by the "View source on GitHub" link in the About section. */
private const val GITHUB_URL = "https://github.com/GuyTeichman/MageKnightBuddy"

/** The author's Buy Me A Coffee page, opened by the "Buy me a coffee" link (issue #104). */
private const val BUY_ME_A_COFFEE_URL = "https://www.buymeacoffee.com/guyteichman"

/**
 * App-icon art sources, credited in the About section. Both are released under CC0 (public domain),
 * so attribution is not legally required - these links are a courtesy to the original creators.
 */
private const val ICON_DRAGON_URL = "https://freesvg.org/dragon-tribal-style-tattoo-vector-illustration"
private const val ICON_SHIELD_URL = "https://svgsilh.com/image/294573.html"

/**
 * The Settings screen (issue #121). Holds Backup & Restore plus an About section with source/support
 * links (issue #104) and art credits for the app icon; the other deferred Settings items (expansion
 * toggles, help-citation visibility)
 * stay out of scope for now (see docs/design/architecture.md). Reached via the shared [SettingsAction]
 * gear on each tab, pushed as a full-screen destination outside the bottom-nav graph; [onBack] pops
 * back to the tab.
 *
 * Backup/restore use the Storage Access Framework (ADR-0009): the launchers below open the system
 * file picker so the user chooses where the JSON snapshot goes (Google Drive, local, etc.) with no
 * Google APIs or sign-in. All logic lives in [SettingsViewModel]; this composable is just wiring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: ScoringSessionRepository,
    onBack: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(repository))
    // collectAsState turns the ViewModel's StateFlow into Compose state: `uiState` reads like a
    // plain value but recomposes this screen whenever a new state is emitted.
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Compose's built-in way to open a URL in the user's browser: openUri fires the platform's
    // ACTION_VIEW handling for us and copes with there being no browser, so the About links below
    // don't need to build an Intent by hand.
    val uriHandler = LocalUriHandler.current

    // SAF "create document": launches the system save-file picker with a suggested name; the
    // callback gets the chosen Uri (null if the user backed out) and hands it to the ViewModel.
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::backUpTo) }

    // SAF "open document": launches the system open-file picker. "*/*" so a backup stored by a
    // provider that mis-tags its MIME type is still selectable - the file's content is validated by
    // BackupCodec.decode either way, so a wrong pick just yields a friendly error, never a crash.
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::prepareRestoreFrom) }

    // Whenever the ViewModel raises a one-shot message, show it once and then clear it so it doesn't
    // re-fire on the next recomposition. keyed on the message so each distinct message shows once.
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message.text)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
            Text(
                "Save your scored games to a file (in Google Drive or anywhere else), and restore " +
                    "them later on this or another device. Only finished games are included, not " +
                    "in-progress sessions.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedButton(
                onClick = { backupLauncher.launch("mageknightbuddy-backup-${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Text("  Back up to a file")
            }

            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Text("  Restore from a file")
            }

            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "MageKnightBuddy is a free, open-source companion app.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedButton(
                onClick = { uriHandler.openUri(GITHUB_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Code, contentDescription = null)
                Text("  View source on GitHub")
            }

            OutlinedButton(
                onClick = { uriHandler.openUri(BUY_ME_A_COFFEE_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Coffee, contentDescription = null)
                Text("  Buy me a coffee")
            }

            Text("Credits", style = MaterialTheme.typography.titleMedium)
            Text(
                "The app icon combines a tribal dragon and a heater-shield silhouette, both released " +
                    "under CC0 (public domain). Credit isn't required, but thanks to their creators:",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedButton(
                onClick = { uriHandler.openUri(ICON_DRAGON_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Brush, contentDescription = null)
                Text("  Dragon art — OpenClipart (FreeSVG)")
            }

            OutlinedButton(
                onClick = { uriHandler.openUri(ICON_SHIELD_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Brush, contentDescription = null)
                Text("  Shield silhouette — svgsilh")
            }
        }
    }

    // Confirm before a restore overwrites anything - shown only once the picked file has decoded
    // cleanly (see SettingsViewModel.prepareRestoreFrom), with the exact counts being swapped.
    uiState.restorePrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestore() },
            title = { Text("Restore from backup?") },
            text = {
                Text(
                    "This will replace all ${prompt.localCount} saved " +
                        "${if (prompt.localCount == 1) "game" else "games"} on this device with the " +
                        "${prompt.backupCount} from the backup. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRestore() }) { Text("Cancel") }
            },
        )
    }
}
