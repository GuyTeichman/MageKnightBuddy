package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun List<CardColor>.toJson(): String = Json.encodeToString(map { it.name })

private fun String.toCardColorList(): List<CardColor> = Json.decodeFromString<List<String>>(this).map { CardColor.valueOf(it) }

private fun DummyPlayerEvent.toDto(): DummyPlayerEventDto = when (this) {
    is DummyPlayerEvent.RoundStarted -> DummyPlayerEventDto.RoundStarted(round)
    is DummyPlayerEvent.TurnPlayed -> DummyPlayerEventDto.TurnPlayed(
        round = round,
        initialReveal = initialReveal.map { it.name },
        additionalReveal = additionalReveal.map { it.name },
    )
    is DummyPlayerEvent.EndOfRoundAnnounced -> DummyPlayerEventDto.EndOfRoundAnnounced(round)
    is DummyPlayerEvent.RoundEnded -> DummyPlayerEventDto.RoundEnded(
        round = round,
        advancedActionOfferColor = advancedActionOfferColor.name,
        spellOfferColor = spellOfferColor.name,
    )
}

private fun DummyPlayerEventDto.toDomain(): DummyPlayerEvent = when (this) {
    is DummyPlayerEventDto.RoundStarted -> DummyPlayerEvent.RoundStarted(round)
    is DummyPlayerEventDto.TurnPlayed -> DummyPlayerEvent.TurnPlayed(
        round = round,
        initialReveal = initialReveal.map { CardColor.valueOf(it) },
        additionalReveal = additionalReveal.map { CardColor.valueOf(it) },
    )
    is DummyPlayerEventDto.EndOfRoundAnnounced -> DummyPlayerEvent.EndOfRoundAnnounced(round)
    is DummyPlayerEventDto.RoundEnded -> DummyPlayerEvent.RoundEnded(
        round = round,
        advancedActionOfferColor = CardColor.valueOf(advancedActionOfferColor),
        spellOfferColor = CardColor.valueOf(spellOfferColor),
    )
}

fun DummyPlayerSession.toEntity(): DummyPlayerSessionEntity = DummyPlayerSessionEntity(
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
    logJson = Json.encodeToString(log.map { it.toDto() }),
)

fun DummyPlayerSessionEntity.toDomain(): DummyPlayerSession = DummyPlayerSession.restore(
    knight = Knight.valueOf(knight),
    wasRandom = wasRandom,
    deckOrder = deckOrderJson.toCardColorList(),
    discardPile = discardPileJson.toCardColorList(),
    crystals = mapOf(
        CardColor.RED to crystalsRed,
        CardColor.GREEN to crystalsGreen,
        CardColor.BLUE to crystalsBlue,
        CardColor.WHITE to crystalsWhite,
    ),
    round = round,
    roundEnded = roundEnded,
    log = Json.decodeFromString<List<DummyPlayerEventDto>>(logJson).map { it.toDomain() },
)
