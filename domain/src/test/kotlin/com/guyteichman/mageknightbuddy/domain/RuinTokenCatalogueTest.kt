package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The mandatory catalogue-validation test ADR-0007 requires, for the [RuinToken] catalogue: it
 * loads and checks the entire shipped `ruin-tokens.json` on every `make test`, and spot-checks a
 * few tokens against values reasoned out by hand from docs/rules/enemy-tokens.md's Ruin tokens
 * section (not read off the JSON).
 */
class RuinTokenCatalogueTest {

    // The base game ships 12 Ruin tokens: 4 Ancient Altars + 8 Enemies With Treasure (see the rules
    // doc). Expansion ruins (Lost Legion, issue #201) carry a non-BASE expansion and are counted
    // separately, so this figure stays fixed as the catalogue grows.
    private val expectedBaseCount = 12

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
    fun `the base game has the expected token count`() {
        val baseTokens = RuinTokenCatalogue.tokens.filter { it.expansion == Expansion.BASE }
        assertEquals(expectedBaseCount, baseTokens.size)
    }

    @Test
    fun `the Lost Legion adds three ruins`() {
        val ll = RuinTokenCatalogue.tokens.filter { it.expansion == Expansion.LOST_LEGION }
        // 1 four-colour altar + 2 Enemies-With-Treasure tokens (rulebook p.20 token strip).
        assertEquals(3, ll.size)
        assertEquals(1, ll.count { it.isAltar })
    }

    @Test
    fun `every token is either an altar or an enemy draw, never both or neither`() {
        RuinTokenCatalogue.tokens.forEach { token ->
            val hasAltar = token.altarColors != null
            val hasEnemyDraw = token.enemyPiles != null
            // XOR: exactly one of the two shapes must be present.
            assertTrue(
                hasAltar != hasEnemyDraw,
                "${token.id} is neither purely an altar nor purely an enemy draw: $token",
            )
            assertEquals(token.isAltar, hasAltar, "${token.id}: isAltar disagrees with altarColors presence")
        }
    }

    @Test
    fun `altar mana lists follow the size convention (1 = pay 3, 4 = pay one each)`() {
        RuinTokenCatalogue.tokens.filter { it.isAltar }.forEach { token ->
            val size = token.altarColors!!.size
            // Only two altar shapes exist in all of Mage Knight: a single-colour altar (pay 3) and
            // the Lost Legion four-colour altar (pay one of each). Any other size is a data error.
            assertTrue(size == 1 || size == 4, "${token.id}: altarColors size $size not in {1, 4}")
            if (size == 4) {
                // The four-colour altar demands one of *each* basic colour, so no repeats.
                assertEquals(4, token.altarColors!!.toSet().size, "${token.id}: 4-colour altar has a repeated colour")
            }
        }
    }

    @Test
    fun `every enemy-draw lists at least one pile, and altars carry no reward`() {
        RuinTokenCatalogue.tokens.forEach { token ->
            if (token.isAltar) {
                // An altar's "reward" is derived Fame, never printed treasure text.
                assertNull(token.reward, "${token.id}: altar should not carry a reward string")
            } else {
                assertTrue(token.enemyPiles!!.isNotEmpty(), "${token.id}: enemy-draw with no piles")
                assertNotNull(token.reward, "${token.id}: Enemies-With-Treasure token missing its reward text")
            }
        }
    }

    @Test
    fun `there are four base game altars, one per basic mana color`() {
        val altarColors = RuinTokenCatalogue.tokens
            .filter { it.isAltar && it.expansion == Expansion.BASE }
            // Each base altar is single-colour, so single() pulls that lone colour out.
            .map { it.altarColors!!.single() }
            .toSet()
        assertEquals(setOf(ManaColor.GREEN, ManaColor.BLUE, ManaColor.WHITE, ManaColor.RED), altarColors)
    }

    @Test
    fun `there are eight base game enemies-with-treasure tokens`() {
        val enemyDraws = RuinTokenCatalogue.tokens.filter { !it.isAltar && it.expansion == Expansion.BASE }
        assertEquals(8, enemyDraws.size)
    }

    @Test
    fun `the green-green ruin draws two tokens from the same pile`() {
        val token = assertNotNull(RuinTokenCatalogue.byId("ruin_green_green"))
        assertEquals(listOf(TokenPileId.GREEN, TokenPileId.GREEN), token.enemyPiles)
        assertTrue(!token.isAltar)
        // Reward reasoned from the rules-doc table (Green + Green -> set of 4 crystals), not the JSON.
        assertEquals("Set of 4 crystals", token.reward)
    }

    @Test
    fun `the red altar pays red mana and draws no enemies`() {
        val token = assertNotNull(RuinTokenCatalogue.byId("ruin_altar_red"))
        assertEquals(listOf(ManaColor.RED), token.altarColors)
        assertNull(token.enemyPiles)
        assertNull(token.reward)
    }

    @Test
    fun `the brown-red ruin draws brown then red for two artifacts`() {
        val token = assertNotNull(RuinTokenCatalogue.byId("ruin_brown_red"))
        assertEquals(listOf(TokenPileId.BROWN, TokenPileId.RED), token.enemyPiles)
        // Ground-truth token strip (rulebook p.20 / TTS reference sheet): Brown + Red -> 2 Artifacts.
        assertEquals("2 Artifacts", token.reward)
    }

    @Test
    fun `the Lost Legion four-colour altar pays one of each basic colour`() {
        val token = assertNotNull(RuinTokenCatalogue.byId("ruin_ll_altar_four_color"))
        // Rulebook p.20: one crystal each of green, blue, white, red -> 10 Fame (Fame is a UI-derived
        // value, not stored). Order matches the printed crystal order on the token.
        assertEquals(listOf(ManaColor.GREEN, ManaColor.BLUE, ManaColor.WHITE, ManaColor.RED), token.altarColors)
        assertNull(token.enemyPiles)
    }

    @Test
    fun `the Lost Legion three-green ruin draws three from Green for a Unit`() {
        val token = assertNotNull(RuinTokenCatalogue.byId("ruin_ll_green_green_green"))
        // The token whose existence forced enemyPiles to be a variable-length list: three Green draws.
        assertEquals(listOf(TokenPileId.GREEN, TokenPileId.GREEN, TokenPileId.GREEN), token.enemyPiles)
        assertEquals("Unit", token.reward)
    }
}
