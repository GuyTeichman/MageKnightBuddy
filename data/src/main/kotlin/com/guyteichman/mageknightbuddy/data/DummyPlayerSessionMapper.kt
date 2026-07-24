// Converts between the domain's DummyPlayerSession/DummyPlayerEvent (plain Kotlin, no
// serialization annotations - see docs/adr/0001-domain-logic-as-plain-kotlin-module.md) and the
// data/ types that Room and kotlinx.serialization actually persist: DummyPlayerSessionEntity for
// the flat/JSON-column Room row, and DummyPlayerEventDto for the serializable mirror of the event
// log. Keeping this conversion in one file means the domain module never needs to know these
// persistence-specific types exist.
package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Maps a domain CardIdentity to its DTO mirror.
private fun CardIdentity.toDto(): CardIdentityDto = when (this) {
    is CardIdentity.SingleColor -> CardIdentityDto.SingleColor(color.name)
    is CardIdentity.DualColor -> CardIdentityDto.DualColor(colorA.name, colorB.name)
}

// The reverse of toDto() above.
private fun CardIdentityDto.toDomain(): CardIdentity = when (this) {
    is CardIdentityDto.SingleColor -> CardIdentity.SingleColor(CardColor.valueOf(color))
    is CardIdentityDto.DualColor -> CardIdentity.DualColor(CardColor.valueOf(colorA), CardColor.valueOf(colorB))
}

// `private fun List<CardIdentity>.toJson()` is a Kotlin extension function: it adds a `toJson()`
// method to the existing `List<CardIdentity>` type (usable as `someList.toJson()`) without
// subclassing or wrapping it. `private` keeps it usable only within this file.
// `Json.encodeToString` (kotlinx.serialization) serializes the given value to a JSON string; here
// each CardIdentity is first mapped to its DTO mirror so the JSON only ever contains DTO types.
private fun List<CardIdentity>.toJson(): String = Json.encodeToString(map { it.toDto() })

// The reverse: decode a JSON string back into a List<CardIdentityDto> (kotlinx.serialization uses
// each entry's @SerialName discriminator to pick single- vs. dual-color), then convert each DTO
// back to its domain CardIdentity.
private fun String.toCardIdentityList(): List<CardIdentity> = Json.decodeFromString<List<CardIdentityDto>>(this).map { it.toDomain() }

// Maps each domain DummyPlayerEvent variant to its DummyPlayerEventDto counterpart. The `when`
// here is exhaustive over the sealed interface's variants (RoundStarted, TurnPlayed, ...) - the
// compiler enforces that every subtype is handled, and the `is DummyPlayerEvent.X ->` branches
// both check the runtime type and smart-cast `this` to it, so its variant-specific fields
// (round, initialReveal, ...) are accessible without an extra cast.
private fun DummyPlayerEvent.toDto(): DummyPlayerEventDto = when (this) {
    is DummyPlayerEvent.RoundStarted -> DummyPlayerEventDto.RoundStarted(round)
    is DummyPlayerEvent.TurnPlayed -> DummyPlayerEventDto.TurnPlayed(
        round = round,
        initialReveal = initialReveal.map { it.toDto() },
        additionalReveal = additionalReveal.map { it.toDto() },
    )
    is DummyPlayerEvent.EndOfRoundAnnounced -> DummyPlayerEventDto.EndOfRoundAnnounced(round)
    is DummyPlayerEvent.RoundEnded -> DummyPlayerEventDto.RoundEnded(
        round = round,
        advancedActionOfferColor = advancedActionOfferColor.toDto(),
        spellOfferColor = spellOfferColor.name,
    )
}

// The reverse of toDto() above: DTO variant back to the matching domain event variant.
private fun DummyPlayerEventDto.toDomain(): DummyPlayerEvent = when (this) {
    is DummyPlayerEventDto.RoundStarted -> DummyPlayerEvent.RoundStarted(round)
    is DummyPlayerEventDto.TurnPlayed -> DummyPlayerEvent.TurnPlayed(
        round = round,
        initialReveal = initialReveal.map { it.toDomain() },
        additionalReveal = additionalReveal.map { it.toDomain() },
    )
    is DummyPlayerEventDto.EndOfRoundAnnounced -> DummyPlayerEvent.EndOfRoundAnnounced(round)
    is DummyPlayerEventDto.RoundEnded -> DummyPlayerEvent.RoundEnded(
        round = round,
        advancedActionOfferColor = advancedActionOfferColor.toDomain(),
        spellOfferColor = CardColor.valueOf(spellOfferColor),
    )
}

/**
 * Converts a domain session into the Room row that persists it, ready for
 * [DummyPlayerSessionDao.upsert]. The crystal-count map is split into four named columns
 * (Room has no direct `Map` column support), and the deck order / discard pile / event log are
 * each serialized to a JSON string column since Room can't store `List`/sealed-class values
 * directly either.
 *
 * [updatedAt] defaults to "now" (epoch millis) since the common case is "save this as of right
 * now"; it's an explicit parameter (rather than always reading the clock internally) so tests can
 * pin it to a fixed value instead of asserting against a moving `System.currentTimeMillis()`.
 */
fun DummyPlayerSession.toEntity(updatedAt: Long = System.currentTimeMillis()): DummyPlayerSessionEntity = DummyPlayerSessionEntity(
    knight = knight.name,
    wasRandom = wasRandom,
    deckOrderJson = deckOrder.toJson(),
    discardPileJson = discardPile.toJson(),
    crystalsRed = crystals.getValue(CardColor.RED),
    crystalsGreen = crystals.getValue(CardColor.GREEN),
    crystalsBlue = crystals.getValue(CardColor.BLUE),
    crystalsWhite = crystals.getValue(CardColor.WHITE),
    round = round,
    roundEnded = roundEnded,
    // log.map { it.toDto() } converts each domain event to its DTO mirror before the whole list
    // is serialized to a single JSON string for the logJson column.
    logJson = Json.encodeToString(log.map { it.toDto() }),
    updatedAt = updatedAt,
    startsAtNight = startsAtNight,
)

/**
 * Converts a persisted Room row back into a domain session, via
 * [DummyPlayerSession.restore] (the reverse of [toEntity] above; used after
 * [DummyPlayerSessionDao.get] loads a saved row).
 */
fun DummyPlayerSessionEntity.toDomain(): DummyPlayerSession = DummyPlayerSession.restore(
    knight = Knight.valueOf(knight),
    wasRandom = wasRandom,
    deckOrder = deckOrderJson.toCardIdentityList(),
    discardPile = discardPileJson.toCardIdentityList(),
    crystals = mapOf(
        CardColor.RED to crystalsRed,
        CardColor.GREEN to crystalsGreen,
        CardColor.BLUE to crystalsBlue,
        CardColor.WHITE to crystalsWhite,
    ),
    round = round,
    roundEnded = roundEnded,
    // Json.decodeFromString<List<DummyPlayerEventDto>>(...) parses the JSON column back into DTOs
    // (kotlinx.serialization uses the @SerialName discriminators from DummyPlayerEventDto to pick
    // the right subtype for each list entry), then each DTO is converted to its domain event.
    log = Json.decodeFromString<List<DummyPlayerEventDto>>(logJson).map { it.toDomain() },
    startsAtNight = startsAtNight,
)
