package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the [ScoringInput] dispatcher functions ([score], [outcome], [breakdown]) - not
 * re-testing each scenario's own scoring rules (already covered by e.g. [SoloConquestScoringTest]
 * and its siblings), just that dispatching through the sealed [ScoringInput] type reaches the
 * right `*Scoring` object for every variant.
 */
class ScoringInputTest {

    private val emptyAchievements = StandardAchievements(
        spellsInDeck = 0,
        advancedActionsInDeck = 0,
        units = emptyList(),
        shieldsOnAdventureSites = 0,
        artifacts = 0,
        crystalsInInventory = 0,
        shieldsOnConquerSites = 0,
        woundsInDeck = 0,
    )

    @Test
    fun `dispatches a SoloConquestScoringInput to SoloConquestScoring`() {
        val input = SoloConquestScoringInput(
            fame = 10,
            standardAchievements = emptyAchievements,
            citiesConquered = 2,
            roundsFinishedEarly = 0,
            cardsRemainingInDummyDeck = 0,
            endOfRoundAnnounced = true,
            questPoints = 0,
        )

        val scoringInput: ScoringInput = input
        assertEquals(SoloConquestScoring.score(input), scoringInput.score())
        assertEquals(SoloConquestScoring.outcome(input), scoringInput.outcome())
        assertEquals(SoloConquestScoring.breakdown(input), scoringInput.breakdown())
    }

    @Test
    fun `dispatches a FirstReconnaissanceScoringInput to FirstReconnaissanceScoring`() {
        val input = FirstReconnaissanceScoringInput(
            fame = 10,
            standardAchievements = emptyAchievements,
            cityRevealed = true,
            roundsFinishedEarly = 0,
            cardsRemainingInDummyDeck = 0,
            endOfRoundAnnounced = true,
        )

        val scoringInput: ScoringInput = input
        assertEquals(FirstReconnaissanceScoring.score(input), scoringInput.score())
        assertEquals(FirstReconnaissanceScoring.outcome(input), scoringInput.outcome())
        assertEquals(FirstReconnaissanceScoring.breakdown(input), scoringInput.breakdown())
    }

    @Test
    fun `dispatches a ForTheCouncilScoringInput to ForTheCouncilScoring`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 10,
            reputationModifier = -1,
            shieldOnXSpace = false,
            reputation = 2,
        )

        val scoringInput: ScoringInput = input
        assertEquals(ForTheCouncilScoring.score(input), scoringInput.score())
        assertEquals(ForTheCouncilScoring.outcome(input), scoringInput.outcome())
        assertEquals(ForTheCouncilScoring.breakdown(input), scoringInput.breakdown())
    }

    @Test
    fun `dispatches a HiddenValleyScoringInput to HiddenValleyScoring`() {
        val input = HiddenValleyScoringInput(
            fame = 10,
            standardAchievements = emptyAchievements,
            highPriestessDefeated = true,
            roundsFinishedEarly = 0,
            cardsRemainingInDummyDeck = 0,
            endOfRoundAnnounced = true,
        )

        val scoringInput: ScoringInput = input
        assertEquals(HiddenValleyScoring.score(input), scoringInput.score())
        assertEquals(HiddenValleyScoring.outcome(input), scoringInput.outcome())
        assertEquals(HiddenValleyScoring.breakdown(input), scoringInput.breakdown())
    }

    @Test
    fun `dispatches a RealmOfTheDeadScoringInput to RealmOfTheDeadScoring`() {
        val input = RealmOfTheDeadScoringInput(
            fame = 10,
            standardAchievements = emptyAchievements,
            graveyardsSealed = 2,
            necromancerDefeated = true,
            roundsFinishedEarly = 0,
            cardsRemainingInDummyDeck = 0,
            endOfRoundAnnounced = true,
        )

        val scoringInput: ScoringInput = input
        assertEquals(RealmOfTheDeadScoring.score(input), scoringInput.score())
        assertEquals(RealmOfTheDeadScoring.outcome(input), scoringInput.outcome())
        assertEquals(RealmOfTheDeadScoring.breakdown(input), scoringInput.breakdown())
    }
}
