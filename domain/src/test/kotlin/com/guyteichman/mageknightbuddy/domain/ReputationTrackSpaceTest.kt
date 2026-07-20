package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReputationTrackSpaceTest {

    @Test
    fun `the track has one entry per printed space, most-negative to most-positive`() {
        val expected = listOf(null, -5, -3, -2, -1, 0, 1, 2, 3, 5)

        assertEquals(expected, ReputationTrackSpace.entries.map { it.modifier })
    }

    @Test
    fun `only the negative end is an X space - the positive end tops out at a real modifier`() {
        assertTrue(ReputationTrackSpace.NEGATIVE_X.isXSpace)
        assertEquals(null, ReputationTrackSpace.NEGATIVE_X.modifier)

        assertFalse(ReputationTrackSpace.PLUS_5.isXSpace)
        assertEquals(5, ReputationTrackSpace.PLUS_5.modifier)

        (ReputationTrackSpace.entries - ReputationTrackSpace.NEGATIVE_X).forEach { space ->
            assertFalse(space.isXSpace, "$space should not be an X space")
        }
    }
}
