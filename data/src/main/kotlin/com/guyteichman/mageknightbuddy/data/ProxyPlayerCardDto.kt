package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard], kept in
 * `data/` for the same reason [DummyPlayerEventDto] is: this near-duplicate hierarchy decouples
 * the persisted wire format from the domain shape, so [ProxyPlayerCard] can be refactored freely
 * without breaking already-stored rows or the backup format. [ProxyPlayerSessionMapper] converts
 * between the two.
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
