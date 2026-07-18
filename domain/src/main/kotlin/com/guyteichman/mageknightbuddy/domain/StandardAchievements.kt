package com.guyteichman.mageknightbuddy.domain

data class UnitTally(val level: Int, val healthyCount: Int, val woundedCount: Int) {
    fun fame(): Int = healthyCount * level + woundedCount * (level / 2)
}

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
    fun greatestKnowledge(): Int = 2 * spellsInDeck + advancedActionsInDeck

    fun greatestLeader(): Int = units.sumOf { it.fame() }

    fun greatestAdventurer(): Int = 2 * shieldsOnAdventureSites

    fun greatestLoot(): Int = 2 * artifacts + crystalsInInventory / 2

    fun greatestConqueror(): Int = 2 * shieldsOnConquerSites

    fun greatestBeating(): Int = -2 * woundsInDeck

    fun total(): Int =
        greatestKnowledge() + greatestLeader() + greatestAdventurer() +
            greatestLoot() + greatestConqueror() + greatestBeating()
}
