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
 * Total score for whichever scenario this input belongs to. Every scenario's own `*Scoring`
 * object defines its `score(input)` as `breakdown(input).sumOf { it.value }` (summing that
 * scenario's line items), so rather than re-dispatching per scenario here, this just sums the
 * dispatcher-level [breakdown] directly - same result, one line instead of fifteen branches.
 */
fun ScoringInput.score(): Int = breakdown().sumOf { it.value }

/** Win/Loss check for whichever scenario this input belongs to; see [score] for the dispatch pattern. */
fun ScoringInput.outcome(): Outcome = when (this) {
    is SoloConquestScoringInput -> SoloConquestScoring.outcome(this)
    is FirstReconnaissanceScoringInput -> FirstReconnaissanceScoring.outcome(this)
    is ForTheCouncilScoringInput -> ForTheCouncilScoring.outcome(this)
    is HiddenValleyScoringInput -> HiddenValleyScoring.outcome(this)
    is RealmOfTheDeadScoringInput -> RealmOfTheDeadScoring.outcome(this)
    is AgainstTheDragonScoringInput -> AgainstTheDragonScoring.outcome(this)
    is AgainstTheHorsemenScoringInput -> AgainstTheHorsemenScoring.outcome(this)
    is ApocalypseIsHereScoringInput -> ApocalypseIsHereScoring.outcome(this)
    is FracturedLandsScoringInput -> FracturedLandsScoring.outcome(this)
    is LifeAndDeathScoringInput -> LifeAndDeathScoring.outcome(this)
    is LostRelicScoringInput -> LostRelicScoring.outcome(this)
    is AgainstTheApocalypseScoringInput -> AgainstTheApocalypseScoring.outcome(this)
    is SoloConquestChallengeScoringInput -> SoloConquestChallengeScoring.outcome(this)
    is VolkaresQuestScoringInput -> VolkaresQuestScoring.outcome(this)
    is VolkaresReturnScoringInput -> VolkaresReturnScoring.outcome(this)
}

/** Itemized score breakdown for whichever scenario this input belongs to; see [score] for the dispatch pattern. */
fun ScoringInput.breakdown(): List<ScoreLineItem> = when (this) {
    is SoloConquestScoringInput -> SoloConquestScoring.breakdown(this)
    is FirstReconnaissanceScoringInput -> FirstReconnaissanceScoring.breakdown(this)
    is ForTheCouncilScoringInput -> ForTheCouncilScoring.breakdown(this)
    is HiddenValleyScoringInput -> HiddenValleyScoring.breakdown(this)
    is RealmOfTheDeadScoringInput -> RealmOfTheDeadScoring.breakdown(this)
    is AgainstTheDragonScoringInput -> AgainstTheDragonScoring.breakdown(this)
    is AgainstTheHorsemenScoringInput -> AgainstTheHorsemenScoring.breakdown(this)
    is ApocalypseIsHereScoringInput -> ApocalypseIsHereScoring.breakdown(this)
    is FracturedLandsScoringInput -> FracturedLandsScoring.breakdown(this)
    is LifeAndDeathScoringInput -> LifeAndDeathScoring.breakdown(this)
    is LostRelicScoringInput -> LostRelicScoring.breakdown(this)
    is AgainstTheApocalypseScoringInput -> AgainstTheApocalypseScoring.breakdown(this)
    is SoloConquestChallengeScoringInput -> SoloConquestChallengeScoring.breakdown(this)
    is VolkaresQuestScoringInput -> VolkaresQuestScoring.breakdown(this)
    is VolkaresReturnScoringInput -> VolkaresReturnScoring.breakdown(this)
}
