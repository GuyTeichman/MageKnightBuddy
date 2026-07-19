package com.guyteichman.mageknightbuddy.domain

private const val HIGH_PRIESTESS_DEFEATED_BONUS = 20

data class HiddenValleyScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val highPriestessDefeated: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
)

object HiddenValleyScoring {
    fun score(input: HiddenValleyScoringInput): Int = breakdown(input).sumOf { it.value }

    fun breakdown(input: HiddenValleyScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        val defeatedBonus = if (input.highPriestessDefeated) HIGH_PRIESTESS_DEFEATED_BONUS else 0
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("High Priestess Defeated", defeatedBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    fun outcome(input: HiddenValleyScoringInput): Outcome =
        if (input.highPriestessDefeated) Outcome.WON else Outcome.LOST
}
