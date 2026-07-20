package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReputationTrackSpaceTest {

    @Test
    fun `fromPosition finds the space at every position on the track, both X spaces included`() {
        val expected = listOf(-6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6)

        assertEquals(expected, expected.map { ReputationTrackSpace.fromPosition(it).position })
    }

    @Test
    fun `only the two end spaces are X spaces, and they have no modifier`() {
        assertTrue(ReputationTrackSpace.NEGATIVE_X.isXSpace)
        assertTrue(ReputationTrackSpace.POSITIVE_X.isXSpace)
        assertEquals(null, ReputationTrackSpace.NEGATIVE_X.modifier)
        assertEquals(null, ReputationTrackSpace.POSITIVE_X.modifier)

        assertFalse(ReputationTrackSpace.CENTER.isXSpace)
        (ReputationTrackSpace.entries - ReputationTrackSpace.NEGATIVE_X - ReputationTrackSpace.POSITIVE_X).forEach { space ->
            assertFalse(space.isXSpace, "$space should not be an X space")
        }
    }

    @Test
    fun `the modifier repeats once on each side, per the printed track`() {
        assertEquals(-1, ReputationTrackSpace.MINUS_1.modifier)
        assertEquals(-1, ReputationTrackSpace.MINUS_2.modifier)
        assertEquals(1, ReputationTrackSpace.PLUS_1.modifier)
        assertEquals(1, ReputationTrackSpace.PLUS_2.modifier)
    }
}
