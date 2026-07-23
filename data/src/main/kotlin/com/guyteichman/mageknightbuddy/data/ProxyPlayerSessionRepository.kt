package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession

/**
 * Persistence entry point for the Dummy Player tab's Proxy Player mode: the rest of the app
 * (`app/`) calls [save]/[restore] with plain domain [ProxyPlayerSession] objects and never has to
 * know about Room, [ProxyPlayerSessionEntity], or the DTO/JSON conversion in
 * [ProxyPlayerSessionMapper]. Mirrors [VolkareSessionRepository]/[DummyPlayerSessionRepository] exactly.
 */
class ProxyPlayerSessionRepository(private val dao: ProxyPlayerSessionDao) {
    /** Autosaves [session], overwriting whatever was previously saved. [updatedAt] defaults to "now". */
    suspend fun save(session: ProxyPlayerSession, updatedAt: Long = System.currentTimeMillis()) {
        dao.upsert(session.toEntity(updatedAt))
    }

    /** Loads the autosaved session, or `null` if nothing has been saved yet. */
    suspend fun restore(): ProxyPlayerSession? = dao.get()?.toDomain()

    /** Reads just the last save's timestamp, or `null` if nothing has been saved yet - lets the setup screen's "Restore Game" flow compare recency across all 3 Dummy Player tab modes. */
    suspend fun updatedAt(): Long? = dao.getUpdatedAt()
}
