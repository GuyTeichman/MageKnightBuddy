package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LifeAndDeathScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and all scenario bonuses when both Avatars were defeated`() {
        val input = minimalInput(
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
            tezlaSpiritDefeated = true,
            darkTezlaDefeated = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
        )

        // 50 fame + 5 achievements (2*2+1) + (2*10 + 15 both-avatars bonus) avatars
        // + 1*30 rounds + 4 dummy cards + 5 end-of-round-not-announced = 129
        assertEquals(129, LifeAndDeathScoring.score(input))
    }

    @Test
    fun `breakdown lists all 12 scoring rules and sums to the same total as score`() {
        val input = minimalInput(
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
            tezlaSpiritDefeated = true,
            darkTezlaDefeated = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
        )

        val breakdown = LifeAndDeathScoring.breakdown(input)

        assertEquals(12, breakdown.size)
        assertEquals(129, breakdown.sumOf { it.value })
        assertEquals(50, breakdown.single { it.label == "Fame" }.value)
        assertEquals(5, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(0, breakdown.single { it.label == "Greatest Leader" }.value)
        assertEquals(20, breakdown.single { it.label == "Avatars Defeated" }.value)
        assertEquals(15, breakdown.single { it.label == "Both Avatars Defeated" }.value)
        assertEquals(30, breakdown.single { it.label == "Rounds Finished Early" }.value)
        assertEquals(4, breakdown.single { it.label == "Dummy Player's Deck" }.value)
        assertEquals(5, breakdown.single { it.label == "End of Round" }.value)
    }

    @Test
    fun `score includes only the per-Avatar bonus, not the both-Avatars bonus, when just one Avatar was defeated`() {
        val input = minimalInput(tezlaSpiritDefeated = true, darkTezlaDefeated = false)

        // 1 Avatar defeated: 1*10 = 10, no +15 both-avatars bonus since Dark Tezla still stands.
        assertEquals(10, LifeAndDeathScoring.score(input))

        val breakdown = LifeAndDeathScoring.breakdown(input)
        assertEquals(10, breakdown.single { it.label == "Avatars Defeated" }.value)
        assertEquals(0, breakdown.single { it.label == "Both Avatars Defeated" }.value)
    }

    @Test
    fun `outcome is Won when both Avatars were defeated`() {
        val input = minimalInput(tezlaSpiritDefeated = true, darkTezlaDefeated = true)

        assertEquals(Outcome.WON, LifeAndDeathScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when only Tezla's Spirit was defeated`() {
        val input = minimalInput(tezlaSpiritDefeated = true, darkTezlaDefeated = false)

        assertEquals(Outcome.LOST, LifeAndDeathScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when only Dark Tezla was defeated`() {
        val input = minimalInput(tezlaSpiritDefeated = false, darkTezlaDefeated = true)

        assertEquals(Outcome.LOST, LifeAndDeathScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when neither Avatar was defeated`() {
        val input = minimalInput(tezlaSpiritDefeated = false, darkTezlaDefeated = false)

        assertEquals(Outcome.LOST, LifeAndDeathScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost even with a positive score, since defeating both Avatars is the only win condition`() {
        val input = minimalInput(
            fame = 40,
            tezlaSpiritDefeated = false,
            darkTezlaDefeated = false,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
        )

        // 40 fame + 0 avatars bonus + 30 rounds + 3 dummy cards = 73
        assertEquals(73, LifeAndDeathScoring.score(input))
        assertEquals(Outcome.LOST, LifeAndDeathScoring.outcome(input))
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
        tezlaSpiritDefeated: Boolean = false,
        darkTezlaDefeated: Boolean = false,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
    ) = LifeAndDeathScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        tezlaSpiritDefeated = tezlaSpiritDefeated,
        darkTezlaDefeated = darkTezlaDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
