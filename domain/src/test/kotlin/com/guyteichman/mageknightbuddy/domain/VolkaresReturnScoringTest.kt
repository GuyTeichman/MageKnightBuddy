package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class VolkaresReturnScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, City Conquered bonus, and Volkare Combat Bonus`() {
        val input = minimalInput(
            fame = 25,
            standardAchievements = StandardAchievements(
                spellsInDeck = 2,
                advancedActionsInDeck = 0,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            cityConquered = true,
            volkareDefeated = true,
            combatLevel = CombatLevel.HEROIC,
            raceLevel = RaceLevel.TIGHT,
            cardsRemainingInVolkareDeck = 6,
        )

        // 25 fame + 4 knowledge (2 spells * 2) + 20 city conquered
        // + Volkare combat bonus: (40 base + 6*2 cards) * 1.5 race multiplier = 78
        // total = 25 + 4 + 20 + 78 = 127
        assertEquals(127, VolkaresReturnScoring.score(input))
    }

    @Test
    fun `breakdown lists all 9 scoring rules and sums to the same total as score`() {
        val input = minimalInput(
            fame = 25,
            standardAchievements = StandardAchievements(
                spellsInDeck = 2,
                advancedActionsInDeck = 0,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            cityConquered = true,
            volkareDefeated = true,
            combatLevel = CombatLevel.HEROIC,
            raceLevel = RaceLevel.TIGHT,
            cardsRemainingInVolkareDeck = 6,
        )

        val breakdown = VolkaresReturnScoring.breakdown(input)

        assertEquals(9, breakdown.size)
        assertEquals(127, breakdown.sumOf { it.value })
        assertEquals(25, breakdown.single { it.label == "Fame" }.value)
        assertEquals(4, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(20, breakdown.single { it.label == "City Conquered" }.value)
        assertEquals(78, breakdown.single { it.label == "Volkare Combat Bonus" }.value)
    }

    @Test
    fun `City Conquered bonus is 0 when the city was not conquered`() {
        val input = minimalInput(fame = 10, cityConquered = false, volkareDefeated = false)

        assertEquals(10, VolkaresReturnScoring.score(input))
        assertEquals(0, VolkaresReturnScoring.breakdown(input).single { it.label == "City Conquered" }.value)
    }

    @Test
    fun `Volkare Combat Bonus is 0 when Volkare was not defeated, regardless of level or cards left`() {
        val input = minimalInput(
            fame = 0,
            cityConquered = false,
            volkareDefeated = false,
            combatLevel = CombatLevel.LEGENDARY,
            raceLevel = RaceLevel.THRILLING,
            cardsRemainingInVolkareDeck = 10,
        )

        // Without defeating Volkare there's no combat bonus at all, even though Legendary +
        // Thrilling + 10 cards would otherwise be a large bonus - the multiplier never applies.
        assertEquals(0, VolkaresReturnScoring.score(input))
    }

    @Test
    fun `Volkare Combat Bonus base value is 30, 40, or 50 for Daring, Heroic, or Legendary Combat Level`() {
        val daring = minimalInput(volkareDefeated = true, combatLevel = CombatLevel.DARING, raceLevel = RaceLevel.FAIR)
        val heroic = minimalInput(volkareDefeated = true, combatLevel = CombatLevel.HEROIC, raceLevel = RaceLevel.FAIR)
        val legendary =
            minimalInput(volkareDefeated = true, combatLevel = CombatLevel.LEGENDARY, raceLevel = RaceLevel.FAIR)

        assertEquals(30, VolkaresReturnScoring.score(daring))
        assertEquals(40, VolkaresReturnScoring.score(heroic))
        assertEquals(50, VolkaresReturnScoring.score(legendary))
    }

    @Test
    fun `Volkare Combat Bonus gains 2 points per card still left in Volkare's deck, before the race multiplier`() {
        val input = minimalInput(
            volkareDefeated = true,
            combatLevel = CombatLevel.DARING,
            raceLevel = RaceLevel.FAIR,
            cardsRemainingInVolkareDeck = 7,
        )

        // (30 base + 7*2 cards) * 1 (Fair multiplier) = 44
        assertEquals(44, VolkaresReturnScoring.score(input))
    }

    @Test
    fun `Race Level multiplies only the Volkare Combat Bonus by 1, 1point5, or 2 for Fair, Tight, or Thrilling`() {
        val fair =
            minimalInput(volkareDefeated = true, combatLevel = CombatLevel.DARING, raceLevel = RaceLevel.FAIR)
        val tight =
            minimalInput(volkareDefeated = true, combatLevel = CombatLevel.DARING, raceLevel = RaceLevel.TIGHT)
        val thrilling =
            minimalInput(volkareDefeated = true, combatLevel = CombatLevel.DARING, raceLevel = RaceLevel.THRILLING)

        // Daring base bonus is 30, with 0 cards left: 30*1, 30*1.5, 30*2.
        assertEquals(30, VolkaresReturnScoring.score(fair))
        assertEquals(45, VolkaresReturnScoring.score(tight))
        assertEquals(60, VolkaresReturnScoring.score(thrilling))
    }

    @Test
    fun `outcome is Won when Volkare was defeated, even if the city was never conquered`() {
        val input = minimalInput(volkareDefeated = true, cityConquered = false)

        assertEquals(Outcome.WON, VolkaresReturnScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when Volkare was not defeated, even if the city was conquered`() {
        val input = minimalInput(volkareDefeated = false, cityConquered = true)

        assertEquals(Outcome.LOST, VolkaresReturnScoring.outcome(input))
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
        cityConquered: Boolean = false,
        volkareDefeated: Boolean,
        combatLevel: CombatLevel = CombatLevel.DARING,
        raceLevel: RaceLevel = RaceLevel.FAIR,
        cardsRemainingInVolkareDeck: Int = 0,
    ) = VolkaresReturnScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        cityConquered = cityConquered,
        volkareDefeated = volkareDefeated,
        combatLevel = combatLevel,
        raceLevel = raceLevel,
        cardsRemainingInVolkareDeck = cardsRemainingInVolkareDeck,
    )
}
