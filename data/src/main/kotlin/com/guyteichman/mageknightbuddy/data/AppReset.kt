package com.guyteichman.mageknightbuddy.data

import androidx.room.Transactor
import androidx.room.execSQL
import androidx.room.useWriterConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The app-wide "reset to default" action (issue #304): returns every persisted store to a
 * first-launch state. That's two independent stores - the Room [database] (all 7 tables: the
 * Scoreboard's scored games, each Dummy Player tab autosave, the Enemy Picker pile, the Score
 * Calculator draft, and the Sites favorites) and the tutorial-seen [tutorialProgress] DataStore -
 * so the reset touches both, not just Room.
 *
 * This lives in `data` (not a ViewModel) because it's the one place that legitimately reaches across
 * every store at once; the Settings screen just calls [resetToDefault] behind its confirm dialog.
 *
 * Not covered here: state a screen is holding in memory *right now* (e.g. a Dummy Player game open on
 * screen). Reset clears what's persisted, but it can't reach into a live tab ViewModel - and if the
 * user keeps playing on that still-open screen, its autosave can write the in-memory game straight back
 * into the just-cleared table. Fully resetting such a session takes leaving and re-entering that screen
 * (or restarting the app). Live Room-Flow readers (Scoreboard, favorites) do update at once.
 */
class AppReset(
    private val database: MageKnightBuddyDatabase,
    private val tutorialProgress: TutorialProgressRepository,
) {
    /**
     * Wipes all Room tables and forgets every seen-tutorial flag.
     *
     * We deliberately do NOT use `RoomDatabase.clearAllTables()`: that's an Android-only method, absent
     * from the `room-runtime-jvm` artifact the data tests run against (ADR-0003), so relying on it would
     * make this reset impossible to test off-device. Instead we go through the common
     * [useWriterConnection] API (identical on Android and JVM) and `DELETE FROM` every user table. The
     * table names are read from SQLite's own `sqlite_master` catalogue rather than hardcoded, so adding
     * a new entity/table later is wiped automatically here with no edit - excluding SQLite's and Room's
     * internal bookkeeping tables (`sqlite_*`, `android_metadata`, `room_master_table`). The whole sweep
     * runs on [Dispatchers.IO] since the bundled SQLite driver's calls are blocking disk work.
     */
    suspend fun resetToDefault() {
        withContext(Dispatchers.IO) {
            database.useWriterConnection { connection ->
                // One transaction around the whole sweep makes it all-or-nothing: if any DELETE fails
                // partway (I/O error, a locked table), everything rolls back rather than leaving some
                // tables cleared and others not. Room's invalidation then also fires once, at commit,
                // so live Flows (Scoreboard, favorites) see a single clean update. Inside the block,
                // `this` is the transaction connection, so usePrepared/execSQL run on it directly.
                connection.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
                    // usePrepared runs the SELECT and hands back a cursor-like statement; step() advances
                    // it row by row, getText(0) reads this row's single `name` column into the list.
                    val tableNames = usePrepared(
                        "SELECT name FROM sqlite_master WHERE type = 'table' " +
                            "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' AND name != 'room_master_table'",
                    ) { statement ->
                        buildList { while (statement.step()) add(statement.getText(0)) }
                    }
                    // Names are quoted defensively; DELETE (not DROP) keeps the schema, just empties rows.
                    tableNames.forEach { name -> execSQL("DELETE FROM \"$name\"") }
                }
            }
        }
        tutorialProgress.clear()
    }
}
