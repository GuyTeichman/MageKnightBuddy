package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random

/**
 * Immutable snapshot of this Round's Tactic Card draft for a Dummy Player/Volkare/Proxy Player
 * session - which Tactic numbers (1-6) have been permanently removed from the Day and Night
 * piles (docs/context-dummy-player.md's "Tactic Card"/"Tactic Selection" entries), plus this
 * Round's two picks (the Dummy/Volkare/Proxy's and the real player's).
 *
 * Deliberately "dumb": this class knows nothing about which Scenario is being played, solo vs.
 * coop, or the session's own round number - see [TacticRemovalRule]/`TacticRules.kt` for where
 * that scenario-specific decision-making lives instead. Kept separate so the same rule-lookup
 * code can drive this same mutation logic regardless of which of the three modes owns it.
 */
data class TacticState(
    val removedDayCards: Set<Int> = emptySet(),
    val removedNightCards: Set<Int> = emptySet(),
    val dummyPick: Int? = null,
    val playerPick: Int? = null,
) {
    /** The active pile's permanently-removed Tactic numbers ([isDay] selects Day vs. Night). */
    fun removedCards(isDay: Boolean): Set<Int> = if (isDay) removedDayCards else removedNightCards

    /**
     * Every Tactic number the UI should grey out for the active pile: permanently removed, plus
     * whichever picks have already been made this Round (a card can't be taken twice).
     */
    fun unavailableCards(isDay: Boolean): Set<Int> = removedCards(isDay) + setOfNotNull(dummyPick, playerPick)

    /**
     * Records the real player's Tactic pick for the active pile ([isDay] selects Day vs.
     * Night). `require` throws [IllegalArgumentException] if [card] is outside the physical
     * 1-6 range or already unavailable - the UI should only ever offer legal, available cards,
     * so both checks are a defensive guard against a caller bug, not an expected user-facing
     * error path.
     */
    fun pickPlayer(card: Int, isDay: Boolean): TacticState {
        require(card in 1..6) { "Tactic $card is outside the 1-6 range" }
        require(card !in unavailableCards(isDay)) { "Tactic $card is not available to pick" }
        // `copy()` is a method every Kotlin `data class` generates for free: it builds a new
        // instance with just the named property changed, carrying every other property over
        // unchanged - the class stays immutable, so this returns a new TacticState rather than
        // mutating the receiver. Same pattern used throughout DummyPlayerSession/VolkareSession.
        return copy(playerPick = card)
    }

    /**
     * Draws the Dummy Player's/Volkare's/Proxy Player's Tactic pick at random from whatever's
     * left in the active pile (numbers 1-6 minus [unavailableCards]). [random] is injectable -
     * same convention as [VolkareSession.playTurn]'s `manaRoll` parameter - so tests can pass a
     * seeded [Random] for deterministic assertions instead of the default fresh one.
     */
    fun pickDummy(isDay: Boolean, random: Random = Random): TacticState {
        val available = (1..6).minus(unavailableCards(isDay))
        return copy(dummyPick = available.random(random))
    }

    /**
     * Applies whatever removal fired at the end of this Round - [remove] is decided externally
     * (`TacticRules.kt`'s `tacticRemovalRule`/`tacticRemovalTarget`, based on Scenario/solo-coop/
     * round number that this class deliberately doesn't know about), `null` meaning nothing
     * fires this Round. [RemovalTarget.BOTH] permanently removes both this Round's picks from
     * the active pile; [RemovalTarget.PLAYER_ONLY] removes only [playerPick] - [dummyPick] is
     * simply discarded, available again next time this pile comes up. Either way, both picks are
     * cleared for the next Round regardless of whether removal fired.
     */
    fun advanceRound(remove: RemovalTarget?, isDay: Boolean): TacticState {
        val newlyRemoved = when (remove) {
            RemovalTarget.BOTH -> setOfNotNull(playerPick, dummyPick)
            RemovalTarget.PLAYER_ONLY -> setOfNotNull(playerPick)
            null -> emptySet()
        }
        val cleared = copy(playerPick = null, dummyPick = null)
        return if (isDay) {
            cleared.copy(removedDayCards = removedDayCards + newlyRemoved)
        } else {
            cleared.copy(removedNightCards = removedNightCards + newlyRemoved)
        }
    }
}
