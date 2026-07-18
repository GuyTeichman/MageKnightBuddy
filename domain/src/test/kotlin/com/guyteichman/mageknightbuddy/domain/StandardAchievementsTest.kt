package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StandardAchievementsTest {

    @Test
    fun `greatest knowledge is 2 Fame per Spell plus 1 Fame per Advanced Action`() {
        val achievements = StandardAchievements(
            spellsInDeck = 3,
            advancedActionsInDeck = 4,
            units = emptyList(),
            shieldsOnAdventureSites = 0,
            artifacts = 0,
            crystalsInInventory = 0,
            shieldsOnConquerSites = 0,
            woundsInDeck = 0,
        )

        // 2*3 spells + 1*4 advanced actions = 10, per docs/rules/solo-scoring-overview.md
        assertEquals(10, achievements.greatestKnowledge())
    }

    @Test
    fun `greatest leader sums unit tallies, halving wounded units rounded down`() {
        val achievements = StandardAchievements(
            spellsInDeck = 0,
            advancedActionsInDeck = 0,
            units = listOf(
                UnitTally(level = 4, healthyCount = 2, woundedCount = 0),
                UnitTally(level = 5, healthyCount = 0, woundedCount = 1),
                UnitTally(level = 3, healthyCount = 1, woundedCount = 1),
            ),
            shieldsOnAdventureSites = 0,
            artifacts = 0,
            crystalsInInventory = 0,
            shieldsOnConquerSites = 0,
            woundsInDeck = 0,
        )

        // level 4: 2 healthy * 4 = 8
        // level 5: 1 wounded * floor(5/2)=2
        // level 3: 1 healthy * 3 + 1 wounded * floor(3/2)=1 => 4
        // 8 + 2 + 4 = 14
        assertEquals(14, achievements.greatestLeader())
    }

    @Test
    fun `greatest adventurer is 2 Fame per Shield on an adventure site`() {
        val achievements = StandardAchievements(
            spellsInDeck = 0,
            advancedActionsInDeck = 0,
            units = emptyList(),
            shieldsOnAdventureSites = 5,
            artifacts = 0,
            crystalsInInventory = 0,
            shieldsOnConquerSites = 0,
            woundsInDeck = 0,
        )

        // 2*5 shields = 10
        assertEquals(10, achievements.greatestAdventurer())
    }

    @Test
    fun `greatest loot is 2 Fame per Artifact plus 1 Fame per 2 crystals, rounded down`() {
        val achievements = StandardAchievements(
            spellsInDeck = 0,
            advancedActionsInDeck = 0,
            units = emptyList(),
            shieldsOnAdventureSites = 0,
            artifacts = 3,
            crystalsInInventory = 5,
            shieldsOnConquerSites = 0,
            woundsInDeck = 0,
        )

        // 2*3 artifacts + floor(5/2)=2 crystal-pairs = 8
        assertEquals(8, achievements.greatestLoot())
    }

    @Test
    fun `greatest conqueror is 2 Fame per Shield on a keep, mage tower, or monastery`() {
        val achievements = StandardAchievements(
            spellsInDeck = 0,
            advancedActionsInDeck = 0,
            units = emptyList(),
            shieldsOnAdventureSites = 0,
            artifacts = 0,
            crystalsInInventory = 0,
            shieldsOnConquerSites = 4,
            woundsInDeck = 0,
        )

        // 2*4 shields = 8
        assertEquals(8, achievements.greatestConqueror())
    }

    @Test
    fun `greatest beating is minus 2 Fame per Wound card in deck`() {
        val achievements = StandardAchievements(
            spellsInDeck = 0,
            advancedActionsInDeck = 0,
            units = emptyList(),
            shieldsOnAdventureSites = 0,
            artifacts = 0,
            crystalsInInventory = 0,
            shieldsOnConquerSites = 0,
            woundsInDeck = 3,
        )

        // -2*3 wounds = -6
        assertEquals(-6, achievements.greatestBeating())
    }

    @Test
    fun `total sums all six Standard Achievements Scoring categories`() {
        val achievements = StandardAchievements(
            spellsInDeck = 3,
            advancedActionsInDeck = 4,
            units = listOf(
                UnitTally(level = 4, healthyCount = 2, woundedCount = 0),
                UnitTally(level = 5, healthyCount = 0, woundedCount = 1),
                UnitTally(level = 3, healthyCount = 1, woundedCount = 1),
            ),
            shieldsOnAdventureSites = 5,
            artifacts = 3,
            crystalsInInventory = 5,
            shieldsOnConquerSites = 4,
            woundsInDeck = 3,
        )

        // knowledge=10 + leader=14 + adventurer=10 + loot=8 + conqueror=8 + beating=-6 = 44
        assertEquals(44, achievements.total())
    }
}
