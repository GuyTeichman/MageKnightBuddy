package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.VolkareCard], kept in
 * `data/` for the same reason [DummyPlayerEventDto] is: keeping this near-duplicate hierarchy
 * separate from the domain type decouples the persisted wire format from the domain shape, so
 * [VolkareCard] can be refactored freely without breaking already-stored rows or the backup
 * format. [VolkareSessionMapper] converts between the two.
 *
 * As with [DummyPlayerEventDto], `@SerialName` on each subtype below is the "type" discriminator
 * kotlinx.serialization writes into the JSON so it knows which subtype a given blob represents
 * when decoding a polymorphic (sealed-hierarchy) list back.
 */
@Serializable
sealed interface VolkareCardDto {
    /**
     * Mirrors [com.guyteichman.mageknightbuddy.domain.VolkareCard.BasicAction]. [color] is stored
     * as the [com.guyteichman.mageknightbuddy.domain.CardColor] enum's name (a plain `String`)
     * rather than the enum itself, since `CardColor` lives in `domain/` and this DTO hierarchy
     * must not reference domain types.
     */
    @Serializable
    @SerialName("basic_action")
    data class BasicAction(val color: String) : VolkareCardDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.VolkareCard.CompetitiveSpell]. */
    @Serializable
    @SerialName("competitive_spell")
    data class CompetitiveSpell(val color: String) : VolkareCardDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.VolkareCard.Wound], itself a `data object`. */
    @Serializable
    @SerialName("wound")
    data object Wound : VolkareCardDto
}
