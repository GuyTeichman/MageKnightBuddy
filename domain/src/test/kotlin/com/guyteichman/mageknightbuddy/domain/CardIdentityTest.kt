package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardIdentityTest {

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
    fun `matchingCrystalCount for DualColor sums both colors' crystal counts`() {
        val card = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)
        val crystals = mapOf(CardColor.RED to 0, CardColor.GREEN to 2, CardColor.BLUE to 1, CardColor.WHITE to 0)

        assertEquals(3, card.matchingCrystalCount(crystals))
    }
}
