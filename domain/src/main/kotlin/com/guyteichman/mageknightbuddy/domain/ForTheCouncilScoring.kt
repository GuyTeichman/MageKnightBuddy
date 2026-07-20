package com.guyteichman.mageknightbuddy.domain

// Solo win condition: Reputation +2 or higher (docs/rules/for-the-council.md, Scoring > Solo).
private const val SOLO_WINNING_REPUTATION = 2

// A Shield token on the Reputation track's X space scores -10 quest points instead of the
// normal Reputation modifier (docs/rules/for-the-council.md, Scoring > Solo).
private const val X_SPACE_PENALTY = -10

/**
 * Inputs for scoring a solo For the Council session (docs/rules/for-the-council.md,
 * "Scoring" > "Solo"): quest points, your Reputation modifier, whether your Shield token sits
 * on the Reputation track's X space, and your final Reputation (used for the Outcome check).
 *
 * [reputationModifier] and [reputation] are deliberately two separate fields, not a duplicate:
 * the Reputation track prints a *modifier* value at each space (used here for scoring) that is
 * usually different from the space's own position (the raw *Reputation* count, used only for the
 * Outcome threshold) - e.g. Reputation +2 prints a +1 modifier. See CONTEXT.md's "Reputation" /
 * "Reputation Modifier" glossary entries and the base rulebook's Reputation track (p.2, p.7).
 */
data class ForTheCouncilScoringInput(
    val questPoints: Int,
    val reputationModifier: Int,
    val shieldOnXSpace: Boolean,
    val reputation: Int,
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
     * "Scoring" > "Solo" section: quest points, then Reputation - your Reputation modifier as
     * a bonus, or the fixed X-space penalty if your Shield sits on the X space instead.
     */
    fun breakdown(input: ForTheCouncilScoringInput): List<ScoreLineItem> {
        // if/else as an expression: picks the X-space penalty or the normal Reputation
        // modifier and assigns whichever applies straight to the val.
        val reputationBonus = if (input.shieldOnXSpace) X_SPACE_PENALTY else input.reputationModifier
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
        if (input.reputation >= SOLO_WINNING_REPUTATION) Outcome.WON else Outcome.LOST
}
