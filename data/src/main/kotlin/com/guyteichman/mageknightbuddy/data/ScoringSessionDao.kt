package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoringSessionDao {
    @Insert
    suspend fun insert(entity: ScoringSessionEntity)

    @Query("SELECT * FROM scoring_sessions ORDER BY playedAtEpochMillis DESC")
    fun getAll(): Flow<List<ScoringSessionEntity>>
}
