package com.guyteichman.mageknightbuddy.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Hand-written Room migrations, registered in [createDatabase].
 *
 * These replace the old blanket [androidx.room.RoomDatabase.Builder.fallbackToDestructiveMigration]
 * for the paths a *shipped* build can actually take, so upgrading over an existing install preserves
 * the user's data instead of dropping every table - and, crucially, avoids the destructive
 * drop-all-tables recreate that crash-looped a v10 -> v11 upgrade (issue #194): that recreate
 * contended with Room's InvalidationTracker over the single-writer connection during open and timed
 * out. A plain `CREATE TABLE` migration does no such wholesale recreate.
 */

/**
 * v10 (shipped in v1.0.0) -> v11: the only schema change was adding the Enemy Picker's autosave
 * table (issue #178); every other table is byte-for-byte identical between the two versions. The SQL
 * below is Room's own generated `CREATE TABLE` for [EnemyPickerSessionEntity], captured from a one-off
 * `exportSchema = true` build (see MageKnightBuddyDatabase for why export isn't left on), so the table
 * this creates matches what Room validates by construction - and MageKnightBuddyDatabaseMigrationTest
 * pins that: it fails if this SQL ever stops satisfying Room's runtime schema check.
 */
// Subclasses the Migration abstract class directly rather than the top-level `Migration(...)` factory
// function: the factory lives in `androidx.room.migration.MigrationKt`, which isn't on the classpath
// of the JVM unit tests' -jvm artifact substitutions (see ADR-0003), so the factory form fails there
// with a ClassNotFoundException at runtime while this form resolves everywhere.
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `enemy_picker_sessions` " +
                "(`id` INTEGER NOT NULL, `drawWithReplacement` INTEGER NOT NULL, " +
                "`tokenSetJson` TEXT NOT NULL, `pilesJson` TEXT NOT NULL, " +
                "`drawLogJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
    }
}
