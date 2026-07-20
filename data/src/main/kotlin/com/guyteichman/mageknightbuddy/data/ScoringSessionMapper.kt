package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import java.time.Instant

/**
 * Converts a domain [ScoringSession] to its Room-persistable [ScoringSessionEntity] form. The
 * domain module can't depend on Room (see docs/adr/0001-domain-logic-as-plain-kotlin-module.md),
 * so this is where the nested [com.guyteichman.mageknightbuddy.domain.StandardAchievements] and
 * the per-unit-level tallies get flattened out into the entity's individual columns.
 */
fun ScoringSession.toEntity(): ScoringSessionEntity {
    // Kotlin's associateBy turns the units list into a Map keyed by level (1-4), so the
    // healthy()/wounded() lookups below don't have to search the list each time.
    val unitsByLevel = input.standardAchievements.units.associateBy { it.level }
    // Local helper functions (scoped to this call only) that look up a level's tally and
    // default to 0 if that level has no entry, since the entity needs one column per level
    // rather than a list.
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
        // Room columns are plain Kotlin/Java types, so Instant (not natively storable) is
        // converted to a Long of milliseconds-since-epoch here, and back to an Instant in
        // toDomain() below.
        playedAtEpochMillis = playedAt.toEpochMilli(),
    )
}

/**
 * Converts a persisted [ScoringSessionEntity] back into the domain [ScoringSession] the rest of
 * the app works with, reversing [toEntity]: rebuilding the nested [StandardAchievements] and
 * the unit tallies from their flattened columns, and re-parsing the enum/scenario ids that were
 * stored as plain strings.
 */
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
        // valueOf(name) is the enum counterpart to Scenario.fromId: it looks up the enum
        // constant whose name matches the stored string, throwing if it no longer exists
        // (e.g. after a rename), which is why knight/outcome are stored by .name in toEntity().
        knight = Knight.valueOf(knight),
        playerName = playerName,
        input = input,
        score = score,
        outcome = Outcome.valueOf(outcome),
        playedAt = Instant.ofEpochMilli(playedAtEpochMillis),
    )
}
