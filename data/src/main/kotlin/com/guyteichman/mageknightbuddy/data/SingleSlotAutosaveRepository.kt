package com.guyteichman.mageknightbuddy.data

import android.util.Log
import kotlin.coroutines.cancellation.CancellationException

/**
 * Generic engine behind a single-slot ("one row, always overwritten") autosave repository, shared
 * by the Dummy Player tab's 3 session types (Dummy Player, Volkare, Proxy Player - see GitHub
 * issue #151). Knows nothing about Room, a specific Dao type, or any domain type: [upsert]/[get]/
 * [getUpdatedAt] are bound method references off a concrete Dao (e.g. `dao::upsert`), and
 * [toEntity]/[toDomain] are that session type's own mapper extension functions (e.g.
 * [DummyPlayerSession.toEntity]/[DummyPlayerSessionEntity.toDomain]) - passing these in as plain
 * lambdas/method references means this class never needs a shared Dao interface, so it adds zero
 * dependency on how Room's code generation handles generic `@Dao` supertypes.
 *
 * @param TDomain the plain-Kotlin domain session type (e.g. [DummyPlayerSession])
 * @param TEntity the Room entity type it's persisted as (e.g. [DummyPlayerSessionEntity])
 */
class SingleSlotAutosaveRepository<TDomain, TEntity>(
    private val upsert: suspend (TEntity) -> Unit,
    private val get: suspend () -> TEntity?,
    private val getUpdatedAt: suspend () -> Long?,
    private val toEntity: TDomain.(updatedAt: Long) -> TEntity,
    private val toDomain: TEntity.() -> TDomain,
) {
    /** Autosaves [session], overwriting whatever was previously saved. [updatedAt] defaults to "now". */
    suspend fun save(session: TDomain, updatedAt: Long = System.currentTimeMillis()) {
        upsert(session.toEntity(updatedAt))
    }

    /**
     * Loads the autosaved session, or `null` if nothing has been saved yet **or** if the saved row
     * no longer deserializes.
     *
     * A row that fails to decode is data written by an older build whose JSON shape has since changed
     * (a field renamed/removed, an enum value gone). Room's `fallbackToDestructiveMigration` can't
     * catch that - it only reacts to *column* changes, never to a `String` column whose JSON content
     * shape moved (see [PersistenceJson] and MageKnightBuddyDatabase's version history) - so rather
     * than let the exception crash the app on launch, we drop the row and return `null`, and the
     * caller starts a fresh session. This is the persistence-layer analogue of a destructive
     * migration; the golden-decode tests in `data/` are what stop a shape change from silently
     * discarding *shipped* users' data unnoticed.
     */
    suspend fun restore(): TDomain? = try {
        get()?.toDomain()
    } catch (e: CancellationException) {
        // Never swallow coroutine cancellation - rethrow so structured concurrency still works.
        throw e
    } catch (e: Exception) {
        Log.w("AutosaveRepository", "Discarding a saved session that no longer deserializes; starting fresh", e)
        null
    }

    /** Reads just the last save's timestamp, or `null` if nothing has been saved yet. */
    suspend fun updatedAt(): Long? = getUpdatedAt()
}
