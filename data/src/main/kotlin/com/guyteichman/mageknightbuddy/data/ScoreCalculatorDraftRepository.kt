package com.guyteichman.mageknightbuddy.data

/**
 * The Score Calculator wizard draft's instantiation of the generic [SingleSlotAutosaveRepository]
 * - see that class for behavior. `TDomain` is `Map<String, String>` rather than a dedicated domain
 * type, since the draft is pure in-progress UI form state (see [Map.toEntity]/[ScoreCalculatorDraftEntity.toDomain]).
 */
typealias ScoreCalculatorDraftRepository = SingleSlotAutosaveRepository<Map<String, String>, ScoreCalculatorDraftEntity>

/**
 * Builds a [ScoreCalculatorDraftRepository] around [dao], wiring in the `Map<String, String>` <->
 * [ScoreCalculatorDraftEntity] mapper pair - the same "factory that looks like a constructor" idiom
 * as [EnemyPickerSessionRepository].
 */
fun ScoreCalculatorDraftRepository(dao: ScoreCalculatorDraftDao): ScoreCalculatorDraftRepository =
    SingleSlotAutosaveRepository(
        upsert = dao::upsert,
        get = dao::get,
        getUpdatedAt = dao::getUpdatedAt,
        toEntity = Map<String, String>::toEntity,
        toDomain = ScoreCalculatorDraftEntity::toDomain,
    )
