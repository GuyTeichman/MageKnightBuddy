package com.guyteichman.mageknightbuddy.ui.scenarioart

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.Scenario
import kotlinx.coroutines.launch

/** The rounded shape shared by the collapsed field and the sheet rows, so they read as one family. */
private val PICKER_SHAPE = RoundedCornerShape(12.dp)

/**
 * Cream ink for text/icons laid over the art. ScenarioArt darkens the image with its own scrim, so a
 * light colour stays legible on any scenario's background (the same cream its placeholder uses).
 */
private val OVERLAY_INK = Color(0xFFF3E7D3)

/**
 * An art-forward Scenario picker (issue #287): a replacement for a plain
 * [com.guyteichman.mageknightbuddy.ui.components.LabeledDropdown] wherever a [Scenario] is chosen.
 * The collapsed field shows the selected scenario's background art with its name; tapping it opens a
 * modal bottom sheet listing [options] as art banners. Lives in `scenarioart` (not a single feature
 * package) because it's used from several screens: the Score Calculator Setup page and the Dummy
 * Player setup's scenario pickers (the coop Tactic-Scenario and Volkare Return/Quest fields).
 *
 * @param selected the scenario currently chosen (its art fills the collapsed banner).
 * @param onSelected called with the picked scenario when a sheet row is tapped.
 * @param options the scenarios offered in the sheet, in the order given (callers pass their own
 *   subset/order - e.g. only the coop scenarios, or just Volkare's two). Defaults to all scenarios
 *   sorted by name, matching the dropdown this replaced (issue #110).
 *
 * Degrades gracefully: [ScenarioArt] draws its bronze placeholder for any scenario without art yet,
 * so this works whether real art is bundled or not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScenarioPickerField(
    selected: Scenario,
    onSelected: (Scenario) -> Unit,
    options: List<Scenario> = Scenario.entries.sortedBy { it.displayName },
) {
    // Whether the sheet is open - transient UI state, so it lives here rather than in the ViewModel.
    // rememberSaveable keeps it across rotation; a Boolean is Parcelable-safe (see workflow.md's
    // saved-state note), so this survives process death without needing a proxy.
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    // A scope tied to this composition, used to play the sheet's hide animation before removing it
    // from composition (see dismissSheet) rather than letting `showSheet = false` snap it shut.
    val scope = rememberCoroutineScope()

    // Animate the sheet closed, then drop it from composition once it's fully hidden.
    fun dismissSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) showSheet = false }
    }

    // Label-above-control layout, matching LabelPillPicker so this field lines up with the wizard's
    // other Setup fields (spacedBy(4.dp) between the label and the banner).
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Scenario",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The collapsed field: the current scenario's art as a short banner with its name and a
        // dropdown chevron overlaid. clip() before clickable() bounds the tap ripple to the rounded
        // shape; the same shape is handed to ScenarioArt so the art clip can't drift from it. The
        // trailing BoxScope lambda is ScenarioArt's overlay slot (drawn above the scrim); the
        // button role + onClickLabel tell TalkBack this opens the picker.
        ScenarioArt(
            scenario = selected,
            shape = PICKER_SHAPE,
            border = ScenarioArtFrame,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(PICKER_SHAPE)
                .clickable(onClickLabel = "Choose a scenario", role = Role.Button) { showSheet = true },
        ) {
            Text(
                text = selected.displayName,
                color = OVERLAY_INK,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                // end padding leaves room for the chevron so a long name doesn't run under it.
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp, end = 48.dp),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = OVERLAY_INK,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            Text(
                "Choose a scenario",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            // The sheet owns its own scroll (a LazyColumn), so there's no nested-scroll fight with
            // whatever's behind it. Rows follow the caller's [options] order (the default is sorted
            // by name to match the dropdown this replaced, issue #110); key = id so recomposition is
            // stable as the list re-renders.
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(options, key = { it.id }) { scenario ->
                    ScenarioSheetRow(
                        scenario = scenario,
                        isSelected = scenario == selected,
                        onClick = {
                            onSelected(scenario)
                            dismissSheet()
                        },
                    )
                }
            }
        }
    }
}

/**
 * One scenario row in the picker sheet: a full-width art banner with the scenario's name, marked
 * with a check and a primary-coloured border when it's the current pick.
 */
@Composable
private fun ScenarioSheetRow(scenario: Scenario, isSelected: Boolean, onClick: () -> Unit) {
    ScenarioArt(
        scenario = scenario,
        shape = PICKER_SHAPE,
        border = ScenarioArtFrame,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(PICKER_SHAPE)
            .clickable(onClickLabel = "Select scenario", role = Role.Button, onClick = onClick)
            // A primary border ringing the card marks the current pick; unselected rows get none.
            // `.then(...)` conditionally chains the border only when selected (Modifier is the no-op).
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, PICKER_SHAPE)
                } else {
                    Modifier
                },
            ),
    ) {
        Text(
            text = scenario.displayName,
            color = OVERLAY_INK,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp, end = 48.dp),
        )
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = OVERLAY_INK,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            )
        }
    }
}
