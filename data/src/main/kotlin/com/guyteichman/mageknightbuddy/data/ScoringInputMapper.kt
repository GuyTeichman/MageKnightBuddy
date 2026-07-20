package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.FirstReconnaissanceScoringInput
import com.guyteichman.mageknightbuddy.domain.ForTheCouncilScoringInput
import com.guyteichman.mageknightbuddy.domain.HiddenValleyScoringInput
import com.guyteichman.mageknightbuddy.domain.RealmOfTheDeadScoringInput
import com.guyteichman.mageknightbuddy.domain.ScoringInput
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally

private fun StandardAchievements.toDto(): StandardAchievementsDto = StandardAchievementsDto(
    spellsInDeck = spellsInDeck,
    advancedActionsInDeck = advancedActionsInDeck,
    units = units.map { UnitTallyDto(level = it.level, healthyCount = it.healthyCount, woundedCount = it.woundedCount) },
    shieldsOnAdventureSites = shieldsOnAdventureSites,
    artifacts = artifacts,
    crystalsInInventory = crystalsInInventory,
    shieldsOnConquerSites = shieldsOnConquerSites,
    woundsInDeck = woundsInDeck,
)

private fun StandardAchievementsDto.toDomain(): StandardAchievements = StandardAchievements(
    spellsInDeck = spellsInDeck,
    advancedActionsInDeck = advancedActionsInDeck,
    units = units.map { UnitTally(level = it.level, healthyCount = it.healthyCount, woundedCount = it.woundedCount) },
    shieldsOnAdventureSites = shieldsOnAdventureSites,
    artifacts = artifacts,
    crystalsInInventory = crystalsInInventory,
    shieldsOnConquerSites = shieldsOnConquerSites,
    woundsInDeck = woundsInDeck,
)

/**
 * Converts any [ScoringInput] variant to its JSON-serializable [ScoringInputDto] mirror, one
 * `when` branch per scenario - see [ScoringInputDto] for why this indirection exists instead of
 * annotating the domain classes directly.
 */
fun ScoringInput.toDto(): ScoringInputDto = when (this) {
    is SoloConquestScoringInput -> ScoringInputDto.SoloConquest(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        citiesConquered = citiesConquered,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
        questPoints = questPoints,
    )
    is FirstReconnaissanceScoringInput -> ScoringInputDto.FirstReconnaissance(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        cityRevealed = cityRevealed,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ForTheCouncilScoringInput -> ScoringInputDto.ForTheCouncil(
        questPoints = questPoints,
        reputationModifier = reputationModifier,
        shieldOnXSpace = shieldOnXSpace,
        reputation = reputation,
    )
    is HiddenValleyScoringInput -> ScoringInputDto.HiddenValley(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        highPriestessDefeated = highPriestessDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is RealmOfTheDeadScoringInput -> ScoringInputDto.RealmOfTheDead(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        graveyardsSealed = graveyardsSealed,
        necromancerDefeated = necromancerDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}

/** Converts any [ScoringInputDto] variant back to its domain [ScoringInput]; the inverse of [ScoringInput.toDto]. */
fun ScoringInputDto.toDomain(): ScoringInput = when (this) {
    is ScoringInputDto.SoloConquest -> SoloConquestScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        citiesConquered = citiesConquered,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
        questPoints = questPoints,
    )
    is ScoringInputDto.FirstReconnaissance -> FirstReconnaissanceScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        cityRevealed = cityRevealed,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ScoringInputDto.ForTheCouncil -> ForTheCouncilScoringInput(
        questPoints = questPoints,
        reputationModifier = reputationModifier,
        shieldOnXSpace = shieldOnXSpace,
        reputation = reputation,
    )
    is ScoringInputDto.HiddenValley -> HiddenValleyScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        highPriestessDefeated = highPriestessDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ScoringInputDto.RealmOfTheDead -> RealmOfTheDeadScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        graveyardsSealed = graveyardsSealed,
        necromancerDefeated = necromancerDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
}
