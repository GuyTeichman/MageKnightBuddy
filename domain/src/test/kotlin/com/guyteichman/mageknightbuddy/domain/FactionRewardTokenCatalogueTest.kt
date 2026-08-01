package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The mandatory catalogue-validation test ADR-0007 requires, for the [FactionRewardToken] catalogue:
 * it loads and checks the entire shipped `faction-reward-tokens.json` on every `make test`, and
 * spot-checks a few tokens against values reasoned out by hand from
 * docs/rules/faction-reward-tokens.md (not read off the JSON).
 */
class FactionRewardTokenCatalogueTest {

    // Which expansion gates each reward pile - the invariant the UI's Token Set toggles rely on:
    // Shades of Tezla is split per faction; both Apocalypse Dragon factions share one expansion.
    private val pileExpansion = mapOf(
        TokenPileId.ELEMENTALIST_REWARDS to Expansion.SHADES_OF_TEZLA_ELEMENTALIST,
        TokenPileId.DARK_CRUSADER_REWARDS to Expansion.SHADES_OF_TEZLA_DARK_CRUSADER,
        TokenPileId.APOCALYPSE_CULT_REWARDS to Expansion.APOCALYPSE_DRAGON,
        TokenPileId.COUNCIL_OF_VOID_REWARDS to Expansion.APOCALYPSE_DRAGON,
    )

    @Test
    fun `the shipped catalogue parses and is non-empty`() {
        assertTrue(FactionRewardTokenCatalogue.tokens.isNotEmpty(), "faction-reward-tokens.json parsed to an empty list")
    }

    @Test
    fun `every token id is unique`() {
        val ids = FactionRewardTokenCatalogue.tokens.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate id(s): ${ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}")
    }

    @Test
    fun `there are exactly the four faction reward piles`() {
        val piles = FactionRewardTokenCatalogue.tokens.map { it.pile }.toSet()
        assertEquals(pileExpansion.keys, piles)
    }

    @Test
    fun `each pile has six distinct token types, two copies each`() {
        pileExpansion.keys.forEach { pile ->
            val inPile = FactionRewardTokenCatalogue.tokens.filter { it.pile == pile }
            // Every faction reward pile in the box holds 12 tokens: 6 types x 2 copies.
            assertEquals(6, inPile.size, "$pile should have 6 token types")
            assertTrue(inPile.all { it.copies == 2 }, "$pile has a token whose copies != 2")
        }
    }

    @Test
    fun `each token's pile is gated by the expected expansion`() {
        FactionRewardTokenCatalogue.tokens.forEach { token ->
            assertEquals(
                pileExpansion.getValue(token.pile), token.expansion,
                "${token.id}: pile ${token.pile} should be gated by ${pileExpansion.getValue(token.pile)}",
            )
        }
    }

    @Test
    fun `every token has a non-blank name and effect text`() {
        FactionRewardTokenCatalogue.tokens.forEach { token ->
            assertTrue(token.name.isNotBlank(), "${token.id}: blank name")
            assertTrue(token.effectText.isNotBlank(), "${token.id}: blank effectText")
        }
    }

    @Test
    fun `the uniform discard-for-Fame line is stripped from every effect text`() {
        // That parenthetical is identical on all 24 tokens, so it lives in the rules doc + UI footer,
        // never in the per-token effectText (see FactionRewardToken's doc). Guard against it creeping
        // back into the catalogue.
        FactionRewardTokenCatalogue.tokens.forEach { token ->
            assertTrue(
                "may be discarded during interactions" !in token.effectText,
                "${token.id}: effectText should not carry the universal discard-for-Fame line",
            )
        }
    }

    @Test
    fun `Healing Herbs is an Elementalist reward that heals`() {
        // Spot-check reasoned from the rules doc, not read off the JSON: the Elementalist token
        // "Healing Herbs" grants a heal.
        val token = assertNotNull(FactionRewardTokenCatalogue.byId("reward_elementalist_healing_herbs"))
        assertEquals(TokenPileId.ELEMENTALIST_REWARDS, token.pile)
        assertEquals("Healing Herbs", token.name)
        assertTrue("Heal 1" in token.effectText, "expected a heal effect, was: ${token.effectText}")
    }

    @Test
    fun `Blade of Dominance is an Apocalypse Cult reward`() {
        val token = assertNotNull(FactionRewardTokenCatalogue.byId("reward_apocalypse_cult_blade_of_dominance"))
        assertEquals(TokenPileId.APOCALYPSE_CULT_REWARDS, token.pile)
        assertEquals(Expansion.APOCALYPSE_DRAGON, token.expansion)
    }
}
