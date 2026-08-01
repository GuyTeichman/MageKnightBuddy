package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The mandatory catalogue-validation test ADR-0007 requires, for the [PossessedToken] catalogue: it
 * loads the entire shipped `possessed-tokens.json` on every `make test` and spot-checks a few tokens
 * against deltas reasoned out by hand from docs/rules/apocalypse-dragon.md's token table (not read
 * off the JSON). The Apocalypse Dragon box holds 12 possessed tokens across 9 distinct types.
 */
class PossessedTokenCatalogueTest {

    private val expectedTypeCount = 9
    private val expectedTokenCount = 12

    @Test
    fun `the shipped catalogue parses and is non-empty`() {
        assertTrue(PossessedTokenCatalogue.tokens.isNotEmpty(), "possessed-tokens.json parsed to an empty list")
    }

    @Test
    fun `every token id is unique`() {
        val ids = PossessedTokenCatalogue.tokens.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate id(s): ${ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}")
    }

    @Test
    fun `there are nine distinct types totalling twelve tokens`() {
        assertEquals(expectedTypeCount, PossessedTokenCatalogue.tokens.size)
        assertEquals(expectedTokenCount, PossessedTokenCatalogue.tokens.sumOf { it.copies })
    }

    @Test
    fun `every token is an Apocalypse Dragon token with at least one copy`() {
        PossessedTokenCatalogue.tokens.forEach { token ->
            assertEquals(Expansion.APOCALYPSE_DRAGON, token.expansion, "${token.id}: wrong expansion")
            assertTrue(token.copies >= 1, "${token.id}: copies must be >= 1, was ${token.copies}")
        }
    }

    @Test
    fun `every printed value stays within the rulebook's ranges`() {
        PossessedTokenCatalogue.tokens.forEach { token ->
            // Rulebook p.7 icon sets: Armor −1/+1/+2, topmost Attack −1/+1, Fame −1/+1, Psychic 1-4.
            assertTrue(token.armorDelta in -1..2, "${token.id}: armorDelta ${token.armorDelta} out of range")
            assertTrue(token.attackDelta in -1..1, "${token.id}: attackDelta ${token.attackDelta} out of range")
            assertTrue(token.fameDelta in -1..1, "${token.id}: fameDelta ${token.fameDelta} out of range")
            token.psychicAttack?.let {
                assertTrue(it in 1..4, "${token.id}: psychicAttack $it out of range")
            }
        }
    }

    @Test
    fun `every token carries at least one modifier`() {
        // A blank possessed token would be a transcription slip - each of the 12 prints something.
        PossessedTokenCatalogue.tokens.forEach { token ->
            val hasModifier = token.armorDelta != 0 || token.attackDelta != 0 ||
                token.fameDelta != 0 || token.psychicAttack != null
            assertTrue(hasModifier, "${token.id}: no modifier at all")
        }
    }

    @Test
    fun `possessed_01 is Fame +1 with a Psychic Attack of 4 and no other modifier`() {
        val token = assertNotNull(PossessedTokenCatalogue.byId("possessed_01"))
        assertEquals(1, token.copies)
        assertEquals(0, token.armorDelta)
        assertEquals(0, token.attackDelta)
        assertEquals(1, token.fameDelta)
        assertEquals(4, token.psychicAttack)
    }

    @Test
    fun `possessed_04 adds +2 Armor, +1 Fame and a Psychic Attack of 2`() {
        val token = assertNotNull(PossessedTokenCatalogue.byId("possessed_04"))
        assertEquals(2, token.armorDelta)
        assertEquals(1, token.fameDelta)
        assertEquals(2, token.psychicAttack)
        assertEquals(0, token.attackDelta)
    }

    @Test
    fun `possessed_07 is the two-copy −1 Armor, −1 Fame token with no psychic attack`() {
        val token = assertNotNull(PossessedTokenCatalogue.byId("possessed_07"))
        assertEquals(2, token.copies)
        assertEquals(-1, token.armorDelta)
        assertEquals(-1, token.fameDelta)
        assertEquals(0, token.attackDelta)
        assertNull(token.psychicAttack)
    }

    @Test
    fun `possessed_09 only boosts the topmost attack by +1`() {
        val token = assertNotNull(PossessedTokenCatalogue.byId("possessed_09"))
        assertEquals(1, token.attackDelta)
        assertEquals(0, token.armorDelta)
        assertEquals(0, token.fameDelta)
        assertNull(token.psychicAttack)
    }
}
