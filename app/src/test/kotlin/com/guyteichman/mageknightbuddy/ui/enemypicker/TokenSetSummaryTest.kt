package com.guyteichman.mageknightbuddy.ui.enemypicker

import com.guyteichman.mageknightbuddy.domain.Expansion
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [tokenSetSummary] — the label the Enemy Picker's setup summary strip (issue #199) shows.
 * The strip must stay on **one line** for any combination of token sets, so the formatter shows the
 * expansion names while they fit and degrades to a "{count} selected" fallback once they wouldn't.
 * Expected values are derived by hand from the spec and the expansions' display names, not read off
 * the code's own output.
 */
class TokenSetSummaryTest {

    @Test
    fun `a single set shows its name`() {
        assertEquals("Token sets: Base game", tokenSetSummary(setOf(Expansion.BASE)))
    }

    @Test
    fun `two short-named sets show both names in enum order, regardless of insertion order`() {
        val expected = "Token sets: Base game · The Lost Legion"
        assertEquals(expected, tokenSetSummary(setOf(Expansion.BASE, Expansion.LOST_LEGION)))
        // Same result no matter which order the set was built in — the formatter iterates Expansion.entries.
        assertEquals(expected, tokenSetSummary(setOf(Expansion.LOST_LEGION, Expansion.BASE)))
    }

    @Test
    fun `a two-set combo whose names would overflow one line degrades to a count`() {
        // "Base game · Shades of Tezla: Elementalist" is far too long for one phone line, so even at
        // two sets the formatter falls back to the count rather than let the strip wrap.
        assertEquals(
            "Token sets: 2 selected",
            tokenSetSummary(setOf(Expansion.BASE, Expansion.SHADES_OF_TEZLA_ELEMENTALIST)),
        )
    }

    @Test
    fun `all five sets degrade to a count`() {
        assertEquals("Token sets: 5 selected", tokenSetSummary(Expansion.entries.toSet()))
    }

    @Test
    fun `an empty set reads as none`() {
        assertEquals("Token sets: none", tokenSetSummary(emptySet()))
    }
}
