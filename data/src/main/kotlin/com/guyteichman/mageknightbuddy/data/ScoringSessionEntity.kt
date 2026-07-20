package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The Room-persisted, flattened form of a completed [com.guyteichman.mageknightbuddy.domain.ScoringSession].
 * Room needs a plain data class with column-shaped (String/Int/Boolean/Long) properties to map
 * to a SQLite table row, which is why this exists separately from the domain type: the domain
 * module stays free of Room annotations (see docs/adr/0001-domain-logic-as-plain-kotlin-module.md),
 * and nested domain objects like [com.guyteichman.mageknightbuddy.domain.StandardAchievements]
 * and enums get spread out into individual columns here. The `toEntity`/`toDomain` extension
 * functions in ScoringSessionMapper.kt convert between the two shapes.
 */
@Entity(tableName = "scoring_sessions")
data class ScoringSessionEntity(
    // autoGenerate = true lets SQLite assign the id on insert; the 0 default is a placeholder
    // for not-yet-persisted instances and is overwritten by Room.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenario: String,
    val knight: String,
    val playerName: String?,
    val fame: Int,
    val spellsInDeck: Int,
    val advancedActionsInDeck: Int,
    val unitsLevel1Healthy: Int,
    val unitsLevel1Wounded: Int,
    val unitsLevel2Healthy: Int,
    val unitsLevel2Wounded: Int,
    val unitsLevel3Healthy: Int,
    val unitsLevel3Wounded: Int,
    val unitsLevel4Healthy: Int,
    val unitsLevel4Wounded: Int,
    val shieldsOnAdventureSites: Int,
    val artifacts: Int,
    val crystalsInInventory: Int,
    val shieldsOnConquerSites: Int,
    val woundsInDeck: Int,
    val questPoints: Int,
    val citiesConquered: Int,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
    val score: Int,
    val outcome: String,
    val playedAtEpochMillis: Long,
)
