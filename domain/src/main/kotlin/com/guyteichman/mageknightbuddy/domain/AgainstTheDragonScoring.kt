package com.guyteichman.mageknightbuddy.domain

// The Apocalypse Dragon always has 5 heads - 1 Control head plus 4 others (docs/rules/
// against-the-dragon.md, "Key facts", citing page 9) - so this total is fixed, not
// scenario-configurable.
private const val TOTAL_NON_CONTROL_HEADS = 4

// +5 Fame per Dragon head defeated, excluding the Control head (docs/rules/against-the-dragon.md,
// Scoring > Solo).
private const val FAME_PER_HEAD_DEFEATED = 5

// +15 more Fame on top of the per-head bonus above if every head was defeated.
private const val ALL_HEADS_DEFEATED_BONUS = 15

/**
 * Inputs for scoring a solo Against the Dragon session (docs/rules/against-the-dragon.md,
 * "Solo" > "Scoring"): Fame, the six Standard Achievements, how many Dragon heads fell, Rounds
 * finished early, Dummy deck cards left, and whether "End of the Round" had already been
 * announced. No separate "Dragon defeated" flag - the Control head can't be attacked and
 * auto-defeats once the other 4 heads fall (page 10), so [AgainstTheDragonScoring.outcome]
 * derives Won/Lost straight from [headsDefeated]. Not a v1 target - kept here as reference for
 * when it's implemented.
 */
data class AgainstTheDragonScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val headsDefeated: Int,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
) {
    // init runs on every construction (including copy()), so an out-of-range tally can never
    // reach the scoring math below - it fails fast at the point the bad value was created.
    init {
        require(headsDefeated in 0..TOTAL_NON_CONTROL_HEADS) {
            "headsDefeated must be between 0 and $TOTAL_NON_CONTROL_HEADS, was $headsDefeated"
        }
    }
}

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
        val allHeadsBonus = if (input.headsDefeated == TOTAL_NON_CONTROL_HEADS) ALL_HEADS_DEFEATED_BONUS else 0
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
     * Apocalypse Dragon itself was defeated; Lost otherwise. The Control head can't be attacked
     * and auto-defeats once the other 4 heads do, so defeating the Dragon and defeating all 4
     * non-Control heads are the same event - Won iff [headsDefeated] reached the total. A score
     * is always produced either way (see [score]).
     */
    fun outcome(input: AgainstTheDragonScoringInput): Outcome =
        if (input.headsDefeated == TOTAL_NON_CONTROL_HEADS) Outcome.WON else Outcome.LOST
}
