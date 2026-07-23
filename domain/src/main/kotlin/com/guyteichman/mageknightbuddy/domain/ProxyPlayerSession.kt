package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random

/**
 * Immutable snapshot of one Proxy Player's state at a point in time - its deck, discard pile,
 * crystal Inventory, current Round, current Objective Card and its Shield count, and a log of
 * events so far. Implements the Proxy Player mode described in docs/rules/proxy-player.md: like
 * [VolkareSession], it narrates rather than simulates the board (see ADR-0004) - it tracks
 * everything the movement-point formula needs, but never decides which map site is targeted or
 * how movement/conquering actually resolves.
 *
 * Mirrors [VolkareSession]'s shape: a `private` constructor reached only through the companion
 * object's [start]/[startRandom]/[restore] factory functions, and every method returns a new
 * `ProxyPlayerSession` via `copy()` rather than mutating this one.
 */
data class ProxyPlayerSession private constructor(
    val knight: Knight,
    val wasRandom: Boolean,
    val deckOrder: List<ProxyPlayerCard>,
    val discardPile: List<ProxyPlayerCard>,
    val crystals: Map<CardColor, Int>,
    val round: Int,
    val roundEnded: Boolean,
    val objectiveCard: ProxyPlayerCard?,
    val objectiveShields: Int,
    val log: List<ProxyPlayerEvent>,
) {
    /**
     * The Proxy Player's total movement points this turn (docs/rules/proxy-player.md's "Movement
     * points"): [objectiveCard]'s own [ProxyPlayerCard.movementBonus] plus [objectiveShields],
     * plus +1 if [hasMatchingManaDie] is true - the player reports this each turn, since whether a
     * matching-color (or gold, by day) mana die sits in the Source is shared table state this app
     * doesn't track (unlike [VolkareSession], which rolls its own die - this is the reverse: an
     * observation, not a randomized event). Returns 0 if there's no current [objectiveCard].
     */
    fun movementPoints(hasMatchingManaDie: Boolean): Int {
        val card = objectiveCard ?: return 0
        return card.movementBonus + objectiveShields + (if (hasMatchingManaDie) 1 else 0)
    }

    companion object {
        /**
         * Which 2 of a Knight's 16 starting Basic Action cards are replaced by that Knight's own
         * Unique Basic Action Cards, by color slot (docs/rules/proxy-player.md's per-Knight
         * table). Feeds [buildStartingDeck].
         */
        private val UNIQUE_CARD_COLORS: Map<Knight, List<CardColor>> = mapOf(
            Knight.TOVAK to listOf(CardColor.BLUE, CardColor.RED),
            Knight.GOLDYX to listOf(CardColor.BLUE, CardColor.GREEN),
            Knight.NOROWAS to listOf(CardColor.WHITE, CardColor.GREEN),
            Knight.WOLFHAWK to listOf(CardColor.BLUE, CardColor.WHITE),
            Knight.ARYTHEA to listOf(CardColor.WHITE, CardColor.RED),
            Knight.KRANG to listOf(CardColor.RED, CardColor.GREEN),
            Knight.BRAEVALAR to listOf(CardColor.BLUE, CardColor.GREEN),
            Knight.CORAL to listOf(CardColor.WHITE, CardColor.RED),
        )

        /**
         * Builds a Knight's starting 16-card deck (docs/rules/proxy-player.md's "Unique Basic
         * Action cards"): the same 16-card, 4-per-color shape as [DummyPlayerSession.start]'s
         * default deck, except the 2 colors in [UNIQUE_CARD_COLORS] each have one of their 4
         * generic copies replaced by a [ProxyPlayerCard.UniqueAction] instead.
         */
        private fun buildStartingDeck(knight: Knight): List<ProxyPlayerCard> {
            val uniqueColors = UNIQUE_CARD_COLORS.getValue(knight)
            // Start every color at 4 generic copies, then subtract one per unique-card color.
            val basicCounts = CardColor.entries.associateWith { 4 }.toMutableMap()
            val cards = mutableListOf<ProxyPlayerCard>()
            for (color in uniqueColors) {
                cards += ProxyPlayerCard.UniqueAction(color)
                basicCounts[color] = basicCounts.getValue(color) - 1
            }
            for (color in CardColor.entries) {
                repeat(basicCounts.getValue(color)) { cards += ProxyPlayerCard.BasicAction(color) }
            }
            return cards
        }

        /**
         * Begins a new Proxy Player session for a chosen [Knight] - session setup per
         * docs/rules/proxy-player.md ("At the start of the game"): a shuffled starting deck (see
         * [buildStartingDeck]), that Knight's starting crystal Inventory (shared with
         * [DummyPlayerSession] via [startingCrystals]), no current objective, and Round 1.
         */
        fun start(
            knight: Knight,
            wasRandom: Boolean = false,
            deckOrder: List<ProxyPlayerCard> = buildStartingDeck(knight).shuffled(),
        ): ProxyPlayerSession = ProxyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = emptyList(),
            crystals = startingCrystals(knight),
            round = 1,
            roundEnded = false,
            objectiveCard = null,
            objectiveShields = 0,
            log = listOf(ProxyPlayerEvent.RoundStarted(round = 1)),
        )

        /** Begins a new session with a randomly-chosen [Knight] - mirrors [DummyPlayerSession.startRandom]. */
        fun startRandom(random: Random = Random): ProxyPlayerSession {
            val knight = Knight.entries.toList().random(random)
            return start(knight, wasRandom = true)
        }

        /** Reconstructs a session from its full persisted state - not for general use; [start]/[startRandom] begin a new session. */
        fun restore(
            knight: Knight,
            wasRandom: Boolean,
            deckOrder: List<ProxyPlayerCard>,
            discardPile: List<ProxyPlayerCard>,
            crystals: Map<CardColor, Int>,
            round: Int,
            roundEnded: Boolean,
            objectiveCard: ProxyPlayerCard?,
            objectiveShields: Int,
            log: List<ProxyPlayerEvent>,
        ): ProxyPlayerSession = ProxyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = discardPile,
            crystals = crystals,
            round = round,
            roundEnded = roundEnded,
            objectiveCard = objectiveCard,
            objectiveShields = objectiveShields,
            log = log,
        )
    }
}
