package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent], the
 * Proxy Player counterpart to [VolkareEventDto]/[DummyPlayerEventDto] - see either's doc comment
 * for why this near-duplicate hierarchy lives in `data/` instead of on the domain type directly.
 */
@Serializable
sealed interface ProxyPlayerEventDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.RoundStarted]. */
    @Serializable
    @SerialName("round_started")
    data class RoundStarted(val round: Int) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.NewObjectiveDrawn]. */
    @Serializable
    @SerialName("new_objective_drawn")
    data class NewObjectiveDrawn(val round: Int, val objectiveCard: ProxyPlayerCardDto, val discarded: List<ProxyPlayerCardDto>) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.TurnContinued]. */
    @Serializable
    @SerialName("turn_continued")
    data class TurnContinued(val round: Int, val objectiveCard: ProxyPlayerCardDto, val shieldsNow: Int, val revealed: List<ProxyPlayerCardDto>) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.EndOfRoundAnnounced]. */
    @Serializable
    @SerialName("end_of_round_announced")
    data class EndOfRoundAnnounced(val round: Int) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.ObjectiveResolved]. */
    @Serializable
    @SerialName("objective_resolved")
    data class ObjectiveResolved(val round: Int, val objectiveCard: ProxyPlayerCardDto) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.RoundEnded]. */
    @Serializable
    @SerialName("round_ended")
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardIdentityDto,
        val spellOfferColor: String,
        val discardedObjective: ProxyPlayerCardDto?,
    ) : ProxyPlayerEventDto
}
