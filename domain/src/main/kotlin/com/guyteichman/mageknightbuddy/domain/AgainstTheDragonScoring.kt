package com.guyteichman.mageknightbuddy.domain

// +5 Fame per Dragon head defeated, excluding the Control head (docs/rules/against-the-dragon.md,
// Scoring > Solo).
private const val FAME_PER_HEAD_DEFEATED = 5

// +15 more Fame on top of the per-head bonus above if every head was defeated.
private const val ALL_HEADS_DEFEATED_BONUS = 15

/**
 * Inputs for scoring a solo Against the Dragon session (docs/rules/against-the-dragon.md,
 * "Solo" > "Scoring"): Fame, the six Standard Achievements, how many Dragon heads fell (and
 * whether every head fell), whether the Apocalypse Dragon itself was defeated (this decides
 * [AgainstTheDragonScoring.outcome], separately from the heads bonuses above), Rounds finished
 * early, Dummy deck cards left, and whether "End of the Round" had already been announced. Not
 * a v1 target - kept here as reference for when it's implemented.
 */
data class AgainstTheDragonScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val headsDefeated: Int,
    val allHeadsDefeated: Boolean,
    val dragonDefeated: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
)

/**
 * Scoring engine for the solo variant of Against the Dragon (docs/rules/against-the-dragon.md,
 * "Solo" > "Scoring" and "Outcome"): Fame plus the Standard Achievements (no Titles in solo
 * play) plus bonuses for Dragon heads defeated and the shared early-finish/Dummy-deck/
 * End-of-Round bonuses.
 */
object AgainstTheDragonScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: AgainstTheDragonScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/against-the-dragon.md's
     * "Solo" > "Scoring" section: Fame, the six Standard Achievements, the per-head-defeated
     * bonus, the all-heads-defeated bonus, Rounds finished early, Dummy Player deck cards
     * remaining, and the End of Round bonus.
     */
    fun breakdown(input: AgainstTheDragonScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // if/else as an expression, assigned straight to the val - no separate ternary in Kotlin.
        val allHeadsBonus = if (input.allHeadsDefeated) ALL_HEADS_DEFEATED_BONUS else 0
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
            ScoreLineItem("Heads Defeated", input.headsDefeated * FAME_PER_HEAD_DEFEATED),
            ScoreLineItem("All Heads Defeated", allHeadsBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/against-the-dragon.md, "Outcome" section): Won iff the
     * Apocalypse Dragon itself was defeated; Lost otherwise, regardless of how many heads fell
     * along the way. A score is always produced either way (see [score]).
     */
    fun outcome(input: AgainstTheDragonScoringInput): Outcome =
        if (input.dragonDefeated) Outcome.WON else Outcome.LOST
}
