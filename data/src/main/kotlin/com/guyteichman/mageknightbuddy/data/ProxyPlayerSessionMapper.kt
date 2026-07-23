// Converts between the domain's ProxyPlayerSession/ProxyPlayerEvent/ProxyPlayerCard (plain
// Kotlin, no serialization annotations - see docs/adr/0001-domain-logic-as-plain-kotlin-module.md)
// and the data/ types Room and kotlinx.serialization actually persist: ProxyPlayerSessionEntity
// for the flat/JSON-column Room row, and ProxyPlayerCardDto/ProxyPlayerEventDto for the
// serializable mirrors. Mirrors VolkareSessionMapper.kt's structure, plus DummyPlayerSessionMapper's
// crystal-flattening convention.
package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Maps each domain ProxyPlayerCard variant to its DTO counterpart. The AdvancedAction case reuses
// CardIdentity's own toDto()/toDomain() shape (declared privately in DummyPlayerSessionMapper.kt
// for that file's own use) - re-declared here as a local helper since Kotlin `private` top-level
// functions aren't visible across files.
private fun CardIdentity.toDto(): CardIdentityDto = when (this) {
    is CardIdentity.SingleColor -> CardIdentityDto.SingleColor(color.name)
    is CardIdentity.DualColor -> CardIdentityDto.DualColor(colorA.name, colorB.name)
}

private fun CardIdentityDto.toDomain(): CardIdentity = when (this) {
    is CardIdentityDto.SingleColor -> CardIdentity.SingleColor(CardColor.valueOf(color))
    is CardIdentityDto.DualColor -> CardIdentity.DualColor(CardColor.valueOf(colorA), CardColor.valueOf(colorB))
}

private fun ProxyPlayerCard.toDto(): ProxyPlayerCardDto = when (this) {
    is ProxyPlayerCard.BasicAction -> ProxyPlayerCardDto.BasicAction(color.name)
    is ProxyPlayerCard.UniqueAction -> ProxyPlayerCardDto.UniqueAction(color.name)
    is ProxyPlayerCard.AdvancedAction -> ProxyPlayerCardDto.AdvancedAction(identity.toDto())
}

private fun ProxyPlayerCardDto.toDomain(): ProxyPlayerCard = when (this) {
    is ProxyPlayerCardDto.BasicAction -> ProxyPlayerCard.BasicAction(CardColor.valueOf(color))
    is ProxyPlayerCardDto.UniqueAction -> ProxyPlayerCard.UniqueAction(CardColor.valueOf(color))
    is ProxyPlayerCardDto.AdvancedAction -> ProxyPlayerCard.AdvancedAction(identity.toDomain())
}

private fun List<ProxyPlayerCard>.toJson(): String = Json.encodeToString(map { it.toDto() })
private fun String.toProxyPlayerCardList(): List<ProxyPlayerCard> = Json.decodeFromString<List<ProxyPlayerCardDto>>(this).map { it.toDomain() }

// Nullable Objective Card: null <-> null, otherwise a single-element JSON round-trip via the same
// DTO conversion as the deck/discard lists.
private fun ProxyPlayerCard?.toJsonOrNull(): String? = this?.let { Json.encodeToString(it.toDto()) }
private fun String?.toProxyPlayerCardOrNull(): ProxyPlayerCard? = this?.let { Json.decodeFromString<ProxyPlayerCardDto>(it).toDomain() }

private fun ProxyPlayerEvent.toDto(): ProxyPlayerEventDto = when (this) {
    is ProxyPlayerEvent.RoundStarted -> ProxyPlayerEventDto.RoundStarted(round)
    is ProxyPlayerEvent.NewObjectiveDrawn -> ProxyPlayerEventDto.NewObjectiveDrawn(round, objectiveCard.toDto(), discarded.map { it.toDto() })
    is ProxyPlayerEvent.TurnContinued -> ProxyPlayerEventDto.TurnContinued(round, objectiveCard.toDto(), shieldsNow, revealed.map { it.toDto() })
    is ProxyPlayerEvent.EndOfRoundAnnounced -> ProxyPlayerEventDto.EndOfRoundAnnounced(round)
    is ProxyPlayerEvent.ObjectiveResolved -> ProxyPlayerEventDto.ObjectiveResolved(round, objectiveCard.toDto(), resolution.name)
    is ProxyPlayerEvent.RoundEnded -> ProxyPlayerEventDto.RoundEnded(round, advancedActionOfferColor.toDto(), spellOfferColor.name, discardedObjective?.toDto())
}

private fun ProxyPlayerEventDto.toDomain(): ProxyPlayerEvent = when (this) {
    is ProxyPlayerEventDto.RoundStarted -> ProxyPlayerEvent.RoundStarted(round)
    is ProxyPlayerEventDto.NewObjectiveDrawn -> ProxyPlayerEvent.NewObjectiveDrawn(round, objectiveCard.toDomain(), discarded.map { it.toDomain() })
    is ProxyPlayerEventDto.TurnContinued -> ProxyPlayerEvent.TurnContinued(round, objectiveCard.toDomain(), shieldsNow, revealed.map { it.toDomain() })
    is ProxyPlayerEventDto.EndOfRoundAnnounced -> ProxyPlayerEvent.EndOfRoundAnnounced(round)
    is ProxyPlayerEventDto.ObjectiveResolved -> ProxyPlayerEvent.ObjectiveResolved(round, objectiveCard.toDomain(), ProxyPlayerObjectiveResolution.valueOf(resolution))
    is ProxyPlayerEventDto.RoundEnded -> ProxyPlayerEvent.RoundEnded(round, advancedActionOfferColor.toDomain(), CardColor.valueOf(spellOfferColor), discardedObjective?.toDomain())
}

/**
 * Converts a domain session into the Room row that persists it, ready for
 * [ProxyPlayerSessionDao.upsert]. Mirrors [DummyPlayerSessionMapper]'s crystal-flattening and
 * [VolkareSessionMapper]'s JSON-column conventions.
 */
fun ProxyPlayerSession.toEntity(updatedAt: Long = System.currentTimeMillis()): ProxyPlayerSessionEntity = ProxyPlayerSessionEntity(
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
    objectiveCardJson = objectiveCard.toJsonOrNull(),
    objectiveShields = objectiveShields,
    logJson = Json.encodeToString(log.map { it.toDto() }),
    updatedAt = updatedAt,
)

/** Converts a persisted Room row back into a domain session, via [ProxyPlayerSession.restore] (the reverse of [toEntity] above). */
fun ProxyPlayerSessionEntity.toDomain(): ProxyPlayerSession = ProxyPlayerSession.restore(
    knight = Knight.valueOf(knight),
    wasRandom = wasRandom,
    deckOrder = deckOrderJson.toProxyPlayerCardList(),
    discardPile = discardPileJson.toProxyPlayerCardList(),
    crystals = mapOf(
        CardColor.RED to crystalsRed,
        CardColor.GREEN to crystalsGreen,
        CardColor.BLUE to crystalsBlue,
        CardColor.WHITE to crystalsWhite,
    ),
    round = round,
    roundEnded = roundEnded,
    objectiveCard = objectiveCardJson.toProxyPlayerCardOrNull(),
    objectiveShields = objectiveShields,
    log = Json.decodeFromString<List<ProxyPlayerEventDto>>(logJson).map { it.toDomain() },
)
