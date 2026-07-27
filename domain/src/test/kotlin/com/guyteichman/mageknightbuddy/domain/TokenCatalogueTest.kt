package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The mandatory catalogue-validation test ADR-0007 requires: it loads and checks the *entire*
 * shipped `enemy-tokens.json` on every `make test`, converting what would otherwise be a runtime
 * parse/typo failure on the phone into a CI failure. It also spot-checks a few tokens against
 * values reasoned out by hand from `docs/rules/enemy-tokens.md` (not read off the JSON), so a
 * wrong transcription is caught, not just a malformed file.
 */
class TokenCatalogueTest {

    // Expected physical token count per pile (copies summed). Grows as each pile is transcribed;
    // for now only the green Marauding Orcs pile exists - 6 types x 2 copies = 12 (see the rules doc).
    private val expectedPileCounts = mapOf(
        TokenPileId.GREEN to 12,
    )

    @Test
    fun `the shipped catalogue parses and is non-empty`() {
        assertTrue(TokenCatalogue.tokens.isNotEmpty(), "enemy-tokens.json parsed to an empty list")
    }

    @Test
    fun `every token id is unique`() {
        val ids = TokenCatalogue.tokens.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate token id(s): ${ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}")
    }

    @Test
    fun `every token has at least one copy and non-negative stats`() {
        TokenCatalogue.tokens.forEach { token ->
            assertTrue(token.copies >= 1, "${token.id} has copies < 1")
            assertTrue(token.armor >= 0, "${token.id} has negative armor")
            assertTrue(token.fame >= 0, "${token.id} has negative fame")
            token.attacks.forEach { assertTrue(it.value >= 0, "${token.id} has a negative attack value") }
        }
    }

    @Test
    fun `each pile's total token count matches the physical component count`() {
        expectedPileCounts.forEach { (pile, expected) ->
            val actual = TokenCatalogue.tokens.filter { it.pile == pile }.sumOf { it.copies }
            assertEquals(expected, actual, "pile $pile has $actual tokens, expected $expected")
        }
        // No token belongs to a pile we haven't declared an expected count for yet.
        val pilesPresent = TokenCatalogue.tokens.map { it.pile }.toSet()
        assertEquals(expectedPileCounts.keys, pilesPresent, "catalogue has tokens in undeclared pile(s)")
    }

    @Test
    fun `green pile has the six Marauding Orc types`() {
        val greenNames = TokenCatalogue.tokens.filter { it.pile == TokenPileId.GREEN }.map { it.name }.toSet()
        assertEquals(
            setOf("Prowlers", "Diggers", "Cursed Hags", "Wolf Riders", "Ironclads", "Orc Summoners"),
            greenNames,
        )
    }

    // The spot-checks below use values derived by hand from docs/rules/enemy-tokens.md's table.

    @Test
    fun `Ironclads is a physically-resistant brutal attacker`() {
        val ironclads = assertNotNull(TokenCatalogue.byId("green_ironclads"))
        assertEquals(3, ironclads.armor)
        assertEquals(4, ironclads.fame)
        assertEquals(setOf(AttackElement.PHYSICAL), ironclads.resistances)
        val attack = ironclads.attacks.single()
        assertEquals(4, attack.value)
        assertEquals(AttackElement.PHYSICAL, attack.element)
        assertEquals(setOf(AttackModifier.BRUTAL), attack.modifiers)
    }

    @Test
    fun `Cursed Hags has a poison attack`() {
        val hags = assertNotNull(TokenCatalogue.byId("green_cursed_hags"))
        assertEquals(5, hags.armor)
        assertEquals(3, hags.fame)
        val attack = hags.attacks.single()
        assertEquals(3, attack.value)
        assertEquals(setOf(AttackModifier.POISON), attack.modifiers)
    }

    @Test
    fun `Diggers is fortified`() {
        val diggers = assertNotNull(TokenCatalogue.byId("green_diggers"))
        assertTrue(EnemyAbility.FORTIFIED in diggers.abilities)
    }

    @Test
    fun `Orc Summoners has a Summon attack`() {
        val summoners = assertNotNull(TokenCatalogue.byId("green_orc_summoners"))
        assertTrue(summoners.attacks.single().modifiers.contains(AttackModifier.SUMMON))
    }
}
