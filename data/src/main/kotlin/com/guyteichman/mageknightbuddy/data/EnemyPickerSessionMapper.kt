// Converts between the domain's EnemyPickerSession/TokenPile/DrawLogEntry (plain Kotlin, no
// serialization annotations - see docs/adr/0001-domain-logic-as-plain-kotlin-module.md) and the
// data/ types Room and kotlinx.serialization actually persist: EnemyPickerSessionEntity for the
// flat/JSON-column Room row, and TokenPileDto/DrawLogEntryDto for the serializable mirrors of the
// pile map and Draw Log. Mirrors VolkareSessionMapper.kt's structure.
package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.DrawLogEntry
import com.guyteichman.mageknightbuddy.domain.EnemyPickerSession
import com.guyteichman.mageknightbuddy.domain.Expansion
import com.guyteichman.mageknightbuddy.domain.TokenPile
import com.guyteichman.mageknightbuddy.domain.TokenPileId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun TokenPile.toDto(): TokenPileDto = TokenPileDto(drawPile = drawPile, discardPile = discardPile)
private fun TokenPileDto.toDomain(): TokenPile = TokenPile(drawPile = drawPile, discardPile = discardPile)

private fun DrawLogEntry.toDto(): DrawLogEntryDto =
    DrawLogEntryDto(tokenId = tokenId, pile = pile.name, batchId = batchId, stillInPlay = stillInPlay, note = note)

private fun DrawLogEntryDto.toDomain(): DrawLogEntry =
    DrawLogEntry(tokenId = tokenId, pile = TokenPileId.valueOf(pile), batchId = batchId, stillInPlay = stillInPlay, note = note)

// The pile map is keyed by the TokenPileId enum in the domain; JSON needs String keys, so the map
// is serialized with each key's enum name and re-keyed back on the way in.
private fun Map<TokenPileId, TokenPile>.toJson(): String =
    Json.encodeToString(entries.associate { (id, pile) -> id.name to pile.toDto() })

private fun String.toPileMap(): Map<TokenPileId, TokenPile> =
    Json.decodeFromString<Map<String, TokenPileDto>>(this)
        .entries.associate { (name, dto) -> TokenPileId.valueOf(name) to dto.toDomain() }

/**
 * Converts a domain session into the Room row that persists it, ready for
 * [EnemyPickerSessionDao.upsert]. The token set, pile map, and Draw Log are each serialized to a
 * JSON string column; [updatedAt] defaults to "now" but is an explicit parameter so tests can pin
 * it, same convention as [VolkareSession.toEntity].
 */
fun EnemyPickerSession.toEntity(updatedAt: Long = System.currentTimeMillis()): EnemyPickerSessionEntity = EnemyPickerSessionEntity(
    drawWithReplacement = drawWithReplacement,
    // Store the expansion set as a JSON array of enum names.
    tokenSetJson = Json.encodeToString(tokenSet.map { it.name }),
    pilesJson = piles.toJson(),
    drawLogJson = Json.encodeToString(drawLog.map { it.toDto() }),
    updatedAt = updatedAt,
)

/**
 * Converts a persisted Room row back into a domain session, via [EnemyPickerSession.restore] (the
 * reverse of [toEntity] above; used after [EnemyPickerSessionDao.get] loads a saved row).
 */
fun EnemyPickerSessionEntity.toDomain(): EnemyPickerSession = EnemyPickerSession.restore(
    tokenSet = Json.decodeFromString<List<String>>(tokenSetJson).map { Expansion.valueOf(it) }.toSet(),
    drawWithReplacement = drawWithReplacement,
    piles = pilesJson.toPileMap(),
    drawLog = Json.decodeFromString<List<DrawLogEntryDto>>(drawLogJson).map { it.toDomain() },
)
