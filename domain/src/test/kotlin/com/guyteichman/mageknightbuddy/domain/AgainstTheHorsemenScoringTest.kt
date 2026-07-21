package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AgainstTheHorsemenScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and scenario bonuses when not all Horsemen were defeated`() {
        val input = minimalInput(
            fame = 40,
            standardAchievements = StandardAchievements(
                spellsInDeck = 1,
                advancedActionsInDeck = 2,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            horsemenDefeated = 2,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        // 40 fame + 4 achievements (2*1+2) + 2*4 horsemen (no all-four bonus)
        // + 1*30 rounds + 3 dummy cards + 5 end-of-round-not-announced = 90
        assertEquals(90, AgainstTheHorsemenScoring.score(input))
    }

    @Test
    fun `breakdown lists all 12 scoring rules and sums to the same total as score`() {
        val input = minimalInput(
            fame = 40,
            standardAchievements = StandardAchievements(
                spellsInDeck = 1,
                advancedActionsInDeck = 2,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 0,
                crystalsInInventory = 0,
                shieldsOnConquerSites = 0,
                woundsInDeck = 0,
            ),
            horsemenDefeated = 2,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        val breakdown = AgainstTheHorsemenScoring.breakdown(input)

        assertEquals(12, breakdown.size)
        assertEquals(90, breakdown.sumOf { it.value })
        assertEquals(40, breakdown.single { it.label == "Fame" }.value)
        assertEquals(4, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(8, breakdown.single { it.label == "Horsemen Defeated" }.value)
        assertEquals(0, breakdown.single { it.label == "All Horsemen Defeated" }.value)
        assertEquals(30, breakdown.single { it.label == "Rounds Finished Early" }.value)
        assertEquals(3, breakdown.single { it.label == "Dummy Player's Deck" }.value)
        assertEquals(5, breakdown.single { it.label == "End of Round" }.value)
    }

    @Test
    fun `score includes the plus 15 bonus for defeating all four Horsemen`() {
        val input = minimalInput(horsemenDefeated = 4)

        // 4*4 (defeated) + 15 (all four bonus) = 31
        assertEquals(31, AgainstTheHorsemenScoring.score(input))
        assertEquals(31, AgainstTheHorsemenScoring.breakdown(input).single { it.label == "Horsemen Defeated" }.value +
            AgainstTheHorsemenScoring.breakdown(input).single { it.label == "All Horsemen Defeated" }.value)
    }

    @Test
    fun `no all-Horsemen bonus when only three of four were defeated`() {
        val input = minimalInput(horsemenDefeated = 3)

        // 3*4 (defeated), no +15 bonus since one Horseman survived = 12
        assertEquals(12, AgainstTheHorsemenScoring.score(input))
        assertEquals(0, AgainstTheHorsemenScoring.breakdown(input).single { it.label == "All Horsemen Defeated" }.value)
    }

    @Test
    fun `outcome is Won when all four Horsemen were defeated`() {
        val input = minimalInput(horsemenDefeated = 4)

        assertEquals(Outcome.WON, AgainstTheHorsemenScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when fewer than four Horsemen were defeated`() {
        val input = minimalInput(horsemenDefeated = 3)

        assertEquals(Outcome.LOST, AgainstTheHorsemenScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when no Horsemen were defeated`() {
        val input = minimalInput(horsemenDefeated = 0)

        assertEquals(Outcome.LOST, AgainstTheHorsemenScoring.outcome(input))
    }

    @Test
    fun `horsemenDefeated above the 4-Horseman total is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(horsemenDefeated = 5)
        }
    }

    @Test
    fun `negative horsemenDefeated is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(horsemenDefeated = -1)
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
        horsemenDefeated: Int,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
    ) = AgainstTheHorsemenScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        horsemenDefeated = horsemenDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
