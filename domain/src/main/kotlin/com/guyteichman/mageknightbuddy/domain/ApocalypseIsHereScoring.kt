package com.guyteichman.mageknightbuddy.domain

// +15 points more if the player defeated every Dragon head (docs/rules/apocalypse-is-here.md,
// Scoring > Solo). The rulebook never states a fixed total head count for this scenario, so
// unlike Solo Conquest's city count this can't be derived from a raw tally - it's its own input.
private const val ALL_HEADS_DEFEATED_BONUS = 15

/**
 * Inputs for scoring a solo Apocalypse is Here session (docs/rules/apocalypse-is-here.md,
 * "Scoring" > "Solo"): Fame, the six Standard Achievements, how many Horsemen and Dragon heads
 * were defeated, whether every head fell, Rounds finished early, Dummy deck cards left, whether
 * "End of the Round" was already announced, and whether the Apocalypse Dragon itself was
 * defeated (drives Outcome). Not a v1 target - kept here as reference for when it's implemented.
 */
data class ApocalypseIsHereScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val horsemenDefeated: Int,
    val headsDefeated: Int,
    val allHeadsDefeated: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
    val dragonDefeated: Boolean,
)

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
     * Apocalypse Dragon was defeated; Lost otherwise. A score is always produced either way
     * (see [score]).
     */
    fun outcome(input: ApocalypseIsHereScoringInput): Outcome =
        if (input.dragonDefeated) Outcome.WON else Outcome.LOST
}
