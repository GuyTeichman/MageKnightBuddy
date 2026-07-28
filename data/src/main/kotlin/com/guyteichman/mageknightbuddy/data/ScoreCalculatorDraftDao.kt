package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room data-access object for the single autosaved Score Calculator wizard draft. Mirrors
 * [EnemyPickerSessionDao] exactly: one saved row (see [ScoreCalculatorDraftEntity]'s fixed-id
 * design), so just "save the current draft", "load whatever was last saved", and a cheap
 * timestamp-only read.
 */
@Dao
interface ScoreCalculatorDraftDao {
    /**
     * Saves [entity] as the current draft, overwriting whatever was saved before. `REPLACE` +
     * the fixed [ScoreCalculatorDraftEntity.SINGLETON_ID] key makes this an upsert (see
     * [EnemyPickerSessionDao.upsert]).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScoreCalculatorDraftEntity)

    /** Loads the autosaved draft, or `null` if nothing has been saved yet. */
    @Query("SELECT * FROM score_calculator_drafts WHERE id = ${ScoreCalculatorDraftEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): ScoreCalculatorDraftEntity?

    /** Reads just the autosaved draft's [ScoreCalculatorDraftEntity.updatedAt], or `null` if none. */
    @Query("SELECT updatedAt FROM score_calculator_drafts WHERE id = ${ScoreCalculatorDraftEntity.SINGLETON_ID} LIMIT 1")
    suspend fun getUpdatedAt(): Long?
}
