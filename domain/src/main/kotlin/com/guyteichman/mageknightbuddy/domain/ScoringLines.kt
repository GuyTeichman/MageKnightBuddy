package com.guyteichman.mageknightbuddy.domain

// Fame awarded per Round finished before the scenario's Round limit (docs/rules/solo-conquest.md
// and every other Dummy-Player-driven scenario's "Scoring" section: "+30 points for each Round
// finished before the Round limit").
const val ROUNDS_EARLY_FAME = 30

// Fame awarded if "End of the Round" had not yet been announced in the player's final Round
// (same scenarios' "Scoring" section: "+5 points if 'End of the Round' was not yet announced").
const val END_OF_ROUND_BONUS = 5

/**
 * The "Fame + six Standard Achievements" block (rulebook p.15) that opens virtually every
 * scenario's score breakdown, in the fixed order the rulebook lists them. Pulled out here
 * because most `*Scoring.breakdown()` functions start with this exact same seven lines -
 * see [StandardAchievements] for the formulas each `greatestX()` call runs.
 */
fun standardScoreLines(fame: Int, achievements: StandardAchievements): List<ScoreLineItem> =
    listOf(
        ScoreLineItem("Fame", fame),
        ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
        ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
        ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
        ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
        ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
        ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
    )

/**
 * The trailing "Rounds Finished Early / Dummy Player's Deck / End of Round" trio that closes
 * most Dummy-Player-driven scenarios' score breakdown (see docs/rules/dummy-player.md for what
 * these three inputs mean: Rounds finished early, cards left in the Dummy Player's deck, and
 * whether "End of the Round" was already announced in the final Round).
 */
fun dummyEndgameLines(
    roundsFinishedEarly: Int,
    cardsRemainingInDummyDeck: Int,
    endOfRoundAnnounced: Boolean,
): List<ScoreLineItem> {
    // +5 only if "End of the Round" had not yet been announced in the final Round.
    val endOfRoundBonus = if (!endOfRoundAnnounced) END_OF_ROUND_BONUS else 0
    return listOf(
        ScoreLineItem("Rounds Finished Early", roundsFinishedEarly * ROUNDS_EARLY_FAME),
        ScoreLineItem("Dummy Player's Deck", cardsRemainingInDummyDeck),
        ScoreLineItem("End of Round", endOfRoundBonus),
    )
}
