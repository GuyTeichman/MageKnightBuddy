package com.guyteichman.mageknightbuddy.domain

/**
 * One entry in a [ProxyPlayerSession]'s history log, in the order it happened - the Proxy Player
 * counterpart to [DummyPlayerEvent]/[VolkareEvent], with the same "sealed interface as a closed
 * set of cases" shape (see [DummyPlayerEvent]'s doc comment for why `sealed` matters here).
 */
sealed interface ProxyPlayerEvent {
    /** Recorded when a new Round begins - only [ProxyPlayerSession.start] logs this; `endRound()` logs [RoundEnded] instead, never a fresh `RoundStarted`. */
    data class RoundStarted(val round: Int) : ProxyPlayerEvent

    /**
     * Recorded when [ProxyPlayerSession.playTurn] draws a fresh Objective Card because there
     * wasn't one already (docs/rules/proxy-player.md's "The Proxy Player's turn", the "don't have
     * an objective card" branch). [objectiveCard] is the newly drawn objective; [discarded] is
     * whatever else was flipped in the same mandatory-3 batch (2 cards, or fewer if the deck ran
     * low, or the mandatory batch's crystal-chain extension - see [ProxyPlayerSession.playTurn]).
     */
    data class NewObjectiveDrawn(val round: Int, val objectiveCard: ProxyPlayerCard, val discarded: List<ProxyPlayerCard>) : ProxyPlayerEvent

    /**
     * Recorded when [ProxyPlayerSession.playTurn] continues an existing Objective Card
     * (docs/rules/proxy-player.md's "have an objective card" branch): a Shield token is added to
     * it and 3 cards (plus any crystal-chain extension) are flipped to the discard pile.
     * [shieldsNow] is the Objective Card's new Shield count, including this turn's addition.
     */
    data class TurnContinued(val round: Int, val objectiveCard: ProxyPlayerCard, val shieldsNow: Int, val revealed: List<ProxyPlayerCard>) : ProxyPlayerEvent

    /** Recorded when [ProxyPlayerSession.playTurn] finds an empty deck - announces End of Round instead of playing. */
    data class EndOfRoundAnnounced(val round: Int) : ProxyPlayerEvent

    /**
     * Recorded after [ProxyPlayerSession.resolveObjective] discards the current Objective Card
     * and its Shields - docs/rules/proxy-player.md's "Resolution". [resolution] distinguishes
     * Explored from Completed for the log's narration only; both have the identical state effect
     * (see [ProxyPlayerObjectiveResolution]'s doc comment).
     */
    data class ObjectiveResolved(val round: Int, val objectiveCard: ProxyPlayerCard, val resolution: ProxyPlayerObjectiveResolution) : ProxyPlayerEvent

    /**
     * Recorded after [ProxyPlayerSession.endRound] resolves the round-prep offer interactions,
     * shared verbatim with [DummyPlayerEvent.RoundEnded] (docs/rules/proxy-player.md's "When
     * preparing a new Round"). [discardedObjective] is non-null if a lingering Objective Card
     * (one that was still being pursued when the Round ended) was discarded as part of this step.
     */
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardIdentity,
        val spellOfferColor: CardColor,
        val discardedObjective: ProxyPlayerCard?,
    ) : ProxyPlayerEvent
}
