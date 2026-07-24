package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession

/** Dummy Player's instantiation of the generic [SingleSlotAutosaveRepository] - see that class for behavior. */
typealias DummyPlayerSessionRepository = SingleSlotAutosaveRepository<DummyPlayerSession, DummyPlayerSessionEntity>

/**
 * Builds a [DummyPlayerSessionRepository] around [dao], wiring in [DummyPlayerSession.toEntity]/
 * [DummyPlayerSessionEntity.toDomain] as the mapper pair. A top-level function with the same name
 * as the typealias above is Kotlin's "factory that looks like a constructor" idiom (the same
 * trick the stdlib uses for e.g. `List(size, init)`) - it's what lets every existing call site
 * (`DummyPlayerSessionRepository(dao)`, in `MageKnightBuddyApplication.kt` and this type's test)
 * keep compiling unchanged even though the real class behind the name is now generic.
 */
fun DummyPlayerSessionRepository(dao: DummyPlayerSessionDao): DummyPlayerSessionRepository =
    SingleSlotAutosaveRepository(
        upsert = dao::upsert,
        get = dao::get,
        getUpdatedAt = dao::getUpdatedAt,
        toEntity = DummyPlayerSession::toEntity,
        toDomain = DummyPlayerSessionEntity::toDomain,
    )
