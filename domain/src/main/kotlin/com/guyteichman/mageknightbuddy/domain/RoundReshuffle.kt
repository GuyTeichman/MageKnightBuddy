package com.guyteichman.mageknightbuddy.domain

/**
 * Combines an undrawn deck, its discard pile, and one newly-added card into a single shuffled
 * pile - the "reshuffle" step from docs/rules/dummy-player.md's "End of Round" (reused verbatim
 * by docs/rules/proxy-player.md's "When preparing a new Round"): the whole discard pile merges
 * back in, not just the undrawn deck - see CONTEXT.md's "Reshuffle" entry. Shared by
 * [DummyPlayerSession.endRound] and [ProxyPlayerSession.endRound] so both apply the identical
 * merge; omitting [discard] here was the historical #148 bug that silently shrank the deck to
 * almost nothing after Round 1.
 *
 * Generic over [T] because the two callers reshuffle different card types ([CardIdentity] for
 * Dummy Player, [ProxyPlayerCard] for Proxy Player) - a plain `List<CardIdentity>` signature
 * wouldn't compile for the latter. [VolkareSession] has no reshuffle step at all (see its own
 * `endRound` doc comment) and never calls this.
 */
fun <T> reshuffleForNewRound(deck: List<T>, discard: List<T>, addedCard: T): List<T> =
    (deck + discard + addedCard).shuffled()
