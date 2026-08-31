package com.guyteichman.mageknightbuddy.ui.scorecalculator

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.ui.scenarioart.ScenarioArt

/** The rounded shape shared by the collapsed field and the sheet rows, so they read as one family. */
private val PICKER_SHAPE = RoundedCornerShape(12.dp)

/**
 * Cream ink for text/icons laid over the art. ScenarioArt darkens the image with its own scrim, so a
 * light colour stays legible on any scenario's background (the same cream its placeholder uses).
 */
private val OVERLAY_INK = Color(0xFFF3E7D3)

/**
 * The Setup page's Scenario picker (issue #287): an art-forward replacement for the plain
 * [com.guyteichman.mageknightbuddy.ui.components.LabeledDropdown]. The collapsed field shows the
 * current scenario's background art with its name; tapping it opens a modal bottom sheet listing
 * every scenario as an art banner. Only the Scenario picker goes art-forward - the Knight picker
 * stays a LabeledDropdown - because this is where the sourced scenario art (issue #288) lives.
 *
 * Degrades gracefully: [ScenarioArt] draws its bronze placeholder for any scenario without art yet,
 * so this works whether real art is bundled or not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScenarioPickerField(
    selected: Scenario,
    onSelected: (Scenario) -> Unit,
) {
    // Whether the sheet is open - transient UI state, so it lives here rather than in the ViewModel.
    // rememberSaveable keeps it across rotation; a Boolean is Parcelable-safe (see workflow.md's
    // saved-state note), so this survives process death without needing a proxy.
    var showSheet by rememberSaveable { mutableStateOf(false) }

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
        // shape. The trailing BoxScope lambda is ScenarioArt's overlay slot (drawn above the scrim).
        ScenarioArt(
            scenario = selected,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(PICKER_SHAPE)
                .clickable { showSheet = true },
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
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Text(
                "Choose a scenario",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            // The sheet owns its own scroll (a LazyColumn), so there's no nested-scroll fight with
            // the wizard behind it. Sorted by name to match the dropdown it replaces (issue #110);
            // key = id so recomposition is stable as the list re-renders.
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Scenario.entries.sortedBy { it.displayName }, key = { it.id }) { scenario ->
                    ScenarioSheetRow(
                        scenario = scenario,
                        isSelected = scenario == selected,
                        onClick = {
                            onSelected(scenario)
                            showSheet = false
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
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(PICKER_SHAPE)
            .clickable { onClick() }
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
