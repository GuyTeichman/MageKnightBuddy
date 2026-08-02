package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * [pillContentColor] keeps [LabelPillPicker]'s text/icon legible on the always-light,
 * theme-independent fills its callers pass. Issue #172: in dark mode the pills inherited the
 * theme's light `LocalContentColor`, so the text vanished against those light fills. These cases
 * pin the crossover where the helper flips between dark and light ink - in particular the gold
 * "hardest difficulty" pill, whose relative luminance (~0.38) sits below a naive 0.5 midpoint yet
 * still needs *dark* ink, so a 0.5 threshold would regress exactly the color the issue is about.
 */
class PillContentColorTest {

    // The actual fills LabelPillPicker is used with today - see DifficultyPillColor (white->gold
    // difficulty gradient) and VolkareScreen's Scenario picker (flat light gray).
    private val white = Color(0xFFFFFFFF) // easiest difficulty pill / general light fill
    private val gold = Color(0xFFC9A227) // hardest difficulty pill
    private val lightGray = Color(0xFFE0E0E0) // Scenario picker's flat, uncolored pill

    @Test
    fun `light fills get dark ink`() {
        for (fill in listOf(white, gold, lightGray)) {
            // luminance() is Compose's relative-luminance (0=black, 1=white); dark ink sits well below it.
            assertTrue(
                pillContentColor(fill).luminance() < 0.3f,
                "expected dark ink on light fill $fill",
            )
        }
    }

    @Test
    fun `dark fills get light ink`() {
        for (fill in listOf(Color(0xFF1A1A1A), Color(0xFF102A54), Color.Black)) {
            assertTrue(
                pillContentColor(fill).luminance() > 0.7f,
                "expected light ink on dark fill $fill",
            )
        }
    }
}
