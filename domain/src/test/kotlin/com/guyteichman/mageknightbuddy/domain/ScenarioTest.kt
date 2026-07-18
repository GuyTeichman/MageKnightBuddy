package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ScenarioTest {

    @Test
    fun `fromId looks up a Scenario by its stable id`() {
        assertEquals(Scenario.SoloConquest, Scenario.fromId("solo_conquest"))
    }

    @Test
    fun `entries lists every known Scenario`() {
        assertEquals(listOf(Scenario.SoloConquest), Scenario.entries)
    }
}
