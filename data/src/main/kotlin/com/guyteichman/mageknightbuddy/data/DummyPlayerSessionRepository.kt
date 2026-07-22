package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession

/**
 * Persistence entry point for the Dummy Player feature: the rest of the app (`app/`) calls
 * [save]/[restore] with plain domain [DummyPlayerSession] objects and never has to know about
 * Room, [DummyPlayerSessionEntity], or the DTO/JSON conversion in [DummyPlayerSessionMapper] -
 * this class hides all of that behind the DAO.
 */
class DummyPlayerSessionRepository(private val dao: DummyPlayerSessionDao) {
    /**
     * Autosaves [session], overwriting whatever was previously saved (there's only ever one
     * saved session - see [DummyPlayerSessionEntity]'s fixed-id design). [updatedAt] defaults to
     * "now" and is stamped onto the saved row - see [DummyPlayerSession.toEntity].
     *
     * `suspend` marks this as a coroutine function: it can perform the DAO's suspending database
     * write without blocking a thread, and must itself be called from a coroutine (or another
     * `suspend` function).
     */
    suspend fun save(session: DummyPlayerSession, updatedAt: Long = System.currentTimeMillis()) {
        dao.upsert(session.toEntity(updatedAt))
    }

    /**
     * Loads the autosaved session, or `null` if nothing has been saved yet.
     *
     * `dao.get()?.toDomain()` uses the safe call operator `?.`: [DummyPlayerSessionDao.get] runs
     * first, and `toDomain()` is only invoked if that returned a non-null entity - otherwise the
     * whole expression short-circuits to `null` without throwing.
     */
    suspend fun restore(): DummyPlayerSession? = dao.get()?.toDomain()

    /**
     * Reads just the last save's timestamp, or `null` if nothing has been saved yet - lets the
     * setup screen's "Restore Game" flow compare this session's recency against a saved
     * [com.guyteichman.mageknightbuddy.domain.VolkareSession] without paying to deserialize
     * either session's full JSON columns via [restore].
     */
    suspend fun updatedAt(): Long? = dao.getUpdatedAt()
}
