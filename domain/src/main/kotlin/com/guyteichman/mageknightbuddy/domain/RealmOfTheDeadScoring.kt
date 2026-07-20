package com.guyteichman.mageknightbuddy.domain

// Solo setup places exactly 2 Graveyards (docs/rules/realm-of-the-dead.md, Solo setup: "exactly
// 2 should contain a Magical Glade") - both must be sealed for the win/all-sealed bonuses.
private const val SOLO_GRAVEYARDS_TOTAL = 2

// +5 points per Graveyard sealed (docs/rules/realm-of-the-dead.md, Scoring > Solo).
private const val GRAVEYARD_SEALED_BONUS = 5

// +10 points for defeating the Necromancer (docs/rules/realm-of-the-dead.md, Scoring > Solo).
private const val NECROMANCER_DEFEATED_BONUS = 10

// +10 extra points if *both* Graveyards were sealed and the Necromancer was defeated
// (docs/rules/realm-of-the-dead.md, Scoring > Solo).
private const val BOTH_ACHIEVED_BONUS = 10

/**
 * Inputs for scoring a solo Realm of the Dead session (docs/rules/realm-of-the-dead.md,
 * "Solo" > "Scoring"): Fame, the six Standard Achievements, how many Graveyards were sealed,
 * whether the Necromancer was defeated, Rounds finished early, Dummy deck cards left, and
 * whether "End of the Round" had already been announced. Not a v1 target - kept here as
 * reference for when this scenario is implemented.
 */
data class RealmOfTheDeadScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val graveyardsSealed: Int,
    val necromancerDefeated: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
) : ScoringInput {
    // A custom getter (not a stored field): this recomputes from graveyardsSealed every time
    // it's read, rather than being set once. Keeps "both Graveyards sealed" as a single
    // derived fact instead of repeating the == SOLO_GRAVEYARDS_TOTAL check at each call site.
    val allGraveyardsSealed: Boolean get() = graveyardsSealed == SOLO_GRAVEYARDS_TOTAL
}

/**
 * Scoring engine for the solo variant of The Realm of the Dead (docs/rules/realm-of-the-dead.md,
 * "Solo" > "Scoring" and "Outcome"): Fame plus the Standard Achievements (no Titles in solo
 * play) plus bonuses for sealing Graveyards and defeating the Necromancer, on top of the
 * shared early-finish/Dummy-deck/End-of-Round bonuses.
 */
object RealmOfTheDeadScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: RealmOfTheDeadScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/realm-of-the-dead.md's
     * "Solo" > "Scoring" section: Fame, the six Standard Achievements, Graveyards sealed,
     * Necromancer defeated, the "both achieved" bonus, Rounds finished early, Dummy Player
     * deck cards remaining, and the End of Round bonus.
     */
    fun breakdown(input: RealmOfTheDeadScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // if/else as an expression, assigned straight to the val.
        val necromancerBonus = if (input.necromancerDefeated) NECROMANCER_DEFEATED_BONUS else 0
        // Both conditions (all Graveyards sealed AND Necromancer defeated) must hold for the
        // combined bonus - this is on top of, not instead of, the two bonuses above.
        val bothBonus = if (input.allGraveyardsSealed && input.necromancerDefeated) BOTH_ACHIEVED_BONUS else 0
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
            ScoreLineItem("Graveyards Sealed", input.graveyardsSealed * GRAVEYARD_SEALED_BONUS),
            ScoreLineItem("Necromancer Defeated", necromancerBonus),
            ScoreLineItem("Both Achieved", bothBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/realm-of-the-dead.md, "Outcome" section): Won iff every
     * placed Graveyard was sealed *and* the Necromancer was defeated; Lost otherwise. A score
     * is always produced either way - the rulebook is explicit that failing still lets you
     * "count your score."
     */
    fun outcome(input: RealmOfTheDeadScoringInput): Outcome =
        if (input.allGraveyardsSealed && input.necromancerDefeated) Outcome.WON else Outcome.LOST
}
