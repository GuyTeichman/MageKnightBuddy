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
    fun `drawing without replacement moves the top token to the discard and logs it`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val after = session.draw(TokenPileId.GREEN, batchId = 7L, shuffle = noShuffle)

        val green = after.piles.getValue(TokenPileId.GREEN)
        // Top ("orc_a") is gone from the draw pile and now sits in the discard.
        assertEquals(listOf("orc_a", "orc_b", "orc_c"), green.drawPile)
        assertEquals(listOf("orc_a"), green.discardPile)
        // Exactly one log entry, describing that draw, not flagged.
        assertEquals(1, after.drawLog.size)
        assertEquals(DrawLogEntry(tokenId = "orc_a", pile = TokenPileId.GREEN, batchId = 7L), after.drawLog.single())
    }

    @Test
    fun `a batch draw of several shares one batch id`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val after = session.draw(TokenPileId.GREEN, count = 2, batchId = 99L, shuffle = noShuffle)

        // First two of greenOrder are both "orc_a".
        assertEquals(listOf("orc_a", "orc_a"), after.drawLog.map { it.tokenId })
        assertEquals(listOf(99L, 99L), after.drawLog.map { it.batchId })
        val green = after.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_b", "orc_c"), green.drawPile)
        assertEquals(listOf("orc_a", "orc_a"), green.discardPile)
    }

    @Test
    fun `draw only touches the pile drawn from`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        val brownBefore = session.piles.getValue(TokenPileId.BROWN)

        val after = session.draw(TokenPileId.GREEN, shuffle = noShuffle)

        assertEquals(brownBefore, after.piles.getValue(TokenPileId.BROWN))
    }

    @Test
    fun `an emptied draw pile replenishes from its shuffled discard`() {
        // Build the emptied state via the class's own draw() calls (per CLAUDE.md), not by hand.
        var session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        repeat(4) { session = session.draw(TokenPileId.GREEN, shuffle = noShuffle) }

        val emptied = session.piles.getValue(TokenPileId.GREEN)
        assertEquals(emptyList(), emptied.drawPile)
        assertEquals(greenOrder, emptied.discardPile)

        // The 5th draw must replenish: discard (identity-shuffled to greenOrder) becomes the new
        // draw pile, then its top ("orc_a") is drawn.
        session = session.draw(TokenPileId.GREEN, shuffle = noShuffle)
        val replenished = session.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_a", "orc_b", "orc_c"), replenished.drawPile)
        assertEquals(listOf("orc_a"), replenished.discardPile)
        assertEquals(5, session.drawLog.size)
    }

    @Test
    fun `drawing with replacement never depletes the pile and keeps the discard empty`() {
        val session = EnemyPickerSession.start(catalogue, drawWithReplacement = true, shuffle = noShuffle)

        var after = session
        repeat(10) { after = after.draw(TokenPileId.GREEN, shuffle = noShuffle) }

        val green = after.piles.getValue(TokenPileId.GREEN)
        // Pile is exactly as built: nothing removed, nothing discarded, even after 10 draws.
        assertEquals(greenOrder, green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        assertEquals(10, after.drawLog.size)
        // Under identity shuffle every with-replacement draw picks the top, "orc_a".
        assertTrue(after.drawLog.all { it.tokenId == "orc_a" })
    }

    @Test
    fun `flagging a log entry changes only that entry and no pile`() {
        val drawn = EnemyPickerSession.start(catalogue, shuffle = noShuffle).draw(TokenPileId.GREEN, shuffle = noShuffle)
        val pilesBefore = drawn.piles

        val flagged = drawn.flagStillInPlay(index = 0, stillInPlay = true, note = "keep, NE tile")

        val entry = flagged.drawLog[0]
        assertTrue(entry.stillInPlay)
        assertEquals("keep, NE tile", entry.note)
        // The load-bearing property (ADR-0006): a flag must not perturb pile state / draw odds.
        assertEquals(pilesBefore, flagged.piles)
    }

    @Test
    fun `reset rebuilds every pile and clears the log while keeping config`() {
        var session = EnemyPickerSession.start(catalogue, tokenSet = setOf(Expansion.BASE), drawWithReplacement = false, shuffle = noShuffle)
        repeat(3) { session = session.draw(TokenPileId.GREEN, shuffle = noShuffle) }

        val reset = session.reset(catalogue, shuffle = noShuffle)

        assertEquals(greenOrder, reset.piles.getValue(TokenPileId.GREEN).drawPile)
        assertEquals(emptyList(), reset.piles.getValue(TokenPileId.GREEN).discardPile)
        assertEquals(emptyList(), reset.drawLog)
        // Config carried through unchanged.
        assertEquals(setOf(Expansion.BASE), reset.tokenSet)
        assertFalse(reset.drawWithReplacement)
    }

    @Test
    fun `draw rejects a non-positive count`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> { session.draw(TokenPileId.GREEN, count = 0) }
    }
}
