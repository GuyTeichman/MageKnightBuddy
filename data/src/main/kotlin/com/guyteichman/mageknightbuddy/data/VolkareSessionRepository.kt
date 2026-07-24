package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.VolkareSession

/** Volkare's instantiation of the generic [SingleSlotAutosaveRepository] - see that class for behavior. */
typealias VolkareSessionRepository = SingleSlotAutosaveRepository<VolkareSession, VolkareSessionEntity>

/**
 * Builds a [VolkareSessionRepository] around [dao], wiring in [VolkareSession.toEntity]/
 * [VolkareSessionEntity.toDomain] as the mapper pair. A top-level function with the same name as
 * the typealias above is Kotlin's "factory that looks like a constructor" idiom (the same trick
 * the stdlib uses for e.g. `List(size, init)`) - it's what lets every existing call site
 * (`VolkareSessionRepository(dao)`, in `MageKnightBuddyApplication.kt` and this type's test) keep
 * compiling unchanged even though the real class behind the name is now generic.
 */
fun VolkareSessionRepository(dao: VolkareSessionDao): VolkareSessionRepository =
    SingleSlotAutosaveRepository(
        upsert = dao::upsert,
        get = dao::get,
        getUpdatedAt = dao::getUpdatedAt,
        toEntity = VolkareSession::toEntity,
        toDomain = VolkareSessionEntity::toDomain,
    )
