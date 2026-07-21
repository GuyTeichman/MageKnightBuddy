package com.guyteichman.mageknightbuddy.domain

// Apocalypse is Here always starts with exactly 4 Horsemen (docs/rules/apocalypse-is-here.md,
// "Overview") and 5 Apocalypse Dragon heads - 1 Control head plus 4 others ("Key facts" section,
// citing page 36) - so both totals below are fixed, not scenario-configurable.
private const val TOTAL_HORSEMEN = 4
private const val TOTAL_NON_CONTROL_HEADS = 4

// +15 points more if the player defeated every Dragon head (docs/rules/apocalypse-is-here.md,
// Scoring > Solo).
private const val ALL_HEADS_DEFEATED_BONUS = 15

/**
 * Inputs for scoring a solo Apocalypse is Here session (docs/rules/apocalypse-is-here.md,
 * "Scoring" > "Solo"): Fame, the six Standard Achievements, how many Horsemen and Dragon heads
 * were defeated, Rounds finished early, Dummy deck cards left, and whether "End of the Round"
 * was already announced. No separate "Dragon defeated" flag - the Control head can't be attacked
 * and auto-defeats once the other 4 heads fall (page 10), so [outcome] derives Won/Lost straight
 * from [headsDefeated].
 */
data class ApocalypseIsHereScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val horsemenDefeated: Int,
    val headsDefeated: Int,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
) : ScoringInput {
    // init runs on every construction (including copy()), so an out-of-range tally can never
    // reach the scoring math below - it fails fast at the point the bad value was created.
    init {
        require(horsemenDefeated in 0..TOTAL_HORSEMEN) {
            "horsemenDefeated must be between 0 and $TOTAL_HORSEMEN, was $horsemenDefeated"
        }
        require(headsDefeated in 0..TOTAL_NON_CONTROL_HEADS) {
            "headsDefeated must be between 0 and $TOTAL_NON_CONTROL_HEADS, was $headsDefeated"
        }
    }
}

/**
 * Scoring engine for the solo variant of Apocalypse is Here (docs/rules/apocalypse-is-here.md,
 * "Scoring" > "Solo" and "Outcome"): Fame plus the Standard Achievements (no Titles in solo
 * play) plus bonuses for Horsemen and Dragon heads defeated and the shared early-finish/
 * Dummy-deck/End-of-Round bonuses.
 */
object ApocalypseIsHereScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: ApocalypseIsHereScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/apocalypse-is-here.md's
     * "Scoring" > "Solo" section: Fame, the six Standard Achievements, Horsemen defeated, heads
     * defeated (excluding the Control head) plus the all-heads bonus, Rounds finished early,
     * Dummy Player deck cards remaining, and the End of Round bonus.
     */
    fun breakdown(input: ApocalypseIsHereScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // if/else as an expression, assigned straight to the val.
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
            ScoreLineItem("Horsemen Defeated", input.horsemenDefeated * 3),
            ScoreLineItem("Heads Defeated", input.headsDefeated * 5),
            ScoreLineItem("All Heads Defeated", allHeadsBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/apocalypse-is-here.md, "Outcome" section): Won iff the
     * Apocalypse Dragon was defeated; Lost otherwise. The Control head can't be attacked and
     * auto-defeats once the other 4 heads do, so defeating the Dragon and defeating all 4
     * non-Control heads are the same event - Won iff [headsDefeated] reached the total. A score
     * is always produced either way (see [score]).
     */
    fun outcome(input: ApocalypseIsHereScoringInput): Outcome =
        if (input.headsDefeated == TOTAL_NON_CONTROL_HEADS) Outcome.WON else Outcome.LOST
}
