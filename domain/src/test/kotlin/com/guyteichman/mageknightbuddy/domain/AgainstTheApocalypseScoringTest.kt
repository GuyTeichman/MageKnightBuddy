package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AgainstTheApocalypseScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and all scenario bonuses when victorious`() {
        val input = minimalInput(
            fame = 40,
            standardAchievements = StandardAchievements(
                spellsInDeck = 1,
                advancedActionsInDeck = 1,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 1,
                crystalsInInventory = 2,
                shieldsOnConquerSites = 1,
                woundsInDeck = 1,
            ),
            destroyedSiteTokens = 3,
            zigguratFloorsConquered = 2,
            pyramidFloorsConquered = 1,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 5,
            endOfRoundAnnounced = false,
        )

        // 40 fame + 6 achievements (2*1+1 knowledge, 2*1+2/2 loot, 2*1 conqueror, -2*1 beating)
        // + 9 destroyed sites (3*3) + 10 sites conquered (2 sites * 5, flat per site) + 15 victorious
        // + 30 rounds + 5 dummy cards + 5 end-of-round-not-announced = 120
        assertEquals(120, AgainstTheApocalypseScoring.score(input))
        assertEquals(Outcome.WON, AgainstTheApocalypseScoring.outcome(input))
    }

    @Test
    fun `breakdown lists all 13 scoring rules and sums to the same total as score`() {
        val input = minimalInput(
            fame = 40,
            standardAchievements = StandardAchievements(
                spellsInDeck = 1,
                advancedActionsInDeck = 1,
                units = emptyList(),
                shieldsOnAdventureSites = 0,
                artifacts = 1,
                crystalsInInventory = 2,
                shieldsOnConquerSites = 1,
                woundsInDeck = 1,
            ),
            destroyedSiteTokens = 3,
            zigguratFloorsConquered = 2,
            pyramidFloorsConquered = 1,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 5,
            endOfRoundAnnounced = false,
        )

        val breakdown = AgainstTheApocalypseScoring.breakdown(input)

        assertEquals(13, breakdown.size)
        assertEquals(120, breakdown.sumOf { it.value })
        assertEquals(40, breakdown.single { it.label == "Fame" }.value)
        assertEquals(9, breakdown.single { it.label == "Destroyed Sites" }.value)
        assertEquals(10, breakdown.single { it.label == "Ziggurat/Pyramid Floors Conquered" }.value)
        assertEquals(15, breakdown.single { it.label == "Victorious" }.value)
        assertEquals(30, breakdown.single { it.label == "Rounds Finished Early" }.value)
        assertEquals(5, breakdown.single { it.label == "Dummy Player's Deck" }.value)
        assertEquals(5, breakdown.single { it.label == "End of Round" }.value)
    }

    @Test
    fun `score includes 3 points per Destroyed Site token`() {
        val input = minimalInput(destroyedSiteTokens = 4)

        assertEquals(12, AgainstTheApocalypseScoring.score(input))
    }

    @Test
    fun `score includes 5 points per ziggurat or pyramid site with a floor conquered, not per floor number`() {
        val input = minimalInput(zigguratFloorsConquered = 2, pyramidFloorsConquered = 3)

        // Flat 5 points per site (ziggurat + pyramid), not scaled by which floor (1/2/3) was reached.
        assertEquals(10, AgainstTheApocalypseScoring.score(input))
    }

    @Test
    fun `score treats reaching floor 3 the same as floor 1, since only whether a site was conquered matters`() {
        val deepFloor = minimalInput(zigguratFloorsConquered = 3, pyramidFloorsConquered = 0)
        val shallowFloor = minimalInput(zigguratFloorsConquered = 1, pyramidFloorsConquered = 0)

        assertEquals(5, AgainstTheApocalypseScoring.score(deepFloor))
        assertEquals(
            AgainstTheApocalypseScoring.score(deepFloor),
            AgainstTheApocalypseScoring.score(shallowFloor),
        )
    }

    @Test
    fun `outcome is Won when exactly at the Solo scenario-end thresholds`() {
        val input = minimalInput(
            destroyedSiteTokens = 2,
            zigguratFloorsConquered = 1,
            pyramidFloorsConquered = 1,
        )

        assertEquals(Outcome.WON, AgainstTheApocalypseScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when fewer than 2 Destroyed Site tokens, even with both floors conquered`() {
        val input = minimalInput(
            destroyedSiteTokens = 1,
            zigguratFloorsConquered = 1,
            pyramidFloorsConquered = 1,
        )

        assertEquals(Outcome.LOST, AgainstTheApocalypseScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the ziggurat has no floor conquered, even with enough Destroyed Sites`() {
        val input = minimalInput(
            destroyedSiteTokens = 5,
            zigguratFloorsConquered = 0,
            pyramidFloorsConquered = 1,
        )

        assertEquals(Outcome.LOST, AgainstTheApocalypseScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the pyramid has no floor conquered, even with enough Destroyed Sites`() {
        val input = minimalInput(
            destroyedSiteTokens = 5,
            zigguratFloorsConquered = 1,
            pyramidFloorsConquered = 0,
        )

        assertEquals(Outcome.LOST, AgainstTheApocalypseScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost even with a positive score, since Destroyed Sites and floors are the only win condition`() {
        val input = minimalInput(
            fame = 40,
            destroyedSiteTokens = 0,
            zigguratFloorsConquered = 0,
            pyramidFloorsConquered = 0,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
        )

        // 40 fame + 0 bonuses + 30 rounds + 3 dummy cards = 73, no victory bonus since not won
        assertEquals(73, AgainstTheApocalypseScoring.score(input))
        assertEquals(Outcome.LOST, AgainstTheApocalypseScoring.outcome(input))
    }

    @Test
    fun `zigguratFloorsConquered above the 3-floor total is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(zigguratFloorsConquered = 4)
        }
    }

    @Test
    fun `pyramidFloorsConquered above the 3-floor total is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(pyramidFloorsConquered = 4)
        }
    }

    @Test
    fun `negative zigguratFloorsConquered is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(zigguratFloorsConquered = -1)
        }
    }

    @Test
    fun `negative pyramidFloorsConquered is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(pyramidFloorsConquered = -1)
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
        destroyedSiteTokens: Int = 0,
        zigguratFloorsConquered: Int = 0,
        pyramidFloorsConquered: Int = 0,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
    ) = AgainstTheApocalypseScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        destroyedSiteTokens = destroyedSiteTokens,
        zigguratFloorsConquered = zigguratFloorsConquered,
        pyramidFloorsConquered = pyramidFloorsConquered,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
