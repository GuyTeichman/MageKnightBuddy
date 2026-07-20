package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AgainstTheDragonScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and all scenario bonuses`() {
        val input = minimalInput(
            fame = 40,
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
            headsDefeated = 3,
            allHeadsDefeated = true,
            dragonDefeated = true,
            roundsFinishedEarly = 2,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        // 40 fame + 5 achievements (2*2+1) + 15 heads (3*5) + 15 all-heads
        // + 60 rounds (2*30) + 3 dummy cards + 5 end-of-round-not-announced = 143
        assertEquals(143, AgainstTheDragonScoring.score(input))
    }

    @Test
    fun `breakdown lists all 12 scoring rules and sums to the same total as score`() {
        val input = minimalInput(
            fame = 40,
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
            headsDefeated = 3,
            allHeadsDefeated = true,
            dragonDefeated = true,
            roundsFinishedEarly = 2,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        val breakdown = AgainstTheDragonScoring.breakdown(input)

        assertEquals(12, breakdown.size)
        assertEquals(143, breakdown.sumOf { it.value })
        assertEquals(40, breakdown.single { it.label == "Fame" }.value)
        assertEquals(5, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(0, breakdown.single { it.label == "Greatest Leader" }.value)
        assertEquals(15, breakdown.single { it.label == "Heads Defeated" }.value)
        assertEquals(15, breakdown.single { it.label == "All Heads Defeated" }.value)
        assertEquals(60, breakdown.single { it.label == "Rounds Finished Early" }.value)
        assertEquals(3, breakdown.single { it.label == "Dummy Player's Deck" }.value)
        assertEquals(5, breakdown.single { it.label == "End of Round" }.value)
    }

    @Test
    fun `heads defeated bonus is 5 Fame per head, excluding the Control head, with no all-heads bonus unless the flag is set`() {
        val input = minimalInput(headsDefeated = 3, allHeadsDefeated = false)

        // 3 heads * 5 = 15, no +15 all-heads bonus since not every head fell
        assertEquals(15, AgainstTheDragonScoring.score(input))
    }

    @Test
    fun `all-heads bonus of 15 stacks on top of the per-head bonus when every head fell`() {
        val input = minimalInput(headsDefeated = 4, allHeadsDefeated = true)

        // 4 heads * 5 = 20, plus +15 all-heads bonus = 35
        assertEquals(35, AgainstTheDragonScoring.score(input))
    }

    @Test
    fun `end of round bonus only applies when End of the Round was not yet announced`() {
        val announced = minimalInput(endOfRoundAnnounced = true)
        val notAnnounced = minimalInput(endOfRoundAnnounced = false)

        assertEquals(0, AgainstTheDragonScoring.score(announced))
        assertEquals(5, AgainstTheDragonScoring.score(notAnnounced))
    }

    @Test
    fun `outcome is Won when the Apocalypse Dragon was defeated`() {
        val input = minimalInput(dragonDefeated = true)

        assertEquals(Outcome.WON, AgainstTheDragonScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the Apocalypse Dragon was not defeated, even with a positive score`() {
        val input = minimalInput(
            fame = 40,
            dragonDefeated = false,
            headsDefeated = 3,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 2,
        )

        // 40 fame + 15 heads (3*5) + 30 rounds + 2 dummy cards = 87, but the Dragon itself
        // was never defeated, so this is still a loss - heads/rounds don't decide Outcome.
        assertEquals(87, AgainstTheDragonScoring.score(input))
        assertEquals(Outcome.LOST, AgainstTheDragonScoring.outcome(input))
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
        headsDefeated: Int = 0,
        allHeadsDefeated: Boolean = false,
        dragonDefeated: Boolean = false,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
    ) = AgainstTheDragonScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        headsDefeated = headsDefeated,
        allHeadsDefeated = allHeadsDefeated,
        dragonDefeated = dragonDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
