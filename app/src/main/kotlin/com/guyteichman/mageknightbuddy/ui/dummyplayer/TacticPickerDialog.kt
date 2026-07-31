package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.guyteichman.mageknightbuddy.domain.TacticState

/**
 * The per-round Tactic Card draft popup shared by the Dummy Player, Proxy Player, and (issue
 * #222) Volkare AI screens - a grid of the 6 numbered Tactic cards for the active Day/Night pile
 * ([isDay]), greying out cards already removed this round-type or already picked
 * ([TacticState.unavailableCards]) and badging [aiLabel]'s own pick so it reads clearly on screen
 * (issue #179's original ask) rather than just fading into "unavailable" the way a removed card
 * does.
 *
 * Deliberately not dismissible: [onDismissRequest] is a no-op and there's no dismiss button. A
 * real game round can't proceed until both picks are made, and the calling screen only renders
 * this dialog while [TacticState.playerPick] or [TacticState.dummyPick] is still null - there's
 * nothing sensible to fall back to if the player dismissed it instead.
 *
 * [enabled] disables taps while a pick is mid-autosave (the caller's `viewModel.isBusy`) or while
 * it isn't the player's turn to pick at all (their own pick is already in, or pick order has the
 * AI going first and it hasn't drawn yet) - callers compute this, this composable just renders it.
 */
@Composable
internal fun TacticPickerDialog(
    isDay: Boolean,
    tacticState: TacticState,
    aiLabel: String,
    enabled: Boolean,
    onPickPlayer: (Int) -> Unit,
) {
    val unavailable = tacticState.unavailableCards(isDay)
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(fraction = 0.94f),
        title = { Text(if (isDay) "Day Tactics" else "Night Tactics") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (tacticState.playerPick == null) {
                        "Pick your Tactic card."
                    } else {
                        "Waiting for $aiLabel's pick…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                // Fixed(3), not Adaptive like EnemyPickerScreen's TokenGridDialog: the Tactic pile
                // is always exactly 6 cards, so a fixed 3x2 layout is simpler than sizing cells to
                // fill whatever width happens to be available.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(6) { index ->
                        val card = index + 1
                        TacticGridCell(
                            card = card,
                            aiLabel = aiLabel,
                            isRemoved = card in tacticState.removedCards(isDay),
                            isDummyPick = card == tacticState.dummyPick,
                            isPlayerPick = card == tacticState.playerPick,
                            clickable = enabled && card !in unavailable,
                            onClick = { onPickPlayer(card) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun TacticGridCell(
    card: Int,
    aiLabel: String,
    isRemoved: Boolean,
    isDummyPick: Boolean,
    isPlayerPick: Boolean,
    clickable: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                isPlayerPick -> MaterialTheme.colorScheme.primaryContainer
                isDummyPick -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .size(56.dp)
                // Only a plain removal fades the whole cell - a pick (by either side) stays at
                // full opacity so its badge below stays legible instead of reading as "unavailable"
                // for the same reason a removed card is.
                .alpha(if (isRemoved) 0.5f else 1f)
                .let { if (clickable) it.clickable(onClick = onClick) else it },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(text = card.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        // A fixed-height label slot under every cell, not just the picked ones - keeps every
        // cell's card square aligned to the same row height whether or not it has a badge.
        Box(modifier = Modifier.height(16.dp)) {
            when {
                isDummyPick -> Text(aiLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                isPlayerPick -> Text("YOU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
