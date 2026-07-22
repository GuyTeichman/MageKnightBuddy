package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// White (easiest) to gold (hardest) - the same gold used by ReputationTrackPicker's positive
// end, for visual consistency across the app's two difficulty-flavored gradients.
private val DifficultyPillStart = Color(0xFFFFFFFF)
private val DifficultyPillEnd = Color(0xFFC9A227)

/**
 * Gradient color for the [ordinal]-th of [total] ordinal difficulty steps, from white (easiest)
 * to gold (hardest). Shared by the Score Calculator's CombatLevel and RaceLevel `LabelPillPicker`s
 * on the Volkare difficulty page, and reused by the Volkare setup screen's difficulty pickers for
 * visual consistency between the two - not tied to either enum type specifically.
 */
internal fun difficultyPillColor(ordinal: Int, total: Int): Color =
    lerp(DifficultyPillStart, DifficultyPillEnd, ordinal / (total - 1).toFloat())
