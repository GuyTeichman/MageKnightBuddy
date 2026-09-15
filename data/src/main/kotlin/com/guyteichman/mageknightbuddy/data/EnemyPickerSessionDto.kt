package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirrors of the Enemy Picker's session-state pieces that Room stores as JSON
 * columns: one [TokenPile][com.guyteichman.mageknightbuddy.domain.TokenPile] and one
 * [DrawLogEntry][com.guyteichman.mageknightbuddy.domain.DrawLogEntry]. Same reason these live in
 * `data/` rather than annotating the domain types directly as everything else here does: it
 * decouples the persisted wire format from the domain shape, so the domain types can be
 * refactored freely without breaking already-stored rows or the backup format. (Note the
 * *catalogue* types like `EnemyToken` are a deliberate exception, serialized in `domain` itself -
 * ADR-0007 - because that catalogue is bundled read-only data, not something persisted per-save
 * that a wire-format change could break.)
 *
 * Enum-typed domain fields ([TokenPileId][com.guyteichman.mageknightbuddy.domain.TokenPileId]) are
 * stored as their plain `String` name, the same convention as [VolkareSessionEntity.raceLevel].
 */
@Serializable
data class TokenPileDto(
    val drawPile: List<String>,
    val discardPile: List<String>,
)

/**
 * Mirror of [com.guyteichman.mageknightbuddy.domain.DrawLogEntry]; [pile] holds the pile enum's
 * name. [parentIndex] mirrors [com.guyteichman.mageknightbuddy.domain.DrawLogEntry.parentIndex]
 * (a Summon Draw child's summoner index), [ephemeral] mirrors
 * [com.guyteichman.mageknightbuddy.domain.DrawLogEntry.ephemeral] (whether a summon child is
 * discarded on draw vs held on the board, issue #251), and [possessedTokenId] mirrors
 * [com.guyteichman.mageknightbuddy.domain.DrawLogEntry.possessedTokenId] (the possessed token paired
 * with a possessed enemy's circular token) - all defaulted so older persisted rows without them
 * still decode, which is what keeps this an additive JSON change with no Room migration (cf. #194).
 */
@Serializable
data class DrawLogEntryDto(
    val tokenId: String,
    val pile: String,
    val batchId: Long,
    val defeated: Boolean = false,
    val note: String = "",
    val parentIndex: Int? = null,
    val ephemeral: Boolean = false,
    val possessedTokenId: String? = null,
)
