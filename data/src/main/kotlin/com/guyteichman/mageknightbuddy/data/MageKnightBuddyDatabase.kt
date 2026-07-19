package com.guyteichman.mageknightbuddy.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScoringSessionEntity::class, DummyPlayerSessionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class MageKnightBuddyDatabase : RoomDatabase() {
    abstract fun scoringSessionDao(): ScoringSessionDao
    abstract fun dummyPlayerSessionDao(): DummyPlayerSessionDao
}
