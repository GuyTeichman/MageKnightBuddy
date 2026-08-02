package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [EnemyPickerSession]'s draw / replenish / defeat / reset logic. All use a tiny hand-built
 * fixture catalogue and an identity `shuffle` (`{ it }`) so pile order is fully deterministic and
 * every expected value below is reasoned out by hand from the fixture, not read off the code.
 *
 * The pile lifecycle is the pile-correct one of issue #251: drawing removes a token from the draw
 * pile and holds it **on the board** (out of both piles); only when it is **defeated** does it enter
 * the discard, and only the discard is reshuffled on a Replenish. So an undefeated drawn token can
 * never be re-drawn.
 */
class EnemyPickerSessionTest {

    // Identity "shuffle": leaves order untouched, so draws are deterministic (top = first element).
    private val noShuffle: (List<String>) -> List<String> = { it }

    // Fixture green pile: orc_a x2, orc_b x1, orc_c x1 -> 4 tokens, in this catalogue order.
    private val greenA = EnemyToken(id = "orc_a", name = "Orc A", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 2, armor = 3, fame = 2, attacks = listOf(EnemyAttack(4)))
    private val greenB = EnemyToken(id = "orc_b", name = "Orc B", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 1, armor = 4, fame = 3, attacks = listOf(EnemyAttack(3)))
    private val greenC = EnemyToken(id = "orc_c", name = "Orc C", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 1, armor = 5, fame = 4, attacks = listOf(EnemyAttack(2)))
    private val brownX = EnemyToken(id = "brown_x", name = "Brown X", pile = TokenPileId.BROWN, expansion = Expansion.BASE, copies = 1, armor = 6, fame = 5, attacks = listOf(EnemyAttack(5)))
    private val legionToken = EnemyToken(id = "green_ll", name = "Legion Green", pile = TokenPileId.GREEN, expansion = Expansion.LOST_LEGION, copies = 1, armor = 3, fame = 2, attacks = listOf(EnemyAttack(4)))

    private val catalogue = listOf(greenA, greenB, greenC, brownX, legionToken)

    // Ruin fixtures: two BASE ruins (an altar and an Enemies-With-Treasure) plus one Lost Legion
    // ruin, to exercise the RUIN pile being built from its own catalogue and gated by expansion.
    private val ruinAltar = RuinToken(id = "ruin_altar_red", expansion = Expansion.BASE, altarColors = listOf(ManaColor.RED))
    private val ruinEwt = RuinToken(id = "ruin_red_brown", expansion = Expansion.BASE, enemyPiles = listOf(TokenPileId.RED, TokenPileId.BROWN), reward = "2 Artifacts")
    private val ruinLegion = RuinToken(id = "ruin_ll_altar", expansion = Expansion.LOST_LEGION, altarColors = listOf(ManaColor.RED, ManaColor.BLUE, ManaColor.GREEN, ManaColor.WHITE))
    private val ruinCatalogue = listOf(ruinAltar, ruinEwt, ruinLegion)

    // Faction reward fixtures (issue #252): two Elementalist rewards (Shades of Tezla) plus one from
    // each Apocalypse Dragon faction, to exercise reward piles being built from their own catalogue,
    // gated by expansion, and (for AD) two piles sharing a single expansion toggle.
    private val rewardElemA = FactionRewardToken(id = "reward_elem_a", expansion = Expansion.SHADES_OF_TEZLA_ELEMENTALIST, pile = TokenPileId.ELEMENTALIST_REWARDS, name = "Elem A", effectText = "Heal 1.", copies = 2)
    private val rewardElemB = FactionRewardToken(id = "reward_elem_b", expansion = Expansion.SHADES_OF_TEZLA_ELEMENTALIST, pile = TokenPileId.ELEMENTALIST_REWARDS, name = "Elem B", effectText = "Move 2.", copies = 1)
    private val rewardCult = FactionRewardToken(id = "reward_cult_a", expansion = Expansion.APOCALYPSE_DRAGON, pile = TokenPileId.APOCALYPSE_CULT_REWARDS, name = "Cult A", effectText = "Attack 2.", copies = 1)
    private val rewardVoid = FactionRewardToken(id = "reward_void_a", expansion = Expansion.APOCALYPSE_DRAGON, pile = TokenPileId.COUNCIL_OF_VOID_REWARDS, name = "Void A", effectText = "Block 2.", copies = 1)
    private val factionRewardCatalogue = listOf(rewardElemA, rewardElemB, rewardCult, rewardVoid)

    // Expected green draw pile under identity shuffle: copies expanded in catalogue order.
    private val greenOrder = listOf("orc_a", "orc_a", "orc_b", "orc_c")

    @Test
    fun `start expands copies into the draw pile in catalogue order with an empty discard`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val green = session.piles.getValue(TokenPileId.GREEN)
        assertEquals(greenOrder, green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        assertEquals(emptyList(), session.drawLog)
        assertFalse(session.drawWithReplacement)
    }

    @Test
    fun `start includes only tokens whose expansion is in the token set`() {
        // Base-only: the Lost Legion green token is excluded, so green has 4 (not 5) tokens.
        val baseOnly = EnemyPickerSession.start(catalogue, tokenSet = setOf(Expansion.BASE), shuffle = noShuffle)
        assertEquals(greenOrder, baseOnly.piles.getValue(TokenPileId.GREEN).drawPile)

        // With Lost Legion added, its token joins the green pile (appended, catalogue order).
        val withLegion = EnemyPickerSession.start(catalogue, tokenSet = setOf(Expansion.BASE, Expansion.LOST_LEGION), shuffle = noShuffle)
        assertEquals(greenOrder + "green_ll", withLegion.piles.getValue(TokenPileId.GREEN).drawPile)
    }

    @Test
    fun `start builds a RUIN pile of one copy per ruin, gated by the token set`() {
        // Base-only: the two BASE ruins appear (catalogue order under identity shuffle), one copy
        // each; the Lost Legion ruin is excluded.
        val baseOnly = EnemyPickerSession.start(
            catalogue, tokenSet = setOf(Expansion.BASE), shuffle = noShuffle, ruinCatalogue = ruinCatalogue,
        )
        val ruinPile = baseOnly.piles.getValue(TokenPileId.RUIN)
        assertEquals(listOf("ruin_altar_red", "ruin_red_brown"), ruinPile.drawPile)
        assertEquals(emptyList(), ruinPile.discardPile)

        // With Lost Legion enabled, its ruin joins the pile (appended, catalogue order).
        val withLegion = EnemyPickerSession.start(
            catalogue, tokenSet = setOf(Expansion.BASE, Expansion.LOST_LEGION), shuffle = noShuffle, ruinCatalogue = ruinCatalogue,
        )
        assertEquals(
            listOf("ruin_altar_red", "ruin_red_brown", "ruin_ll_altar"),
            withLegion.piles.getValue(TokenPileId.RUIN).drawPile,
        )
    }

    @Test
    fun `start without a ruin catalogue builds no RUIN pile`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        assertFalse(session.piles.containsKey(TokenPileId.RUIN))
    }

    @Test
    fun `start builds a faction's reward pile only when that faction's expansion is in the token set`() {
        // Base-only: no reward pile exists at all.
        val baseOnly = EnemyPickerSession.start(
            catalogue, tokenSet = setOf(Expansion.BASE), shuffle = noShuffle, factionRewardCatalogue = factionRewardCatalogue,
        )
        assertFalse(baseOnly.piles.containsKey(TokenPileId.ELEMENTALIST_REWARDS))
        assertFalse(baseOnly.piles.containsKey(TokenPileId.APOCALYPSE_CULT_REWARDS))

        // Elementalist ticked: its reward pile appears with copies expanded in catalogue order
        // (reward_elem_a x2, reward_elem_b x1); the AD piles stay absent.
        val elem = EnemyPickerSession.start(
            catalogue,
            tokenSet = setOf(Expansion.BASE, Expansion.SHADES_OF_TEZLA_ELEMENTALIST),
            shuffle = noShuffle,
            factionRewardCatalogue = factionRewardCatalogue,
        )
        assertEquals(
            listOf("reward_elem_a", "reward_elem_a", "reward_elem_b"),
            elem.piles.getValue(TokenPileId.ELEMENTALIST_REWARDS).drawPile,
        )
        assertEquals(emptyList(), elem.piles.getValue(TokenPileId.ELEMENTALIST_REWARDS).discardPile)
        assertFalse(elem.piles.containsKey(TokenPileId.APOCALYPSE_CULT_REWARDS))
        assertFalse(elem.piles.containsKey(TokenPileId.COUNCIL_OF_VOID_REWARDS))
    }

    @Test
    fun `the single Apocalypse Dragon toggle builds both AD faction reward piles`() {
        val ad = EnemyPickerSession.start(
            catalogue,
            tokenSet = setOf(Expansion.BASE, Expansion.APOCALYPSE_DRAGON),
            shuffle = noShuffle,
            factionRewardCatalogue = factionRewardCatalogue,
        )
        // One expansion toggle, two distinct piles (Apocalypse Cult + Council of the Void).
        assertEquals(listOf("reward_cult_a"), ad.piles.getValue(TokenPileId.APOCALYPSE_CULT_REWARDS).drawPile)
        assertEquals(listOf("reward_void_a"), ad.piles.getValue(TokenPileId.COUNCIL_OF_VOID_REWARDS).drawPile)
        // The Shades of Tezla reward pile is not pulled in by the AD toggle.
        assertFalse(ad.piles.containsKey(TokenPileId.ELEMENTALIST_REWARDS))
    }

    @Test
    fun `start without a faction reward catalogue builds no reward piles`() {
        val session = EnemyPickerSession.start(
            catalogue, tokenSet = setOf(Expansion.SHADES_OF_TEZLA_ELEMENTALIST), shuffle = noShuffle,
        )
        assertFalse(session.piles.containsKey(TokenPileId.ELEMENTALIST_REWARDS))
    }

    @Test
    fun `a drawn faction reward token is held on the board until spent (issue 251)`() {
        // A reward token's lifecycle is draw -> hold -> spend: drawing holds it on the board (out of
        // both piles) exactly like an enemy, and "spend" is the same action as "defeat" - only then
        // does it enter the discard.
        val session = EnemyPickerSession.start(
            catalogue,
            tokenSet = setOf(Expansion.SHADES_OF_TEZLA_ELEMENTALIST),
            shuffle = noShuffle,
            factionRewardCatalogue = factionRewardCatalogue,
        )

        val drawn = session.draw(mapOf(TokenPileId.ELEMENTALIST_REWARDS to 1), batchId = 3L, shuffle = noShuffle)

        val pile = drawn.piles.getValue(TokenPileId.ELEMENTALIST_REWARDS)
        // Top ("reward_elem_a") left the draw pile but is NOT in the discard - it's held on the board.
        assertEquals(listOf("reward_elem_a", "reward_elem_b"), pile.drawPile)
        assertEquals(emptyList(), pile.discardPile)
        assertEquals(
            DrawLogEntry(tokenId = "reward_elem_a", pile = TokenPileId.ELEMENTALIST_REWARDS, batchId = 3L),
            drawn.drawLog.single(),
        )

        // Spend it (setDefeated true): now it moves into the discard, where a Replenish can reshuffle it.
        val spent = drawn.setDefeated(index = 0, defeated = true)
        assertEquals(listOf("reward_elem_a"), spent.piles.getValue(TokenPileId.ELEMENTALIST_REWARDS).discardPile)
        assertTrue(spent.drawLog.single().defeated)
    }

    @Test
    fun `drawing without replacement removes the top token from the draw pile and holds it on the board`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val after = session.draw(mapOf(TokenPileId.GREEN to 1), batchId = 7L, shuffle = noShuffle)

        val green = after.piles.getValue(TokenPileId.GREEN)
        // Top ("orc_a") is gone from the draw pile, but the discard stays empty: it's on the board,
        // not discarded (issue #251). The remaining draw pile is greenOrder minus its first element.
        assertEquals(listOf("orc_a", "orc_b", "orc_c"), green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        // Exactly one log entry, describing that draw, not defeated, not ephemeral.
        assertEquals(1, after.drawLog.size)
        assertEquals(DrawLogEntry(tokenId = "orc_a", pile = TokenPileId.GREEN, batchId = 7L), after.drawLog.single())
        assertFalse(after.drawLog.single().ephemeral)
    }

    @Test
    fun `a batch draw of several from one pile shares one batch id and holds them all on the board`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val after = session.draw(mapOf(TokenPileId.GREEN to 2), batchId = 99L, shuffle = noShuffle)

        // First two of greenOrder are both "orc_a".
        assertEquals(listOf("orc_a", "orc_a"), after.drawLog.map { it.tokenId })
        assertEquals(listOf(99L, 99L), after.drawLog.map { it.batchId })
        val green = after.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_b", "orc_c"), green.drawPile)
        // Both drawn tokens are on the board - the discard stays empty (nothing defeated yet).
        assertEquals(emptyList(), green.discardPile)
    }

    @Test
    fun `draw only touches the piles requested`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        val brownBefore = session.piles.getValue(TokenPileId.BROWN)

        val after = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)

        assertEquals(brownBefore, after.piles.getValue(TokenPileId.BROWN))
    }

    @Test
    fun `a pile drawn empty with nothing defeated is genuinely empty and cannot be drawn`() {
        // Build the state via the class's own draw() calls (per CLAUDE.md), not by hand.
        var session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        // Draw all 4 green tokens without defeating any: each is held on the board, so nothing lands
        // in the discard. Unlike the old discard-on-draw model, there is no discard to reshuffle -
        // the pile is genuinely empty (issue #251's new edge case).
        repeat(4) { session = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }

        val green = session.piles.getValue(TokenPileId.GREEN)
        assertEquals(emptyList(), green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        // All 4 draws are still logged, in draw order, all on the board.
        assertEquals(listOf("orc_a", "orc_a", "orc_b", "orc_c"), session.drawLog.map { it.tokenId })
        assertTrue(session.drawLog.none { it.defeated })

        // A further draw now has no token to take (nothing in the draw pile, nothing to reshuffle).
        val stuck = session
        assertFailsWith<IllegalArgumentException> { stuck.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }
    }

    @Test
    fun `replenish reshuffles only the discard - an on-board token is never reshuffled back`() {
        // Draw all 4 green tokens (all on the board), then defeat just two of the copies of orc_a and
        // orc_b, leaving orc_c on the board. Build the state through the class's own methods.
        var session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        repeat(4) { session = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }
        // Log order is [orc_a(0), orc_a(1), orc_b(2), orc_c(3)]; defeat indices 0 and 2.
        session = session.setDefeated(index = 0, defeated = true) // orc_a -> discard
        session = session.setDefeated(index = 2, defeated = true) // orc_b -> discard

        val beforeDraw = session.piles.getValue(TokenPileId.GREEN)
        assertEquals(emptyList(), beforeDraw.drawPile)
        assertEquals(listOf("orc_a", "orc_b"), beforeDraw.discardPile)

        // Now draw: the empty draw pile replenishes from the discard [orc_a, orc_b] (identity shuffle),
        // then its top (orc_a) is drawn onto the board. orc_c was on the board, so it is NOT among the
        // reshuffled tokens and can never reappear.
        val after = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        val green = after.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_b"), green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        assertEquals("orc_a", after.drawLog.last().tokenId)
        // The load-bearing correctness property of #251: the undefeated orc_c was never re-drawable.
        assertTrue(green.drawPile.none { it == "orc_c" })
    }

    @Test
    fun `drawing with replacement never depletes the pile and keeps the discard empty`() {
        val session = EnemyPickerSession.start(catalogue, drawWithReplacement = true, shuffle = noShuffle)

        var after = session
        repeat(10) { after = after.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }

        val green = after.piles.getValue(TokenPileId.GREEN)
        // Pile is exactly as built: nothing removed, nothing discarded, even after 10 draws.
        assertEquals(greenOrder, green.drawPile)
        assertEquals(emptyList(), green.discardPile)
        assertEquals(10, after.drawLog.size)
        // Under identity shuffle every with-replacement draw picks the top, "orc_a".
        assertTrue(after.drawLog.all { it.tokenId == "orc_a" })
    }

    @Test
    fun `a freshly drawn enemy is on the board, not defeated`() {
        val drawn = EnemyPickerSession.start(catalogue, shuffle = noShuffle).draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        // Default lifecycle: revealed onto the board, awaiting a Defeat tap (D2), and not ephemeral.
        assertFalse(drawn.drawLog.single().defeated)
        assertFalse(drawn.drawLog.single().ephemeral)
    }

    @Test
    fun `defeating an on-board token moves it into the discard, and un-defeating removes it`() {
        val drawn = EnemyPickerSession.start(catalogue, shuffle = noShuffle).draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        val drawPileBefore = drawn.piles.getValue(TokenPileId.GREEN).drawPile

        val defeated = drawn.setDefeated(index = 0, defeated = true, note = "keep, NE tile")

        val entry = defeated.drawLog[0]
        assertTrue(entry.defeated)
        assertEquals("keep, NE tile", entry.note)
        val greenAfterDefeat = defeated.piles.getValue(TokenPileId.GREEN)
        // The token moved on-board -> discard (issue #251); the draw pile is untouched.
        assertEquals(listOf("orc_a"), greenAfterDefeat.discardPile)
        assertEquals(drawPileBefore, greenAfterDefeat.drawPile)

        // Un-defeating pulls it back out of the discard (on-board again).
        val restored = defeated.setDefeated(index = 0, defeated = false)
        assertFalse(restored.drawLog[0].defeated)
        assertEquals(emptyList(), restored.piles.getValue(TokenPileId.GREEN).discardPile)
    }

    @Test
    fun `setDefeated is a pure memory flag under Draw with Replacement, never touching a pile`() {
        // With replacement no discard ever accumulates, so "defeat" is a memory aid only - it must
        // not push the token into a discard the pile model doesn't use.
        val drawn = EnemyPickerSession.start(catalogue, drawWithReplacement = true, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        val pilesBefore = drawn.piles

        val defeated = drawn.setDefeated(index = 0, defeated = true)

        assertTrue(defeated.drawLog[0].defeated)
        assertEquals(pilesBefore, defeated.piles)
    }

    @Test
    fun `onBoardCount counts undefeated non-ephemeral drawn tokens per pile`() {
        var session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        // Green: draw 2 (both on the board), brown: draw 1 (on the board).
        session = session.draw(mapOf(TokenPileId.GREEN to 2), shuffle = noShuffle) // indices 0,1
        session = session.draw(mapOf(TokenPileId.BROWN to 1), shuffle = noShuffle) // index 2
        // Defeat one green copy - it leaves the board.
        session = session.setDefeated(index = 0, defeated = true)
        // Summon an ephemeral green child off the brown entry - discarded on draw, never on the board.
        session = session.summon(parentIndex = 2, pileIds = listOf(TokenPileId.GREEN), shuffle = noShuffle) // index 3, ephemeral

        assertEquals(1, session.onBoardCount(TokenPileId.GREEN)) // index 1 only (0 defeated, 3 ephemeral)
        assertEquals(1, session.onBoardCount(TokenPileId.BROWN)) // index 2
        assertEquals(0, session.onBoardCount(TokenPileId.RED))
    }

    @Test
    fun `reset rebuilds every pile and clears the log while keeping config`() {
        var session = EnemyPickerSession.start(catalogue, tokenSet = setOf(Expansion.BASE), drawWithReplacement = false, shuffle = noShuffle)
        repeat(3) { session = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }

        val reset = session.reset(catalogue, shuffle = noShuffle)

        assertEquals(greenOrder, reset.piles.getValue(TokenPileId.GREEN).drawPile)
        assertEquals(emptyList(), reset.piles.getValue(TokenPileId.GREEN).discardPile)
        assertEquals(emptyList(), reset.drawLog)
        // Config carried through unchanged.
        assertEquals(setOf(Expansion.BASE), reset.tokenSet)
        assertFalse(reset.drawWithReplacement)
    }

    @Test
    fun `draw rejects an empty pile map`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> { session.draw(emptyMap()) }
    }

    @Test
    fun `draw rejects a non-positive count for any requested pile`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> { session.draw(mapOf(TokenPileId.GREEN to 0)) }
        assertFailsWith<IllegalArgumentException> {
            session.draw(mapOf(TokenPileId.GREEN to 1, TokenPileId.BROWN to -1))
        }
    }

    @Test
    fun `a multi-pile draw shares one batch id across every pile's entries`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val after = session.draw(mapOf(TokenPileId.GREEN to 2, TokenPileId.BROWN to 1), batchId = 42L, shuffle = noShuffle)

        assertEquals(3, after.drawLog.size)
        assertTrue(after.drawLog.all { it.batchId == 42L })
    }

    @Test
    fun `a multi-pile draw orders log entries by TokenPileId enum order, not map insertion order`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        // BROWN listed first in the map, but GREEN precedes BROWN in TokenPileId.entries.
        val after = session.draw(mapOf(TokenPileId.BROWN to 1, TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle)

        assertEquals(listOf(TokenPileId.GREEN, TokenPileId.BROWN), after.drawLog.map { it.pile })
        assertEquals(listOf("orc_a", "brown_x"), after.drawLog.map { it.tokenId })
    }

    @Test
    fun `a multi-pile draw replenishes an individual pile independently within the same batch`() {
        // Draw 3 green (on the board), then defeat all 3 so the discard has something to replenish
        // from; the 4th token (orc_c) stays in the draw pile.
        var session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        repeat(3) { session = session.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) }
        repeat(3) { i -> session = session.setDefeated(index = i, defeated = true) }
        assertEquals(listOf("orc_c"), session.piles.getValue(TokenPileId.GREEN).drawPile)
        assertEquals(listOf("orc_a", "orc_a", "orc_b"), session.piles.getValue(TokenPileId.GREEN).discardPile)

        // One multi-pile batch: green needs 2 (only 1 in the draw pile, so it must replenish from the
        // discard mid-batch), brown needs 1.
        val after = session.draw(mapOf(TokenPileId.GREEN to 2, TokenPileId.BROWN to 1), batchId = 5L, shuffle = noShuffle)

        // Green: draws its last card ("orc_c") onto the board, empties, replenishes (discard
        // "orc_a","orc_a","orc_b" identity-shuffled back to that order), then draws its new top
        // ("orc_a"). Only this batch's entries are checked - the log already carries the 3 prior draws.
        val thisBatch = after.drawLog.takeLast(3)
        val greenEntries = thisBatch.filter { it.pile == TokenPileId.GREEN }
        assertEquals(listOf("orc_c", "orc_a"), greenEntries.map { it.tokenId })
        val green = after.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_a", "orc_b"), green.drawPile)
        assertEquals(emptyList(), green.discardPile)

        // Brown drew its only token onto the board, independent of green's replenish.
        val brownEntries = thisBatch.filter { it.pile == TokenPileId.BROWN }
        assertEquals(listOf("brown_x"), brownEntries.map { it.tokenId })

        assertEquals(6, after.drawLog.size)
        assertTrue(thisBatch.all { it.batchId == 5L })
    }

    @Test
    fun `summon draws an ephemeral child - discarded on draw - tagged with the parent's log index`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle) // parent at index 0

        val after = session.summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 50L, shuffle = noShuffle)

        assertEquals(2, after.drawLog.size)
        // A true Summon child is ephemeral: discarded on draw, never independently defeated.
        assertEquals(
            DrawLogEntry(tokenId = "brown_x", pile = TokenPileId.BROWN, batchId = 50L, parentIndex = 0, ephemeral = true),
            after.drawLog[1],
        )
        val brown = after.piles.getValue(TokenPileId.BROWN)
        // BROWN holds a single copy; the ephemeral child discards on draw, emptying the draw pile,
        // which eager-Replenishes on the spot (issue #231) - the token is reshuffled back, discard empty.
        assertEquals(listOf("brown_x"), brown.drawPile)
        assertEquals(emptyList(), brown.discardPile)
    }

    @Test
    fun `a non-ephemeral summon child (a ruin enemy) stays on the board and is independently defeatable`() {
        // Ruin Enemies-With-Treasure enemies reuse the summon machinery but are real, defeatable
        // enemies (ephemeral = false): drawing holds them on the board like any normal draw.
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle) // parent at index 0

        val after = session.summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), ephemeral = false, batchId = 60L, shuffle = noShuffle)

        val child = after.drawLog[1]
        assertEquals(
            DrawLogEntry(tokenId = "brown_x", pile = TokenPileId.BROWN, batchId = 60L, parentIndex = 0, ephemeral = false),
            child,
        )
        val brown = after.piles.getValue(TokenPileId.BROWN)
        // On the board: BROWN's only copy left the draw pile but is NOT discarded, so with an empty
        // discard the pile is genuinely empty (no eager replenish).
        assertEquals(emptyList(), brown.drawPile)
        assertEquals(emptyList(), brown.discardPile)

        // Defeating the ruin enemy moves it into the discard, exactly like a normal enemy.
        val defeated = after.setDefeated(index = 1, defeated = true)
        assertEquals(listOf("brown_x"), defeated.piles.getValue(TokenPileId.BROWN).discardPile)
    }

    @Test
    fun `a restored pile with an already-empty draw pile still replenishes on the next draw`() {
        // Simulates a pile whose draw pile is empty but whose discard still holds defeated tokens
        // (e.g. after defeating tokens while the draw pile was empty). restore() must cope: the draw
        // replenishes from the discard on entry.
        val legacy = EnemyPickerSession.restore(
            tokenSet = setOf(Expansion.BASE),
            drawWithReplacement = false,
            piles = mapOf(TokenPileId.GREEN to TokenPile(drawPile = emptyList(), discardPile = greenOrder)),
            drawLog = emptyList(),
        )

        val after = legacy.draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)

        // Discard (identity-shuffled to greenOrder) replenishes the draw pile, then its top ("orc_a")
        // is drawn onto the board; 3 remain in the draw pile and the discard is empty (nothing
        // defeated, so nothing discarded).
        val green = after.piles.getValue(TokenPileId.GREEN)
        assertEquals(listOf("orc_a", "orc_b", "orc_c"), green.drawPile)
        assertEquals(emptyList(), green.discardPile)
    }

    @Test
    fun `summon rejects an out-of-range parent index`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> {
            session.summon(parentIndex = 1, pileIds = listOf(TokenPileId.BROWN))
        }
    }

    @Test
    fun `summon rejects an empty pile list`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)
        assertFailsWith<IllegalArgumentException> {
            session.summon(parentIndex = 0, pileIds = emptyList())
        }
    }

    @Test
    fun `re-summoning the same parent appends a new set of children rather than replacing the old one`() {
        // Two-slot summoner: drains BROWN's only copy first, so the second summon must replenish it.
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle) // parent at index 0

        val firstSummon = session.summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 10L, shuffle = noShuffle)
        val resummoned = firstSummon.summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 20L, shuffle = noShuffle)

        // Both summons are still in the log (append-only) - nothing overwritten.
        assertEquals(3, resummoned.drawLog.size)
        assertEquals(listOf(10L, 20L), resummoned.drawLog.drop(1).map { it.batchId })
        assertTrue(resummoned.drawLog.drop(1).all { it.parentIndex == 0 })
    }

    @Test
    fun `currentChildrenOf returns only the most recent summon batch for a parent`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle) // parent at index 0
            .summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 10L, shuffle = noShuffle) // stale child at index 1
            .summon(parentIndex = 0, pileIds = listOf(TokenPileId.BROWN), batchId = 20L, shuffle = noShuffle) // current child at index 2

        assertEquals(listOf(2), session.currentChildrenOf(parentIndex = 0))
    }

    @Test
    fun `currentChildrenOf is empty for an entry that was never summoned from`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle)

        assertEquals(emptyList(), session.currentChildrenOf(parentIndex = 0))
    }

    @Test
    fun `a two-slot summon shares one batch id across both children`() {
        // BROWN and GREEN both used as summon piles here purely as two distinct fixture piles.
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), shuffle = noShuffle) // parent at index 0, leaves 3 in GREEN

        val after = session.summon(
            parentIndex = 0,
            pileIds = listOf(TokenPileId.BROWN, TokenPileId.GREEN),
            batchId = 77L,
            shuffle = noShuffle,
        )

        val children = after.drawLog.drop(1)
        assertEquals(2, children.size)
        assertTrue(children.all { it.batchId == 77L && it.parentIndex == 0 })
        assertEquals(listOf("brown_x", "orc_a"), children.map { it.tokenId })
    }
}
