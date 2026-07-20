package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.ScoringSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The persistence-facing entry point the rest of the app uses to save and load
 * [ScoringSession]s, hiding the Room [ScoringSessionDao] and the entity<->domain conversion
 * behind a domain-typed API. This is the only place outside ScoringSessionMapper.kt that should
 * need to know [ScoringSessionEntity] exists.
 */
class ScoringSessionRepository(private val dao: ScoringSessionDao) {
    // toEntity() (see ScoringSessionMapper.kt) does the domain -> Room conversion before handing
    // off to the DAO's generated insert.
    suspend fun save(session: ScoringSession) {
        dao.insert(session.toEntity())
    }

    // Flow.map here transforms each emitted List<ScoringSessionEntity> into a
    // List<ScoringSession> via toDomain(), without subscribing/collecting the flow itself -
    // callers still get a live, auto-updating stream, just of domain objects instead of entities.
    fun getAll(): Flow<List<ScoringSession>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }
}
