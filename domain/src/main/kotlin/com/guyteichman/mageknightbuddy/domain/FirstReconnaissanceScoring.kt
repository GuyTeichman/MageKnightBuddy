package com.guyteichman.mageknightbuddy.domain

// +10 points if the capital city was revealed (docs/rules/first-reconnaissance.md, Scoring > Solo).
private const val CITY_REVEALED_BONUS = 10

/**
 * Inputs for scoring a solo First Reconnaissance session (docs/rules/first-reconnaissance.md,
 * "Scoring" > "Solo"): Fame, the six Standard Achievements, whether the capital city was
 * revealed, Rounds finished early, Dummy deck cards left, and whether "End of the Round" had
 * already been announced.
 */
data class FirstReconnaissanceScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val cityRevealed: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
) : ScoringInput

/**
 * Scoring engine for the solo variant of First Reconnaissance
 * (docs/rules/first-reconnaissance.md, "Scoring" > "Solo" and "Outcome"): Fame plus the
 * Standard Achievements (no Titles in solo play) plus a bonus for revealing the capital city
 * and the shared early-finish/Dummy-deck/End-of-Round bonuses.
 */
object FirstReconnaissanceScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: FirstReconnaissanceScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results; `it` is the
        // implicit lambda parameter for the current item.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/first-reconnaissance.md's
     * "Scoring" > "Solo" section: Fame, the six Standard Achievements, the city-revealed bonus,
     * Rounds finished early, Dummy Player deck cards remaining, and the End of Round bonus.
     */
    fun breakdown(input: FirstReconnaissanceScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // if/else as an expression, not a statement - its value is assigned directly to the val.
        val cityRevealedBonus = if (input.cityRevealed) CITY_REVEALED_BONUS else 0
        // +5 if "End of the Round" was not yet announced in the last Round.
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

    /**
     * Win/Loss check (docs/rules/first-reconnaissance.md, "Outcome" section): Won iff the
     * capital city was revealed by the end of the game; Lost otherwise. A score is always
     * produced either way (see [score]).
     */
    fun outcome(input: FirstReconnaissanceScoringInput): Outcome =
        if (input.cityRevealed) Outcome.WON else Outcome.LOST
}
