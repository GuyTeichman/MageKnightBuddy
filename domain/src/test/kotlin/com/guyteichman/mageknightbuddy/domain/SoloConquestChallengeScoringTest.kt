package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoloConquestChallengeScoringTest {

    @Test
    fun `score sums Fame, Standard Achievements, and scenario bonuses using Tovak's Greatest Conqueror override`() {
        val input = minimalInput(
            knight = Knight.TOVAK,
            fame = 50,
            standardAchievements = achievements(shieldsOnConquerSites = 3),
            citiesConquered = 2,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
        )

        // 50 fame + 12 Greatest Conqueror (Tovak: 4 Fame per Shield, not 2, so 4*3)
        // + (2*10 + 15 all-cities) cities + 1*30 rounds + 4 dummy cards + 5 end-of-round = 136
        assertEquals(136, SoloConquestChallengeScoring.score(input))
    }

    @Test
    fun `breakdown lists all 13 scoring rules for a Knight with no line-count override, summing to the same total as score`() {
        val input = minimalInput(
            knight = Knight.TOVAK,
            fame = 50,
            standardAchievements = achievements(shieldsOnConquerSites = 3),
            citiesConquered = 2,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
        )

        val breakdown = SoloConquestChallengeScoring.breakdown(input)

        assertEquals(13, breakdown.size)
        assertEquals(136, breakdown.sumOf { it.value })
        assertEquals(12, breakdown.single { it.label == "Greatest Conqueror" }.value)
    }

    @Test
    fun `Arythea's Greatest Leader counts Wounded Units at full level, and Wound cards cost no Fame`() {
        val input = minimalInput(
            knight = Knight.ARYTHEA,
            standardAchievements = achievements(
                units = listOf(UnitTally(level = 4, healthyCount = 1, woundedCount = 1)),
                woundsInDeck = 3,
            ),
        )

        // Greatest Leader: (1 healthy + 1 wounded) * level 4 = 8, no halving for the wounded Unit.
        // Greatest Beating: Wound cards cost no Fame for Arythea, so 0 despite 3 Wounds in deck.
        val breakdown = SoloConquestChallengeScoring.breakdown(input)
        assertEquals(8, breakdown.single { it.label == "Greatest Leader" }.value)
        assertEquals(0, breakdown.single { it.label == "Greatest Beating" }.value)
    }

    @Test
    fun `Goldyx scores 3 Fame per Spell and 1 Fame per crystal instead of the standard rates`() {
        val input = minimalInput(
            knight = Knight.GOLDYX,
            standardAchievements = achievements(
                spellsInDeck = 2,
                advancedActionsInDeck = 1,
                artifacts = 1,
                crystalsInInventory = 3,
            ),
        )

        // Greatest Knowledge: 3*2 spells + 1 advanced action = 7 (standard would be 2*2+1=5).
        // Greatest Loot: 2*1 artifact + 3 crystals (1 Fame each, not per 2) = 5 (standard would be 2+1=3).
        val breakdown = SoloConquestChallengeScoring.breakdown(input)
        assertEquals(7, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(5, breakdown.single { it.label == "Greatest Loot" }.value)
    }

    @Test
    fun `Norowas scores 2 Fame per level of each unwounded Unit, leaving wounded Units at half level`() {
        val input = minimalInput(
            knight = Knight.NOROWAS,
            standardAchievements = achievements(
                units = listOf(UnitTally(level = 4, healthyCount = 2, woundedCount = 1)),
            ),
        )

        // 2 healthy Units * 2 Fame/level * level 4 = 16, plus 1 wounded Unit * half of level 4 = 2. Total 18.
        val breakdown = SoloConquestChallengeScoring.breakdown(input)
        assertEquals(18, breakdown.single { it.label == "Greatest Leader" }.value)
    }

    @Test
    fun `Wolfhawk scores 4 Fame per Shield token for Greatest Adventurer instead of 2`() {
        val input = minimalInput(
            knight = Knight.WOLFHAWK,
            standardAchievements = achievements(shieldsOnAdventureSites = 3),
        )

        val breakdown = SoloConquestChallengeScoring.breakdown(input)
        assertEquals(12, breakdown.single { it.label == "Greatest Adventurer" }.value)
    }

    @Test
    fun `Coral scores 4 Fame per Artifact and 1 Fame per crystal for Greatest Loot`() {
        val input = minimalInput(
            knight = Knight.CORAL,
            standardAchievements = achievements(artifacts = 2, crystalsInInventory = 5),
        )

        // 4*2 artifacts + 5 crystals (1 Fame each) = 13.
        val breakdown = SoloConquestChallengeScoring.breakdown(input)
        assertEquals(13, breakdown.single { it.label == "Greatest Loot" }.value)
    }

    @Test
    fun `Braevalar scores 2 Fame per Advanced Action and adds a Final Space Move Cost bonus line`() {
        val input = minimalInput(
            knight = Knight.BRAEVALAR,
            standardAchievements = achievements(spellsInDeck = 1, advancedActionsInDeck = 2),
            finalSpaceMoveCostAtNight = 5,
        )

        // Greatest Knowledge: 2*1 spell + 2*2 advanced actions = 6 (standard would be 2+2=4).
        val breakdown = SoloConquestChallengeScoring.breakdown(input)
        assertEquals(14, breakdown.size)
        assertEquals(6, breakdown.single { it.label == "Greatest Knowledge" }.value)
        assertEquals(5, breakdown.single { it.label == "Final Space Move Cost" }.value)
    }

    @Test
    fun `Krang's Puppet Master line replaces all six Standard Achievements entirely`() {
        val input = minimalInput(
            knight = Knight.KRANG,
            standardAchievements = achievements(spellsInDeck = 5, artifacts = 5), // should be ignored for Krang
            puppetMasterHighestFameValue = 7,
            puppetMasterDistinctFameValues = 4,
        )

        val breakdown = SoloConquestChallengeScoring.breakdown(input)
        // 8 lines: Fame, Puppet Master, Greatest Quester, Cities Conquered, All Cities
        // Conquered, Rounds Finished Early, Dummy Player's Deck, End of Round.
        assertEquals(8, breakdown.size)
        assertEquals(null, breakdown.find { it.label == "Greatest Knowledge" })
        // Puppet Master: highest Fame value (7) + 2 Fame per distinct value (4) = 15.
        assertEquals(15, breakdown.single { it.label == "Puppet Master" }.value)
    }

    @Test
    fun `outcome is Lost when both cities are conquered but the Knight's additional objective is not met`() {
        val input = minimalInput(knight = Knight.TOVAK, citiesConquered = 2, standardAchievements = achievements(shieldsOnConquerSites = 0))

        assertEquals(Outcome.LOST, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost when the Knight's objective is met but not both cities are conquered`() {
        val input = minimalInput(knight = Knight.TOVAK, citiesConquered = 1, standardAchievements = achievements(shieldsOnConquerSites = 4))

        assertEquals(Outcome.LOST, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `outcome is Won for Tovak when both cities are conquered and 4 Conqueror Shields are held`() {
        val input = minimalInput(knight = Knight.TOVAK, citiesConquered = 2, standardAchievements = achievements(shieldsOnConquerSites = 4))

        assertEquals(Outcome.WON, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `outcome is Won for Wolfhawk when both cities are conquered and 4 Adventurer Shields are held`() {
        val input = minimalInput(knight = Knight.WOLFHAWK, citiesConquered = 2, standardAchievements = achievements(shieldsOnAdventureSites = 4))

        assertEquals(Outcome.WON, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `outcome is Won for Arythea when both cities are conquered and 10 total Wound cards (deck plus Units) are counted`() {
        val input = minimalInput(
            knight = Knight.ARYTHEA,
            citiesConquered = 2,
            standardAchievements = achievements(woundsInDeck = 7),
            woundCardsOnUnits = 3,
        )

        assertEquals(Outcome.WON, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `outcome is Lost for Arythea when Wound cards are short of 10 even counting both deck and Units`() {
        val input = minimalInput(
            knight = Knight.ARYTHEA,
            citiesConquered = 2,
            standardAchievements = achievements(woundsInDeck = 7),
            woundCardsOnUnits = 2,
        )

        assertEquals(Outcome.LOST, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `outcome is Won for Goldyx only when both 4 Spells and all 4 crystal colors are met`() {
        val bothMet = minimalInput(
            knight = Knight.GOLDYX,
            citiesConquered = 2,
            standardAchievements = achievements(spellsInDeck = 4),
            distinctCrystalColorsInInventory = 4,
        )
        val onlySpellsMet = minimalInput(
            knight = Knight.GOLDYX,
            citiesConquered = 2,
            standardAchievements = achievements(spellsInDeck = 4),
            distinctCrystalColorsInInventory = 3,
        )

        assertEquals(Outcome.WON, SoloConquestChallengeScoring.outcome(bothMet))
        assertEquals(Outcome.LOST, SoloConquestChallengeScoring.outcome(onlySpellsMet))
    }

    @Test
    fun `outcome is Won for Norowas when both cities are conquered and total Unit levels reach 10`() {
        val input = minimalInput(
            knight = Knight.NOROWAS,
            citiesConquered = 2,
            standardAchievements = achievements(
                units = listOf(UnitTally(level = 5, healthyCount = 1, woundedCount = 1)),
            ),
        )

        // (1 healthy + 1 wounded) * level 5 = 10 total Unit levels.
        assertEquals(Outcome.WON, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `outcome is Won for Krang when both cities are conquered and 4 distinct Puppet Master Fame values are held`() {
        val input = minimalInput(
            knight = Knight.KRANG,
            citiesConquered = 2,
            puppetMasterDistinctFameValues = 4,
        )

        assertEquals(Outcome.WON, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `outcome is Won for Braevalar only when all Basic Actions remain and all 4 Advanced Action colors are in deck`() {
        val bothMet = minimalInput(
            knight = Knight.BRAEVALAR,
            citiesConquered = 2,
            allBasicActionsInDeck = true,
            distinctAdvancedActionColorsInDeck = 4,
        )
        val missingBasicActions = minimalInput(
            knight = Knight.BRAEVALAR,
            citiesConquered = 2,
            allBasicActionsInDeck = false,
            distinctAdvancedActionColorsInDeck = 4,
        )

        assertEquals(Outcome.WON, SoloConquestChallengeScoring.outcome(bothMet))
        assertEquals(Outcome.LOST, SoloConquestChallengeScoring.outcome(missingBasicActions))
    }

    @Test
    fun `outcome is Won for Coral when both cities are conquered, 3 Artifacts and 4 crystals are held`() {
        val input = minimalInput(
            knight = Knight.CORAL,
            citiesConquered = 2,
            standardAchievements = achievements(artifacts = 3, crystalsInInventory = 4),
        )

        assertEquals(Outcome.WON, SoloConquestChallengeScoring.outcome(input))
    }

    @Test
    fun `citiesConquered above the 2-city total is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.TOVAK, citiesConquered = 3)
        }
    }

    @Test
    fun `negative citiesConquered is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.TOVAK, citiesConquered = -1)
        }
    }

    @Test
    fun `distinctCrystalColorsInInventory above the 4-color total is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.GOLDYX, distinctCrystalColorsInInventory = 5)
        }
    }

    @Test
    fun `negative distinctCrystalColorsInInventory is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.GOLDYX, distinctCrystalColorsInInventory = -1)
        }
    }

    @Test
    fun `distinctAdvancedActionColorsInDeck above the 4-color total is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.BRAEVALAR, distinctAdvancedActionColorsInDeck = 5)
        }
    }

    @Test
    fun `negative distinctAdvancedActionColorsInDeck is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.BRAEVALAR, distinctAdvancedActionColorsInDeck = -1)
        }
    }

    @Test
    fun `finalSpaceMoveCostAtNight above 5 is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.BRAEVALAR, finalSpaceMoveCostAtNight = 6)
        }
    }

    @Test
    fun `finalSpaceMoveCostAtNight below 2 is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.BRAEVALAR, finalSpaceMoveCostAtNight = 1)
        }
    }

    @Test
    fun `finalSpaceMoveCostAtNight of 0 is rejected - it must be a real move cost, not the unused-field sentinel`() {
        assertFailsWith<IllegalArgumentException> {
            minimalInput(knight = Knight.BRAEVALAR, finalSpaceMoveCostAtNight = 0)
        }
    }

    private fun achievements(
        spellsInDeck: Int = 0,
        advancedActionsInDeck: Int = 0,
        units: List<UnitTally> = emptyList(),
        shieldsOnAdventureSites: Int = 0,
        artifacts: Int = 0,
        crystalsInInventory: Int = 0,
        shieldsOnConquerSites: Int = 0,
        woundsInDeck: Int = 0,
    ) = StandardAchievements(
        spellsInDeck = spellsInDeck,
        advancedActionsInDeck = advancedActionsInDeck,
        units = units,
        shieldsOnAdventureSites = shieldsOnAdventureSites,
        artifacts = artifacts,
        crystalsInInventory = crystalsInInventory,
        shieldsOnConquerSites = shieldsOnConquerSites,
        woundsInDeck = woundsInDeck,
    )

    private fun minimalInput(
        knight: Knight,
        fame: Int = 0,
        standardAchievements: StandardAchievements = achievements(),
        citiesConquered: Int = 0,
        roundsFinishedEarly: Int = 0,
        cardsRemainingInDummyDeck: Int = 0,
        endOfRoundAnnounced: Boolean = true,
        questPoints: Int = 0,
        woundCardsOnUnits: Int = 0,
        distinctCrystalColorsInInventory: Int = 0,
        puppetMasterHighestFameValue: Int = 0,
        puppetMasterDistinctFameValues: Int = 0,
        allBasicActionsInDeck: Boolean = false,
        distinctAdvancedActionColorsInDeck: Int = 0,
        finalSpaceMoveCostAtNight: Int = 2,
    ) = SoloConquestChallengeScoringInput(
        knight = knight,
        fame = fame,
        standardAchievements = standardAchievements,
        citiesConquered = citiesConquered,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
        questPoints = questPoints,
        woundCardsOnUnits = woundCardsOnUnits,
        distinctCrystalColorsInInventory = distinctCrystalColorsInInventory,
        puppetMasterHighestFameValue = puppetMasterHighestFameValue,
        puppetMasterDistinctFameValues = puppetMasterDistinctFameValues,
        allBasicActionsInDeck = allBasicActionsInDeck,
        distinctAdvancedActionColorsInDeck = distinctAdvancedActionColorsInDeck,
        finalSpaceMoveCostAtNight = finalSpaceMoveCostAtNight,
    )
}
