package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class TurnsRemainingEstimatorTest {

    // ---- Hand-derived cases (expected values reasoned out from the turn rule, not read off code) ----

    @Test
    fun `an empty deck is one turn - only the End of Round declaration remains`() {
        assertEquals(TurnEstimate(1, 1), estimateTurnsRemaining(emptyList()))
    }

    @Test
    fun `a single card takes one flip turn, then the declaration turn`() {
        // 1 card < 3: one productive turn flips it, next turn declares End of Round. Bonus is
        // impossible with nothing left to chain, so min == max == 2.
        assertEquals(TurnEstimate(2, 2), estimateTurnsRemaining(listOf(0)))
        assertEquals(TurnEstimate(2, 2), estimateTurnsRemaining(listOf(3)))
    }

    @Test
    fun `exactly three no-bonus cards is one flip turn plus the declaration`() {
        // flip-3 empties the deck in one turn (no room for a chained card), then declaration = 2.
        assertEquals(TurnEstimate(2, 2), estimateTurnsRemaining(listOf(0, 0, 0)))
    }

    @Test
    fun `six no-bonus cards flip three at a time - two turns plus the declaration`() {
        // No crystals match anything, so every turn flips exactly 3: 6 / 3 = 2 turns, + declaration.
        assertEquals(TurnEstimate(3, 3), estimateTurnsRemaining(listOf(0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun `three no-bonus cards and one high-bonus card gives a real range`() {
        // Cards' chain bonuses: three 0s and one 2.
        // Fastest (min): trigger the 2-card - flip 3 + 2 chained = all 4 cards in ONE turn, then
        //   declaration => 2.
        // Slowest (max): trigger a 0-card - flip 3 (bonus 0), leaving 1 card => a second flip turn,
        //   then declaration => 3.
        assertEquals(TurnEstimate(2, 3), estimateTurnsRemaining(listOf(0, 0, 0, 2)))
    }

    @Test
    fun `every card forcing a bonus means the max is below the naive cards-over-three`() {
        // Seven cards that each chain +1 (e.g. the Dummy holds a crystal of every color present).
        // No turn can flip only 3 - the trigger always forces one chained card - so each full turn
        // takes 4: 4 + 3 = 7 in two productive turns, + declaration = 3. A naive ceil(7/3)+1 = 4
        // would be wrong here; this is why max can't assume a bonus-free turn is available.
        assertEquals(TurnEstimate(3, 3), estimateTurnsRemaining(List(7) { 1 }))
    }

    @Test
    fun `a big bonus with tiny supply cannot be reused every turn`() {
        // One card chains +3, nine chain +0 (ten cards total).
        // Fastest (min): the single +3 card can trigger only ONE big turn (flip 3 + 3 = 6 cards);
        //   the remaining four 0-cards then need 3 + 1 = two more turns => 3 productive, + 1 = 4.
        //   A naive ceil(10 / (3+3)) = 2 would be wrong - there aren't enough +3 cards to repeat it.
        // Slowest (max): never trigger the +3 card (spend it as filler); all turns flip 3:
        //   ceil(10/3) = 4 productive, + 1 = 5.
        assertEquals(TurnEstimate(4, 5), estimateTurnsRemaining(listOf(3) + List(9) { 0 }))
    }

    @Test
    fun `isExact reflects whether the range collapses`() {
        assertEquals(true, estimateTurnsRemaining(listOf(0, 0, 0)).isExact)
        assertEquals(false, estimateTurnsRemaining(listOf(0, 0, 0, 2)).isExact)
    }

    // ---- Session accessor wiring (independently-derived expected values, not a re-spelling of the
    //      accessor's own body) ----

    @Test
    fun `DummyPlayerSession turnsRemaining is the hand-derived estimate for Tovak's start deck`() {
        // Tovak starts with 16 cards (4 of each colour) and 1 red + 2 blue crystals, so the deck's
        // chain bonuses are 8x0 (green/white cards), 4x1 (red), 4x2 (blue). Fastest: trigger the
        // blue (+2) cards - three 5-card turns clear 15, then a 1-card turn, + declaration = 5.
        // Slowest: never chain (trigger a green/white card) - ceil(16/3) = 6 flip turns, + the
        // declaration = 7. (The Proxy accessor is covered independently by the Proxy oracle below.)
        assertEquals(TurnEstimate(5, 7), DummyPlayerSession.start(Knight.TOVAK).turnsRemaining)
    }

    // ---- Brute-force oracle: exhaustively simulate the REAL turn logic over every deck order ----

    @Test
    fun `oracle - single-color deck with mixed crystals`() {
        assertMatchesOracle(
            deck = listOf(RED, RED, GREEN, BLUE, BLUE).map(::single),
            crystals = crystalsOf(red = 1, blue = 2),
        )
    }

    @Test
    fun `oracle - crystals of every colour force a bonus on every card`() {
        assertMatchesOracle(
            deck = listOf(RED, GREEN, BLUE, WHITE, RED).map(::single),
            crystals = crystalsOf(red = 1, green = 1, blue = 1, white = 1),
        )
    }

    @Test
    fun `oracle - one high-bonus colour with few matching cards`() {
        assertMatchesOracle(
            deck = listOf(BLUE, RED, RED, RED, RED, RED).map(::single),
            crystals = crystalsOf(blue = 3),
        )
    }

    @Test
    fun `oracle - dual-color card chains on the higher of its two colours`() {
        val deck = listOf(
            CardIdentity.DualColor(RED, WHITE), // chains max(crystals red, white)
            single(RED),
            single(WHITE),
            single(GREEN),
        )
        assertMatchesOracle(deck = deck, crystals = crystalsOf(red = 2, white = 1))
    }

    @Test
    fun `oracle - seven-card mix with a dual card and multi-colour crystals`() {
        val deck = listOf(
            CardIdentity.DualColor(GREEN, BLUE),
            single(GREEN), single(GREEN),
            single(BLUE),
            single(RED), single(RED),
            single(WHITE),
        )
        assertMatchesOracle(deck = deck, crystals = crystalsOf(green = 1, blue = 2, red = 1))
    }

    @Test
    fun `oracle - boundary deck sizes of three and four`() {
        assertMatchesOracle(listOf(RED, GREEN, BLUE).map(::single), crystalsOf(red = 2))
        assertMatchesOracle(List(4) { single(RED) }, crystalsOf(red = 1))
    }

    @Test
    fun `oracle - the hand-derived three-zeros-and-a-two case, as a real deck`() {
        // Mirrors `three no-bonus cards and one high-bonus card`: G,G,G bonus 0; B bonus 2.
        assertMatchesOracle(listOf(GREEN, GREEN, GREEN, BLUE).map(::single), crystalsOf(blue = 2))
    }

    // ---- Proxy oracle: the SAME estimator, verified against the real ProxyPlayerSession turn loop
    //      (which has its own objective-card branch), and asserting the ProxyPlayerSession accessor ----

    @Test
    fun `proxy oracle - basic and unique cards with mixed crystals`() {
        assertProxyMatchesOracle(
            deck = listOf(
                ProxyPlayerCard.BasicAction(RED),
                ProxyPlayerCard.BasicAction(RED),
                ProxyPlayerCard.UniqueAction(WHITE), // chains on crystals[white] like any white card
                ProxyPlayerCard.BasicAction(GREEN),
                ProxyPlayerCard.BasicAction(BLUE),
            ),
            crystals = crystalsOf(red = 1, white = 2),
        )
    }

    @Test
    fun `proxy oracle - dual-color advanced action chains on the higher colour`() {
        assertProxyMatchesOracle(
            deck = listOf(
                ProxyPlayerCard.AdvancedAction(CardIdentity.DualColor(GREEN, BLUE)),
                ProxyPlayerCard.BasicAction(GREEN),
                ProxyPlayerCard.BasicAction(BLUE),
                ProxyPlayerCard.BasicAction(RED),
            ),
            crystals = crystalsOf(green = 1, blue = 2),
        )
    }

    @Test
    fun `proxy oracle - crystals of every colour and a boundary size`() {
        assertProxyMatchesOracle(
            deck = listOf(RED, GREEN, BLUE, WHITE, RED, GREEN).map { ProxyPlayerCard.BasicAction(it) },
            crystals = crystalsOf(red = 1, green = 1, blue = 1, white = 1),
        )
        assertProxyMatchesOracle(
            deck = listOf(RED, GREEN, BLUE).map { ProxyPlayerCard.BasicAction(it) },
            crystals = crystalsOf(blue = 2),
        )
    }

    // ---- Exhaustive property test: the greedy solver vs the real turn loop over a bounded space ----

    @Test
    fun `property - matches the real turn loop for every small deck and crystal config`() {
        // Exhaustive over a bounded space: decks of 0..6 cards drawn from 3 colours, with 0..3
        // crystals of each colour. This covers multiple bonus buckets, the bonus cap (bonus > cards
        // left after the base 3), and short final turns. If the greedy solver ever disagreed with the
        // real playTurn loop, one of these thousands of cases would catch it - which is what lets the
        // shipped estimator use the fast greedy walk instead of an exponential exact search.
        val colours = listOf(RED, GREEN, BLUE)
        var checked = 0
        for (size in 0..6) {
            for (deck in colourMultisets(colours, size)) {
                val identities = deck.map(::single)
                for (crystals in crystalConfigs(colours, maxEach = 3)) {
                    assertEquals(
                        dummyOracle(identities, crystals),
                        estimateTurnsRemaining(identities.map { it.matchingCrystalCount(crystals) }),
                        "deck=$deck crystals=$crystals",
                    )
                    checked++
                }
            }
        }
        println("PROPERTY checked $checked deck/crystal combinations")
    }

    // ---- helpers ----

    /**
     * Proxy-Player counterpart of [assertMatchesOracle]: drives the real [ProxyPlayerSession.playTurn]
     * loop (objective-card branch and all) over every distinct order of [deck], and asserts the
     * shipped [ProxyPlayerSession.turnsRemaining] accessor equals the true min/max. Independent of the
     * estimator's own logic and of the Dummy path.
     */
    private fun assertProxyMatchesOracle(deck: List<ProxyPlayerCard>, crystals: Map<CardColor, Int>) {
        var oracleMin = Int.MAX_VALUE
        var oracleMax = Int.MIN_VALUE
        for (order in distinctPermutations(deck)) {
            var session = ProxyPlayerSession.restore(
                knight = Knight.GOLDYX, // irrelevant: crystals supplied explicitly
                wasRandom = false,
                deckOrder = order,
                discardPile = emptyList(),
                crystals = crystals,
                round = 1,
                roundEnded = false,
                objectiveCard = null,
                objectiveShields = 0,
                log = emptyList(),
            )
            var turns = 0
            // Never resolve the objective - it just persists; the deck still empties at 3+chain/turn,
            // which is all the turn count depends on.
            while (!session.roundEnded) {
                session = session.playTurn()
                turns++
            }
            oracleMin = minOf(oracleMin, turns)
            oracleMax = maxOf(oracleMax, turns)
        }
        val fixed = ProxyPlayerSession.restore(
            knight = Knight.GOLDYX, wasRandom = false, deckOrder = deck, discardPile = emptyList(),
            crystals = crystals, round = 1, roundEnded = false, objectiveCard = null,
            objectiveShields = 0, log = emptyList(),
        )
        assertEquals(TurnEstimate(oracleMin, oracleMax), fixed.turnsRemaining)
    }

    /**
     * Asserts the fast [estimateTurnsRemaining] matches an independent brute force: for EVERY distinct
     * order of [deck], play the real [DummyPlayerSession.playTurn] loop until End of Round and count
     * the turns; the true min/max across all orders must equal the estimate. This shares none of the
     * estimator's own logic - it drives the shipped turn mechanic directly.
     */
    private fun assertMatchesOracle(deck: List<CardIdentity>, crystals: Map<CardColor, Int>) {
        assertEquals(
            dummyOracle(deck, crystals),
            estimateTurnsRemaining(deck.map { it.matchingCrystalCount(crystals) }),
            "deck=$deck crystals=$crystals",
        )
    }

    /** The true (min, max) turns via the real [DummyPlayerSession.playTurn] loop over every distinct order of [deck]. */
    private fun dummyOracle(deck: List<CardIdentity>, crystals: Map<CardColor, Int>): TurnEstimate {
        var oracleMin = Int.MAX_VALUE
        var oracleMax = Int.MIN_VALUE
        for (order in distinctPermutations(deck)) {
            var session = DummyPlayerSession.restore(
                knight = Knight.GOLDYX, // irrelevant: crystals are supplied explicitly below
                wasRandom = false,
                deckOrder = order,
                discardPile = emptyList(),
                crystals = crystals,
                round = 1,
                roundEnded = false,
                log = emptyList(),
            )
            var turns = 0
            // Each playTurn is one turn; the final call (deck empty at its start) is the End of Round
            // declaration, so the count already includes the "+1" the estimator adds.
            while (!session.roundEnded) {
                session = session.playTurn()
                turns++
            }
            oracleMin = minOf(oracleMin, turns)
            oracleMax = maxOf(oracleMax, turns)
        }
        return TurnEstimate(oracleMin, oracleMax)
    }

    private companion object {
        val RED = CardColor.RED
        val GREEN = CardColor.GREEN
        val BLUE = CardColor.BLUE
        val WHITE = CardColor.WHITE

        fun single(color: CardColor): CardIdentity = CardIdentity.SingleColor(color)

        /** A full crystal map (all four colours present, as matchingCrystalCount requires) from the given counts. */
        fun crystalsOf(red: Int = 0, green: Int = 0, blue: Int = 0, white: Int = 0): Map<CardColor, Int> =
            mapOf(RED to red, GREEN to green, BLUE to blue, WHITE to white)

        /** Every distinct ordering of [items] (deduping identical elements so repeats don't explode). */
        fun <T> distinctPermutations(items: List<T>): List<List<T>> {
            if (items.isEmpty()) return listOf(emptyList())
            val result = mutableListOf<List<T>>()
            val usedValues = mutableSetOf<T>()
            for (i in items.indices) {
                val head = items[i]
                if (!usedValues.add(head)) continue // skip a value already placed at this position
                val rest = items.toMutableList().apply { removeAt(i) }
                for (tail in distinctPermutations(rest)) result.add(listOf(head) + tail)
            }
            return result
        }

        /** Every multiset of [size] cards drawn from [colours], as a sorted colour list (no duplicate orderings). */
        fun colourMultisets(colours: List<CardColor>, size: Int): List<List<CardColor>> {
            if (size == 0) return listOf(emptyList())
            if (colours.isEmpty()) return emptyList()
            val head = colours.first()
            val rest = colours.drop(1)
            val result = mutableListOf<List<CardColor>>()
            // Take k copies of the first colour, then fill the remaining size from the other colours.
            for (k in 0..size) {
                for (tail in colourMultisets(rest, size - k)) result.add(List(k) { head } + tail)
            }
            return result
        }

        /** Every crystal map giving each of [colours] a count in 0..[maxEach] (all other colours 0). */
        fun crystalConfigs(colours: List<CardColor>, maxEach: Int): List<Map<CardColor, Int>> {
            var configs = listOf(CardColor.entries.associateWith { 0 })
            // Fold each colour in, branching over its 0..maxEach possibilities (Cartesian product).
            for (colour in colours) {
                configs = configs.flatMap { base -> (0..maxEach).map { base + (colour to it) } }
            }
            return configs
        }
    }
}
