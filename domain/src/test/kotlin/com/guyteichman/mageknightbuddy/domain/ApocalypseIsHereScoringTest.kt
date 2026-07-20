package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ApocalypseIsHereScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and all scenario bonuses`() {
        val input = ApocalypseIsHereScoringInput(
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
            horsemenDefeated = 2,
            headsDefeated = 3,
            allHeadsDefeated = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
            dragonDefeated = true,
        )

        // 40 fame + 5 achievements (2*2+1) + 2*3 horsemen + 3*5 heads + 15 all-heads
        // + 1*30 rounds + 4 dummy cards + 5 end-of-round-not-announced = 120
        assertEquals(120, ApocalypseIsHereScoring.score(input))
    }

    @Test
    fun `breakdown lists all 13 scoring rules and sums to the same total as score`() {
        val input = ApocalypseIsHereScoringInput(
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
            horsemenDefeated = 2,
            headsDefeated = 3,
            allHeadsDefeated = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
            dragonDefeated = true,
        )

        val breakdown = ApocalypseIsHereScoring.breakdown(input)

        assertEquals(13, breakdown.size)
        assertEquals(120, breakdown.sumOf { it.value })
        assertEquals(40, breakdown.single { it.label == "Fame" }.value)
        assertEquals(5, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(0, breakdown.single { it.label == "Greatest Leader" }.value)
        assertEquals(6, breakdown.single { it.label == "Horsemen Defeated" }.value)
        assertEquals(15, breakdown.single { it.label == "Heads Defeated" }.value)
        assertEquals(15, breakdown.single { it.label == "All Heads Defeated" }.value)
        assertEquals(30, breakdown.single { it.label == "Rounds Finished Early" }.value)
        assertEquals(4, breakdown.single { it.label == "Dummy Player's Deck" }.value)
        assertEquals(5, breakdown.single { it.label == "End of Round" }.value)
    }

    @Test
    fun `score includes 3 Fame per Horseman defeated`() {
        val input = minimalInput(horsemenDefeated = 4)

        assertEquals(12, ApocalypseIsHereScoring.score(input))
    }

    @Test
    fun `score includes 5 Fame per head defeated, excluding the Control head`() {
        val input = minimalInput(headsDefeated = 3)

        assertEquals(15, ApocalypseIsHereScoring.score(input))
    }

    @Test
    fun `all-heads-defeated bonus is independent of the raw heads-defeated count`() {
        // The rulebook never states a fixed head total for this scenario, so the "+15 more if
        // you defeated all heads" bonus is its own explicit input rather than something derived
        // from headsDefeated - mirrors how HiddenValleyScoring treats highPriestessDefeated.
        val input = minimalInput(headsDefeated = 0, allHeadsDefeated = true)

        // 0 heads bonus + 15 all-heads bonus = 15
        assertEquals(15, ApocalypseIsHereScoring.score(input))
    }

    @Test
    fun `outcome is Won when the Apocalypse Dragon was defeated`() {
        val input = minimalInput(dragonDefeated = true)

        assertEquals(Outcome.WON, ApocalypseIsHereScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the Apocalypse Dragon was not defeated`() {
        val input = minimalInput(dragonDefeated = false)

        assertEquals(Outcome.LOST, ApocalypseIsHereScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost even with a positive score, since defeating the Dragon is the only win condition`() {
        val input = minimalInput(
            fame = 40,
            dragonDefeated = false,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
        )

        // 40 fame + 30 rounds + 3 dummy cards = 73
        assertEquals(73, ApocalypseIsHereScoring.score(input))
        assertEquals(Outcome.LOST, ApocalypseIsHereScoring.outcome(input))
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
        horsemenDefeated: Int = 0,
        headsDefeated: Int = 0,
        allHeadsDefeated: Boolean = false,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
        dragonDefeated: Boolean = false,
    ) = ApocalypseIsHereScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        horsemenDefeated = horsemenDefeated,
        headsDefeated = headsDefeated,
        allHeadsDefeated = allHeadsDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
        dragonDefeated = dragonDefeated,
    )
}
