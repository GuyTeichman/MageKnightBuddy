package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DummyPlayerSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DummyPlayerSessionEntity)

    @Query("SELECT * FROM dummy_player_sessions WHERE id = ${DummyPlayerSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): DummyPlayerSessionEntity?
}
