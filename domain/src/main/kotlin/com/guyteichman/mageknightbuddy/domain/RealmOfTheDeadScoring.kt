package com.guyteichman.mageknightbuddy.domain

private const val SOLO_GRAVEYARDS_TOTAL = 2
private const val GRAVEYARD_SEALED_BONUS = 5
private const val NECROMANCER_DEFEATED_BONUS = 10
private const val BOTH_ACHIEVED_BONUS = 10

data class RealmOfTheDeadScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val graveyardsSealed: Int,
    val necromancerDefeated: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
) {
    val allGraveyardsSealed: Boolean get() = graveyardsSealed == SOLO_GRAVEYARDS_TOTAL
}

object RealmOfTheDeadScoring {
    fun score(input: RealmOfTheDeadScoringInput): Int = breakdown(input).sumOf { it.value }

    fun breakdown(input: RealmOfTheDeadScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        val necromancerBonus = if (input.necromancerDefeated) NECROMANCER_DEFEATED_BONUS else 0
        val bothBonus = if (input.allGraveyardsSealed && input.necromancerDefeated) BOTH_ACHIEVED_BONUS else 0
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("Graveyards Sealed", input.graveyardsSealed * GRAVEYARD_SEALED_BONUS),
            ScoreLineItem("Necromancer Defeated", necromancerBonus),
            ScoreLineItem("Both Achieved", bothBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    fun outcome(input: RealmOfTheDeadScoringInput): Outcome =
        if (input.allGraveyardsSealed && input.necromancerDefeated) Outcome.WON else Outcome.LOST
}
