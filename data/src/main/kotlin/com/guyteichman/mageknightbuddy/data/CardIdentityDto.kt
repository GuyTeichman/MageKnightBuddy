package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.CardIdentity], kept in
 * `data/` for the same reason [DummyPlayerEventDto] is: `domain/` must stay free of
 * kotlinx.serialization annotations/dependencies (see
 * docs/adr/0001-domain-logic-as-plain-kotlin-module.md). [DummyPlayerSessionMapper] converts
 * between the two.
 */
@Serializable
sealed interface CardIdentityDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.CardIdentity.SingleColor]. */
    @Serializable
    @SerialName("single_color")
    data class SingleColor(val color: String) : CardIdentityDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.CardIdentity.DualColor]. */
    @Serializable
    @SerialName("dual_color")
    data class DualColor(val colorA: String, val colorB: String) : CardIdentityDto
}
