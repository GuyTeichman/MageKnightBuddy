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
    val deckOrder: List<CardColor>,
    val discardPile: List<CardColor>,
    val crystals: Map<CardColor, Int>,
    val round: Int,
    val roundEnded: Boolean,
    val log: List<DummyPlayerEvent>,
) {
    /**
     * How many cards of each [CardColor] remain in the deck - e.g. to show the player how close the
     * Dummy Player is to running out (the rulebook notes its deck can empty faster than a real
     * player's).
     *
     * `associateWith` is a Kotlin stdlib function that turns a list of keys - here,
     * `CardColor.entries`, i.e. every enum value - into a `Map` by computing a value for each key
     * with the given lambda.
     */
    val remainingByColor: Map<CardColor, Int>
        get() = CardColor.entries.associateWith { color -> deckOrder.count { it == color } }

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
        val lastColor = initialReveal.last() // Chaining is decided by the 3rd (last) card's color only.
        // getValue (vs. get) throws if the key is missing; safe here because crystals always has
        // every CardColor as a key (see startingCrystals below).
        val matchingCrystals = crystals.getValue(lastColor)
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
     * and advances to the next round. Callers pass in the colors of the two cards that round-prep
     * just removed (the lowest card from each offer): the Advanced Action offer's card is added to
     * the Dummy Player's deck (then the deck is reshuffled) instead of being discarded as usual, and
     * the Spell offer's card grants the Dummy Player one crystal of its color (uncapped, unlike real
     * players' 3-crystal-per-color limit).
     */
    fun endRound(advancedActionOfferColor: CardColor, spellOfferColor: CardColor): DummyPlayerSession = copy(
        // Card that would normally be discarded instead joins the deck, which is then reshuffled.
        deckOrder = (deckOrder + advancedActionOfferColor).shuffled(),
        // `map + Pair` builds a new Map with that one key's value replaced (or added) - the rest of
        // the entries are carried over unchanged, consistent with this class's immutable style.
        crystals = crystals + (spellOfferColor to crystals.getValue(spellOfferColor) + 1),
        round = round + 1,
        roundEnded = false,
        log = log + DummyPlayerEvent.RoundEnded(round, advancedActionOfferColor, spellOfferColor),
    )

    companion object {
        /**
         * Starting crystal dots per Knight, from docs/rules/dummy-player.md ("Setup"): the Dummy
         * Player starts with one crystal per colored dot on the bottom of its Hero card (e.g.
         * Goldyx's dots are green, green, blue, so it starts with 2 green + 1 blue crystal). The
         * base rulebook only shows Goldyx as its worked example - the other 7 Knights' values were
         * sourced by visually reading their actual Hero cards (see the full per-Knight table and
         * source citations in docs/rules/dummy-player.md, or research tickets #31/#66/#70). Feeds
         * [startingCrystals] below.
         */
        private val STARTING_CRYSTAL_DOTS: Map<Knight, List<CardColor>> = mapOf(
            Knight.TOVAK to listOf(CardColor.RED, CardColor.BLUE, CardColor.BLUE),
            Knight.GOLDYX to listOf(CardColor.GREEN, CardColor.GREEN, CardColor.BLUE),
            Knight.NOROWAS to listOf(CardColor.GREEN, CardColor.WHITE, CardColor.WHITE),
            Knight.WOLFHAWK to listOf(CardColor.WHITE, CardColor.WHITE, CardColor.BLUE),
            Knight.ARYTHEA to listOf(CardColor.RED, CardColor.RED, CardColor.WHITE),
            Knight.KRANG to listOf(CardColor.RED, CardColor.RED, CardColor.GREEN),
            Knight.BRAEVALAR to listOf(CardColor.GREEN, CardColor.BLUE, CardColor.BLUE),
            Knight.CORAL to listOf(CardColor.WHITE, CardColor.WHITE, CardColor.RED),
        )

        /** Converts a Knight's starting dots (see [STARTING_CRYSTAL_DOTS]) into a color-to-count map for the initial `crystals` inventory. */
        private fun startingCrystals(knight: Knight): Map<CardColor, Int> {
            val dots = STARTING_CRYSTAL_DOTS.getValue(knight)
            return CardColor.entries.associateWith { color -> dots.count { it == color } }
        }

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
            // flatMap here expands each color into 4 copies of itself, then flattens all the
            // per-color lists into one combined list of 16 cards before shuffling.
            deckOrder: List<CardColor> = CardColor.entries.flatMap { color -> List(4) { color } }.shuffled(),
        ): DummyPlayerSession = DummyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = emptyList(),
            crystals = startingCrystals(knight),
            round = 1,
            roundEnded = false,
            log = listOf(DummyPlayerEvent.RoundStarted(round = 1)),
        )

        /**
         * Begins a new session standing in for the "randomly choose one Hero not in the game to be
         * the Dummy Player" step in docs/rules/dummy-player.md ("Setup"). Picks a random [Knight]
         * and delegates to [start] with [wasRandom] set to true.
         */
        fun startRandom(random: Random = Random): DummyPlayerSession {
            val knight = Knight.entries.toList().random(random)
            return start(knight, wasRandom = true)
        }

        /**
         * Reconstructs a session from its full persisted state (used by the persistence layer to
         * restore a saved game). Not for general use - [start]/[startRandom] are the entry points
         * for beginning a new session.
         */
        fun restore(
            knight: Knight,
            wasRandom: Boolean,
            deckOrder: List<CardColor>,
            discardPile: List<CardColor>,
            crystals: Map<CardColor, Int>,
            round: Int,
            roundEnded: Boolean,
            log: List<DummyPlayerEvent>,
        ): DummyPlayerSession = DummyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = discardPile,
            crystals = crystals,
            round = round,
            roundEnded = roundEnded,
            log = log,
        )
    }
}
