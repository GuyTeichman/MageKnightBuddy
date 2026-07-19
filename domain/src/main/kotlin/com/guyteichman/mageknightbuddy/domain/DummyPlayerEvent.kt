package com.guyteichman.mageknightbuddy.domain

sealed interface DummyPlayerEvent {
    data class RoundStarted(val round: Int) : DummyPlayerEvent

    data class TurnPlayed(
        val round: Int,
        val initialReveal: List<CardColor>,
        val additionalReveal: List<CardColor>,
    ) : DummyPlayerEvent

    data class EndOfRoundAnnounced(val round: Int) : DummyPlayerEvent

    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardColor,
        val spellOfferColor: CardColor,
    ) : DummyPlayerEvent
}
