package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LostRelicScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and scenario bonuses when all relic pieces were found`() {
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
            relicPiecesFound = 2,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        // 40 fame + 4 achievements (2*1+2) + (2*5=10 pieces + 10 all-pieces bonus)
        // + 3 dummy cards + 5 end-of-round-not-announced = 72
        assertEquals(72, LostRelicScoring.score(input))
        assertEquals(Outcome.WON, LostRelicScoring.outcome(input))
    }

    @Test
    fun `breakdown lists all 11 scoring rules and sums to the same total as score`() {
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
            relicPiecesFound = 2,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        val breakdown = LostRelicScoring.breakdown(input)

        assertEquals(11, breakdown.size)
        assertEquals(72, breakdown.sumOf { it.value })
        assertEquals(40, breakdown.single { it.label == "Fame" }.value)
        assertEquals(4, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(0, breakdown.single { it.label == "Greatest Leader" }.value)
        assertEquals(10, breakdown.single { it.label == "Relic Pieces Found" }.value)
        assertEquals(10, breakdown.single { it.label == "All Relic Pieces Found" }.value)
        assertEquals(3, breakdown.single { it.label == "Dummy Player's Deck" }.value)
        assertEquals(5, breakdown.single { it.label == "End of Round" }.value)
    }

    @Test
    fun `finding only one of two relic pieces scores 5 points but no all-pieces bonus`() {
        val input = minimalInput(relicPiecesFound = 1)

        val breakdown = LostRelicScoring.breakdown(input)

        assertEquals(5, breakdown.single { it.label == "Relic Pieces Found" }.value)
        assertEquals(0, breakdown.single { it.label == "All Relic Pieces Found" }.value)
        assertEquals(5, LostRelicScoring.score(input))
    }

    @Test
    fun `finding no relic pieces scores zero for both relic-related lines`() {
        val input = minimalInput(relicPiecesFound = 0)

        val breakdown = LostRelicScoring.breakdown(input)

        assertEquals(0, breakdown.single { it.label == "Relic Pieces Found" }.value)
        assertEquals(0, breakdown.single { it.label == "All Relic Pieces Found" }.value)
        assertEquals(0, LostRelicScoring.score(input))
    }

    @Test
    fun `End of Round bonus only applies when it was not yet announced`() {
        val notAnnounced = minimalInput(relicPiecesFound = 0, endOfRoundAnnounced = false)
        val announced = minimalInput(relicPiecesFound = 0, endOfRoundAnnounced = true)

        assertEquals(5, LostRelicScoring.score(notAnnounced))
        assertEquals(0, LostRelicScoring.score(announced))
    }

    @Test
    fun `score includes 1 point per card remaining in the Dummy Player's deck`() {
        val input = minimalInput(relicPiecesFound = 0, cardsRemainingInDummyDeck = 7)

        assertEquals(7, LostRelicScoring.score(input))
    }

    @Test
    fun `outcome is Won when both relic pieces were found`() {
        val input = minimalInput(relicPiecesFound = 2)

        assertEquals(Outcome.WON, LostRelicScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when only one relic piece was found`() {
        val input = minimalInput(relicPiecesFound = 1)

        assertEquals(Outcome.LOST, LostRelicScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when no relic pieces were found`() {
        val input = minimalInput(relicPiecesFound = 0)

        assertEquals(Outcome.LOST, LostRelicScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost even with a positive score, since collecting every relic piece is the only win condition`() {
        val input = minimalInput(
            fame = 50,
            relicPiecesFound = 1,
            cardsRemainingInDummyDeck = 6,
            endOfRoundAnnounced = false,
        )

        // 50 fame + 5 pieces (1*5, no all-pieces bonus) + 6 dummy cards + 5 end-of-round = 66
        assertEquals(66, LostRelicScoring.score(input))
        assertEquals(Outcome.LOST, LostRelicScoring.outcome(input))
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
        relicPiecesFound: Int,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
    ) = LostRelicScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        relicPiecesFound = relicPiecesFound,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
