package com.guyteichman.mageknightbuddy.domain

// Solo win condition: Reputation +2 or higher (docs/rules/for-the-council.md, Scoring > Solo).
private const val SOLO_WINNING_REPUTATION = 2

// A Shield token on the Reputation track's X space scores -10 quest points instead of a
// Reputation modifier - the two X spaces have no modifier at all (ReputationTrackSpace.modifier
// is null for both; docs/rules/for-the-council.md, Scoring > Solo).
private const val X_SPACE_PENALTY = -10

/**
 * Inputs for scoring a solo For the Council session (docs/rules/for-the-council.md,
 * "Scoring" > "Solo"): quest points, and which [ReputationTrackSpace] your Shield token sits
 * on. That one space determines both your Reputation modifier (for scoring) and your raw
 * Reputation (for the Outcome check) - see [ReputationTrackSpace] - so this only needs to record
 * the space itself, not the two derived values separately.
 */
data class ForTheCouncilScoringInput(
    val questPoints: Int,
    val reputationTrackSpace: ReputationTrackSpace,
) : ScoringInput

/**
 * Scoring engine for the solo variant of For the Council (docs/rules/for-the-council.md,
 * "Scoring" > "Solo" and "Outcome"). Unlike the other scenarios here, this one is Quest-driven:
 * there's no base Fame or Standard Achievements, just quest points plus/minus a Reputation
 * bonus or penalty.
 */
object ForTheCouncilScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: ForTheCouncilScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown for For the Council, per docs/rules/for-the-council.md's
     * "Scoring" > "Solo" section: quest points, then Reputation - the space's modifier as a
     * bonus/penalty, or the fixed X-space penalty for the two spaces with no modifier.
     */
    fun breakdown(input: ForTheCouncilScoringInput): List<ScoreLineItem> {
        // ?: only substitutes X_SPACE_PENALTY when modifier is null (the two X spaces) - every
        // other space always has a real modifier value, even 0 at the center.
        val reputationBonus = input.reputationTrackSpace.modifier ?: X_SPACE_PENALTY
        return listOf(
            ScoreLineItem("Quest Points", input.questPoints),
            ScoreLineItem("Reputation", reputationBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/for-the-council.md, "Outcome" section): Won iff you ended
     * with Reputation +2 or higher; Lost otherwise. A score is always produced either way.
     */
    fun outcome(input: ForTheCouncilScoringInput): Outcome =
        if (input.reputationTrackSpace.position >= SOLO_WINNING_REPUTATION) Outcome.WON else Outcome.LOST
}
