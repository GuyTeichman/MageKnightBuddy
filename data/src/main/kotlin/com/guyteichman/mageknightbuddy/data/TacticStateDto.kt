package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.TacticState
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.TacticState], shared by all
 * three Dummy Player tab mappers ([DummyPlayerSessionMapper], [ProxyPlayerSessionMapper],
 * [VolkareSessionMapper]). Kept as a separate `data/`-only type - rather than annotating
 * [TacticState] itself - so the persisted wire format is decoupled from the domain shape: the
 * domain class can be refactored freely without breaking already-stored rows or the backup
 * format, since [TacticStateDto] (and its `@Serializable` shape/field names) only changes when a
 * `toDto()`/`toDomain()` edit here deliberately changes it.
 */
@Serializable
data class TacticStateDto(
    val removedDayCards: Set<Int> = emptySet(),
    val removedNightCards: Set<Int> = emptySet(),
    val dummyPick: Int? = null,
    val playerPick: Int? = null,
)

// Maps the domain TacticState to its flat DTO mirror, field-for-field (both shapes are identical -
// TacticStateDto exists purely so kotlinx.serialization has an @Serializable type to encode).
// `internal` (rather than `private`) makes this visible to all three mapper files above, which
// live in the same Gradle module but different source files - a single shared copy instead of one
// private copy per mapper.
internal fun TacticState.toDto(): TacticStateDto = TacticStateDto(removedDayCards, removedNightCards, dummyPick, playerPick)

// The reverse of toDto() above.
internal fun TacticStateDto.toDomain(): TacticState = TacticState(removedDayCards, removedNightCards, dummyPick, playerPick)
