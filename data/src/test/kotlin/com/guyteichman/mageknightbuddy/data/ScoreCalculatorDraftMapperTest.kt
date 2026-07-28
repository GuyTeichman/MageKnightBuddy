package com.guyteichman.mageknightbuddy.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreCalculatorDraftMapperTest {

    @Test
    fun `toEntity then toDomain round-trips a draft's field map`() {
        val draft = mapOf(
            "pageIndex" to "2",
            "scenarioId" to "solo_conquest",
            "fame" to "50",
            "city1Conquered" to "true",
            "knight" to "WOLFHAWK",
        )

        val roundTripped = draft.toEntity().toDomain()

        assertEquals(draft, roundTripped)
        // Independent ground truth (per CLAUDE.md's round-trip-test guidance): the entity's
        // fieldsJson is a plain JSON object of the same key -> string pairs, not read off `draft`.
        assertEquals(
            """{"pageIndex":"2","scenarioId":"solo_conquest","fame":"50","city1Conquered":"true","knight":"WOLFHAWK"}""",
            draft.toEntity().fieldsJson,
        )
    }

    @Test
    fun `toEntity round-trips an empty draft`() {
        val roundTripped = emptyMap<String, String>().toEntity().toDomain()

        assertEquals(emptyMap(), roundTripped)
    }

    @Test
    fun `toEntity stamps the given updatedAt onto the entity`() {
        val entity = mapOf("fame" to "10").toEntity(updatedAt = 99L)

        assertEquals(99L, entity.updatedAt)
    }

    @Test
    fun `toEntity defaults updatedAt to roughly the current time when not given explicitly`() {
        val before = System.currentTimeMillis()

        val entity = mapOf("fame" to "10").toEntity()

        val after = System.currentTimeMillis()
        assertTrue(entity.updatedAt in before..after)
    }
}
