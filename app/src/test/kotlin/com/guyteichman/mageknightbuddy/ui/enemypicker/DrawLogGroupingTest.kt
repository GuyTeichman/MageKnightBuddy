package com.guyteichman.mageknightbuddy.ui.enemypicker

import com.guyteichman.mageknightbuddy.domain.DrawLogEntry
import com.guyteichman.mageknightbuddy.domain.TokenPileId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for issue #203/D18-D19: collapsing the Draw Log into per-batch groups (a fresh, real
 * decision - not previously covered, since [groupDrawLog] didn't exist before this issue) and
 * deciding which "on the board"/"defeated" bucket each group belongs in.
 */
class DrawLogGroupingTest {

    private fun entry(
        tokenId: String = "token",
        pile: TokenPileId = TokenPileId.GREEN,
        batchId: Long = 1L,
        defeated: Boolean = false,
        parentIndex: Int? = null,
    ) = DrawLogEntry(tokenId = tokenId, pile = pile, batchId = batchId, defeated = defeated, parentIndex = parentIndex)

    @Test
    fun `single-entry batches each become their own group`() {
        val log = listOf(entry(batchId = 1L), entry(batchId = 2L), entry(batchId = 3L))

        val groups = groupDrawLog(log)

        assertEquals(listOf(listOf(2), listOf(1), listOf(0)), groups.map { it.logIndices })
    }

    @Test
    fun `entries sharing a batchId collapse into one group`() {
        val log = listOf(
            entry(batchId = 1L),
            entry(batchId = 2L),
            entry(batchId = 2L),
            entry(batchId = 2L),
        )

        val groups = groupDrawLog(log)

        assertEquals(2, groups.size)
        // Newest batch (2L, drawn second) first.
        assertEquals(listOf(1, 2, 3), groups[0].logIndices)
        assertEquals(2L, groups[0].batchId)
        assertEquals(listOf(0), groups[1].logIndices)
    }

    @Test
    fun `entries sharing a batchId merge into one group even if not contiguous in the log`() {
        // Shouldn't happen from a real draw() call (batches are always appended contiguously), but
        // grouping is keyed by batchId, not position, so it shouldn't fold unrelated batches
        // together just because they're interleaved.
        val log = listOf(entry(batchId = 1L), entry(batchId = 2L), entry(batchId = 1L))

        val groups = groupDrawLog(log)

        assertEquals(2, groups.size)
        assertEquals(setOf(0, 2), groups.first { it.batchId == 1L }.logIndices.toSet())
        assertEquals(listOf(1), groups.first { it.batchId == 2L }.logIndices)
    }

    @Test
    fun `summon draw children are excluded from grouping`() {
        val log = listOf(
            entry(batchId = 1L),
            entry(batchId = 2L, parentIndex = 0),
            entry(batchId = 2L, parentIndex = 0),
        )

        val groups = groupDrawLog(log)

        assertEquals(1, groups.size)
        assertEquals(listOf(0), groups.single().logIndices)
    }

    @Test
    fun `empty log has no groups`() {
        assertEquals(emptyList(), groupDrawLog(emptyList()))
    }

    @Test
    fun `a batch is not allDefeated while any member is on the board`() {
        val log = listOf(
            entry(batchId = 1L, defeated = true),
            entry(batchId = 1L, defeated = false),
        )
        val group = groupDrawLog(log).single()

        assertTrue(!group.allDefeated(log))
    }

    @Test
    fun `a batch is allDefeated only once every member is defeated`() {
        val log = listOf(
            entry(batchId = 1L, defeated = true),
            entry(batchId = 1L, defeated = true),
        )
        val group = groupDrawLog(log).single()

        assertTrue(group.allDefeated(log))
    }

    @Test
    fun `a single undefeated entry is its own not-allDefeated group`() {
        val log = listOf(entry(defeated = false))
        val group = groupDrawLog(log).single()

        assertTrue(!group.allDefeated(log))
    }
}
