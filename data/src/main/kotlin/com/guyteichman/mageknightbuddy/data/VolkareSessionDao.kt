package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room data-access object for the single autosaved Volkare session. Mirrors
 * [DummyPlayerSessionDao] exactly: there is only ever one saved session (see
 * [VolkareSessionEntity]'s fixed-id design), so this DAO has no per-session lookups - just "save
 * the current state", "load whatever was last saved", and a cheap way to read just the save
 * timestamp without loading everything else.
 *
 * `@Dao` is a Room annotation: it tells Room to generate the actual SQL-backed implementation of
 * this interface at compile time, so the rest of the app only ever talks to the interface.
 */
@Dao
interface VolkareSessionDao {
    /**
     * Saves [entity] as the current session, overwriting whatever was saved before.
     *
     * `@Insert(onConflict = OnConflictStrategy.REPLACE)` makes this an upsert: because the entity
     * always carries the same fixed primary key ([VolkareSessionEntity.SINGLETON_ID]), a second
     * call collides with the first row's key and REPLACE tells Room to overwrite it rather than
     * throw a conflict error - which is exactly the single-slot autosave behavior wanted here
     * (see GitHub issue #27).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VolkareSessionEntity)

    /**
     * Loads the autosaved session, or `null` if nothing has been saved yet.
     *
     * The query filters on the same fixed [VolkareSessionEntity.SINGLETON_ID] used by [upsert],
     * since that id is the only row this table is ever expected to hold.
     */
    @Query("SELECT * FROM volkare_sessions WHERE id = ${VolkareSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): VolkareSessionEntity?

    /**
     * Reads just the autosaved session's [VolkareSessionEntity.updatedAt], or `null` if nothing
     * has been saved yet - a cheap recency check for the setup screen's "Restore Game" flow,
     * which needs to compare this session's freshness against a saved
     * [com.guyteichman.mageknightbuddy.domain.DummyPlayerSession] without paying the cost of
     * decoding [get]'s full JSON columns just to read one field.
     */
    @Query("SELECT updatedAt FROM volkare_sessions WHERE id = ${VolkareSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun getUpdatedAt(): Long?
}
