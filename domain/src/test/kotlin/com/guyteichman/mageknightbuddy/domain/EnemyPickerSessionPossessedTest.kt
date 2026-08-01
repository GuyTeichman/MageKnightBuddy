package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Tests for the Apocalypse Dragon **possessed** draw path on [EnemyPickerSession] (issue #189,
 * docs/rules/apocalypse-dragon.md): building the [TokenPileId.POSSESSED] pile, and pairing each
 * circular enemy with a possessed token in one batch. Uses an identity `shuffle` so every drawn id
 * is deterministic and reasoned out by hand from the fixture below.
 */
class EnemyPickerSessionPossessedTest {

    private val noShuffle: (List<String>) -> List<String> = { it }

    // Circular enemy fixtures (base game): green has 2 copies of one type; brown has 1.
    private val green = EnemyToken(id = "orc_a", name = "Orc A", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 2, armor = 3, fame = 2, attacks = listOf(EnemyAttack(4)))
    private val brown = EnemyToken(id = "brown_x", name = "Brown X", pile = TokenPileId.BROWN, expansion = Expansion.BASE, copies = 1, armor = 6, fame = 5, attacks = listOf(EnemyAttack(5)))
    private val catalogue = listOf(green, brown)

    // Two possessed tokens (1 copy each) -> POSSESSED draw pile [poss_1, poss_2] under identity shuffle.
    private val poss1 = PossessedToken(id = "poss_1", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1, armorDelta = 2, psychicAttack = 3)
    private val poss2 = PossessedToken(id = "poss_2", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1, attackDelta = 1)
    private val possessedCatalogue = listOf(poss1, poss2)

    private val adSet = setOf(Expansion.BASE, Expansion.APOCALYPSE_DRAGON)

    private fun startAd() = EnemyPickerSession.start(
        catalogue, tokenSet = adSet, shuffle = noShuffle, possessedCatalogue = possessedCatalogue,
    )

    @Test
    fun `start builds a POSSESSED pile only when Apocalypse Dragon is in the token set`() {
        val ad = startAd()
        assertEquals(listOf("poss_1", "poss_2"), ad.piles.getValue(TokenPileId.POSSESSED).drawPile)

        // Base-only session: the possessed catalogue is supplied but AD isn't in the set, so no pile.
        val baseOnly = EnemyPickerSession.start(
            catalogue, tokenSet = setOf(Expansion.BASE), shuffle = noShuffle, possessedCatalogue = possessedCatalogue,
        )
        assertFalse(baseOnly.piles.containsKey(TokenPileId.POSSESSED))
    }

    @Test
    fun `start expands possessed copies into the POSSESSED pile`() {
        val multi = PossessedToken(id = "poss_multi", expansion = Expansion.APOCALYPSE_DRAGON, copies = 3, fameDelta = 1)
        val session = EnemyPickerSession.start(catalogue, tokenSet = adSet, shuffle = noShuffle, possessedCatalogue = listOf(multi))
        assertEquals(listOf("poss_multi", "poss_multi", "poss_multi"), session.piles.getValue(TokenPileId.POSSESSED).drawPile)
    }

    @Test
    fun `a possessed single draw pairs the circular with the top possessed token`() {
        val after = startAd().draw(mapOf(TokenPileId.GREEN to 1), batchId = 7L, possessed = true, shuffle = noShuffle)

        assertEquals(1, after.drawLog.size)
        val entry = after.drawLog.single()
        assertEquals("orc_a", entry.tokenId) // circular is the enemy...
        assertEquals(TokenPileId.GREEN, entry.pile) // ...and the entry's pile stays the colour pile
        assertEquals("poss_1", entry.possessedTokenId) // top of the possessed pile
        assertEquals(7L, entry.batchId)

        // The circular enemy is held on the board (issue #251): it leaves the GREEN draw pile but
        // isn't discarded until it's defeated, so the GREEN discard is still empty.
        assertEquals(listOf("orc_a"), after.piles.getValue(TokenPileId.GREEN).drawPile)
        assertEquals(emptyList(), after.piles.getValue(TokenPileId.GREEN).discardPile)
        // The possessed companion has no on-board tracking, so it cycles straight to the POSSESSED
        // discard (draw -> discard -> replenish), leaving poss_2 on top of the draw pile.
        assertEquals(listOf("poss_2"), after.piles.getValue(TokenPileId.POSSESSED).drawPile)
        assertEquals(listOf("poss_1"), after.piles.getValue(TokenPileId.POSSESSED).discardPile)
    }

    @Test
    fun `a possessed group draw across piles pairs each enemy with its own possessed token in one batch`() {
        // GREEN x2 + BROWN x1, all possessed. Drawn in TokenPileId.entries order: GREEN, GREEN, BROWN.
        // Possessed pile [poss_1, poss_2] serves poss_1, poss_2, then replenishes and serves poss_1.
        val after = startAd().draw(mapOf(TokenPileId.GREEN to 2, TokenPileId.BROWN to 1), batchId = 5L, possessed = true, shuffle = noShuffle)

        assertEquals(3, after.drawLog.size)
        assertEquals(listOf("orc_a", "orc_a", "brown_x"), after.drawLog.map { it.tokenId })
        assertEquals(listOf(TokenPileId.GREEN, TokenPileId.GREEN, TokenPileId.BROWN), after.drawLog.map { it.pile })
        assertEquals(listOf("poss_1", "poss_2", "poss_1"), after.drawLog.map { it.possessedTokenId })
        // One batch: every entry shares the batchId, so the UI groups them as one grouped draw.
        assertEquals(setOf(5L), after.drawLog.map { it.batchId }.toSet())
    }

    @Test
    fun `a non-possessed draw leaves possessedTokenId null and never touches the possessed pile`() {
        val after = startAd().draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, possessed = false, shuffle = noShuffle)

        assertNull(after.drawLog.single().possessedTokenId)
        // The possessed pile is untouched by an ordinary draw.
        assertEquals(listOf("poss_1", "poss_2"), after.piles.getValue(TokenPileId.POSSESSED).drawPile)
        assertEquals(emptyList(), after.piles.getValue(TokenPileId.POSSESSED).discardPile)
    }

    @Test
    fun `a possessed draw may not target the RUIN or POSSESSED pile`() {
        val session = startAd()
        assertFailsWith<IllegalArgumentException> {
            session.draw(mapOf(TokenPileId.RUIN to 1), possessed = true, shuffle = noShuffle)
        }
        assertFailsWith<IllegalArgumentException> {
            session.draw(mapOf(TokenPileId.POSSESSED to 1), possessed = true, shuffle = noShuffle)
        }
    }
}
