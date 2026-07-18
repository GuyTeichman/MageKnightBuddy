package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.ScoringSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScoringSessionRepository(private val dao: ScoringSessionDao) {
    suspend fun save(session: ScoringSession) {
        dao.insert(session.toEntity())
    }

    fun getAll(): Flow<List<ScoringSession>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }
}
