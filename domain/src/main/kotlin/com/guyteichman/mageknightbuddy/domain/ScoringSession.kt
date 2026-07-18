package com.guyteichman.mageknightbuddy.domain

import java.time.Instant

data class ScoringSession(
    val scenario: Scenario,
    val knight: Knight,
    val playerName: String?,
    val input: SoloConquestScoringInput,
    val score: Int,
    val outcome: Outcome,
    val playedAt: Instant,
) {
    companion object {
        fun create(
            scenario: Scenario,
            knight: Knight,
            playerName: String?,
            input: SoloConquestScoringInput,
            playedAt: Instant,
        ): ScoringSession = ScoringSession(
            scenario = scenario,
            knight = knight,
            playerName = playerName,
            input = input,
            score = SoloConquestScoring.score(input),
            outcome = SoloConquestScoring.outcome(input),
            playedAt = playedAt,
        )
    }
}
