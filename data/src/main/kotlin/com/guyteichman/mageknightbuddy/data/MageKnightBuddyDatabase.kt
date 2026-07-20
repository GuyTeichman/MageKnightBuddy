package com.guyteichman.mageknightbuddy.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's single Room database: one physical SQLite file (or in-memory instance in tests,
 * see ADR-0003) holding both the score-calculator and Dummy-Player session tables.
 *
 * This class only declares *what* the database contains - Room generates the actual
 * implementation. Obtain a real instance via [createDatabase]; tests build their own directly
 * against this class using Room's in-memory builder.
 */
// The @Database annotation is Room's entry point: it tells the Room compiler (KSP) which
// entities (tables) belong to this database and what version the schema is at, so it can
// generate the actual RoomDatabase implementation (a class named `MageKnightBuddyDatabase_Impl`)
// behind the scenes. `exportSchema = false` skips writing that schema to a JSON file on disk,
// since this project isn't tracking schema history for migrations yet.
@Database(
    entities = [ScoringSessionEntity::class, DummyPlayerSessionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class MageKnightBuddyDatabase : RoomDatabase() {
    // Room generates the DAO implementation for each abstract accessor below and wires it
    // to this database, so callers just call these functions to get a working DAO instance.
    abstract fun scoringSessionDao(): ScoringSessionDao
    abstract fun dummyPlayerSessionDao(): DummyPlayerSessionDao
}
