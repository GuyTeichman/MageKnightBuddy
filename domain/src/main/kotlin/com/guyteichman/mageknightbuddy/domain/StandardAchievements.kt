package com.guyteichman.mageknightbuddy.domain

/**
 * How many Units of a given level the player has at game end, split into healthy vs.
 * wounded, so [fame] can apply the "Wounded Units count as half their level" rule
 * (see docs/rules/solo-scoring-overview.md, Greatest Leader).
 */
data class UnitTally(val level: Int, val healthyCount: Int, val woundedCount: Int) {
    /** Fame contributed by this level's Units towards Greatest Leader. */
    fun fame(): Int = healthyCount * level + woundedCount * (level / 2)
}

/**
 * The fixed six "Standard Achievements Scoring" categories (rulebook p.15) that apply to
 * virtually every scenario, plus their raw game-state inputs. Each `greatestX()` function
 * below implements one category's Fame formula; see docs/rules/solo-scoring-overview.md
 * for the source table and page citations.
 */
data class StandardAchievements(
    val spellsInDeck: Int,
    val advancedActionsInDeck: Int,
    val units: List<UnitTally>,
    val shieldsOnAdventureSites: Int,
    val artifacts: Int,
    val crystalsInInventory: Int,
    val shieldsOnConquerSites: Int,
    val woundsInDeck: Int,
) {
    /** Greatest Knowledge: 2 Fame per Spell in deck + 1 Fame per Advanced Action in deck. */
    fun greatestKnowledge(): Int = 2 * spellsInDeck + advancedActionsInDeck

    /** Greatest Leader: sum of [UnitTally.fame] across all owned Units. */
    fun greatestLeader(): Int = units.sumOf { it.fame() } // sumOf: add up fame() called on every UnitTally in the list

    /** Greatest Adventurer: 2 Fame per Shield token on an adventure site. */
    fun greatestAdventurer(): Int = 2 * shieldsOnAdventureSites

    /** Greatest Loot: 2 Fame per Artifact + 1 Fame per 2 crystals in Inventory. */
    fun greatestLoot(): Int = 2 * artifacts + crystalsInInventory / 2

    /** Greatest Conqueror: 2 Fame per Shield token on a keep, mage tower, or monastery. */
    fun greatestConqueror(): Int = 2 * shieldsOnConquerSites

    /** Greatest Beating: -2 Fame per Wound card in deck (a penalty, hence the negative sign). */
    fun greatestBeating(): Int = -2 * woundsInDeck

    /**
     * Sum of all six categories. In solo play there are no Titles to add on top (only one
     * player, nothing to compare against), so this is the full Standard Achievements score.
     */
    fun total(): Int =
        greatestKnowledge() + greatestLeader() + greatestAdventurer() +
            greatestLoot() + greatestConqueror() + greatestBeating()
}
