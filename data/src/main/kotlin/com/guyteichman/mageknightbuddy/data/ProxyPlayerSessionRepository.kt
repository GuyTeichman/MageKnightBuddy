package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession

/** Proxy Player's instantiation of the generic [SingleSlotAutosaveRepository] - see that class for behavior. */
typealias ProxyPlayerSessionRepository = SingleSlotAutosaveRepository<ProxyPlayerSession, ProxyPlayerSessionEntity>

/**
 * Builds a [ProxyPlayerSessionRepository] around [dao], wiring in [ProxyPlayerSession.toEntity]/
 * [ProxyPlayerSessionEntity.toDomain] as the mapper pair. A top-level function with the same name
 * as the typealias above is Kotlin's "factory that looks like a constructor" idiom (the same
 * trick the stdlib uses for e.g. `List(size, init)`) - it's what lets every existing call site
 * (`ProxyPlayerSessionRepository(dao)`, in `MageKnightBuddyApplication.kt` and this type's test)
 * keep compiling unchanged even though the real class behind the name is now generic.
 */
fun ProxyPlayerSessionRepository(dao: ProxyPlayerSessionDao): ProxyPlayerSessionRepository =
    SingleSlotAutosaveRepository(
        upsert = dao::upsert,
        get = dao::get,
        getUpdatedAt = dao::getUpdatedAt,
        toEntity = ProxyPlayerSession::toEntity,
        toDomain = ProxyPlayerSessionEntity::toDomain,
    )
