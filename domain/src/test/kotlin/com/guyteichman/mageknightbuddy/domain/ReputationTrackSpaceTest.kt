package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ReputationTrackSpaceTest {

    @Test
    fun `the track has one entry per printed space, most-negative to most-positive`() {
        val expected = listOf(null, -5, -3, -2, -1, 0, 1, 2, 3, 5)

        assertEquals(expected, ReputationTrackSpace.entries.map { it.modifier })
    }
}
