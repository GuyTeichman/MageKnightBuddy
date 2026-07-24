package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random

/**
 * Immutable snapshot of one Dummy Player's state at a point in time - its deck, discard pile,
 * crystal Inventory, current round, and a log of events so far. Implements the Dummy Player
 * mechanic described in docs/rules/dummy-player.md: the automated pacing opponent used by every
 * cooperative and solo scenario (including Solo Conquest).
 *
 * Two Kotlin style choices worth calling out for anyone coming from an OOP background where you'd
 * normally mutate an object in place:
 * - Every method below (`playTurn`, `endRound`) returns a **new** `DummyPlayerSession` rather than
 *   changing this one. `data class` gives every instance a generated `copy()` function that builds
 *   a new instance with just the listed properties changed and everything else carried over - that's
 *   how each method below produces its "next" state.
 * - The constructor is `private`, so nothing outside this class can call `DummyPlayerSession(...)`
 *   directly. Instead, the only way to obtain an instance is through the named factory functions in
 *   the companion object below: [start] (begin a new session for a chosen Knight), [startRandom]
 *   (begin a new session with a randomly-chosen Knight), and [restore] (reconstruct a session from
 *   previously-persisted state). Routing every creation path through one of these three functions
 *   guarantees you can never end up with a `DummyPlayerSession` in an invalid or half-set-up state.
 */
data class DummyPlayerSession private constructor(
    val knight: Knight,
    val wasRandom: Boolean,
    val deckOrder: List<CardIdentity>,
    val discardPile: List<CardIdentity>,
    val crystals: Map<CardColor, Int>,
    val round: Int,
    val roundEnded: Boolean,
    val log: List<DummyPlayerEvent>,
    // Whether this session began on a night Round (Round 1 = night) instead of the usual day
    // start - set once at setup, never changed afterward. See [isDay].
    val startsAtNight: Boolean = false,
) {
    /**
     * Whether the current [round] is a day round - see [isDayRound] for the odd/even derivation
     * from [startsAtNight]. No Dummy Player rule currently reads this; it's tracked for parity
     * with [ProxyPlayerSession]/[VolkareSession] ahead of a planned day/night indicator.
     */
    val isDay: Boolean
        get() = isDayRound(round, startsAtNight)

    /**
     * How many cards of each [CardColor] remain in the deck - e.g. to show the player how close the
     * Dummy Player is to running out (the rulebook notes its deck can empty faster than a real
     * player's). A [CardIdentity.DualColor] card counts toward both of its colors here, via
     * [CardIdentity.matches].
     *
     * `associateWith` is a Kotlin stdlib function that turns a list of keys - here,
     * `CardColor.entries`, i.e. every enum value - into a `Map` by computing a value for each key
     * with the given lambda.
     */
    val remainingByColor: Map<CardColor, Int>
        get() = CardColor.entries.associateWith { color -> deckOrder.count { it.matches(color) } }

    /**
     * How many turns have been played so far in the *current* [round] - e.g. "Round 2, Turn 4" in
     * the AI screen's header (issue #125). Counts [DummyPlayerEvent.TurnPlayed] entries in [log]
     * whose own `round` matches this session's current [round]; every other event kind (including
     * [DummyPlayerEvent.EndOfRoundAnnounced], which fires when the deck is empty instead of a real
     * turn) doesn't count. Filtering by the event's own `round` field - rather than, say, the
     * position since the last `RoundStarted`/`RoundEnded` log entry - means this stays correct even
     * though `endRound()` never logs a fresh `RoundStarted` for the round it advances into (see
     * that event's own doc comment): a freshly-advanced round simply has no [TurnPlayed] entries
     * tagged with its number yet, so this naturally reads 0 right after `endRound()`.
     */
    val turnInRound: Int
        get() = log.count { it is DummyPlayerEvent.TurnPlayed && it.round == round }

    /**
     * Plays one Dummy Player turn: the flip-3-cards-then-chain-on-crystal-match procedure from
     * docs/rules/dummy-player.md ("Turn procedure"). Flips the top 3 cards from the deck onto the
     * discard pile unconditionally, then looks at the color of the 3rd (last) card - if the Dummy
     * Player holds any crystals of that color, it flips one further card per matching crystal
     * (those extra cards' own colors don't trigger any more chaining). If the deck is empty at the
     * start of the turn, the Round ends instead (see [DummyPlayerEvent.EndOfRoundAnnounced]); if the
     * deck runs out partway through a flip, it flips as many cards as are available rather than
     * ending the Round mid-turn. Returns `this` unchanged if the Round has already ended, since
     * other players still take their final turns before round-prep (`endRound`) runs.
     */
    fun playTurn(): DummyPlayerSession {
        if (roundEnded) return this

        if (deckOrder.isEmpty()) {
            // Deck's empty at the start of the turn: announce End of Round instead of flipping cards.
            return copy(roundEnded = true, log = log + DummyPlayerEvent.EndOfRoundAnnounced(round))
        }

        val initialReveal = deckOrder.take(3) // Mandatory first 3 cards, flipped unconditionally.
        val afterInitial = deckOrder.drop(3)
        val lastCard = initialReveal.last() // Chaining is decided by the 3rd (last) card's color(s) only.
        // matchingCrystalCount sums crystals for every color the card matches - one color for a
        // CardIdentity.SingleColor card, both colors for a CardIdentity.DualColor card.
        val matchingCrystals = lastCard.matchingCrystalCount(crystals)
        // Cap the extra flips at what's actually left, in case the deck runs out mid-flip.
        val additionalCount = minOf(matchingCrystals, afterInitial.size)
        val additionalReveal = afterInitial.take(additionalCount)
        val remainingDeck = afterInitial.drop(additionalCount)

        return copy(
            deckOrder = remainingDeck,
            discardPile = discardPile + initialReveal + additionalReveal,
            log = log + DummyPlayerEvent.TurnPlayed(round, initialReveal, additionalReveal),
        )
    }

    /**
     * Applies the round-prep offer interactions from docs/rules/dummy-player.md ("End of Round")
     * and advances to the next round. Callers pass in the identity/color of the two cards that
     * round-prep just removed (the lowest card from each offer): the Advanced Action offer's card
     * is added to the Dummy Player's deck instead of being discarded as usual - it may be a
     * [CardIdentity.DualColor] card - and the Spell offer's card grants the Dummy Player one
     * crystal of its color (uncapped, unlike real players' 3-crystal-per-color limit; Spells are
     * never dual-color). The deck is then reshuffled, and this reshuffle folds the *entire discard
     * pile* back in too - not just the undrawn deck plus the new card - so [discardPile] is empty
     * immediately afterward. This mirrors the general "Preparing a New Round" step every real
     * player also does (rulebook p.4: deck + discard pile + hand, combined and reshuffled); see
     * CONTEXT.md's "Reshuffle" entry for the full cross-reference. Omitting the discard pile here
     * was a real historical bug (issue #148) that shrank the deck to almost nothing after Round 1.
     */
    fun endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor): DummyPlayerSession = copy(
        // Reshuffle = undrawn deck + the ENTIRE discard pile + the newly-added offer card, all
        // combined into one pile. discardPile resets to empty since every card it held just moved
        // into deckOrder.
        deckOrder = (deckOrder + discardPile + advancedActionOfferColor).shuffled(),
        discardPile = emptyList(),
        // `map + Pair` builds a new Map with that one key's value replaced (or added) - the rest of
        // the entries are carried over unchanged, consistent with this class's immutable style.
        crystals = crystals + (spellOfferColor to crystals.getValue(spellOfferColor) + 1),
        round = round + 1,
        roundEnded = false,
        log = log + DummyPlayerEvent.RoundEnded(round, advancedActionOfferColor, spellOfferColor),
    )

    companion object {
        /**
         * Begins a new Dummy Player session for a chosen [Knight] - session setup per
         * docs/rules/dummy-player.md ("Setup"): a shuffled starting deck of that Knight's 16 Basic
         * Action cards (the default [deckOrder] builds this as 4 cards of each [CardColor], then
         * shuffles), that Knight's starting crystal Inventory (see [startingCrystals]), and Round 1.
         * [wasRandom] only records whether the Knight was picked randomly (via [startRandom]) or
         * chosen explicitly, for display purposes - it has no effect on setup itself.
         */
        fun start(
            knight: Knight,
            wasRandom: Boolean = false,
            // flatMap here expands each color into 4 copies of itself (wrapped as a single-color
            // CardIdentity), then flattens all the per-color lists into one combined list of 16
            // cards before shuffling.
            deckOrder: List<CardIdentity> = CardColor.entries
                .flatMap { color -> List(4) { CardIdentity.SingleColor(color) } }
                .shuffled(),
            startsAtNight: Boolean = false,
        ): DummyPlayerSession = DummyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = emptyList(),
            crystals = startingCrystals(knight),
            round = 1,
            roundEnded = false,
            log = listOf(DummyPlayerEvent.RoundStarted(round = 1)),
            startsAtNight = startsAtNight,
        )

        /**
         * Begins a new session standing in for the "randomly choose one Hero not in the game to be
         * the Dummy Player" step in docs/rules/dummy-player.md ("Setup"). Picks a random [Knight]
         * and delegates to [start] with [wasRandom] set to true.
         */
        fun startRandom(random: Random = Random, startsAtNight: Boolean = false): DummyPlayerSession {
            val knight = Knight.entries.toList().random(random)
            return start(knight, wasRandom = true, startsAtNight = startsAtNight)
        }

        /**
         * Reconstructs a session from its full persisted state (used by the persistence layer to
         * restore a saved game). Not for general use - [start]/[startRandom] are the entry points
         * for beginning a new session.
         */
        fun restore(
            knight: Knight,
            wasRandom: Boolean,
            deckOrder: List<CardIdentity>,
            discardPile: List<CardIdentity>,
            crystals: Map<CardColor, Int>,
            round: Int,
            roundEnded: Boolean,
            log: List<DummyPlayerEvent>,
            startsAtNight: Boolean = false,
        ): DummyPlayerSession = DummyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = discardPile,
            crystals = crystals,
            round = round,
            roundEnded = roundEnded,
            log = log,
            startsAtNight = startsAtNight,
        )
    }
}

/**
 * Starting crystal dots per Knight, from docs/rules/dummy-player.md ("Setup"): the Dummy
 * Player (and, per docs/rules/proxy-player.md's "At the start of the game", the Proxy Player
 * too) starts with one crystal per colored dot on the bottom of its Hero card (e.g. Goldyx's
 * dots are green, green, blue, so it starts with 2 green + 1 blue crystal). The base rulebook
 * only shows Goldyx as its worked example - the other 7 Knights' values were sourced by
 * visually reading their actual Hero cards (see the full per-Knight table and source citations
 * in docs/rules/dummy-player.md, or research tickets #31/#66/#70). Feeds [startingCrystals]
 * below. `internal` (not `private`) so [ProxyPlayerSession] can reuse it too.
 */
internal val STARTING_CRYSTAL_DOTS: Map<Knight, List<CardColor>> = mapOf(
    Knight.TOVAK to listOf(CardColor.RED, CardColor.BLUE, CardColor.BLUE),
    Knight.GOLDYX to listOf(CardColor.GREEN, CardColor.GREEN, CardColor.BLUE),
    Knight.NOROWAS to listOf(CardColor.GREEN, CardColor.WHITE, CardColor.WHITE),
    Knight.WOLFHAWK to listOf(CardColor.WHITE, CardColor.WHITE, CardColor.BLUE),
    Knight.ARYTHEA to listOf(CardColor.RED, CardColor.RED, CardColor.WHITE),
    Knight.KRANG to listOf(CardColor.RED, CardColor.RED, CardColor.GREEN),
    Knight.BRAEVALAR to listOf(CardColor.GREEN, CardColor.BLUE, CardColor.BLUE),
    Knight.CORAL to listOf(CardColor.WHITE, CardColor.WHITE, CardColor.RED),
)

/** Converts a Knight's starting dots (see [STARTING_CRYSTAL_DOTS]) into a color-to-count map for the initial crystal Inventory. Shared by [DummyPlayerSession] and [ProxyPlayerSession]. */
internal fun startingCrystals(knight: Knight): Map<CardColor, Int> {
    val dots = STARTING_CRYSTAL_DOTS.getValue(knight)
    return CardColor.entries.associateWith { color -> dots.count { it == color } }
}
