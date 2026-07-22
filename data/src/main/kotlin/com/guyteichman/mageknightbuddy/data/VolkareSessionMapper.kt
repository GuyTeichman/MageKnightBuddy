// Converts between the domain's VolkareSession/VolkareEvent/VolkareCard (plain Kotlin, no
// serialization annotations - see docs/adr/0001-domain-logic-as-plain-kotlin-module.md) and the
// data/ types that Room and kotlinx.serialization actually persist: VolkareSessionEntity for the
// flat/JSON-column Room row, and VolkareEventDto/VolkareCardDto for the serializable mirrors of
// the event log and its cards. Mirrors DummyPlayerSessionMapper.kt's structure exactly - keeping
// this conversion in one file means the domain module never needs to know these
// persistence-specific types exist.
package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareCard
import com.guyteichman.mageknightbuddy.domain.VolkareEvent
import com.guyteichman.mageknightbuddy.domain.VolkareSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Maps each domain VolkareCard variant to its VolkareCardDto counterpart. `VolkareCard.Wound` is
// a `data object` (a singleton), so its branch matches on the object itself rather than an
// `is` check - the `is` form is only needed for the two data-class variants that carry a color.
private fun VolkareCard.toDto(): VolkareCardDto = when (this) {
    is VolkareCard.BasicAction -> VolkareCardDto.BasicAction(color.name)
    is VolkareCard.CompetitiveSpell -> VolkareCardDto.CompetitiveSpell(color.name)
    VolkareCard.Wound -> VolkareCardDto.Wound
}

// The reverse of toDto() above: DTO variant back to the matching domain card variant.
private fun VolkareCardDto.toDomain(): VolkareCard = when (this) {
    is VolkareCardDto.BasicAction -> VolkareCard.BasicAction(CardColor.valueOf(color))
    is VolkareCardDto.CompetitiveSpell -> VolkareCard.CompetitiveSpell(CardColor.valueOf(color))
    VolkareCardDto.Wound -> VolkareCard.Wound
}

// `private fun List<VolkareCard>.toJson()` is a Kotlin extension function: it adds a `toJson()`
// method to the existing `List<VolkareCard>` type without subclassing or wrapping it. `private`
// keeps it usable only within this file. Each card is first mapped to its DTO mirror so the whole
// list can be serialized polymorphically (see VolkareCardDto's @SerialName discriminators).
private fun List<VolkareCard>.toJson(): String = Json.encodeToString(map { it.toDto() })

// The reverse: decode a JSON string back into a List<VolkareCardDto>, then convert each DTO to
// its domain card.
private fun String.toVolkareCardList(): List<VolkareCard> = Json.decodeFromString<List<VolkareCardDto>>(this).map { it.toDomain() }

// Maps each domain VolkareEvent variant to its VolkareEventDto counterpart. The `when` here is
// exhaustive over the sealed interface's variants - the compiler enforces every subtype is
// handled, and the `is VolkareEvent.X ->` branches both check the runtime type and smart-cast
// `this` to it, so its variant-specific fields (round, card, cityRevealed, ...) are accessible
// without an extra cast.
private fun VolkareEvent.toDto(): VolkareEventDto = when (this) {
    is VolkareEvent.RoundStarted -> VolkareEventDto.RoundStarted(round)
    is VolkareEvent.CardRevealed -> VolkareEventDto.CardRevealed(round, card.toDto(), cityRevealed)
    is VolkareEvent.Frenzy -> VolkareEventDto.Frenzy(round)
    is VolkareEvent.RoundEnded -> VolkareEventDto.RoundEnded(round)
    is VolkareEvent.QuestLost -> VolkareEventDto.QuestLost(round)
}

// The reverse of toDto() above: DTO variant back to the matching domain event variant.
private fun VolkareEventDto.toDomain(): VolkareEvent = when (this) {
    is VolkareEventDto.RoundStarted -> VolkareEvent.RoundStarted(round)
    is VolkareEventDto.CardRevealed -> VolkareEvent.CardRevealed(round, card.toDomain(), cityRevealed)
    is VolkareEventDto.Frenzy -> VolkareEvent.Frenzy(round)
    is VolkareEventDto.RoundEnded -> VolkareEvent.RoundEnded(round)
    is VolkareEventDto.QuestLost -> VolkareEvent.QuestLost(round)
}

/**
 * Converts a domain session into the Room row that persists it, ready for
 * [VolkareSessionDao.upsert]. [scenario] and [raceLevel] are stored by name/id since Room maps
 * enums/sealed types to `String` columns rather than natively, and the deck order / discard pile
 * / event log are each serialized to a JSON string column since Room can't store
 * `List`/sealed-class values directly either.
 *
 * [updatedAt] defaults to "now" (epoch millis), same as [DummyPlayerSession.toEntity] - an
 * explicit parameter (rather than always reading the clock internally) so tests can pin it to a
 * fixed value instead of asserting against a moving `System.currentTimeMillis()`.
 */
fun VolkareSession.toEntity(updatedAt: Long = System.currentTimeMillis()): VolkareSessionEntity = VolkareSessionEntity(
    scenario = scenario.id,
    raceLevel = raceLevel.name,
    deckOrderJson = deckOrder.toJson(),
    discardPileJson = discardPile.toJson(),
    round = round,
    cityRevealed = cityRevealed,
    lost = lost,
    // log.map { it.toDto() } converts each domain event to its DTO mirror before the whole list
    // is serialized to a single JSON string for the logJson column.
    logJson = Json.encodeToString(log.map { it.toDto() }),
    updatedAt = updatedAt,
)

/**
 * Converts a persisted Room row back into a domain session, via [VolkareSession.restore] (the
 * reverse of [toEntity] above; used after [VolkareSessionDao.get] loads a saved row).
 */
fun VolkareSessionEntity.toDomain(): VolkareSession = VolkareSession.restore(
    scenario = Scenario.fromId(scenario),
    raceLevel = RaceLevel.valueOf(raceLevel),
    deckOrder = deckOrderJson.toVolkareCardList(),
    discardPile = discardPileJson.toVolkareCardList(),
    round = round,
    cityRevealed = cityRevealed,
    lost = lost,
    // Json.decodeFromString<List<VolkareEventDto>>(...) parses the JSON column back into DTOs
    // (kotlinx.serialization uses the @SerialName discriminators from VolkareEventDto to pick the
    // right subtype for each list entry), then each DTO is converted to its domain event.
    log = Json.decodeFromString<List<VolkareEventDto>>(logJson).map { it.toDomain() },
)
