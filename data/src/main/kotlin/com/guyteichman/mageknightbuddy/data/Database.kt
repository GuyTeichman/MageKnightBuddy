package com.guyteichman.mageknightbuddy.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Builds the app's real [MageKnightBuddyDatabase] instance, backed by a file on disk.
 *
 * This is the one place production code constructs the database (tests build their own
 * in-memory instances instead), so any wiring that should apply everywhere - the driver,
 * migration strategy, file name - lives here.
 */
fun createDatabase(context: Context): MageKnightBuddyDatabase =
    // Room.databaseBuilder returns a builder; each `.xyz(...)` call below configures one aspect
    // and returns the same builder, so the chain reads top-to-bottom as a list of settings
    // before `.build()` actually constructs the database.
    Room.databaseBuilder(context, MageKnightBuddyDatabase::class.java, "mageknightbuddy.db")
        // BundledSQLiteDriver runs Room's SQLite engine itself rather than relying on the
        // Android platform's built-in SQLite. Using the same driver in production and in
        // JVM unit tests keeps their behavior identical - see ADR-0003 for why tests need this.
        .setDriver(BundledSQLiteDriver())
        // No migrations are written yet, so a version bump would otherwise crash at runtime.
        // Instead, wipe and recreate all tables on schema changes - acceptable pre-release,
        // since there's no user data to preserve yet.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
