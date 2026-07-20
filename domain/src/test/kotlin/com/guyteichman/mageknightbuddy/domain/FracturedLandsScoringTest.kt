package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FracturedLandsScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and Greatest Quester`() {
        val input = FracturedLandsScoringInput(
            fame = 20,
            standardAchievements = StandardAchievements(
                spellsInDeck = 2,
                advancedActionsInDeck = 1,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            questPoints = 6,
        )

        // 20 fame + 5 achievements (2*2 + 1) + 6 quest points = 31
        assertEquals(31, FracturedLandsScoring.score(input))
    }

    @Test
    fun `breakdown lists all 8 scoring rules and sums to the same total as score`() {
        val input = FracturedLandsScoringInput(
            fame = 20,
            standardAchievements = StandardAchievements(
                spellsInDeck = 2,
                advancedActionsInDeck = 1,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            questPoints = 6,
        )

        val breakdown = FracturedLandsScoring.breakdown(input)

        assertEquals(8, breakdown.size)
        assertEquals(31, breakdown.sumOf { it.value })
        assertEquals(20, breakdown.single { it.label == "Fame" }.value)
        assertEquals(5, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(6, breakdown.single { it.label == "Greatest Quester" }.value)
    }

    @Test
    fun `Greatest Beating subtracts 2 Fame per Wound in deck, same as every other scenario`() {
        val input = minimalInput(
            fame = 10,
            standardAchievements = StandardAchievements(
                spellsInDeck = 0,
                advancedActionsInDeck = 0,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 3,
            ),
        )

        // 10 fame - 6 (2 * 3 wounds) = 4
        assertEquals(4, FracturedLandsScoring.score(input))
    }

    @Test
    fun `outcome is always null, since The Fractured Lands has no win-lose condition`() {
        val wellScoredInput = minimalInput(fame = 100, questPoints = 20)
        val poorlyScoredInput = minimalInput(fame = 0, questPoints = 0)

        // Unlike every other scenario in this app's scope, a high score doesn't imply a Won
        // Outcome here - the rulebook defines no victory condition at all for The Fractured Lands.
        assertNull(FracturedLandsScoring.outcome(wellScoredInput))
        assertNull(FracturedLandsScoring.outcome(poorlyScoredInput))
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
        questPoints: Int = 0,
    ) = FracturedLandsScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        questPoints = questPoints,
    )
}
