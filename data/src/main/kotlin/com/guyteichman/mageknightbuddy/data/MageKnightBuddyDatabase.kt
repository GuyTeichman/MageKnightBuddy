package com.guyteichman.mageknightbuddy.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScoringSessionEntity::class], version = 1, exportSchema = false)
abstract class MageKnightBuddyDatabase : RoomDatabase() {
    abstract fun scoringSessionDao(): ScoringSessionDao
}
