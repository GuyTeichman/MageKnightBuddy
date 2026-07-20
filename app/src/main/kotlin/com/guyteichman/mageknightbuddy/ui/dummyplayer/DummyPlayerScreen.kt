package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlinx.coroutines.launch

private const val DUMMY_PLAYER_SETUP_ROUTE = "dummy_player_setup"
private const val DUMMY_PLAYER_AI_ROUTE = "dummy_player_ai"

/**
 * Root composable for the Dummy Player tab: the Knight-select setup screen is the tab's start
 * destination, with the AI (turn/round) screen pushed on top of it once a session is started or
 * restored. Runs its own nested [NavHost], the same pattern [com.guyteichman.mageknightbuddy.ui.scoreboard.ScoreboardTab]
 * uses, so this tab's own back stack (setup vs. AI screen) is independent of the app's top-level
 * tab switching.
 */
@Composable
fun DummyPlayerTab(repository: DummyPlayerSessionRepository) {
    val nestedNavController = rememberNavController()

    NavHost(navController = nestedNavController, startDestination = DUMMY_PLAYER_SETUP_ROUTE) {
        composable(DUMMY_PLAYER_SETUP_ROUTE) {
            DummyPlayerSetupScreen(
                repository = repository,
                // Both Start and Restore Game land on the same AI-screen route - once a session
                // exists (freshly started or restored), the AI screen just loads whatever's saved.
                onStart = { nestedNavController.navigate(DUMMY_PLAYER_AI_ROUTE) },
                onRestore = { nestedNavController.navigate(DUMMY_PLAYER_AI_ROUTE) },
            )
        }
        composable(DUMMY_PLAYER_AI_ROUTE) {
            // Placeholder until the AI screen itself is implemented (issue #35). Back is a plain
            // pop with no confirmation - autosave means nothing is ever unsaved (per #27).
            DummyPlayerAiScreenPlaceholder(onBack = { nestedNavController.popBackStack() })
        }
    }
}

/**
 * The Knight-select setup screen: pick a Knight (or "Random"), then either Start a fresh session
 * or Restore a previously saved one.
 */
@Composable
private fun DummyPlayerSetupScreen(
    repository: DummyPlayerSessionRepository,
    onStart: () -> Unit,
    onRestore: () -> Unit,
) {
    val viewModel: DummyPlayerSetupViewModel = viewModel(factory = DummyPlayerSetupViewModel.factory(repository))
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Dummy Player")

        KnightPicker(
            knight = viewModel.knight,
            wasRandom = viewModel.wasRandom,
            onKnightSelected = viewModel::pickKnight,
            onRandomSelected = viewModel::pickRandom,
        )

        Button(
            onClick = {
                // Building the session and autosaving it is a suspend call (Room I/O), so it
                // needs a coroutine scope; onStart() only runs after that save completes.
                scope.launch {
                    viewModel.start()
                    onStart()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start")
        }

        OutlinedButton(
            onClick = onRestore,
            enabled = viewModel.hasSavedSession,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Restore Game")
        }
    }
}

/**
 * The Knight dropdown: "Random" is an entry alongside the 8 Knights, not a separate control.
 * Picking "Random" resolves immediately (see [DummyPlayerSetupViewModel.pickRandom]), so the
 * field then shows the rolled Knight with a "(Random)" suffix rather than staying on "Random".
 *
 * Every entry gets a leading shield icon; per-Knight shield art hasn't been sourced/imported yet
 * (see issue #30), so a generic shield glyph stands in until that's a separate follow-up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnightPicker(
    knight: Knight,
    wasRandom: Boolean,
    onKnightSelected: (Knight) -> Unit,
    onRandomSelected: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (wasRandom) "${knight.displayName} (Random)" else knight.displayName

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Knight") },
            leadingIcon = { Icon(Icons.Filled.Shield, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Random") },
                onClick = {
                    onRandomSelected()
                    expanded = false
                },
            )
            Knight.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    leadingIcon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                    onClick = {
                        onKnightSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DummyPlayerAiScreenPlaceholder(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dummy Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("AI screen - coming soon (issue #35)")
        }
    }
}
