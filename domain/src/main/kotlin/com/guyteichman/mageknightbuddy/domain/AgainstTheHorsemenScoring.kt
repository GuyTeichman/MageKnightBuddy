package com.guyteichman.mageknightbuddy.domain

// Against the Horsemen always has exactly 4 Horsemen enemy tokens (docs/rules/against-the-horsemen.md,
// Setup): all four must fall for the "all Horsemen defeated" bonus and the win condition.
private const val TOTAL_HORSEMEN = 4

/**
 * Everything the player enters at the end of a solo Against the Horsemen session, matching the
 * inputs docs/rules/against-the-horsemen.md's "Solo" > "Scoring" section needs: base Fame, the
 * six Standard Achievements, how many Horsemen were defeated, how many Rounds were finished
 * early, Dummy deck cards left, and whether "End of the Round" was already announced. Not a v1
 * target - kept here as reference for when this scenario is implemented.
 */
data class AgainstTheHorsemenScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val horsemenDefeated: Int,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
)

/**
 * Scoring engine for the solo variant of Against the Horsemen (docs/rules/against-the-horsemen.md,
 * "Scoring" > "Solo" and "Outcome"): Fame plus the Standard Achievements (summed directly, no
 * Titles - solo play has nobody to compare against) plus this scenario's own bonuses for
 * defeating Horsemen, finishing Rounds early, and so on.
 */
object AgainstTheHorsemenScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: AgainstTheHorsemenScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/against-the-horsemen.md's
     * "Scoring" > "Solo" section: Fame, the six Standard Achievements, Horsemen defeated, the
     * all-four-Horsemen bonus, Rounds finished early, Dummy Player deck cards remaining, and
     * the End of Round bonus.
     */
    fun breakdown(input: AgainstTheHorsemenScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // if/else used as an expression (Kotlin has no separate ternary operator) - the result
        // is assigned straight to the val. +15 for defeating *all four* Horsemen.
        val allHorsemenBonus = if (input.horsemenDefeated == TOTAL_HORSEMEN) 15 else 0
        // +5 if "End of the Round" had not yet been announced in the final Round.
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0
        // listOf builds an immutable, ordered list in one expression; each entry below is a
        // rulebook-mandated scoring category or bonus, in the order the rulebook lists them.
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("Horsemen Defeated", input.horsemenDefeated * 4),
            ScoreLineItem("All Horsemen Defeated", allHorsemenBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/against-the-horsemen.md, "Outcome" section): Won iff all four
     * Horsemen were defeated before the ritual completed; Lost otherwise. A score is always
     * produced either way (see [score]).
     */
    fun outcome(input: AgainstTheHorsemenScoringInput): Outcome =
        if (input.horsemenDefeated == TOTAL_HORSEMEN) Outcome.WON else Outcome.LOST
}
