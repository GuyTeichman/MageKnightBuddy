package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DayNightTest {

    @Test
    fun `round 1 is day when the session did not start at night`() {
        assertEquals(true, isDayRound(round = 1, startsAtNight = false))
    }

    @Test
    fun `round 2 is night when the session did not start at night`() {
        assertEquals(false, isDayRound(round = 2, startsAtNight = false))
    }

    @Test
    fun `round 1 is night when the session started at night`() {
        assertEquals(false, isDayRound(round = 1, startsAtNight = true))
    }

    @Test
    fun `round 2 is day when the session started at night`() {
        assertEquals(true, isDayRound(round = 2, startsAtNight = true))
    }

    @Test
    fun `odd-even alternation continues past round 2`() {
        assertEquals(true, isDayRound(round = 3, startsAtNight = false))
        assertEquals(false, isDayRound(round = 4, startsAtNight = false))
        assertEquals(false, isDayRound(round = 5, startsAtNight = true))
        assertEquals(true, isDayRound(round = 6, startsAtNight = true))
    }
}
