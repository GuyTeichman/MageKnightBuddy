package com.guyteichman.mageknightbuddy.domain

/**
 * A best-/worst-case estimate of how many turns the Dummy Player (or Proxy Player) has left in the
 * current Round - see docs/rules/dummy-player.md ("Turn procedure") and issue #283. Because a real
 * player can't see the shuffled deck order, the estimate is a range over every possible order:
 * [min] is the fewest turns the Round could last (the automated player races to End of Round by
 * chaining as many crystal-matched reveals as possible), [max] the most (it chains as few as
 * possible). Both counts include the final turn on which End of Round is actually declared.
 */
data class TurnEstimate(val min: Int, val max: Int) {
    /** True when the range collapses to a single value, so the UI can show "~N" instead of "~N-M". */
    val isExact: Boolean get() = min == max
}

/**
 * Estimates the [TurnEstimate] range for a deck described by [chainBonuses] - one entry per card
 * left in the deck, giving how many *extra* cards that card would chain if it were flipped as a
 * turn's trigger (3rd) card, i.e. its [CardIdentity.matchingCrystalCount] against the current
 * crystals. A card's color/identity is irrelevant beyond that number, so the whole deck collapses
 * to a multiset of bonus values.
 *
 * The turn rule (docs/rules/dummy-player.md): each turn flips the top 3 cards unconditionally, then
 * - if 4+ cards remain - flips `bonus` more, where `bonus` is the trigger card's chain value capped
 * by what's left. Finding the fewest/most turns to empty the deck is therefore a small optimization
 * (knapsack-like: pack the deck into turns whose sizes you partly choose via the trigger), which
 * [solveProductiveTurns] solves exactly. The `+ 1` adds the separate turn on which End of Round is
 * declared once the deck is empty.
 */
fun estimateTurnsRemaining(chainBonuses: List<Int>): TurnEstimate {
    // groupingBy/eachCount collapses the per-card list into a `bonusValue -> count` multiset - the
    // only state the optimization actually depends on.
    val buckets = chainBonuses.groupingBy { it }.eachCount()
    return TurnEstimate(
        min = productiveTurns(buckets, biggestBites = true) + 1,
        max = productiveTurns(buckets, biggestBites = false) + 1,
    )
}

/**
 * The [TurnEstimate] for this Dummy Player's current deck and crystals - see [estimateTurnsRemaining].
 */
val DummyPlayerSession.turnsRemaining: TurnEstimate
    get() = estimateTurnsRemaining(deckOrder.map { it.matchingCrystalCount(crystals) })

/**
 * The [TurnEstimate] for this Proxy Player's current deck and crystals - see [estimateTurnsRemaining].
 * The Proxy Player draws exactly the same 3-cards-then-chain-on-crystal-match count each turn as the
 * standard Dummy Player (docs/rules/proxy-player.md's "The Proxy Player's turn"), so it shares this
 * estimator - the objective-card handling doesn't change how many cards leave the deck per turn.
 */
val ProxyPlayerSession.turnsRemaining: TurnEstimate
    get() = estimateTurnsRemaining(deckOrder.map { it.matchingCrystalCount(crystals) })

/**
 * Exact fewest ([biggestBites] = true) or most ([biggestBites] = false) *productive* turns (turns
 * that actually flip cards, excluding the End of Round declaration) to empty a deck given as a
 * `bonusValue -> count` multiset. Linear in the deck size.
 *
 * It's a greedy walk, and the greedy choice is provably optimal (an exchange argument, exhaustively
 * confirmed against the real [DummyPlayerSession.playTurn] loop in TurnsRemainingEstimatorTest):
 * - **Fewest turns** wants the biggest turn each time, so trigger with the *highest*-bonus card
 *   available, and spend the *lowest*-bonus cards as the turn's other cards - keeping high-bonus
 *   cards in reserve to trigger (and enlarge) later turns too.
 * - **Most turns** wants the smallest turn each time, so trigger with the *lowest*-bonus card and
 *   spend the *highest*-bonus cards as filler - a high-bonus card only ever enlarges a turn when it
 *   triggers, so burning it as filler keeps future turns small.
 * A trigger's bonus (extra cards chained) is capped by what's left after the mandatory 3.
 */
private fun productiveTurns(buckets: Map<Int, Int>, biggestBites: Boolean): Int {
    var remaining = buckets
    var turns = 0
    while (true) {
        val total = remaining.values.sum()
        if (total == 0) return turns
        turns++
        // 3 or fewer cards: the mandatory flip-3 takes them all in one turn, with no room left for a
        // chained card, so this is the last productive turn.
        if (total <= 3) return turns
        val triggerValue = if (biggestBites) remaining.keys.max() else remaining.keys.min()
        val bonus = minOf(triggerValue, total - 3)
        // Cards removed this turn besides the trigger: the other 2 of the base 3, plus `bonus` chained.
        val othersToRemove = 2 + bonus
        remaining = remaining.decrement(triggerValue).removeCards(othersToRemove, fromLowest = biggestBites)
    }
}

/** A copy of this multiset with one card of [value] removed, dropping the key entirely when it hits 0. */
private fun Map<Int, Int>.decrement(value: Int): Map<Int, Int> {
    val remaining = getValue(value) - 1
    // `this - key` / `this + (key to n)` produce new maps, keeping this helper non-mutating.
    return if (remaining == 0) this - value else this + (value to remaining)
}

/**
 * A copy of this multiset with exactly [count] cards removed, taken from the lowest bonus values
 * first when [fromLowest] (else the highest first). [count] is always <= the cards available, so the
 * whole quota is met.
 */
private fun Map<Int, Int>.removeCards(count: Int, fromLowest: Boolean): Map<Int, Int> {
    val result = toMutableMap()
    var toRemove = count
    // Visit bonus values in the chosen direction; `reversed()` flips the ascending sort to descending.
    val order = keys.sorted().let { if (fromLowest) it else it.reversed() }
    for (value in order) {
        if (toRemove == 0) break
        val available = result.getValue(value)
        val take = minOf(available, toRemove)
        val kept = available - take
        if (kept == 0) result.remove(value) else result[value] = kept
        toRemove -= take
    }
    return result
}
