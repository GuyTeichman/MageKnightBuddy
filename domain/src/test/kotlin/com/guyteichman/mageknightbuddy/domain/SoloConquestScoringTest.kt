package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoloConquestScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and all four scenario bonuses`() {
        val input = SoloConquestScoringInput(
            fame = 50,
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
            citiesConquered = 2,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
            questPoints = 0,
        )

        // 50 fame + 5 achievements (2*2+1) + (2*10 + 15 all-cities) cities
        // + 1*30 rounds + 4 dummy cards + 5 end-of-round-not-announced = 129
        assertEquals(129, SoloConquestScoring.score(input))
    }

    @Test
    fun `breakdown lists all 13 scoring rules and sums to the same total as score`() {
        val input = SoloConquestScoringInput(
            fame = 50,
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
            citiesConquered = 2,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
            questPoints = 0,
        )

        val breakdown = SoloConquestScoring.breakdown(input)

        assertEquals(13, breakdown.size)
        assertEquals(129, breakdown.sumOf { it.value })
        assertEquals(50, breakdown.single { it.label == "Fame" }.value)
        assertEquals(5, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(0, breakdown.single { it.label == "Greatest Leader" }.value)
        assertEquals(20, breakdown.single { it.label == "Cities Conquered" }.value)
        assertEquals(15, breakdown.single { it.label == "All Cities Conquered" }.value)
        assertEquals(30, breakdown.single { it.label == "Rounds Finished Early" }.value)
        assertEquals(4, breakdown.single { it.label == "Dummy Player's Deck" }.value)
        assertEquals(5, breakdown.single { it.label == "End of Round" }.value)
    }

    @Test
    fun `score includes 1 Fame per Quest Point from Greatest Quester`() {
        val input = minimalInput(citiesConquered = 0, questPoints = 6)

        // Greatest Quester: 1 Fame per Quest Point, per docs/rules/solo-scoring-overview.md
        assertEquals(6, SoloConquestScoring.score(input))
    }

    @Test
    fun `outcome is Won when all cities were conquered`() {
        val input = minimalInput(citiesConquered = 2)

        assertEquals(Outcome.WON, SoloConquestScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when not all cities were conquered`() {
        val input = minimalInput(citiesConquered = 1)

        assertEquals(Outcome.LOST, SoloConquestScoring.outcome(input))
    }

    @Test
    fun `citiesConquered above the 2-city total is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(citiesConquered = 3)
        }
    }

    @Test
    fun `negative citiesConquered is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(citiesConquered = -1)
        }
    }

    private fun minimalInput(citiesConquered: Int, questPoints: Int = 0) = SoloConquestScoringInput(
        fame = 0,
        standardAchievements = StandardAchievements(
            spellsInDeck = 0,
            advancedActionsInDeck = 0,
            units = emptyList(),
            shieldsOnAdventureSites = 0,
            artifacts = 0,
            crystalsInInventory = 0,
            shieldsOnConquerSites = 0,
            woundsInDeck = 0,
        ),
        citiesConquered = citiesConquered,
        roundsFinishedEarly = 0,
        cardsRemainingInDummyDeck = 0,
        endOfRoundAnnounced = true,
        questPoints = questPoints,
    )
}
