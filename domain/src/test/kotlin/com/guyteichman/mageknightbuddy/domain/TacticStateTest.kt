package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TacticStateTest {

    @Test
    fun `pickPlayer sets playerPick and leaves dummyPick and removed sets unchanged`() {
        val state = TacticState()

        val afterPick = state.pickPlayer(card = 3, isDay = true)

        assertEquals(3, afterPick.playerPick)
        assertEquals(null, afterPick.dummyPick)
        assertEquals(emptySet(), afterPick.removedDayCards)
        assertEquals(emptySet(), afterPick.removedNightCards)
    }

    @Test
    fun `pickPlayer rejects a card already removed from the active pile`() {
        val state = TacticState(removedDayCards = setOf(2))

        assertFailsWith<IllegalArgumentException> { state.pickPlayer(card = 2, isDay = true) }
    }

    @Test
    fun `pickPlayer rejects a card the Dummy already took this round`() {
        val state = TacticState(dummyPick = 5)

        assertFailsWith<IllegalArgumentException> { state.pickPlayer(card = 5, isDay = true) }
    }

    @Test
    fun `pickPlayer rejects a card outside the 1-6 range`() {
        val state = TacticState()

        assertFailsWith<IllegalArgumentException> { state.pickPlayer(card = 0, isDay = true) }
        assertFailsWith<IllegalArgumentException> { state.pickPlayer(card = 7, isDay = true) }
    }

    @Test
    fun `pickPlayer does not reject a card removed from the inactive pile`() {
        val state = TacticState(removedNightCards = setOf(2))

        val afterPick = state.pickPlayer(card = 2, isDay = true)

        assertEquals(2, afterPick.playerPick)
    }

    @Test
    fun `pickDummy sets dummyPick and leaves playerPick and removed sets unchanged`() {
        val state = TacticState()

        val afterPick = state.pickDummy(isDay = true, random = Random(0))

        assertTrue(afterPick.dummyPick != null)
        assertEquals(null, afterPick.playerPick)
        assertEquals(emptySet(), afterPick.removedDayCards)
        assertEquals(emptySet(), afterPick.removedNightCards)
    }

    @Test
    fun `pickDummy never returns a card already removed or already picked this round`() {
        // Only card 4 is left: 1,2,3 removed from the Day pile, 5 already taken by the player,
        // 6 already taken by... itself would be nonsensical, so leave 6 as the only other
        // candidate and run many seeds to make sure 4 or 6 is always the result, never 1/2/3/5.
        val state = TacticState(removedDayCards = setOf(1, 2, 3), playerPick = 5)

        repeat(50) { seed ->
            val picked = state.pickDummy(isDay = true, random = Random(seed)).dummyPick
            assertTrue(picked == 4 || picked == 6, "picked $picked for seed $seed")
        }
    }

    @Test
    fun `pickDummy only draws from the active pile's removed set`() {
        val state = TacticState(removedDayCards = setOf(1, 2, 3, 4, 5))

        val afterPick = state.pickDummy(isDay = false, random = Random(0))

        // Night pile has nothing removed, so any of 1-6 is a legal draw despite the Day removals.
        assertTrue(afterPick.dummyPick in 1..6)
    }

    @Test
    fun `advanceRound with BOTH moves both picks into the active pile's removed set and clears both picks`() {
        val state = TacticState(playerPick = 2, dummyPick = 5)

        val next = state.advanceRound(remove = RemovalTarget.BOTH, isDay = true)

        assertEquals(setOf(2, 5), next.removedDayCards)
        assertEquals(emptySet(), next.removedNightCards)
        assertEquals(null, next.playerPick)
        assertEquals(null, next.dummyPick)
    }

    @Test
    fun `advanceRound with PLAYER_ONLY moves only the player's pick, discarding the Dummy's`() {
        val state = TacticState(playerPick = 2, dummyPick = 5)

        val next = state.advanceRound(remove = RemovalTarget.PLAYER_ONLY, isDay = true)

        assertEquals(setOf(2), next.removedDayCards)
        assertEquals(null, next.playerPick)
        assertEquals(null, next.dummyPick)
    }

    @Test
    fun `advanceRound with null removal clears picks without touching removed sets`() {
        val state = TacticState(removedDayCards = setOf(1), playerPick = 2, dummyPick = 5)

        val next = state.advanceRound(remove = null, isDay = true)

        assertEquals(setOf(1), next.removedDayCards)
        assertEquals(null, next.playerPick)
        assertEquals(null, next.dummyPick)
    }

    @Test
    fun `advanceRound targets the Night pile when isDay is false`() {
        val state = TacticState(playerPick = 2, dummyPick = 5)

        val next = state.advanceRound(remove = RemovalTarget.BOTH, isDay = false)

        assertEquals(emptySet(), next.removedDayCards)
        assertEquals(setOf(2, 5), next.removedNightCards)
    }

    @Test
    fun `advanceRound is safe when the Dummy's pick is still null`() {
        val state = TacticState(playerPick = 2, dummyPick = null)

        val next = state.advanceRound(remove = RemovalTarget.BOTH, isDay = true)

        assertEquals(setOf(2), next.removedDayCards)
    }
}
