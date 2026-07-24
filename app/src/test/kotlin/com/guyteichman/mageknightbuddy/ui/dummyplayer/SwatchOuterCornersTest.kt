package com.guyteichman.mageknightbuddy.ui.dummyplayer

import kotlin.test.Test
import kotlin.test.assertEquals

class SwatchOuterCornersTest {

    @Test
    fun `single-color card rounds all 4 corners`() {
        val corners = swatchOuterCorners(index = 0, count = 1)

        assertEquals(SwatchCorners(topStart = true, topEnd = true, bottomStart = true, bottomEnd = true), corners)
    }

    @Test
    fun `left half of a dual-color card rounds only the left corners`() {
        val corners = swatchOuterCorners(index = 0, count = 2)

        assertEquals(SwatchCorners(topStart = true, topEnd = false, bottomStart = true, bottomEnd = false), corners)
    }

    @Test
    fun `right half of a dual-color card rounds only the right corners`() {
        val corners = swatchOuterCorners(index = 1, count = 2)

        assertEquals(SwatchCorners(topStart = false, topEnd = true, bottomStart = false, bottomEnd = true), corners)
    }
}
