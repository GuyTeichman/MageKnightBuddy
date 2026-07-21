package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.AgainstTheApocalypseScoringInput
import com.guyteichman.mageknightbuddy.domain.AgainstTheDragonScoringInput
import com.guyteichman.mageknightbuddy.domain.AgainstTheHorsemenScoringInput
import com.guyteichman.mageknightbuddy.domain.ApocalypseIsHereScoringInput
import com.guyteichman.mageknightbuddy.domain.CombatLevel
import com.guyteichman.mageknightbuddy.domain.FirstReconnaissanceScoringInput
import com.guyteichman.mageknightbuddy.domain.ForTheCouncilScoringInput
import com.guyteichman.mageknightbuddy.domain.FracturedLandsScoringInput
import com.guyteichman.mageknightbuddy.domain.HiddenValleyScoringInput
import com.guyteichman.mageknightbuddy.domain.LifeAndDeathScoringInput
import com.guyteichman.mageknightbuddy.domain.LostRelicScoringInput
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.RealmOfTheDeadScoringInput
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.ScoringInput
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import com.guyteichman.mageknightbuddy.domain.VolkaresQuestScoringInput
import com.guyteichman.mageknightbuddy.domain.VolkaresReturnScoringInput

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
        reputationTrackSpaceName = reputationTrackSpace.name,
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
    is AgainstTheDragonScoringInput -> ScoringInputDto.AgainstTheDragon(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        headsDefeated = headsDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is AgainstTheHorsemenScoringInput -> ScoringInputDto.AgainstTheHorsemen(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        horsemenDefeated = horsemenDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ApocalypseIsHereScoringInput -> ScoringInputDto.ApocalypseIsHere(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        horsemenDefeated = horsemenDefeated,
        headsDefeated = headsDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is FracturedLandsScoringInput -> ScoringInputDto.FracturedLands(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        questPoints = questPoints,
    )
    is LifeAndDeathScoringInput -> ScoringInputDto.LifeAndDeath(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        tezlaSpiritDefeated = tezlaSpiritDefeated,
        darkTezlaDefeated = darkTezlaDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is LostRelicScoringInput -> ScoringInputDto.LostRelic(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        relicPiecesFound = relicPiecesFound,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is AgainstTheApocalypseScoringInput -> ScoringInputDto.AgainstTheApocalypse(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        destroyedSiteTokens = destroyedSiteTokens,
        zigguratFloorsConquered = zigguratFloorsConquered,
        pyramidFloorsConquered = pyramidFloorsConquered,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is VolkaresQuestScoringInput -> ScoringInputDto.VolkaresQuest(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        citiesConquered = citiesConquered,
        combatLevelName = combatLevel.name,
        raceLevelName = raceLevel.name,
        volkareDefeated = volkareDefeated,
        cardsRemainingInVolkaresDeck = cardsRemainingInVolkaresDeck,
    )
    is VolkaresReturnScoringInput -> ScoringInputDto.VolkaresReturn(
        fame = fame,
        standardAchievements = standardAchievements.toDto(),
        cityConquered = cityConquered,
        volkareDefeated = volkareDefeated,
        combatLevelName = combatLevel.name,
        raceLevelName = raceLevel.name,
        cardsRemainingInVolkareDeck = cardsRemainingInVolkareDeck,
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
        reputationTrackSpace = ReputationTrackSpace.valueOf(reputationTrackSpaceName),
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
    is ScoringInputDto.AgainstTheDragon -> AgainstTheDragonScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        headsDefeated = headsDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ScoringInputDto.AgainstTheHorsemen -> AgainstTheHorsemenScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        horsemenDefeated = horsemenDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ScoringInputDto.ApocalypseIsHere -> ApocalypseIsHereScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        horsemenDefeated = horsemenDefeated,
        headsDefeated = headsDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ScoringInputDto.FracturedLands -> FracturedLandsScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        questPoints = questPoints,
    )
    is ScoringInputDto.LifeAndDeath -> LifeAndDeathScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        tezlaSpiritDefeated = tezlaSpiritDefeated,
        darkTezlaDefeated = darkTezlaDefeated,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ScoringInputDto.LostRelic -> LostRelicScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        relicPiecesFound = relicPiecesFound,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ScoringInputDto.AgainstTheApocalypse -> AgainstTheApocalypseScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        destroyedSiteTokens = destroyedSiteTokens,
        zigguratFloorsConquered = zigguratFloorsConquered,
        pyramidFloorsConquered = pyramidFloorsConquered,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
    )
    is ScoringInputDto.VolkaresQuest -> VolkaresQuestScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        citiesConquered = citiesConquered,
        combatLevel = CombatLevel.valueOf(combatLevelName),
        raceLevel = RaceLevel.valueOf(raceLevelName),
        volkareDefeated = volkareDefeated,
        cardsRemainingInVolkaresDeck = cardsRemainingInVolkaresDeck,
    )
    is ScoringInputDto.VolkaresReturn -> VolkaresReturnScoringInput(
        fame = fame,
        standardAchievements = standardAchievements.toDomain(),
        cityConquered = cityConquered,
        volkareDefeated = volkareDefeated,
        combatLevel = CombatLevel.valueOf(combatLevelName),
        raceLevel = RaceLevel.valueOf(raceLevelName),
        cardsRemainingInVolkareDeck = cardsRemainingInVolkareDeck,
    )
}
