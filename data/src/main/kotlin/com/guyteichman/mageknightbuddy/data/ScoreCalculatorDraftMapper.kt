// Converts between the Score Calculator wizard's draft and the Room row that persists it. Unlike
// DummyPlayerSession/EnemyPickerSession, there's no dedicated domain type here: the draft is pure
// in-progress UI form state (see ScoreCalculatorViewModel's `resettable`), not a game-rule concept,
// so it stays a plain Map<String, String> (field key -> stringified value) instead of round-tripping
// through a ~50-field data class.
package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Converts a wizard draft into the Room row that persists it, ready for
 * [ScoreCalculatorDraftDao.upsert]. [updatedAt] defaults to "now" but is an explicit parameter so
 * tests can pin it, same convention as [EnemyPickerSession.toEntity][com.guyteichman.mageknightbuddy.domain.EnemyPickerSession].
 */
fun Map<String, String>.toEntity(updatedAt: Long = System.currentTimeMillis()): ScoreCalculatorDraftEntity =
    ScoreCalculatorDraftEntity(fieldsJson = Json.encodeToString(this), updatedAt = updatedAt)

/** Converts a persisted Room row back into a wizard draft (the reverse of [toEntity] above). */
fun ScoreCalculatorDraftEntity.toDomain(): Map<String, String> = Json.decodeFromString<Map<String, String>>(fieldsJson)
