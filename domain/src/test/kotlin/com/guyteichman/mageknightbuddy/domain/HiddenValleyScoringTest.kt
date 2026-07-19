package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class HiddenValleyScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and scenario bonuses when the High Priestess was defeated`() {
        val input = minimalInput(
            fame = 30,
            standardAchievements = StandardAchievements(
                spellsInDeck = 0,
                advancedActionsInDeck = 0,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 4,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            highPriestessDefeated = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 2,
            endOfRoundAnnounced = false,
        )

        // 30 fame + 2 loot (4 crystals / 2) + 20 defeated + 30 rounds + 2 dummy cards + 5 end-of-round = 89
        assertEquals(89, HiddenValleyScoring.score(input))
        assertEquals(Outcome.WON, HiddenValleyScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the High Priestess was not defeated`() {
        val input = minimalInput(fame = 15, highPriestessDefeated = false)

        assertEquals(15, HiddenValleyScoring.score(input))
        assertEquals(Outcome.LOST, HiddenValleyScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost even with a positive score, since defeating the High Priestess is the only win condition`() {
        val input = minimalInput(
            fame = 40,
            highPriestessDefeated = false,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
        )

        // 40 fame + 0 defeated bonus + 30 rounds + 3 dummy cards = 73
        assertEquals(73, HiddenValleyScoring.score(input))
        assertEquals(Outcome.LOST, HiddenValleyScoring.outcome(input))
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
        highPriestessDefeated: Boolean,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
    ) = HiddenValleyScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        highPriestessDefeated = highPriestessDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
