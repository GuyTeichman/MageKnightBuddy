package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ScenarioTest {

    @Test
    fun `fromId looks up a Scenario by its stable id`() {
        assertEquals(Scenario.SoloConquest, Scenario.fromId("solo_conquest"))
        assertEquals(Scenario.FirstReconnaissance, Scenario.fromId("first_reconnaissance"))
        assertEquals(Scenario.ForTheCouncil, Scenario.fromId("for_the_council"))
        assertEquals(Scenario.HiddenValley, Scenario.fromId("hidden_valley"))
        assertEquals(Scenario.RealmOfTheDead, Scenario.fromId("realm_of_the_dead"))
    }

    @Test
    fun `entries lists every known Scenario`() {
        assertEquals(
            listOf(
                Scenario.SoloConquest,
                Scenario.FirstReconnaissance,
                Scenario.ForTheCouncil,
                Scenario.HiddenValley,
                Scenario.RealmOfTheDead,
            ),
            Scenario.entries,
        )
    }
}
