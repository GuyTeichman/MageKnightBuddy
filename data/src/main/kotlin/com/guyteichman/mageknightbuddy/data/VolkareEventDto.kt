package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.VolkareEvent], the
 * Volkare-mode counterpart to [DummyPlayerEventDto] - see that file's doc comment for why this
 * near-duplicate hierarchy exists in `data/` instead of putting `@Serializable` directly on the
 * domain sealed interface (docs/adr/0001-domain-logic-as-plain-kotlin-module.md).
 */
@Serializable
sealed interface VolkareEventDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.VolkareEvent.RoundStarted]. */
    @Serializable
    @SerialName("round_started")
    data class RoundStarted(val round: Int) : VolkareEventDto

    /**
     * Mirrors [com.guyteichman.mageknightbuddy.domain.VolkareEvent.CardRevealed]. [card] nests
     * the [VolkareCardDto] mirror rather than a raw domain [com.guyteichman.mageknightbuddy.domain.VolkareCard],
     * for the same domain-purity reason as everything else in this file.
     */
    @Serializable
    @SerialName("card_revealed")
    data class CardRevealed(val round: Int, val card: VolkareCardDto, val cityRevealed: Boolean) : VolkareEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.VolkareEvent.Frenzy]. */
    @Serializable
    @SerialName("frenzy")
    data class Frenzy(val round: Int) : VolkareEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.VolkareEvent.RoundEnded]. */
    @Serializable
    @SerialName("round_ended")
    data class RoundEnded(val round: Int) : VolkareEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.VolkareEvent.QuestLost]. */
    @Serializable
    @SerialName("quest_lost")
    data class QuestLost(val round: Int) : VolkareEventDto
}
