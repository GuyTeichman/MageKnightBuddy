package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    // One-shot snapshot (a suspend List, not a live Flow) - used to export the whole history to a
    // backup file, where a single point-in-time read is all that's wanted.
    @Query("SELECT * FROM scoring_sessions ORDER BY playedAtEpochMillis DESC")
    suspend fun getAllOnce(): List<ScoringSessionEntity>

    // Bulk insert for restore, so a whole backup's worth of rows goes in with one call.
    @Insert
    suspend fun insertAll(entities: List<ScoringSessionEntity>)

    @Query("DELETE FROM scoring_sessions")
    suspend fun deleteAll()

    /**
     * Replaces the entire table with [entities] (used by "restore from backup"). @Transaction makes
     * the wipe + re-insert atomic: if the insert fails, the delete rolls back too, so a failed
     * restore can never leave the history empty or half-populated. Room generates the transaction
     * wrapper around this concrete method's body.
     */
    @Transaction
    suspend fun replaceAll(entities: List<ScoringSessionEntity>) {
        deleteAll()
        insertAll(entities)
    }
}
