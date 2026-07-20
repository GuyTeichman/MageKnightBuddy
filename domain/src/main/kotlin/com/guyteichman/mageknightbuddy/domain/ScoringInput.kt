package com.guyteichman.mageknightbuddy.domain

/**
 * Common type for every scenario's `*ScoringInput` data class (e.g. [SoloConquestScoringInput],
 * [FirstReconnaissanceScoringInput]). Lets [ScoringSession] and the app layer hold "whichever
 * scenario's input the player filled in" without hardcoding one scenario. Sealed so the
 * dispatcher functions below ([score], [outcome], [breakdown]) can `when` over every
 * implementation without needing an `else` branch - the compiler flags it if a new scenario is
 * added here but a dispatcher is forgotten.
 */
sealed interface ScoringInput

/**
 * Total score for whichever scenario this input belongs to, delegating to that scenario's own
 * `*Scoring` object (e.g. [SoloConquestScoring]) rather than duplicating any scoring logic here.
 */
fun ScoringInput.score(): Int = when (this) {
    is SoloConquestScoringInput -> SoloConquestScoring.score(this)
    is FirstReconnaissanceScoringInput -> FirstReconnaissanceScoring.score(this)
    is ForTheCouncilScoringInput -> ForTheCouncilScoring.score(this)
    is HiddenValleyScoringInput -> HiddenValleyScoring.score(this)
    is RealmOfTheDeadScoringInput -> RealmOfTheDeadScoring.score(this)
}

/** Win/Loss check for whichever scenario this input belongs to; see [score] for the dispatch pattern. */
fun ScoringInput.outcome(): Outcome = when (this) {
    is SoloConquestScoringInput -> SoloConquestScoring.outcome(this)
    is FirstReconnaissanceScoringInput -> FirstReconnaissanceScoring.outcome(this)
    is ForTheCouncilScoringInput -> ForTheCouncilScoring.outcome(this)
    is HiddenValleyScoringInput -> HiddenValleyScoring.outcome(this)
    is RealmOfTheDeadScoringInput -> RealmOfTheDeadScoring.outcome(this)
}

/** Itemized score breakdown for whichever scenario this input belongs to; see [score] for the dispatch pattern. */
fun ScoringInput.breakdown(): List<ScoreLineItem> = when (this) {
    is SoloConquestScoringInput -> SoloConquestScoring.breakdown(this)
    is FirstReconnaissanceScoringInput -> FirstReconnaissanceScoring.breakdown(this)
    is ForTheCouncilScoringInput -> ForTheCouncilScoring.breakdown(this)
    is HiddenValleyScoringInput -> HiddenValleyScoring.breakdown(this)
    is RealmOfTheDeadScoringInput -> RealmOfTheDeadScoring.breakdown(this)
}
