package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProxyPlayerCardTest {

    @Test
    fun `BasicAction has a movement bonus of 1 and matches only its own color`() {
        val card = ProxyPlayerCard.BasicAction(CardColor.RED)

        assertEquals(1, card.movementBonus)
        assertTrue(card.matches(CardColor.RED))
        assertFalse(card.matches(CardColor.GREEN))
    }

    @Test
    fun `UniqueAction has a movement bonus of 2 despite being a Basic Action`() {
        val card = ProxyPlayerCard.UniqueAction(CardColor.BLUE)

        assertEquals(2, card.movementBonus)
        assertTrue(card.matches(CardColor.BLUE))
    }

    @Test
    fun `AdvancedAction has a movement bonus of 2 and defers matching to its CardIdentity`() {
        val singleColor = ProxyPlayerCard.AdvancedAction(CardIdentity.SingleColor(CardColor.WHITE))
        val dualColor = ProxyPlayerCard.AdvancedAction(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE))

        assertEquals(2, singleColor.movementBonus)
        assertTrue(singleColor.matches(CardColor.WHITE))
        assertEquals(2, dualColor.movementBonus)
        assertTrue(dualColor.matches(CardColor.GREEN))
        assertTrue(dualColor.matches(CardColor.BLUE))
        assertFalse(dualColor.matches(CardColor.RED))
    }

    @Test
    fun `matchingCrystalCount delegates to each card kind's own color(s)`() {
        val crystals = mapOf(CardColor.RED to 2, CardColor.GREEN to 1, CardColor.BLUE to 1, CardColor.WHITE to 0)

        assertEquals(2, ProxyPlayerCard.BasicAction(CardColor.RED).matchingCrystalCount(crystals))
        assertEquals(0, ProxyPlayerCard.UniqueAction(CardColor.WHITE).matchingCrystalCount(crystals))
        // Green=1, Blue=1 - the higher of the two (not their sum, which would be 2) is 1.
        assertEquals(
            1,
            ProxyPlayerCard.AdvancedAction(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)).matchingCrystalCount(crystals),
        )
    }
}
