package com.guyteichman.mageknightbuddy.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Text/icon "ink" for a colored pill. Deliberately near-black / near-white rather than pure
// #000000 / #FFFFFF, to soften the contrast a touch, and deliberately theme-independent literals
// (not MaterialTheme colors): the pill fills these sit on don't change between light and dark
// theme, so the ink on top mustn't either - see [pillContentColor].
private val PillInkDark = Color(0xFF1A1A1A)
private val PillInkLight = Color(0xFFF5F5F5)

// The relative-luminance point where black and white ink give equal contrast against a fill
// (WCAG: (L+0.05)/0.05 == 1.05/(L+0.05) solves to L ~= 0.179). Above it, dark ink is more legible;
// below it, light ink. Using this crossover rather than a naive 0.5 midpoint is what keeps *dark*
// ink on the gold "hardest difficulty" pill (luminance ~0.38) - the exact color issue #172 was about.
private const val PILL_INK_LUMINANCE_CROSSOVER = 0.179f

/**
 * A legible text/icon color for content drawn on top of [background]. Picks dark ink on lighter
 * fills and light ink on darker ones, keyed off the fill's own relative luminance so it stays
 * readable on any caller-supplied fill.
 *
 * Exists because [LabelPillPicker] paints arbitrary, theme-independent fills (e.g. the white->gold
 * difficulty gradient) but its `Text`/`Icon` would otherwise inherit the theme's `LocalContentColor`
 * - which is light in dark mode and vanished against those light fills (issue #172). Because the
 * fills don't change with the theme, this result doesn't either.
 */
internal fun pillContentColor(background: Color): Color =
    if (background.luminance() > PILL_INK_LUMINANCE_CROSSOVER) PillInkDark else PillInkLight
