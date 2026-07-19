package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent], kept in
 * `data/` so the domain module stays free of serialization annotations/dependencies.
 */
@Serializable
sealed interface DummyPlayerEventDto {
    @Serializable
    @SerialName("round_started")
    data class RoundStarted(val round: Int) : DummyPlayerEventDto

    @Serializable
    @SerialName("turn_played")
    data class TurnPlayed(
        val round: Int,
        val initialReveal: List<String>,
        val additionalReveal: List<String>,
    ) : DummyPlayerEventDto

    @Serializable
    @SerialName("end_of_round_announced")
    data class EndOfRoundAnnounced(val round: Int) : DummyPlayerEventDto

    @Serializable
    @SerialName("round_ended")
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: String,
        val spellOfferColor: String,
    ) : DummyPlayerEventDto
}
