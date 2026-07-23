package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardIdentityTest {

    @Test
    fun `DualColor rejects being constructed with the same color twice`() {
        // A DualColor card with colorA == colorB would silently double-count that one color's
        // crystals in matchingCrystalCount - this must never be a constructible value.
        assertFailsWith<IllegalArgumentException> {
            CardIdentity.DualColor(CardColor.RED, CardColor.RED)
        }
    }

    @Test
    fun `SingleColor matches only its own color`() {
        val card = CardIdentity.SingleColor(CardColor.RED)

        assertTrue(card.matches(CardColor.RED))
        assertFalse(card.matches(CardColor.GREEN))
    }

    @Test
    fun `DualColor matches either of its two colors`() {
        val card = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)

        assertTrue(card.matches(CardColor.GREEN))
        assertTrue(card.matches(CardColor.BLUE))
        assertFalse(card.matches(CardColor.RED))
    }

    @Test
    fun `matchingCrystalCount for SingleColor is that color's crystal count`() {
        val card = CardIdentity.SingleColor(CardColor.RED)
        val crystals = mapOf(CardColor.RED to 2, CardColor.GREEN to 0, CardColor.BLUE to 1, CardColor.WHITE to 0)

        assertEquals(2, card.matchingCrystalCount(crystals))
    }

    @Test
    fun `matchingCrystalCount for DualColor is the higher of its two colors' crystal counts`() {
        val card = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)
        val crystals = mapOf(CardColor.RED to 0, CardColor.GREEN to 2, CardColor.BLUE to 1, CardColor.WHITE to 0)

        assertEquals(2, card.matchingCrystalCount(crystals))
    }

    @Test
    fun `DUAL_COLOR_CARDS contains exactly the 4 real dual-color cards`() {
        assertEquals(
            listOf(
                CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
                CardIdentity.DualColor(CardColor.BLUE, CardColor.WHITE),
                CardIdentity.DualColor(CardColor.RED, CardColor.WHITE),
                CardIdentity.DualColor(CardColor.RED, CardColor.GREEN),
            ),
            CardIdentity.DUAL_COLOR_CARDS,
        )
    }

    @Test
    fun `objectiveLabel for SingleColor delegates straight to that color's objectiveLabel`() {
        assertEquals(CardColor.WHITE.objectiveLabel, CardIdentity.SingleColor(CardColor.WHITE).objectiveLabel)
    }

    @Test
    fun `objectiveLabel for a non-Blue DualColor pair joins both colors' labels with 'or', in declared color order`() {
        assertEquals(
            "Conquer Fortified Site or Interact",
            CardIdentity.DualColor(CardColor.RED, CardColor.WHITE).objectiveLabel,
        )
        assertEquals(
            "Conquer Fortified Site or Conquer Adventure Site",
            CardIdentity.DualColor(CardColor.GREEN, CardColor.RED).objectiveLabel, // reversed input order
        )
    }

    @Test
    fun `objectiveLabel for any DualColor pair containing Blue is Blue's own label alone, not a joined string`() {
        assertEquals(
            CardColor.BLUE.objectiveLabel,
            CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE).objectiveLabel,
        )
        assertEquals(
            CardColor.BLUE.objectiveLabel,
            CardIdentity.DualColor(CardColor.BLUE, CardColor.WHITE).objectiveLabel,
        )
    }
}
