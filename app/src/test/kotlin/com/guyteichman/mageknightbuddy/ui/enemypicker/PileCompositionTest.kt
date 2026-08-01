package com.guyteichman.mageknightbuddy.ui.enemypicker

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for issue #231's pile-composition view: grouping a face-down pile's remaining tokens by
 * identity, sorted alphabetically by display name, so a player can survey what's still in a pile.
 * A fake `nameOf` resolver stands in for the real catalogues, so expected values are reasoned out
 * by hand here rather than read off any catalogue.
 */
class PileCompositionTest {

    // Display names deliberately out of `id` order, so a correct sort must key on the name, not the id.
    private val names = mapOf(
        "orc" to "Orc",
        "drac" to "Draconum",
        "guard" to "Guardian",
    )
    private val nameOf: (String) -> String = { id -> names[id] ?: id }

    @Test
    fun `groups tokens by identity, sorts alphabetically by display name, and totals them`() {
        // Two orcs and one each of the others, in a jumbled order.
        val drawPile = listOf("orc", "guard", "drac", "orc")

        val composition = composePile(drawPile, nameOf)

        // Alphabetical by display name: Draconum, Guardian, Orc - regardless of draw-pile order.
        assertEquals(
            listOf(
                PileTokenGroup(tokenId = "drac", displayName = "Draconum", count = 1),
                PileTokenGroup(tokenId = "guard", displayName = "Guardian", count = 1),
                PileTokenGroup(tokenId = "orc", displayName = "Orc", count = 2),
            ),
            composition.groups,
        )
        assertEquals(4, composition.total)
    }

    @Test
    fun `an empty pile composes to no groups and a zero total`() {
        val composition = composePile(emptyList(), nameOf)

        assertEquals(emptyList(), composition.groups)
        assertEquals(0, composition.total)
    }

    @Test
    fun `alphabetical sort is case-insensitive`() {
        // "apple" would sort after "Banana" under naive ASCII ordering (uppercase < lowercase);
        // a case-insensitive sort must put apple first.
        val caseNames: (String) -> String = { id -> mapOf("b" to "Banana", "a" to "apple")[id] ?: id }

        val composition = composePile(listOf("b", "a"), caseNames)

        assertEquals(listOf("apple", "Banana"), composition.groups.map { it.displayName })
    }
}
