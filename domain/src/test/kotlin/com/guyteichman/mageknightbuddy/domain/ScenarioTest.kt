package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ScenarioTest {

    @Test
    fun `SoloConquest displayName is Conquest, not the rulebook's Solo Conquest`() {
        // App is solo-only, so "Solo" is redundant in every UI spot displayName appears (issue #102).
        // The stable id is untouched, so persisted sessions still resolve correctly.
        assertEquals("Conquest", Scenario.SoloConquest.displayName)
        assertEquals("solo_conquest", Scenario.SoloConquest.id)
    }

    @Test
    fun `fromId looks up a Scenario by its stable id`() {
        assertEquals(Scenario.SoloConquest, Scenario.fromId("solo_conquest"))
        assertEquals(Scenario.FirstReconnaissance, Scenario.fromId("first_reconnaissance"))
        assertEquals(Scenario.ForTheCouncil, Scenario.fromId("for_the_council"))
        assertEquals(Scenario.HiddenValley, Scenario.fromId("hidden_valley"))
        assertEquals(Scenario.RealmOfTheDead, Scenario.fromId("realm_of_the_dead"))
        assertEquals(Scenario.AgainstTheDragon, Scenario.fromId("against_the_dragon"))
        assertEquals(Scenario.AgainstTheHorsemen, Scenario.fromId("against_the_horsemen"))
        assertEquals(Scenario.ApocalypseIsHere, Scenario.fromId("apocalypse_is_here"))
        assertEquals(Scenario.FracturedLands, Scenario.fromId("the_fractured_lands"))
        assertEquals(Scenario.LifeAndDeath, Scenario.fromId("life_and_death"))
        assertEquals(Scenario.LostRelic, Scenario.fromId("lost_relic"))
        assertEquals(Scenario.AgainstTheApocalypse, Scenario.fromId("against_the_apocalypse"))
        assertEquals(Scenario.SoloConquestChallenge, Scenario.fromId("solo_conquest_challenge"))
        assertEquals(Scenario.VolkaresQuest, Scenario.fromId("volkares_quest"))
        assertEquals(Scenario.VolkaresReturn, Scenario.fromId("volkares_return"))
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
                Scenario.AgainstTheDragon,
                Scenario.AgainstTheHorsemen,
                Scenario.ApocalypseIsHere,
                Scenario.FracturedLands,
                Scenario.LifeAndDeath,
                Scenario.LostRelic,
                Scenario.AgainstTheApocalypse,
                Scenario.SoloConquestChallenge,
                Scenario.VolkaresQuest,
                Scenario.VolkaresReturn,
            ),
            Scenario.entries,
        )
    }
}
