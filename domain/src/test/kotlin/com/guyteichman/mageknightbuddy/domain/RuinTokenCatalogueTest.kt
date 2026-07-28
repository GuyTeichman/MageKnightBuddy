package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The mandatory catalogue-validation test ADR-0007 requires, for the [RuinToken] catalogue: it
 * loads and checks the entire shipped `ruin-tokens.json` on every `make test`, and spot-checks a
 * few tokens against values reasoned out by hand from docs/rules/enemy-tokens.md's Ruin tokens
 * section (not read off the JSON).
 */
class RuinTokenCatalogueTest {

    // The base game ships 12 Ruin tokens: 4 Ancient Altars + 8 Enemies With Treasure (see the rules
    // doc). The Lost Legion expansion's own additions (issue #188) will raise this when they're added.
    private val expectedTotalCount = 12

    @Test
    fun `the shipped catalogue parses and is non-empty`() {
        assertTrue(RuinTokenCatalogue.tokens.isNotEmpty(), "ruin-tokens.json parsed to an empty list")
    }

    @Test
    fun `every token id is unique`() {
        val ids = RuinTokenCatalogue.tokens.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate token id(s): ${ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}")
    }

    @Test
    fun `the base game has the expected total token count`() {
        assertEquals(expectedTotalCount, RuinTokenCatalogue.tokens.size)
    }

    @Test
    fun `every token is either an altar or an enemy draw, never both or neither`() {
        RuinTokenCatalogue.tokens.forEach { token ->
            // Checked separately from the hasAltar/hasEnemyDraw XOR below: without this, a token
            // with both altarColor and a lone firstPile (secondPile left null) would slip through,
            // since hasEnemyDraw only goes true once *both* pile fields are set.
            assertEquals(
                token.firstPile == null,
                token.secondPile == null,
                "${token.id} has only one of firstPile/secondPile set: $token",
            )
            val hasAltar = token.altarColor != null
            val hasEnemyDraw = token.firstPile != null && token.secondPile != null
            assertTrue(
                hasAltar != hasEnemyDraw,
                "${token.id} is neither purely an altar nor purely an enemy draw: $token",
            )
            assertEquals(token.isAltar, hasAltar)
        }
    }

    @Test
    fun `there are four base game altars, one per basic mana color`() {
        val altarColors = RuinTokenCatalogue.tokens.filter { it.isAltar }.map { it.altarColor }.toSet()
        assertEquals(setOf(ManaColor.GREEN, ManaColor.BLUE, ManaColor.WHITE, ManaColor.RED), altarColors)
    }

    @Test
    fun `there are eight enemies-with-treasure tokens`() {
        val enemyDraws = RuinTokenCatalogue.tokens.filterNot { it.isAltar }
        assertEquals(8, enemyDraws.size)
    }

    @Test
    fun `the green-green ruin draws two tokens from the same pile`() {
        val token = assertNotNull(RuinTokenCatalogue.byId("ruin_green_green"))
        assertEquals(TokenPileId.GREEN, token.firstPile)
        assertEquals(TokenPileId.GREEN, token.secondPile)
        assertTrue(!token.isAltar)
    }

    @Test
    fun `the red altar pays red mana`() {
        val token = assertNotNull(RuinTokenCatalogue.byId("ruin_altar_red"))
        assertEquals(ManaColor.RED, token.altarColor)
        assertEquals(null, token.firstPile)
        assertEquals(null, token.secondPile)
    }
}
