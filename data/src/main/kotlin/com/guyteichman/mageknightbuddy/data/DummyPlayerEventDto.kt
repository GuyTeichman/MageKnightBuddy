package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent], kept in
 * `data/` as a separate near-duplicate hierarchy rather than annotating the domain type directly,
 * so the persisted wire format is decoupled from the domain shape: [DummyPlayerEvent] can be
 * refactored freely without breaking already-stored rows or the backup format, since this DTO
 * hierarchy (and its `@Serializable`/`@SerialName` shape) only changes when a
 * `toDto()`/`toDomain()` edit in [DummyPlayerSessionMapper] deliberately changes it.
 *
 * `@Serializable` (from kotlinx.serialization) marks a type so the compiler generates the code
 * needed to turn instances to/from JSON. Because this is a sealed interface with several
 * implementations, kotlinx.serialization also needs a way to tell which subtype a given JSON blob
 * represents when decoding it back - that's what `@SerialName` on each subtype below provides (a
 * "type" discriminator string written into the JSON, e.g. `"round_started"`), enabling
 * polymorphic (sealed-hierarchy) serialization.
 */
@Serializable
sealed interface DummyPlayerEventDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent.RoundStarted]. */
    @Serializable
    @SerialName("round_started")
    data class RoundStarted(val round: Int) : DummyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent.TurnPlayed]. */
    @Serializable
    @SerialName("turn_played")
    data class TurnPlayed(
        val round: Int,
        val initialReveal: List<CardIdentityDto>,
        val additionalReveal: List<CardIdentityDto>,
    ) : DummyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent.EndOfRoundAnnounced]. */
    @Serializable
    @SerialName("end_of_round_announced")
    data class EndOfRoundAnnounced(val round: Int) : DummyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent.RoundEnded]. */
    @Serializable
    @SerialName("round_ended")
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardIdentityDto,
        val spellOfferColor: String,
    ) : DummyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent.TacticPicked]. */
    @Serializable
    @SerialName("tactic_picked")
    data class TacticPicked(val round: Int, val isDay: Boolean, val card: Int, val pickedByPlayer: Boolean) : DummyPlayerEventDto
}
