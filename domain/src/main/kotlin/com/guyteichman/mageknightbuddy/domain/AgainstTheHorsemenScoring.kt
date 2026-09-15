package com.guyteichman.mageknightbuddy.domain

// Against the Horsemen always has exactly 4 Horsemen enemy tokens (docs/rules/against-the-horsemen.md,
// Setup): all four must fall for the "all Horsemen defeated" bonus and the win condition.
private const val TOTAL_HORSEMEN = 4

/**
 * Everything the player enters at the end of a solo Against the Horsemen session, matching the
 * inputs docs/rules/against-the-horsemen.md's "Solo" > "Scoring" section needs: base Fame, the
 * six Standard Achievements, how many Horsemen were defeated, how many Rounds were finished
 * early, Dummy deck cards left, and whether "End of the Round" was already announced.
 */
data class AgainstTheHorsemenScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val horsemenDefeated: Int,
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
    }
}

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
        // if/else used as an expression (Kotlin has no separate ternary operator) - the result
        // is assigned straight to the val. +15 for defeating *all four* Horsemen.
        val allHorsemenBonus = if (input.horsemenDefeated == TOTAL_HORSEMEN) 15 else 0
        // `+` concatenates lists here: the shared Fame/Achievements block, this scenario's own
        // bonus lines, then the shared Rounds-Finished-Early/Dummy-Deck/End-of-Round trio.
        return standardScoreLines(input.fame, input.standardAchievements) +
            listOf(
                ScoreLineItem("Horsemen Defeated", input.horsemenDefeated * 4),
                ScoreLineItem("All Horsemen Defeated", allHorsemenBonus),
            ) +
            dummyEndgameLines(input.roundsFinishedEarly, input.cardsRemainingInDummyDeck, input.endOfRoundAnnounced)
    }

    /**
     * Win/Loss check (docs/rules/against-the-horsemen.md, "Outcome" section): Won iff all four
     * Horsemen were defeated before the ritual completed; Lost otherwise. A score is always
     * produced either way (see [score]).
     */
    fun outcome(input: AgainstTheHorsemenScoringInput): Outcome =
        if (input.horsemenDefeated == TOTAL_HORSEMEN) Outcome.WON else Outcome.LOST
}
