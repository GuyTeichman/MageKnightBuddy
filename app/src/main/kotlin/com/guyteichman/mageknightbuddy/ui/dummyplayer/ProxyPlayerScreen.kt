package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution
import com.guyteichman.mageknightbuddy.ui.components.CardColorDot
import com.guyteichman.mageknightbuddy.ui.components.CrystalIcon
import com.guyteichman.mageknightbuddy.ui.components.LabeledCheckbox
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.help.HelpButton
import kotlinx.coroutines.launch

/**
 * Human-readable label for a [ProxyPlayerCard], distinguishing the 3 kinds that matter for the
 * movement-point bonus - a generic Basic Action, a Knight's own Unique Basic Action Card, or an
 * Advanced Action (single- or dual-color) - see [ProxyPlayerCard.movementBonus].
 */
private fun ProxyPlayerCard.displayText(): String = when (this) {
    is ProxyPlayerCard.BasicAction -> "${color.name.lowercase().replaceFirstChar { it.uppercase() }} (Basic Action)"
    is ProxyPlayerCard.UniqueAction -> "${color.name.lowercase().replaceFirstChar { it.uppercase() }} (Unique)"
    is ProxyPlayerCard.AdvancedAction -> when (val id = identity) {
        is CardIdentity.SingleColor -> "${id.color.name.lowercase().replaceFirstChar { it.uppercase() }} (Advanced Action)"
        is CardIdentity.DualColor -> "${id.colorA.name.lowercase().replaceFirstChar { it.uppercase() }}/${id.colorB.name.lowercase().replaceFirstChar { it.uppercase() }} (Advanced Action)"
    }
}

/**
 * Proxy Player mode's AI (turn/round) screen: shows the current Objective Card (or a prompt to
 * play a turn, before the first one) via [displayText], its Shield count, the computed
 * movement-point total (see [com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession.movementPoints]),
 * and 3 actions - Play Turn, and (only once there's a current objective) Explored/Completed to
 * resolve it - plus an End Round dialog matching `DummyPlayerScreen.kt`'s `DummyPlayerAiScreen`
 * shape. Per docs/rules/proxy-player.md's app-scope note: this screen never decides *where* the
 * Hero moves or *what* gets conquered - only the movement-point number and Objective Card/Shield
 * bookkeeping. Not `private`: called from `DummyPlayerScreen.kt`'s `DummyPlayerTab`, the same
 * cross-file relationship `VolkareScreen.kt`'s `VolkareAiScreen` has with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyPlayerAiScreen(repository: ProxyPlayerSessionRepository, fieldHelp: Map<String, FieldHelp>, onBack: () -> Unit) {
    val viewModel: ProxyPlayerAiViewModel = viewModel(factory = ProxyPlayerAiViewModel.factory(repository))
    val scope = rememberCoroutineScope()
    val session = viewModel.session
    // The player's per-turn report of whether a matching-color (or gold, by day) mana die
    // currently sits in the Source - see docs/rules/proxy-player.md's "Movement points". A plain
    // per-composition remember (not saved across process death) is fine here: it's a fresh report
    // made anew each turn, not state that needs to survive beyond this screen being on-screen.
    var hasMatchingManaDie by remember { mutableStateOf(false) }
    var showEndRoundDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proxy Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (session == null) {
            // Restoring from Room is asynchronous (see ProxyPlayerAiViewModel's init block); this
            // only shows for the brief window before that first restore completes.
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Round ${session.round}", style = MaterialTheme.typography.titleMedium)

                // A local val, not repeated session.objectiveCard reads: ProxyPlayerCard is
                // declared in the domain module, so Kotlin can't smart-cast a nullable property
                // read from a different module across two statements - capturing it once here
                // gives the compiler (and this code) one non-null local to work with instead.
                val objectiveCard = session.objectiveCard
                if (objectiveCard == null) {
                    Text("No current objective - tap Play Turn to draw one.")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Objective: ${objectiveCard.displayText()}")
                        HelpButton(keys = listOf("Proxy Player Objective"), fieldHelp = fieldHelp)
                    }
                    Text("Shields: ${session.objectiveShields}")

                    LabeledCheckbox(
                        checked = hasMatchingManaDie,
                        onCheckedChange = { hasMatchingManaDie = it },
                        label = "Matching mana die in the Source",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Movement points: ${session.movementPoints(hasMatchingManaDie)}")
                        HelpButton(keys = listOf("Proxy Player Movement"), fieldHelp = fieldHelp)
                    }
                }

                Button(
                    onClick = { scope.launch { viewModel.playTurn() } },
                    enabled = !viewModel.isBusy && !session.roundEnded,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Play Turn")
                }

                if (objectiveCard != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { scope.launch { viewModel.resolveObjective(ProxyPlayerObjectiveResolution.EXPLORED) } },
                            enabled = !viewModel.isBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Explored")
                        }
                        OutlinedButton(
                            onClick = { scope.launch { viewModel.resolveObjective(ProxyPlayerObjectiveResolution.COMPLETED) } },
                            enabled = !viewModel.isBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Completed")
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showEndRoundDialog = true },
                    enabled = !viewModel.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("End Round")
                }
            }
        }
    }

    if (showEndRoundDialog) {
        ProxyPlayerEndRoundDialog(
            fieldHelp = fieldHelp,
            onDismiss = { showEndRoundDialog = false },
            onConfirm = { advancedActionOfferColor, spellOfferColor ->
                scope.launch {
                    viewModel.endRound(advancedActionOfferColor, spellOfferColor)
                    showEndRoundDialog = false
                }
            },
        )
    }
}

/**
 * The End Round dialog: a Proxy Player-mode copy of `DummyPlayerScreen.kt`'s file-private
 * `EndRoundDialog` (same fields - Advanced Action offer color, optionally dual-color via a
 * checkbox + second-color picker, and Spell offer color - since [ProxyPlayerAiViewModel.endRound]
 * takes the exact same `(CardIdentity, CardColor)` shape as the standard Dummy Player's `endRound`
 * does). Duplicated rather than shared directly because the two AI screens' ViewModels are
 * distinct types with their own `onConfirm` call sites - matching how `VolkareScreen.kt` keeps its
 * own copies of `RoundChip`/`MiniCard`/`LogRow` instead of reaching into `DummyPlayerScreen.kt`'s
 * private ones.
 */
@Composable
private fun ProxyPlayerEndRoundDialog(
    fieldHelp: Map<String, FieldHelp>,
    onDismiss: () -> Unit,
    onConfirm: (advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor) -> Unit,
) {
    var advancedActionColor by remember { mutableStateOf(CardColor.entries.first()) }
    var spellColor by remember { mutableStateOf(CardColor.entries.first()) }
    // Whether the Advanced Action offer's card is one of the 4 Dual-Color cards - unchecked by
    // default, since most Advanced Action cards are single-color.
    var isDualColor by remember { mutableStateOf(false) }
    // The card's 2nd color; only read when isDualColor is checked. Defaults to a color distinct
    // from advancedActionColor's initial value - CardIdentity.DualColor's init block rejects
    // colorA == colorB, so this default (plus the "Second color" picker excluding
    // advancedActionColor's current value below, and the LaunchedEffect keeping them apart if
    // advancedActionColor changes afterward) keeps that combination unreachable through this UI.
    var secondColor by remember { mutableStateOf(CardColor.entries[1]) }

    // LaunchedEffect re-runs its block whenever its key (advancedActionColor) changes. If the
    // player picks an Advanced Action offer color that now matches the already-picked second
    // color, bump secondColor to the next distinct color instead of leaving an invalid same-color
    // pair selected (the "Second color" picker's excluded param stops the reverse case - picking
    // a second color equal to the current advancedActionColor - from being selectable at all).
    LaunchedEffect(advancedActionColor) {
        if (secondColor == advancedActionColor) {
            secondColor = CardColor.entries.first { it != advancedActionColor }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        // usePlatformDefaultWidth = false + an explicit fillMaxWidth lets the dialog use most of
        // the screen's width instead of Material's narrow default - same as the standard Dummy
        // Player's End Round dialog.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(fraction = 0.94f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("End Round", modifier = Modifier.weight(1f))
                HelpButton(keys = listOf("Round-Prep Offers"), fieldHelp = fieldHelp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Pick the color removed from each offer during round-prep.",
                    style = MaterialTheme.typography.bodySmall,
                )
                ColorPickerRow(
                    label = "Advanced Action offer",
                    selected = advancedActionColor,
                    onSelect = { advancedActionColor = it },
                    // A card of this color is what's added to the deck - CardColorDot fits. Both
                    // chipIcons here pass the same `size` (12.dp) so the square and the diamond
                    // read as the same size in the chip, not just the same nominal Dp value.
                    chipIcon = { color -> CardColorDot(color = color, size = 12.dp) },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDualColor, onCheckedChange = { isDualColor = it })
                    Text("Dual-color card", style = MaterialTheme.typography.bodySmall)
                }
                if (isDualColor) {
                    ColorPickerRow(
                        label = "Second color",
                        selected = secondColor,
                        onSelect = { secondColor = it },
                        chipIcon = { color -> CardColorDot(color = color, size = 12.dp) },
                        // Excludes whatever the Advanced Action offer picker currently holds, so a
                        // same-color DualColor pair can't be selected via the chips in the first place.
                        excluded = advancedActionColor,
                    )
                }
                ColorPickerRow(
                    label = "Spell offer",
                    selected = spellColor,
                    onSelect = { spellColor = it },
                    // A crystal of this color is what's granted, not a card - CrystalIcon fits better.
                    chipIcon = { color -> CrystalIcon(color = color, size = 12.dp) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Assemble the CardIdentity onConfirm expects from the checkbox + 1-2 color picks.
                val advancedActionOfferColor = if (isDualColor) {
                    CardIdentity.DualColor(advancedActionColor, secondColor)
                } else {
                    CardIdentity.SingleColor(advancedActionColor)
                }
                onConfirm(advancedActionOfferColor, spellColor)
            }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
