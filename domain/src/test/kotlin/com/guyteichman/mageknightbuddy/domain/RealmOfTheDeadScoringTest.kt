package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class RealmOfTheDeadScoringTest {

    @Test
    fun `outcome is Won only when both Graveyards are sealed and the Necromancer is defeated`() {
        val input = minimalInput(
            fame = 25,
            graveyardsSealed = 2,
            necromancerDefeated = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        // 25 fame + 10 graveyards (2*5) + 10 necromancer + 10 both-achieved + 30 rounds + 3 dummy cards + 5 end-of-round = 93
        assertEquals(93, RealmOfTheDeadScoring.score(input))
        assertEquals(Outcome.WON, RealmOfTheDeadScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the Necromancer is defeated but not every Graveyard was sealed`() {
        val input = minimalInput(fame = 20, graveyardsSealed = 1, necromancerDefeated = true)

        // 20 fame + 5 graveyards (1*5) + 10 necromancer + 0 both-achieved (not all sealed) = 35
        assertEquals(35, RealmOfTheDeadScoring.score(input))
        assertEquals(Outcome.LOST, RealmOfTheDeadScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when every Graveyard is sealed but the Necromancer was not defeated`() {
        val input = minimalInput(fame = 20, graveyardsSealed = 2, necromancerDefeated = false)

        // 20 fame + 10 graveyards (2*5) + 0 necromancer + 0 both-achieved (necromancer not defeated) = 30
        assertEquals(30, RealmOfTheDeadScoring.score(input))
        assertEquals(Outcome.LOST, RealmOfTheDeadScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when neither condition was met`() {
        val input = minimalInput(fame = 5, graveyardsSealed = 0, necromancerDefeated = false)

        assertEquals(5, RealmOfTheDeadScoring.score(input))
        assertEquals(Outcome.LOST, RealmOfTheDeadScoring.outcome(input))
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
        graveyardsSealed: Int,
        necromancerDefeated: Boolean,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
    ) = RealmOfTheDeadScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        graveyardsSealed = graveyardsSealed,
        necromancerDefeated = necromancerDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
