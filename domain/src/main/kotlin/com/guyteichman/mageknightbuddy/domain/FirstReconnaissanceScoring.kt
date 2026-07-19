package com.guyteichman.mageknightbuddy.domain

private const val CITY_REVEALED_BONUS = 10

data class FirstReconnaissanceScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val cityRevealed: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
)

object FirstReconnaissanceScoring {
    fun score(input: FirstReconnaissanceScoringInput): Int = breakdown(input).sumOf { it.value }

    fun breakdown(input: FirstReconnaissanceScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        val cityRevealedBonus = if (input.cityRevealed) CITY_REVEALED_BONUS else 0
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("City Revealed", cityRevealedBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    fun outcome(input: FirstReconnaissanceScoringInput): Outcome =
        if (input.cityRevealed) Outcome.WON else Outcome.LOST
}
