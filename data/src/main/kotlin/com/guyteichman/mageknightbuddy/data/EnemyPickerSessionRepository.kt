package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.EnemyPickerSession

/** The Enemy Picker's instantiation of the generic [SingleSlotAutosaveRepository] - see that class for behavior. */
typealias EnemyPickerSessionRepository = SingleSlotAutosaveRepository<EnemyPickerSession, EnemyPickerSessionEntity>

/**
 * Builds an [EnemyPickerSessionRepository] around [dao], wiring in [EnemyPickerSession.toEntity]/
 * [EnemyPickerSessionEntity.toDomain] as the mapper pair - the same "factory that looks like a
 * constructor" idiom as [VolkareSessionRepository].
 */
fun EnemyPickerSessionRepository(dao: EnemyPickerSessionDao): EnemyPickerSessionRepository =
    SingleSlotAutosaveRepository(
        upsert = dao::upsert,
        get = dao::get,
        getUpdatedAt = dao::getUpdatedAt,
        toEntity = EnemyPickerSession::toEntity,
        toDomain = EnemyPickerSessionEntity::toDomain,
    )
