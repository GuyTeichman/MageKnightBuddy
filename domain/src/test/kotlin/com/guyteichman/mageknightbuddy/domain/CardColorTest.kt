package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CardColorTest {

    @Test
    fun `objectiveLabel names each color's Proxy Player movement target verbatim from the rulebook`() {
        assertEquals("Conquer Adventure Site", CardColor.GREEN.objectiveLabel)
        assertEquals("Conquer Fortified Site", CardColor.RED.objectiveLabel)
        assertEquals("Interact", CardColor.WHITE.objectiveLabel)
        assertEquals("Advance (closest site further from portal)", CardColor.BLUE.objectiveLabel)
    }
}
