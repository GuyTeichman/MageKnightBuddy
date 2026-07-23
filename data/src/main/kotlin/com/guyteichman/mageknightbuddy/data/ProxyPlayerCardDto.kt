package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard], kept in
 * `data/` for the same reason [DummyPlayerEventDto] is (domain/ must stay free of serialization
 * annotations - see docs/adr/0001-domain-logic-as-plain-kotlin-module.md). [ProxyPlayerSessionMapper]
 * converts between the two.
 */
@Serializable
sealed interface ProxyPlayerCardDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard.BasicAction]. */
    @Serializable
    @SerialName("basic_action")
    data class BasicAction(val color: String) : ProxyPlayerCardDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard.UniqueAction]. */
    @Serializable
    @SerialName("unique_action")
    data class UniqueAction(val color: String) : ProxyPlayerCardDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard.AdvancedAction]. */
    @Serializable
    @SerialName("advanced_action")
    data class AdvancedAction(val identity: CardIdentityDto) : ProxyPlayerCardDto
}
