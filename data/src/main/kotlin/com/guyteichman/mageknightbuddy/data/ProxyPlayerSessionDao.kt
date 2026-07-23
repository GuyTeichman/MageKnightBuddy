package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room data-access object for the single autosaved Proxy Player session. Mirrors
 * [VolkareSessionDao]/[DummyPlayerSessionDao] exactly - see either's doc comment for why there
 * are no per-session lookups here.
 */
@Dao
interface ProxyPlayerSessionDao {
    /** Saves [entity] as the current session, overwriting whatever was saved before - see [ProxyPlayerSessionEntity.SINGLETON_ID]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProxyPlayerSessionEntity)

    /** Loads the autosaved session, or `null` if nothing has been saved yet. */
    @Query("SELECT * FROM proxy_player_sessions WHERE id = ${ProxyPlayerSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): ProxyPlayerSessionEntity?

    /** Reads just the autosaved session's [ProxyPlayerSessionEntity.updatedAt], or `null` if nothing has been saved yet - a cheap recency check for the setup screen's "Restore Game" flow. */
    @Query("SELECT updatedAt FROM proxy_player_sessions WHERE id = ${ProxyPlayerSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun getUpdatedAt(): Long?
}
