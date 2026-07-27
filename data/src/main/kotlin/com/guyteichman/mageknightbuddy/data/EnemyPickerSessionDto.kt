package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirrors of the Enemy Picker's session-state pieces that Room stores as JSON
 * columns: one [TokenPile][com.guyteichman.mageknightbuddy.domain.TokenPile] and one
 * [DrawLogEntry][com.guyteichman.mageknightbuddy.domain.DrawLogEntry]. Same reason these live in
 * `data/` rather than annotating the domain types directly as everything else here does - keep the
 * domain module serialization-free per ADR-0001. (Note the *catalogue* types like `EnemyToken` are
 * the deliberate exception, serialized in `domain` itself - ADR-0007 - but session state is not.)
 *
 * Enum-typed domain fields ([TokenPileId][com.guyteichman.mageknightbuddy.domain.TokenPileId]) are
 * stored as their plain `String` name, the same convention as [VolkareSessionEntity.raceLevel].
 */
@Serializable
data class TokenPileDto(
    val drawPile: List<String>,
    val discardPile: List<String>,
)

/** Mirror of [com.guyteichman.mageknightbuddy.domain.DrawLogEntry]; [pile] holds the pile enum's name. */
@Serializable
data class DrawLogEntryDto(
    val tokenId: String,
    val pile: String,
    val batchId: Long,
    val defeated: Boolean = false,
    val note: String = "",
)
