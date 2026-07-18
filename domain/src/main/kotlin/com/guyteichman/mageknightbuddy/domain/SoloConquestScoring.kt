package com.guyteichman.mageknightbuddy.domain

private const val TOTAL_CITIES_IN_SOLO_CONQUEST = 2

data class SoloConquestScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val citiesConquered: Int,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
    val questPoints: Int,
)

data class ScoreLineItem(val label: String, val value: Int)

object SoloConquestScoring {
    fun score(input: SoloConquestScoringInput): Int = breakdown(input).sumOf { it.value }

    fun breakdown(input: SoloConquestScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        val allCitiesBonus = if (input.citiesConquered == TOTAL_CITIES_IN_SOLO_CONQUEST) 15 else 0
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("Greatest Quester", input.questPoints),
            ScoreLineItem("Cities Conquered", input.citiesConquered * 10),
            ScoreLineItem("All Cities Conquered", allCitiesBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    fun outcome(input: SoloConquestScoringInput): Outcome =
        if (input.citiesConquered == TOTAL_CITIES_IN_SOLO_CONQUEST) Outcome.WON else Outcome.LOST
}
