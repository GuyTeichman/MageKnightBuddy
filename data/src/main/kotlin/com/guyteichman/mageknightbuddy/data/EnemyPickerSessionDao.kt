package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room data-access object for the single autosaved Enemy Picker session. Mirrors
 * [VolkareSessionDao] exactly: one saved session (see [EnemyPickerSessionEntity]'s fixed-id
 * design), so just "save the current state", "load whatever was last saved", and a cheap
 * timestamp-only read.
 */
@Dao
interface EnemyPickerSessionDao {
    /**
     * Saves [entity] as the current session, overwriting whatever was saved before. `REPLACE` +
     * the fixed [EnemyPickerSessionEntity.SINGLETON_ID] key makes this an upsert (see [VolkareSessionDao.upsert]).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EnemyPickerSessionEntity)

    /** Loads the autosaved session, or `null` if nothing has been saved yet. */
    @Query("SELECT * FROM enemy_picker_sessions WHERE id = ${EnemyPickerSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): EnemyPickerSessionEntity?

    /** Reads just the autosaved session's [EnemyPickerSessionEntity.updatedAt], or `null` if none. */
    @Query("SELECT updatedAt FROM enemy_picker_sessions WHERE id = ${EnemyPickerSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun getUpdatedAt(): Long?
}
