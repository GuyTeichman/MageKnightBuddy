package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import java.time.Instant

fun ScoringSession.toEntity(): ScoringSessionEntity {
    val unitsByLevel = input.standardAchievements.units.associateBy { it.level }
    fun healthy(level: Int) = unitsByLevel[level]?.healthyCount ?: 0
    fun wounded(level: Int) = unitsByLevel[level]?.woundedCount ?: 0

    return ScoringSessionEntity(
        scenario = scenario.id,
        knight = knight.name,
        playerName = playerName,
        fame = input.fame,
        spellsInDeck = input.standardAchievements.spellsInDeck,
        advancedActionsInDeck = input.standardAchievements.advancedActionsInDeck,
        unitsLevel1Healthy = healthy(1),
        unitsLevel1Wounded = wounded(1),
        unitsLevel2Healthy = healthy(2),
        unitsLevel2Wounded = wounded(2),
        unitsLevel3Healthy = healthy(3),
        unitsLevel3Wounded = wounded(3),
        unitsLevel4Healthy = healthy(4),
        unitsLevel4Wounded = wounded(4),
        shieldsOnAdventureSites = input.standardAchievements.shieldsOnAdventureSites,
        artifacts = input.standardAchievements.artifacts,
        crystalsInInventory = input.standardAchievements.crystalsInInventory,
        shieldsOnConquerSites = input.standardAchievements.shieldsOnConquerSites,
        woundsInDeck = input.standardAchievements.woundsInDeck,
        questPoints = input.questPoints,
        citiesConquered = input.citiesConquered,
        roundsFinishedEarly = input.roundsFinishedEarly,
        cardsRemainingInDummyDeck = input.cardsRemainingInDummyDeck,
        endOfRoundAnnounced = input.endOfRoundAnnounced,
        score = score,
        outcome = outcome.name,
        playedAtEpochMillis = playedAt.toEpochMilli(),
    )
}

fun ScoringSessionEntity.toDomain(): ScoringSession {
    val standardAchievements = StandardAchievements(
        spellsInDeck = spellsInDeck,
        advancedActionsInDeck = advancedActionsInDeck,
        units = listOf(
            UnitTally(level = 1, healthyCount = unitsLevel1Healthy, woundedCount = unitsLevel1Wounded),
            UnitTally(level = 2, healthyCount = unitsLevel2Healthy, woundedCount = unitsLevel2Wounded),
            UnitTally(level = 3, healthyCount = unitsLevel3Healthy, woundedCount = unitsLevel3Wounded),
            UnitTally(level = 4, healthyCount = unitsLevel4Healthy, woundedCount = unitsLevel4Wounded),
        ),
        shieldsOnAdventureSites = shieldsOnAdventureSites,
        artifacts = artifacts,
        crystalsInInventory = crystalsInInventory,
        shieldsOnConquerSites = shieldsOnConquerSites,
        woundsInDeck = woundsInDeck,
    )
    val input = SoloConquestScoringInput(
        fame = fame,
        standardAchievements = standardAchievements,
        citiesConquered = citiesConquered,
        roundsFinishedEarly = roundsFinishedEarly,
        cardsRemainingInDummyDeck = cardsRemainingInDummyDeck,
        endOfRoundAnnounced = endOfRoundAnnounced,
        questPoints = questPoints,
    )
    return ScoringSession(
        scenario = Scenario.fromId(scenario),
        knight = Knight.valueOf(knight),
        playerName = playerName,
        input = input,
        score = score,
        outcome = Outcome.valueOf(outcome),
        playedAt = Instant.ofEpochMilli(playedAtEpochMillis),
    )
}
