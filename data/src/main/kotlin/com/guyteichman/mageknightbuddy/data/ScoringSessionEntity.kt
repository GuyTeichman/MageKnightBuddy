package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one scored session. [inputJson] holds the scenario's `*ScoringInput` encoded via
 * [ScoringInputDto] (see that file for why) - one JSON column instead of a separate wide column
 * per field of every scenario's differently-shaped input, which couldn't scale past Solo Conquest.
 */
@Entity(tableName = "scoring_sessions")
data class ScoringSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenario: String,
    val knight: String,
    val playerName: String?,
    val inputJson: String,
    val score: Int,
    val outcome: String,
    val playedAtEpochMillis: Long,
)
