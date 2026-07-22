package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VolkaresQuestScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, cities conquered, and the Volkare combat bonus when Volkare was defeated`() {
        val input = minimalInput(
            fame = 40,
            standardAchievements = StandardAchievements(
                spellsInDeck = 1,
                advancedActionsInDeck = 0,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 1,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            citiesConquered = 2,
            combatLevel = CombatLevel.HEROIC,
            raceLevel = RaceLevel.TIGHT,
            volkareDefeated = true,
            cardsRemainingInVolkaresDeck = 5,
        )

        // 40 fame + 4 achievements (2 knowledge + 2 loot) + 10 cities (2*5)
        // + Volkare combat bonus: (40 Heroic base + 2*5 cards) * 3/2 Tight multiplier = 50*1.5 = 75
        // total = 40 + 4 + 10 + 75 = 129
        assertEquals(129, VolkaresQuestScoring.score(input))
    }

    @Test
    fun `breakdown lists all 9 scoring rules and sums to the same total as score`() {
        val input = minimalInput(
            fame = 40,
            standardAchievements = StandardAchievements(
                spellsInDeck = 1,
                advancedActionsInDeck = 0,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 1,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            citiesConquered = 2,
            combatLevel = CombatLevel.HEROIC,
            raceLevel = RaceLevel.TIGHT,
            volkareDefeated = true,
            cardsRemainingInVolkaresDeck = 5,
        )

        val breakdown = VolkaresQuestScoring.breakdown(input)

        assertEquals(9, breakdown.size)
        assertEquals(129, breakdown.sumOf { it.value })
        assertEquals(40, breakdown.single { it.label == "Fame" }.value)
        assertEquals(2, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(2, breakdown.single { it.label == "Greatest Loot" }.value)
        assertEquals(10, breakdown.single { it.label == "Cities Conquered" }.value)
        assertEquals(75, breakdown.single { it.label == "Volkare Combat Bonus" }.value)
    }

    @Test
    fun `score has no Volkare combat bonus when Volkare was not defeated, even with cards remaining and a difficulty set`() {
        val input = minimalInput(
            fame = 20,
            citiesConquered = 1,
            combatLevel = CombatLevel.LEGENDARY,
            raceLevel = RaceLevel.THRILLING,
            volkareDefeated = false,
            cardsRemainingInVolkaresDeck = 10,
        )

        // 20 fame + 0 achievements + 5 cities (1*5) + 0 Volkare combat bonus (not defeated) = 25
        assertEquals(25, VolkaresQuestScoring.score(input))
        assertEquals(0, VolkaresQuestScoring.breakdown(input).single { it.label == "Volkare Combat Bonus" }.value)
    }

    @Test
    fun `Volkare combat bonus multiplier scales only the combat-bonus subtotal, not Fame, Achievements, or cities`() {
        // Fixed non-bonus total: 10 fame + 5 cities (1*5) = 15, held constant across all three cases.
        // Combat Level Daring's base bonus is 30, with 0 cards remaining so the subtotal is exactly 30.
        val fairInput = minimalInput(
            fame = 10,
            citiesConquered = 1,
            combatLevel = CombatLevel.DARING,
            raceLevel = RaceLevel.FAIR,
            volkareDefeated = true,
            cardsRemainingInVolkaresDeck = 0,
        )
        val tightInput = fairInput.copy(raceLevel = RaceLevel.TIGHT)
        val thrillingInput = fairInput.copy(raceLevel = RaceLevel.THRILLING)

        // Fair: 30 * 1 = 30 -> 15 + 30 = 45
        assertEquals(45, VolkaresQuestScoring.score(fairInput))
        // Tight: 30 * 1.5 = 45 -> 15 + 45 = 60
        assertEquals(60, VolkaresQuestScoring.score(tightInput))
        // Thrilling: 30 * 2 = 60 -> 15 + 60 = 75
        assertEquals(75, VolkaresQuestScoring.score(thrillingInput))
    }

    @Test
    fun `outcome is Won when Volkare was defeated`() {
        val input = minimalInput(volkareDefeated = true)

        assertEquals(Outcome.WON, VolkaresQuestScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when Volkare was not defeated`() {
        val input = minimalInput(volkareDefeated = false)

        assertEquals(Outcome.LOST, VolkaresQuestScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost even with a positive score, since defeating Volkare is the only win condition`() {
        val input = minimalInput(
            fame = 50,
            citiesConquered = 2,
            volkareDefeated = false,
        )

        // 50 fame + 0 achievements + 10 cities (2*5) + 0 Volkare combat bonus = 60, still a Loss
        assertEquals(60, VolkaresQuestScoring.score(input))
        assertEquals(Outcome.LOST, VolkaresQuestScoring.outcome(input))
    }

    @Test
    fun `citiesConquered above the 2-city total is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(citiesConquered = 3, volkareDefeated = false)
        }
    }

    @Test
    fun `negative citiesConquered is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(citiesConquered = -1, volkareDefeated = false)
        }
    }

    private fun minimalInput(
        fame: Int = 0,
        standardAchievements: StandardAchievements = StandardAchievements(
            spellsInDeck = 0,
            advancedActionsInDeck = 0,
            units = emptyList(),
            shieldsOnAdventureSites = 0,
            artifacts = 0,
            crystalsInInventory = 0,
            shieldsOnConquerSites = 0,
            woundsInDeck = 0,
        ),
        citiesConquered: Int = 0,
        combatLevel: CombatLevel = CombatLevel.DARING,
        raceLevel: RaceLevel = RaceLevel.FAIR,
        volkareDefeated: Boolean,
        cardsRemainingInVolkaresDeck: Int = 0,
    ) = VolkaresQuestScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        citiesConquered = citiesConquered,
        combatLevel = combatLevel,
        raceLevel = raceLevel,
        volkareDefeated = volkareDefeated,
        cardsRemainingInVolkaresDeck = cardsRemainingInVolkaresDeck,
    )
}
