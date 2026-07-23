package com.guyteichman.mageknightbuddy.domain

/**
 * One entry in a [DummyPlayerSession]'s history log: a record of something notable that happened
 * during the session, in the order it happened, so the UI can show a play-by-play of what the
 * Dummy Player has done this game.
 *
 * This is a "sealed interface as a closed set of cases": every possible kind of event is one of the
 * `data class`es declared inside it, and nothing outside this file can add another. That closure is
 * what lets consuming code safely dispatch on the concrete type with a Kotlin `when` expression
 * (e.g. `when (event) { is RoundStarted -> ...; is TurnPlayed -> ...; ... }`) and get a compiler
 * error if a new event type is ever added without being handled everywhere.
 */
sealed interface DummyPlayerEvent {
    /** Recorded when a new Round begins - only [DummyPlayerSession.start] logs this; `endRound()` logs [RoundEnded] instead, never a fresh `RoundStarted`. */
    data class RoundStarted(val round: Int) : DummyPlayerEvent

    /**
     * Recorded after [DummyPlayerSession.playTurn] resolves a turn: the flip-3-cards-then-chain-
     * on-crystal-match procedure from docs/rules/dummy-player.md ("Turn procedure"). [initialReveal]
     * is the mandatory first 3 cards flipped; [additionalReveal] is any extra cards flipped because
     * the 3rd card's color(s) matched a crystal the Dummy Player holds (empty if there was no match).
     */
    data class TurnPlayed(
        val round: Int,
        val initialReveal: List<CardIdentity>,
        val additionalReveal: List<CardIdentity>,
    ) : DummyPlayerEvent

    /**
     * Recorded when the Dummy Player's deck is empty at the start of its turn, so it announces End
     * of Round instead of playing - docs/rules/dummy-player.md ("Turn procedure").
     */
    data class EndOfRoundAnnounced(val round: Int) : DummyPlayerEvent

    /**
     * Recorded after [DummyPlayerSession.endRound] resolves the round-prep offer interactions that
     * feed the Dummy Player's deck and crystals - docs/rules/dummy-player.md ("End of Round").
     * [advancedActionOfferColor] is the card added to its deck (single- or dual-color, see
     * [CardIdentity]); [spellOfferColor] is the color of the crystal added to its Inventory (Spells
     * are never dual-color).
     */
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardIdentity,
        val spellOfferColor: CardColor,
    ) : DummyPlayerEvent
}
