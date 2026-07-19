package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class FirstReconnaissanceScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and scenario bonuses when the city was revealed`() {
        val input = minimalInput(
            fame = 20,
            standardAchievements = StandardAchievements(
                spellsInDeck = 1,
                advancedActionsInDeck = 0,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            cityRevealed = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        // 20 fame + 2 knowledge (1 spell) + 10 city revealed + 30 rounds + 3 dummy cards + 5 end-of-round = 70
        assertEquals(70, FirstReconnaissanceScoring.score(input))
        assertEquals(Outcome.WON, FirstReconnaissanceScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the city was never revealed, even with zero bonuses`() {
        val input = minimalInput(cityRevealed = false)

        assertEquals(0, FirstReconnaissanceScoring.score(input))
        assertEquals(Outcome.LOST, FirstReconnaissanceScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the city was not revealed even if the score is otherwise positive`() {
        val input = minimalInput(
            fame = 10,
            cityRevealed = false,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 2,
            endOfRoundAnnounced = true,
        )

        // 10 fame + 0 city bonus + 30 rounds + 2 dummy cards = 42, but the city was never found
        assertEquals(42, FirstReconnaissanceScoring.score(input))
        assertEquals(Outcome.LOST, FirstReconnaissanceScoring.outcome(input))
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
        cityRevealed: Boolean,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
    ) = FirstReconnaissanceScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        cityRevealed = cityRevealed,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
