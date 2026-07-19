package com.guyteichman.mageknightbuddy.domain

// +20 points for defeating the High Priestess (docs/rules/hidden-valley.md, Scoring > Solo).
private const val HIGH_PRIESTESS_DEFEATED_BONUS = 20

/**
 * Inputs for scoring a solo Hidden Valley session (docs/rules/hidden-valley.md,
 * "Solo" > "Scoring"): Fame, the six Standard Achievements, whether the High Priestess was
 * defeated, Rounds finished early, Dummy deck cards left, and whether "End of the Round" had
 * already been announced. Not a v1 target - kept here as reference for when it's implemented.
 */
data class HiddenValleyScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val highPriestessDefeated: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
)

/**
 * Scoring engine for the solo variant of The Hidden Valley (docs/rules/hidden-valley.md,
 * "Solo" > "Scoring" and "Outcome"): Fame plus the Standard Achievements (no Titles in solo
 * play) plus a bonus for defeating the High Priestess and the shared early-finish/Dummy-deck/
 * End-of-Round bonuses.
 */
object HiddenValleyScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: HiddenValleyScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/hidden-valley.md's
     * "Solo" > "Scoring" section: Fame, the six Standard Achievements, the High-Priestess-
     * defeated bonus, Rounds finished early, Dummy Player deck cards remaining, and the End of
     * Round bonus.
     */
    fun breakdown(input: HiddenValleyScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // if/else as an expression, assigned straight to the val.
        val defeatedBonus = if (input.highPriestessDefeated) HIGH_PRIESTESS_DEFEATED_BONUS else 0
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
            ScoreLineItem("High Priestess Defeated", defeatedBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/hidden-valley.md, "Outcome" section): Won iff the High
     * Priestess was defeated; Lost otherwise. A score is always produced either way - the
     * rulebook is explicit that failing still lets you "count your score."
     */
    fun outcome(input: HiddenValleyScoringInput): Outcome =
        if (input.highPriestessDefeated) Outcome.WON else Outcome.LOST
}
