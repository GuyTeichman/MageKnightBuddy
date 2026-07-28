package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.DrawLogEntry
import com.guyteichman.mageknightbuddy.domain.Expansion
import com.guyteichman.mageknightbuddy.domain.TokenPile
import com.guyteichman.mageknightbuddy.domain.TokenPileId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden-decode ("tripwire") test for the Enemy Picker's persisted JSON columns.
 *
 * Unlike [EnemyPickerSessionMapperTest]'s round-trips - which encode with *today's* code and decode
 * it straight back, so both sides move together and a field rename passes unnoticed - this test
 * decodes a **frozen** JSON payload representing what an already-*shipped* build wrote. Its whole job
 * is to fail if a future DTO change stops that shipped payload decoding to the values a user would
 * expect. That failure is the moment to decide, consciously, between writing a real Room migration
 * (bump + `Migration`) and accepting a destructive reset - rather than discovering it as a silent
 * data wipe (or, before [PersistenceJson] was made lenient, a crash - issue #178 follow-up) on a
 * real device after release.
 *
 * Rule for maintainers: treat [GOLDEN_JSON_*] as immutable. Never regenerate them from current code -
 * that would defeat the point. Add a *new* frozen payload for a new released shape instead.
 */
class EnemyPickerSessionGoldenTest {

    @Test
    fun `a shipped Enemy Picker save still decodes to the expected session`() {
        // Verbatim columns as a shipped build writes them - see the class doc; do not regenerate.
        val shipped = EnemyPickerSessionEntity(
            id = EnemyPickerSessionEntity.SINGLETON_ID,
            drawWithReplacement = false,
            tokenSetJson = """["BASE","LOST_LEGION"]""",
            pilesJson = """{"GREEN":{"drawPile":["green_diggers","green_prowlers"],"discardPile":["green_ironclads"]}}""",
            drawLogJson = """[{"tokenId":"green_ironclads","pile":"GREEN","batchId":100,"defeated":true,"note":"NE tile"}]""",
            updatedAt = 1_785_175_881_955,
        )

        val session = shipped.toDomain()

        // Every expected value is read off the frozen JSON above by hand, never off `session`.
        assertEquals(setOf(Expansion.BASE, Expansion.LOST_LEGION), session.tokenSet)
        assertEquals(false, session.drawWithReplacement)
        assertEquals(
            TokenPile(drawPile = listOf("green_diggers", "green_prowlers"), discardPile = listOf("green_ironclads")),
            session.piles.getValue(TokenPileId.GREEN),
        )
        assertEquals(
            listOf(
                DrawLogEntry(
                    tokenId = "green_ironclads",
                    pile = TokenPileId.GREEN,
                    batchId = 100L,
                    defeated = true,
                    note = "NE tile",
                ),
            ),
            session.drawLog,
        )
    }
}
