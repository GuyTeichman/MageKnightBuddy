package com.guyteichman.mageknightbuddy.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Adds the dummy_player_sessions table (see DummyPlayerSessionEntity) without touching the
// existing scoring_sessions table/data - a personal-use app may already have real scores saved.
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dummy_player_sessions` (
                `id` INTEGER NOT NULL,
                `knight` TEXT NOT NULL,
                `wasRandom` INTEGER NOT NULL,
                `deckOrderJson` TEXT NOT NULL,
                `discardPileJson` TEXT NOT NULL,
                `crystalsRed` INTEGER NOT NULL,
                `crystalsGreen` INTEGER NOT NULL,
                `crystalsBlue` INTEGER NOT NULL,
                `crystalsWhite` INTEGER NOT NULL,
                `round` INTEGER NOT NULL,
                `roundEnded` INTEGER NOT NULL,
                `logJson` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}
