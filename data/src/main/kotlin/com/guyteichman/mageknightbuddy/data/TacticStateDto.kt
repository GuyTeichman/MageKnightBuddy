package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.TacticState], shared by all
 * three Dummy Player tab mappers ([DummyPlayerSessionMapper], [ProxyPlayerSessionMapper],
 * [VolkareSessionMapper]) - kept in `data/` for the same reason [CardIdentityDto] is: `domain/`
 * must stay free of kotlinx.serialization annotations/dependencies (see
 * docs/adr/0001-domain-logic-as-plain-kotlin-module.md). Each mapper file declares its own private
 * `toDto()`/`toDomain()` conversion functions, since Kotlin `private` top-level functions aren't
 * visible across files (same reason [CardIdentityDto]'s conversion is redeclared per-mapper).
 */
@Serializable
data class TacticStateDto(
    val removedDayCards: Set<Int> = emptySet(),
    val removedNightCards: Set<Int> = emptySet(),
    val dummyPick: Int? = null,
    val playerPick: Int? = null,
)
