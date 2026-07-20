package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room's database-access interface for the `scoring_sessions` table. Room generates the
 * implementation of this interface at compile time from the annotations below, so there's no
 * hand-written SQL-execution code to maintain here.
 */
@Dao
interface ScoringSessionDao {
    // @Insert is a Room shortcut: Room writes the INSERT SQL itself just from the entity's
    // fields, no @Query string needed. `suspend` means this runs as a coroutine so it doesn't
    // block the calling thread while writing to disk.
    @Insert
    suspend fun insert(entity: ScoringSessionEntity)

    // Newest sessions first. Returning a Flow (rather than a plain List) means Room will
    // re-emit this query's results automatically whenever the table changes, so callers get
    // live updates instead of a one-time snapshot.
    @Query("SELECT * FROM scoring_sessions ORDER BY playedAtEpochMillis DESC")
    fun getAll(): Flow<List<ScoringSessionEntity>>
}
