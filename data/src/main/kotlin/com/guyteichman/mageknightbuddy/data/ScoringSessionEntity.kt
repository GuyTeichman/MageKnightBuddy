package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scoring_sessions")
data class ScoringSessionEntity(
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
