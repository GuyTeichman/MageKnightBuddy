package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [EnemyPickerSession]'s draw / replenish / flag / reset logic. All use a tiny hand-built
 * fixture catalogue and an identity `shuffle` (`{ it }`) so pile order is fully deterministic and
 * every expected value below is reasoned out by hand from the fixture, not read off the code.
 */
class EnemyPickerSessionTest {

    // Identity "shuffle": leaves order untouched, so draws are deterministic (top = first element).
    private val noShuffle: (List<String>) -> List<String> = { it }

    // Fixture green pile: orc_a x2, orc_b x1, orc_c x1 -> 4 tokens, in this catalogue order.
    private val greenA = EnemyToken(id = "orc_a", name = "Orc A", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 2, armor = 3, fame = 2, attacks = listOf(EnemyAttack(4)))
    private val greenB = EnemyToken(id = "orc_b", name = "Orc B", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 1, armor = 4, fame = 3, attacks = listOf(EnemyAttack(3)))
    private val greenC = EnemyToken(id = "orc_c", name = "Orc C", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 1, armor = 5, fame = 4, attacks = listOf(EnemyAttack(2)))
    private val brownX = EnemyToken(id = "brown_x", name = "Brown X", pile = TokenPileId.BROWN, expansion = Expansion.BASE, copies = 1, armor = 6, fame = 5, attacks = listOf(EnemyAttack(5)))
    private val legionToken = EnemyToken(id = "green_ll", name = "Legion Green", pile = TokenPileId.GREEN, expansion = Expansion.LOST_LEGION, copies = 1, armor = 3, fame = 2, attacks = listOf(EnemyAttack(4)))

    private val catalogue = listOf(greenA, greenB, greenC, brownX, legionToken)

    // Ruin fixtures: two BASE ruins (an altar and an Enemies-With-Treasure) plus one Lost Legion
    // ruin, to exercise the RUIN pile being built from its own catalogue and gated by expansion.
    private val ruinAltar = RuinToken(id = "ruin_altar_red", expansion = Expansion.BASE, altarColors = listOf(ManaColor.RED))
    private val ruinEwt = RuinToken(id = "ruin_red_brown", expansion = Expansion.BASE, enemyPiles = listOf(TokenPileId.RED, TokenPileId.BROWN), reward = "2 Artifacts")
    private val ruinLegion = RuinToken(id = "ruin_ll_altar", expansion = Expansion.LOST_LEGION, altarColors = listOf(ManaColor.RED, ManaColor.BLUE, ManaColor.GREEN, ManaColor.WHITE))
    private val ruinCatalogue = listOf(ruinAltar, ruinEwt, ruinLegion)

    // Expected green draw pile under identity shuffle: copies expanded in catalogue order.
    private val greenOrder = listOf("orc_a", "orc_a", "orc_b", "orc_c")

    @Test
    fun `start expands copies into the draw pile in catalogue order with an empty discard`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val green = session.piles.getValue(TokenPileId.GREEN)
        assertEquals(greenOrder, green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        assertEquals(emptyList(), session.drawLog)
        assertFalse(session.drawWithReplacement)
    }

    @Test
    fun `start includes only tokens whose expansion is in the token set`() {
        // Base-only: the Lost Legion green token is excluded, so green has 4 (not 5) tokens.
        val baseOnly = EnemyPickerSession.start(catalogue, tokenSet = setOf(Expansion.BASE), shuffle = noShuffle)
        assertEquals(greenOrder, baseOnly.piles.getValue(TokenPileId.GREEN).drawPile)

        // With Lost Legion added, its token joins the green pile (appended, catalogue order).
        val withLegion = EnemyPickerSession.start(catalogue, tokenSet = setOf(Expansion.BASE, Expansion.LOST_LEGION), shuffle = noShuffle)
        assertEquals(greenOrder + "green_ll", withLegion.piles.getValue(TokenPileId.GREEN).drawPile)
    }

    @Test
    fun `start builds a RUIN pile of one copy per ruin, gated by the token set`() {
        // Base-only: the two BASE ruins appear (catalogue order under identity shuffle), one copy
        // each; the Lost Legion ruin is excluded.
        val baseOnly = EnemyPickerSession.start(
            catalogue, tokenSet = setOf(Expansion.BASE), shuffle = noShuffle, ruinCatalogue = ruinCatalogue,
        )
        val ruinPile = baseOnly.piles.getValue(TokenPileId.RUIN)
        assertEquals(listOf("ruin_altar_red", "ruin_red_brown"), ruinPile.drawPile)
        assertEquals(emptyList(), ruinPile.discardPile)

        // With Lost Legion enabled, its ruin joins the pile (appended, catalogue order).
        val withLegion = EnemyPickerSession.start(
            catalogue, tokenSet = setOf(Expansion.BASE, Expansion.LOST_LEGION), shuffle = noShuffle, ruinCatalogue = ruinCatalogue,
        )
        assertEquals(
            listOf("ruin_altar_red", "ruin_red_brown", "ruin_ll_altar"),
            withLegion.piles.getValue(TokenPileId.RUIN).drawPile,
        )
    }

    @Test
    fun `start without a ruin catalogue builds no RUIN pile`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        assertFalse(session.piles.containsKey(TokenPileId.RUIN))
    }

    @Test
    fun `drawing without replacement moves the top token to the discard and logs it`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val after = session.draw(mapOf(TokenPileId.GREEN to 1), batchId = 7L, shuffle = noShuffle)

        val green = after.piles.getValue(TokenPileId.GREEN)
        // Top ("orc_a") is gone from the draw pile and now sits in the discard.
        assertEquals(listOf("orc_a", "orc_b", "orc_c"), green.drawPile)
        assertEquals(listOf("orc_a"), green.discardPile)
        // Exactly one log entry, describing that draw, not flagged.
        assertEquals(1, after.drawLog.size)
        assertEquals(DrawLogEntry(tokenId = "orc_a", pile = TokenPileId.GREEN, batchId = 7L), after.drawLog.single())
    }

    @Test
    fun `a batch draw of several from one pile shares one batch id`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val after = session.draw(mapOf(TokenPileId.GREEN to 2), batchId = 99L, shuffle = noShuffle)

        // First two of greenOrder are both "orc_a".
        assertEquals(listOf("orc_a", "orc_a"), after.drawLog.map { it.tokenId })
        assertEquals(listOf(99L, 99L), after.drawLog.map { it.batchId })
        val green = after.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_b", "orc_c"), green.drawPile)
        assertEquals(listOf("orc_a", "orc_a"), green.discardPile)
    }

    @Test
    fun `draw only touches the piles requested`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        val brownBefore = session.piles.getValue(TokenPileId.BROWN)

        val after = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)

        assertEquals(brownBefore, after.piles.getValue(TokenPileId.BROWN))
    }

    @Test
    fun `a pile reshuffles the instant its last token is drawn, never sitting empty`() {
        // Build the state via the class's own draw() calls (per CLAUDE.md), not by hand.
        var session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        // Draw all 4 green tokens: the 4th draw empties the draw pile, which must reshuffle on
        // the spot (issue #231 / eager Replenish) rather than resting empty until the next draw.
        repeat(4) { session = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }

        val green = session.piles.getValue(TokenPileId.GREEN)
        // Discard (all 4, identity-shuffled back to greenOrder) has already become the new draw pile
        // and the discard is empty again - the "empty draw pile, 4 in discard" resting state that
        // lazy replenish used to leave behind is now never observable.
        assertEquals(greenOrder, green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        // Every draw is still logged, in draw order (a, a, b, c) - the reshuffle is pile state only.
        assertEquals(listOf("orc_a", "orc_a", "orc_b", "orc_c"), session.drawLog.map { it.tokenId })

        // A further draw now simply comes off the reshuffled pile's top ("orc_a").
        session = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        val next = session.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_a", "orc_b", "orc_c"), next.drawPile)
        assertEquals(listOf("orc_a"), next.discardPile)
        assertEquals(5, session.drawLog.size)
    }

    @Test
    fun `drawing with replacement never depletes the pile and keeps the discard empty`() {
        val session = EnemyPickerSession.start(catalogue, drawWithReplacement = true, shuffle = noShuffle)

        var after = session
        repeat(10) { after = after.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }

        val green = after.piles.getValue(TokenPileId.GREEN)
        // Pile is exactly as built: nothing removed, nothing discarded, even after 10 draws.
        assertEquals(greenOrder, green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        assertEquals(10, after.drawLog.size)
        // Under identity shuffle every with-replacement draw picks the top, "orc_a".
        assertTrue(after.drawLog.all { it.tokenId == "orc_a" })
    }

    @Test
    fun `a freshly drawn enemy is on the board, not defeated`() {
        val drawn = EnemyPickerSession.start(catalogue, shuffle = noShuffle).draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        // Default lifecycle: revealed onto the board, awaiting a Defeat tap (D2).
        assertFalse(drawn.drawLog.single().defeated)
    }

    @Test
    fun `marking a log entry defeated changes only that entry and no pile`() {
        val drawn = EnemyPickerSession.start(catalogue, shuffle = noShuffle).draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        val pilesBefore = drawn.piles

        val defeated = drawn.setDefeated(index = 0, defeated = true, note = "keep, NE tile")

        val entry = defeated.drawLog[0]
        assertTrue(entry.defeated)
        assertEquals("keep, NE tile", entry.note)
        // The load-bearing property (ADR-0006): the flag must not perturb pile state / draw odds.
        assertEquals(pilesBefore, defeated.piles)
    }

    @Test
    fun `reset rebuilds every pile and clears the log while keeping config`() {
        var session = EnemyPickerSession.start(catalogue, tokenSet = setOf(Expansion.BASE), drawWithReplacement = false, shuffle = noShuffle)
        repeat(3) { session = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }

        val reset = session.reset(catalogue, shuffle = noShuffle)

        assertEquals(greenOrder, reset.piles.getValue(TokenPileId.GREEN).drawPile)
        assertEquals(emptyList(), reset.piles.getValue(TokenPileId.GREEN).discardPile)
        assertEquals(emptyList(), reset.drawLog)
        // Config carried through unchanged.
        assertEquals(setOf(Expansion.BASE), reset.tokenSet)
        assertFalse(reset.drawWithReplacement)
    }

    @Test
    fun `draw rejects an empty pile map`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> { session.draw(emptyMap()) }
    }

    @Test
    fun `draw rejects a non-positive count for any requested pile`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> { session.draw(mapOf(TokenPileId.GREEN to 0)) }
        assertFailsWith<IllegalArgumentException> {
            session.draw(mapOf(TokenPileId.GREEN to 1, TokenPileId.BROWN to -1))
        }
    }

    @Test
    fun `a multi-pile draw shares one batch id across every pile's entries`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val after = session.draw(mapOf(TokenPileId.GREEN to 2, TokenPileId.BROWN to 1), batchId = 42L, shuffle = noShuffle)

        assertEquals(3, after.drawLog.size)
        assertTrue(after.drawLog.all { it.batchId == 42L })
    }

    @Test
    fun `a multi-pile draw orders log entries by TokenPileId enum order, not map insertion order`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        // BROWN listed first in the map, but GREEN precedes BROWN in TokenPileId.entries.
        val after = session.draw(mapOf(TokenPileId.BROWN to 1, TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle)

        assertEquals(listOf(TokenPileId.GREEN, TokenPileId.BROWN), after.drawLog.map { it.pile })
        assertEquals(listOf("orc_a", "brown_x"), after.drawLog.map { it.tokenId })
    }

    @Test
    fun `a multi-pile draw replenishes an individual pile independently within the same batch`() {
        // Drain green to its last token (3 single-pile draws leave 1 remaining, 3 in discard).
        var session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        repeat(3) { session = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }
        assertEquals(listOf("orc_c"), session.piles.getValue(TokenPileId.GREEN).drawPile)

        // One multi-pile batch: green needs 2 (only 1 left, so it must replenish mid-batch), brown needs 1.
        val after = session.draw(mapOf(TokenPileId.GREEN to 2, TokenPileId.BROWN to 1), batchId = 5L, shuffle = noShuffle)

        // Green: draws its last card ("orc_c"), empties, replenishes (discard "orc_a","orc_a","orc_b",
        // "orc_c" identity-shuffled back to that same order), then draws its new top ("orc_a"). Only
        // this batch's entries are checked - the log already carries the 3 prior single draws.
        val thisBatch = after.drawLog.takeLast(3)
        val greenEntries = thisBatch.filter { it.pile == TokenPileId.GREEN }
        assertEquals(listOf("orc_c", "orc_a"), greenEntries.map { it.tokenId })
        val green = after.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_a", "orc_b", "orc_c"), green.drawPile)
        assertEquals(listOf("orc_a"), green.discardPile)

        // Brown drew its only token, independent of green's replenish.
        val brownEntries = thisBatch.filter { it.pile == TokenPileId.BROWN }
        assertEquals(listOf("brown_x"), brownEntries.map { it.tokenId })

        assertEquals(6, after.drawLog.size)
        assertTrue(thisBatch.all { it.batchId == 5L })
    }

    @Test
    fun `summon draws one token per requested pile and tags each entry with the parent's log index`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle) // parent at index 0

        val after = session.summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 50L, shuffle = noShuffle)

        assertEquals(2, after.drawLog.size)
        assertEquals(
            DrawLogEntry(tokenId = "brown_x", pile = TokenPileId.BROWN, batchId = 50L, parentIndex = 0),
            after.drawLog[1],
        )
        // The summon draw goes through the same pile mechanics as any other draw (ADR-0006).
        val brown = after.piles.getValue(TokenPileId.BROWN)
        // BROWN holds a single copy, so drawing it empties the pile - which eager-Replenishes on
        // the spot (issue #231): the token is already reshuffled back and the discard is empty.
        assertEquals(listOf("brown_x"), brown.drawPile)
        assertEquals(emptyList(), brown.discardPile)
    }

    @Test
    fun `a restored pile with an already-empty draw pile still replenishes on the next draw`() {
        // Simulates a session persisted under the old lazy semantics: draw pile empty, all 4 green
        // tokens sitting in the discard. The eager reshuffle never produces this state itself, but
        // restore() must still cope with it (the defensive replenish-on-entry branch).
        val legacy = EnemyPickerSession.restore(
            tokenSet = setOf(Expansion.BASE),
            drawWithReplacement = false,
            piles = mapOf(TokenPileId.GREEN to TokenPile(drawPile = emptyList(), discardPile = greenOrder)),
            drawLog = emptyList(),
        )

        val after = legacy.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)

        // Discard (identity-shuffled to greenOrder) replenishes the draw pile, then its top
        // ("orc_a") is drawn; 3 remain, so no further reshuffle fires.
        val green = after.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_a", "orc_b", "orc_c"), green.drawPile)
        assertEquals(listOf("orc_a"), green.discardPile)
    }

    @Test
    fun `summon rejects an out-of-range parent index`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> {
            session.summon(parentIndex = 1, pileIds = listOf(TokenPileId.BROWN))
        }
    }

    @Test
    fun `summon rejects an empty pile list`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> {
            session.summon(parentIndex = 0, pileIds = emptyList())
        }
    }

    @Test
    fun `re-summoning the same parent appends a new set of children rather than replacing the old one`() {
        // Two-slot summoner: drains BROWN's only copy first, so the second summon must replenish it.
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle) // parent at index 0

        val firstSummon = session.summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 10L, shuffle = noShuffle)
        val resummoned = firstSummon.summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 20L, shuffle = noShuffle)

        // Both summons are still in the log (append-only) - nothing overwritten.
        assertEquals(3, resummoned.drawLog.size)
        assertEquals(listOf(10L, 20L), resummoned.drawLog.drop(1).map { it.batchId })
        assertTrue(resummoned.drawLog.drop(1).all { it.parentIndex == 0 })
    }

    @Test
    fun `currentChildrenOf returns only the most recent summon batch for a parent`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle) // parent at index 0
            .summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 10L, shuffle = noShuffle) // stale child at index 1
            .summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 20L, shuffle = noShuffle) // current child at index 2

        assertEquals(listOf(2), session.currentChildrenOf(parentIndex = 0))
    }

    @Test
    fun `currentChildrenOf is empty for an entry that was never summoned from`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)

        assertEquals(emptyList(), session.currentChildrenOf(parentIndex = 0))
    }

    @Test
    fun `a two-slot summon shares one batch id across both children`() {
        // BROWN and GREEN both used as summon piles here purely as two distinct fixture piles.
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) // parent at index 0, leaves 3 in GREEN

        val after = session.summon(
            parentIndex = 0,
            pileIds = listOf(TokenPileId.BROWN, TokenPileId.GREEN),
            batchId = 77L,
            shuffle = noShuffle,
        )

        val children = after.drawLog.drop(1)
        assertEquals(2, children.size)
        assertTrue(children.all { it.batchId == 77L && it.parentIndex == 0 })
        assertEquals(listOf("brown_x", "orc_a"), children.map { it.tokenId })
    }
}
