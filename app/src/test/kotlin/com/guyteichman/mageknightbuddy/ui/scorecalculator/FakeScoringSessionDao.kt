package com.guyteichman.mageknightbuddy.ui.scorecalculator

import com.guyteichman.mageknightbuddy.data.ScoringSessionDao
import com.guyteichman.mageknightbuddy.data.ScoringSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeScoringSessionDao : ScoringSessionDao {
    val inserted = mutableListOf<ScoringSessionEntity>()

    override suspend fun insert(entity: ScoringSessionEntity) {
        inserted += entity
    }

    override fun getAll(): Flow<List<ScoringSessionEntity>> = flowOf(inserted)

    override suspend fun getAllOnce(): List<ScoringSessionEntity> = inserted.toList()

    override suspend fun insertAll(entities: List<ScoringSessionEntity>) {
        inserted += entities
    }

    override suspend fun deleteAll() {
        inserted.clear()
    }
}
