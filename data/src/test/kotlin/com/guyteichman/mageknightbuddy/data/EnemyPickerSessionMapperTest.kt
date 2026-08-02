package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.DrawLogEntry
import com.guyteichman.mageknightbuddy.domain.EnemyAttack
import com.guyteichman.mageknightbuddy.domain.EnemyPickerSession
import com.guyteichman.mageknightbuddy.domain.EnemyToken
import com.guyteichman.mageknightbuddy.domain.Expansion
import com.guyteichman.mageknightbuddy.domain.FactionRewardToken
import com.guyteichman.mageknightbuddy.domain.TokenPile
import com.guyteichman.mageknightbuddy.domain.TokenPileId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnemyPickerSessionMapperTest {

    private val noShuffle: (List<String>) -> List<String> = { it }

    private val greenA = EnemyToken(id = "orc_a", name = "Orc A", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 2, armor = 3, fame = 2, attacks = listOf(EnemyAttack(4)))
    private val greenB = EnemyToken(id = "orc_b", name = "Orc B", pile = TokenPileId.GREEN, expansion = Expansion.BASE, copies = 1, armor = 4, fame = 3, attacks = listOf(EnemyAttack(3)))
    private val catalogue = listOf(greenA, greenB)

    private val rewardElem = FactionRewardToken(id = "reward_elem_a", expansion = Expansion.SHADES_OF_TEZLA_ELEMENTALIST, pile = TokenPileId.ELEMENTALIST_REWARDS, name = "Elem A", effectText = "Heal 1.", copies = 2)
    private val factionRewardCatalogue = listOf(rewardElem)

    @Test
    fun `toEntity then toDomain round-trips a session with drawn and defeated log entries`() {
        // Build realistic state via the session's own methods, not by hand-constructing it (per
        // CLAUDE.md): start, draw two (one batch), then mark the first entry defeated.
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 2), batchId = 42L, shuffle = noShuffle)
            .setDefeated(index = 0, defeated = true, note = "keep, NE tile")

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)

        // Independent ground truth, reasoned out by hand from the fixture (green pile is
        // [orc_a, orc_a, orc_b] under identity shuffle), NOT read off `session` - a bare
        // assertEquals(session, roundTripped) can't tell a correct mapper from a buggy one that
        // preserved wrong data (issue #150). Two draws take the two leading orc_a copies to the
        // discard, leaving orc_b in the draw pile.
        assertEquals(
            TokenPile(drawPile = listOf("orc_b"), discardPile = listOf("orc_a", "orc_a")),
            roundTripped.piles.getValue(TokenPileId.GREEN),
        )
        assertEquals(
            listOf(
                DrawLogEntry(tokenId = "orc_a", pile = TokenPileId.GREEN, batchId = 42L, defeated = true, note = "keep, NE tile"),
                DrawLogEntry(tokenId = "orc_a", pile = TokenPileId.GREEN, batchId = 42L),
            ),
            roundTripped.drawLog,
        )
        assertEquals(setOf(Expansion.BASE), roundTripped.tokenSet)
        assertEquals(false, roundTripped.drawWithReplacement)
    }

    @Test
    fun `toEntity then toDomain round-trips the token set and draw-with-replacement config`() {
        val session = EnemyPickerSession.start(
            catalogue,
            tokenSet = setOf(Expansion.BASE, Expansion.LOST_LEGION),
            drawWithReplacement = true,
            shuffle = noShuffle,
        )

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)
        assertEquals(setOf(Expansion.BASE, Expansion.LOST_LEGION), roundTripped.tokenSet)
        assertEquals(true, roundTripped.drawWithReplacement)
    }

    @Test
    fun `toEntity then toDomain round-trips a Summon Draw child's parentIndex`() {
        // Realistic state via the session's own methods: draw a parent, then summon a child from it.
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
            .draw(mapOf(TokenPileId.GREEN to 1), batchId = 1L, shuffle = noShuffle) // parent at index 0
            .summon(parentIndex = 0, pileIds = listOf(TokenPileId.GREEN), batchId = 2L, shuffle = noShuffle)

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)

        // Independent ground truth (not read off `session`, per CLAUDE.md's round-trip-test
        // guidance): green is [orc_a, orc_a, orc_b] under identity shuffle, so the parent draws the
        // first "orc_a" and the summon draws the second.
        assertEquals(
            listOf(
                DrawLogEntry(tokenId = "orc_a", pile = TokenPileId.GREEN, batchId = 1L),
                DrawLogEntry(tokenId = "orc_a", pile = TokenPileId.GREEN, batchId = 2L, parentIndex = 0),
            ),
            roundTripped.drawLog,
        )
        // The child's parentIndex specifically, called out on its own since it's the new field.
        assertEquals(0, roundTripped.drawLog[1].parentIndex)
        assertEquals(null, roundTripped.drawLog[0].parentIndex)
    }

    @Test
    fun `toEntity then toDomain round-trips a session that holds a drawn faction reward token`() {
        // A faction reward pile (issue #252) is just another TokenPileId, so the generic pile-map
        // mapper should carry it with no special-casing - this proves it does.
        val session = EnemyPickerSession.start(
            catalogue,
            tokenSet = setOf(Expansion.BASE, Expansion.SHADES_OF_TEZLA_ELEMENTALIST),
            shuffle = noShuffle,
            factionRewardCatalogue = factionRewardCatalogue,
        ).draw(mapOf(TokenPileId.ELEMENTALIST_REWARDS to 1), batchId = 8L, shuffle = noShuffle)

        val roundTripped = session.toEntity().toDomain()

        assertEquals(session, roundTripped)

        // Independent ground truth (reward pile is [reward_elem_a, reward_elem_a] under identity
        // shuffle): one draw moves the top copy to the discard, leaving the other in the draw pile.
        assertEquals(
            TokenPile(drawPile = listOf("reward_elem_a"), discardPile = listOf("reward_elem_a")),
            roundTripped.piles.getValue(TokenPileId.ELEMENTALIST_REWARDS),
        )
        assertEquals(
            DrawLogEntry(tokenId = "reward_elem_a", pile = TokenPileId.ELEMENTALIST_REWARDS, batchId = 8L),
            roundTripped.drawLog.single(),
        )
        assertTrue(Expansion.SHADES_OF_TEZLA_ELEMENTALIST in roundTripped.tokenSet)
    }

    @Test
    fun `toDomain drops an unknown expansion name instead of crashing`() {
        // A row saved by an older build can name an Expansion this build no longer has - e.g.
        // "SHADES_OF_TEZLA" from before it was split into two faction entries (issue #188). A raw
        // Expansion.valueOf would throw and crash the load (cf. the #194 migration crash-loop), so
        // an unknown name is silently dropped, keeping the known ones. Built by hand because the
        // legacy name can't be produced through the current enum/session API.
        val entity = EnemyPickerSessionEntity(
            drawWithReplacement = false,
            tokenSetJson = """["BASE","SHADES_OF_TEZLA","LOST_LEGION"]""",
            pilesJson = "{}",
            drawLogJson = "[]",
            updatedAt = 1L,
        )

        val restored = entity.toDomain()

        // The two names this build still knows survive; the stale one is gone (not a crash).
        assertEquals(setOf(Expansion.BASE, Expansion.LOST_LEGION), restored.tokenSet)
    }

    @Test
    fun `toEntity stamps the given updatedAt onto the entity`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)

        val entity = session.toEntity(updatedAt = 99L)

        assertEquals(99L, entity.updatedAt)
    }

    @Test
    fun `toEntity defaults updatedAt to roughly the current time when not given explicitly`() {
        val session = EnemyPickerSession.start(catalogue, shuffle = noShuffle)
        val before = System.currentTimeMillis()

        val entity = session.toEntity()

        val after = System.currentTimeMillis()
        assertTrue(entity.updatedAt in before..after)
    }
}
