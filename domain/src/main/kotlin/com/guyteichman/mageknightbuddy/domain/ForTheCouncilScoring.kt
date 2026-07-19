package com.guyteichman.mageknightbuddy.domain

private const val SOLO_WINNING_REPUTATION = 2
private const val X_SPACE_PENALTY = -10

data class ForTheCouncilScoringInput(
    val questPoints: Int,
    val reputationModifier: Int,
    val shieldOnXSpace: Boolean,
    val reputation: Int,
)

object ForTheCouncilScoring {
    fun score(input: ForTheCouncilScoringInput): Int = breakdown(input).sumOf { it.value }

    fun breakdown(input: ForTheCouncilScoringInput): List<ScoreLineItem> {
        val reputationBonus = if (input.shieldOnXSpace) X_SPACE_PENALTY else input.reputationModifier
        return listOf(
            ScoreLineItem("Quest Points", input.questPoints),
            ScoreLineItem("Reputation", reputationBonus),
        )
    }

    fun outcome(input: ForTheCouncilScoringInput): Outcome =
        if (input.reputation >= SOLO_WINNING_REPUTATION) Outcome.WON else Outcome.LOST
}
