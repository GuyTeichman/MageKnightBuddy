package com.guyteichman.mageknightbuddy.data

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

    /** Loads the autosaved session, or `null` if nothing has been saved yet. */
    suspend fun restore(): TDomain? = get()?.toDomain()

    /** Reads just the last save's timestamp, or `null` if nothing has been saved yet. */
    suspend fun updatedAt(): Long? = getUpdatedAt()
}
