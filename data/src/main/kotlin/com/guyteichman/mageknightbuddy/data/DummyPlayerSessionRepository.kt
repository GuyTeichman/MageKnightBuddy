package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession

class DummyPlayerSessionRepository(private val dao: DummyPlayerSessionDao) {
    suspend fun save(session: DummyPlayerSession) {
        dao.upsert(session.toEntity())
    }

    suspend fun restore(): DummyPlayerSession? = dao.get()?.toDomain()
}
