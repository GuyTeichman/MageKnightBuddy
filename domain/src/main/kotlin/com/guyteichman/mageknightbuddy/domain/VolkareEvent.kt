package com.guyteichman.mageknightbuddy.domain

/**
 * One entry in a [VolkareSession]'s history log, in the order it happened - the Volkare-mode
 * counterpart to [DummyPlayerEvent], with the same "sealed interface as a closed set of cases"
 * shape (see that file's doc comment for why `sealed` matters here: it lets a `when` over
 * [VolkareEvent] dispatch exhaustively, with a compiler error if a new case is ever added without
 * being handled everywhere).
 */
sealed interface VolkareEvent {
    /** Recorded when a new Round begins - see [VolkareSession.start]. */
    data class RoundStarted(val round: Int) : VolkareEvent

    /**
     * Recorded after [VolkareSession.playTurn] reveals a card from Volkare's deck. [cityRevealed]
     * is the session's City Revealed flag *as it was at the moment this card was revealed*, not
     * read live off the session when this log entry is later displayed - see ADR-0004 and
     * `CONTEXT.md`'s "City Revealed" entry for why: toggling the flag afterward must never
     * retroactively change how an already-logged reveal reads. [manaRoll] is non-null if and only
     * if [card] is [VolkareCard.Wound] - the app rolls Volkare's mana die itself and reports the
     * result, rather than asking the player to roll a physical die (see [ManaColor]).
     */
    data class CardRevealed(val round: Int, val card: VolkareCard, val cityRevealed: Boolean, val manaRoll: ManaColor? = null) : VolkareEvent

    /**
     * Recorded in Volkare's Return only, each time [VolkareSession.playTurn] is called with an
     * empty deck - see `CONTEXT.md`'s "Frenzy" entry. Unlike the standard Dummy Player's deck
     * (which reshuffles at End of Round), Volkare's deck never reshuffles once empty, so this
     * event can recur indefinitely across repeated turns.
     */
    data class Frenzy(val round: Int) : VolkareEvent

    /** Recorded after [VolkareSession.endRound] advances to the next round. */
    data class RoundEnded(val round: Int) : VolkareEvent

    /**
     * Recorded in Volkare's Quest only, the instant [VolkareSession.playTurn] reveals the *last*
     * non-Wound card left in the deck (immediately after the matching [CardRevealed] entry for
     * that same card) - see [VolkareSession.playTurn]'s doc comment for why that reveal, not an
     * empty deck, is the real losing moment. Unlike Volkare's Return's [Frenzy], this means the
     * scenario is lost (see [VolkareSession.lost] and docs/rules/volkares-quest.md's "Scenario end").
     */
    data class QuestLost(val round: Int) : VolkareEvent
}
