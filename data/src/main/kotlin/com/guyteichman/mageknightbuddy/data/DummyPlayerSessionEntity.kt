package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dummy_player_sessions")
data class DummyPlayerSessionEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val knight: String,
    val wasRandom: Boolean,
    val deckOrderJson: String,
    val discardPileJson: String,
    val crystalsRed: Int,
    val crystalsGreen: Int,
    val crystalsBlue: Int,
    val crystalsWhite: Int,
    val round: Int,
    val roundEnded: Boolean,
    val logJson: String,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
