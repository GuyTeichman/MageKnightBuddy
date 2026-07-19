package com.guyteichman.mageknightbuddy.domain

import java.time.Instant

/**
 * A single completed, scored play-through: which scenario/knight was played, the raw
 * inputs the player entered, and the resulting score/outcome. This is the record that
 * gets persisted and shown in history.
 */
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
        /**
         * Builds a [ScoringSession] from raw player input, computing [score] and [outcome]
         * via [SoloConquestScoring] instead of making the caller pass them in by hand.
         * Prefer this over the raw constructor so score/outcome can never drift out of
         * sync with [input].
         */
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
