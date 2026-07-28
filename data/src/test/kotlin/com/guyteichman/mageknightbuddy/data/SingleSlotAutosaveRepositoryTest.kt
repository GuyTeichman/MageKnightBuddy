package com.guyteichman.mageknightbuddy.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/**
 * Behavioural tests for the generic single-slot autosave engine. Exercised with tiny stand-in
 * domain/entity types (not a real session or Room) so they cover the engine itself: crucially, its
 * resilience when a persisted row no longer deserializes.
 */
class SingleSlotAutosaveRepositoryTest {

    // Minimal stand-ins so the tests depend on the generic engine only, not any real session type.
    private data class FakeEntity(val payload: String)
    private data class FakeDomain(val value: String)

    // Builds a repository over an in-memory "stored row", with a pluggable toDomain so a test can
    // make deserialization throw the way a real mapper's Json.decodeFromString / enum valueOf would.
    private fun repository(
        stored: FakeEntity?,
        toDomain: FakeEntity.() -> FakeDomain = { FakeDomain(payload) },
    ): SingleSlotAutosaveRepository<FakeDomain, FakeEntity> =
        SingleSlotAutosaveRepository(
            upsert = {},
            get = { stored },
            getUpdatedAt = { null },
            toEntity = { FakeEntity(value) },
            toDomain = toDomain,
        )

    @Test
    fun `restore returns the decoded domain when the stored row deserializes cleanly`() = runTest {
        val repo = repository(stored = FakeEntity("hello"))

        assertEquals(FakeDomain("hello"), repo.restore())
    }

    @Test
    fun `restore returns null when the stored row fails to deserialize, instead of throwing`() = runTest {
        // Simulates a row written by an older build whose JSON shape no longer decodes. The app must
        // fall back to a fresh session rather than crash on launch (the stillInPlay crash, issue #178
        // follow-up): Room's destructive migration can't catch this, since only a String column's
        // *content* shape changed, so the resilience has to live here.
        val repo = repository(stored = FakeEntity("stale")) { error("unknown key 'stillInPlay'") }

        assertNull(repo.restore())
    }
}
