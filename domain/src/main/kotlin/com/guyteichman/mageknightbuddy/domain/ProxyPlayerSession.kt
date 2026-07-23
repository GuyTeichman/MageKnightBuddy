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
     * Plays one Proxy Player turn (docs/rules/proxy-player.md's "The Proxy Player's turn"). If
     * the deck is empty, announces End of Round instead (mirrors
     * [DummyPlayerSession.playTurn]'s empty-deck guard). Otherwise branches on whether there's a
     * current [objectiveCard]:
     * - **Has one**: adds a Shield token to it, then flips 3 cards (plus any crystal-chain
     *   extension - see [flipMandatoryAndChain]) onto the discard pile.
     * - **Doesn't have one**: flips the same mandatory-3-plus-chain batch, but the *first* card
     *   becomes the new [objectiveCard] instead of being discarded - the rest go to the discard
     *   pile. If the deck had only 1 card left, that single card becomes the objective and
     *   nothing is discarded (see docs/rules/proxy-player.md's note on this case).
     *
     * Once [roundEnded] is true, further calls are a no-op (mirrors [DummyPlayerSession.playTurn]).
     */
    fun playTurn(): ProxyPlayerSession {
        if (roundEnded) return this

        if (deckOrder.isEmpty()) {
            return copy(roundEnded = true, log = log + ProxyPlayerEvent.EndOfRoundAnnounced(round))
        }

        val (revealed, remainingDeck) = flipMandatoryAndChain(deckOrder, mandatoryCount = 3, crystals)

        return if (objectiveCard != null) {
            val shieldsNow = objectiveShields + 1
            copy(
                deckOrder = remainingDeck,
                discardPile = discardPile + revealed,
                objectiveShields = shieldsNow,
                log = log + ProxyPlayerEvent.TurnContinued(round, objectiveCard, shieldsNow, revealed),
            )
        } else {
            val newObjective = revealed.first()
            val discarded = revealed.drop(1)
            copy(
                deckOrder = remainingDeck,
                discardPile = discardPile + discarded,
                objectiveCard = newObjective,
                objectiveShields = 0,
                log = log + ProxyPlayerEvent.NewObjectiveDrawn(round, newObjective, discarded),
            )
        }
    }

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

/**
 * Flips up to [mandatoryCount] cards off the front of [deck], then - if there's a last flipped
 * card and it matches any crystals in [crystals] (see [ProxyPlayerCard.matchingCrystalCount]) -
 * flips one additional card per matching crystal, capped by what's left in the deck. Returns the
 * full list of revealed cards (in flip order) and the remaining deck. Shared by both branches of
 * [ProxyPlayerSession.playTurn], since the flip-and-chain mechanics are identical regardless of
 * which flipped card ends up becoming the new objective vs. going straight to discard.
 */
private fun flipMandatoryAndChain(
    deck: List<ProxyPlayerCard>,
    mandatoryCount: Int,
    crystals: Map<CardColor, Int>,
): Pair<List<ProxyPlayerCard>, List<ProxyPlayerCard>> {
    val mandatory = deck.take(mandatoryCount)
    val afterMandatory = deck.drop(mandatoryCount)
    val lastCard = mandatory.lastOrNull() ?: return mandatory to afterMandatory
    val matching = lastCard.matchingCrystalCount(crystals)
    val additionalCount = minOf(matching, afterMandatory.size)
    val additional = afterMandatory.take(additionalCount)
    val remaining = afterMandatory.drop(additionalCount)
    return (mandatory + additional) to remaining
}
