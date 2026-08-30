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
    val (minProductive, maxProductive) = solveProductiveTurns(buckets, HashMap())
    return TurnEstimate(minProductive + 1, maxProductive + 1)
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
 * Exact fewest/most *productive* turns (turns that actually flip cards, excluding the End of Round
 * declaration) to empty a deck given as a `bonusValue -> count` multiset. Memoized on that multiset
 * in [memo]. Returns `min to max` productive turns.
 *
 * `min` and `max` decompose independently over the same choice tree: any real draw order is a
 * sequence of turns, so the fewest total turns is 1 + the best child's `min` and the most is
 * 1 + the best child's `max`, taken over every legal way to play the next turn.
 */
private fun solveProductiveTurns(
    buckets: Map<Int, Int>,
    memo: HashMap<Map<Int, Int>, Pair<Int, Int>>,
): Pair<Int, Int> {
    val total = buckets.values.sum()
    if (total == 0) return 0 to 0
    // 3 or fewer cards: the mandatory flip-3 takes them all in one turn, with no room left over for
    // any chained bonus card - so exactly one productive turn remains regardless of colors.
    if (total <= 3) return 1 to 1

    memo[buckets]?.let { return it }

    var minTurns = Int.MAX_VALUE
    var maxTurns = Int.MIN_VALUE
    // Enumerate every value we could arrange as this turn's trigger (3rd) card. Its bonus fixes the
    // turn's total size at 3 + bonus, where bonus is capped by the cards left after the base 3.
    for ((triggerValue, count) in buckets) {
        if (count == 0) continue
        val bonus = minOf(triggerValue, total - 3)
        // Cards removed this turn besides the trigger itself: the other 2 of the base 3, plus bonus.
        val othersToRemove = 2 + bonus
        val afterTrigger = buckets.decrement(triggerValue)
        // Those other cards can be any remaining cards; which we spend now changes what's available
        // to trigger later turns, so we branch over every distinct removal and let memoization dedup.
        for (child in removalOutcomes(afterTrigger, othersToRemove)) {
            val (childMin, childMax) = solveProductiveTurns(child, memo)
            minTurns = minOf(minTurns, 1 + childMin)
            maxTurns = maxOf(maxTurns, 1 + childMax)
        }
    }

    val result = minTurns to maxTurns
    memo[buckets] = result
    return result
}

/** A copy of this multiset with one card of [value] removed, dropping the key entirely when it hits 0. */
private fun Map<Int, Int>.decrement(value: Int): Map<Int, Int> {
    val remaining = getValue(value) - 1
    // `this - key` / `this + (key to n)` produce new maps, keeping this helper non-mutating.
    return if (remaining == 0) this - value else this + (value to remaining)
}

/**
 * Every distinct multiset that can result from removing exactly [k] cards from [buckets] (spread
 * across the bonus-value buckets however we like, bounded by each bucket's count), returned as the
 * *remaining* bucket maps. `k` is always <= the cards available here, so the list is never empty.
 */
private fun removalOutcomes(buckets: Map<Int, Int>, k: Int): List<Map<Int, Int>> {
    val keys = buckets.keys.toList()
    val outcomes = mutableListOf<Map<Int, Int>>()
    // Walk the distinct bucket values, deciding how many to remove from each so the removals sum to k.
    fun recurse(index: Int, remaining: Int, kept: Map<Int, Int>) {
        if (index == keys.size) {
            if (remaining == 0) outcomes.add(kept)
            return
        }
        val key = keys[index]
        val available = buckets.getValue(key)
        val maxRemove = minOf(available, remaining)
        for (remove in 0..maxRemove) {
            val keptHere = available - remove
            val nextKept = if (keptHere == 0) kept else kept + (key to keptHere)
            recurse(index + 1, remaining - remove, nextKept)
        }
    }
    recurse(0, k, emptyMap())
    return outcomes
}
