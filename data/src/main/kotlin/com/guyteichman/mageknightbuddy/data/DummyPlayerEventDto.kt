package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent], kept in
 * `data/` so the domain module stays free of serialization annotations/dependencies. `domain/` is
 * meant to be pure Kotlin with zero Android/library dependencies (see
 * docs/adr/0001-domain-logic-as-plain-kotlin-module.md) so it could move to Kotlin Multiplatform
 * unchanged if iOS ever happens - putting `@Serializable` straight on the domain classes would
 * pull kotlinx.serialization into that module, so instead this near-duplicate hierarchy lives
 * here, and [DummyPlayerSessionMapper] converts between the two.
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
}
