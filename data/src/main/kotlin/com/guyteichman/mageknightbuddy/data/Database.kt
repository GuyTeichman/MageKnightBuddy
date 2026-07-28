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
        // Real, data-preserving migrations for the version jumps a shipped build can take. v10 was
        // released as v1.0.0, so v10 -> v11 is the upgrade real users actually hit - handling it with
        // a proper Migration (not a destructive recreate) is what fixes issue #194's crash loop and
        // keeps their Scoreboard/session data. Every future bump should add its Migration here too.
        .addMigrations(MIGRATION_10_11)
        // Fallback for any *other* version gap with no migration written (e.g. a dev install left on
        // an old pre-release schema): recreate from scratch. Shipped users never reach this, since
        // their only live upgrade path (v10 -> v11) now has a real migration above.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
