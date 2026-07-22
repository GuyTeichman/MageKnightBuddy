package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.VolkareSessionDao
import com.guyteichman.mageknightbuddy.data.VolkareSessionEntity
import kotlinx.coroutines.CompletableDeferred

/** The Volkare-mode counterpart to [FakeDummyPlayerSessionDao] - see that file's doc comment. */
class FakeVolkareSessionDao : VolkareSessionDao {
    private var stored: VolkareSessionEntity? = null

    // Lets a test hold an in-flight upsert() suspended at a known point, standing in for Room's
    // real IO-dispatched suspension - null (the default) means upsert() completes immediately, as
    // every existing test expects.
    var upsertGate: CompletableDeferred<Unit>? = null

    override suspend fun upsert(entity: VolkareSessionEntity) {
        upsertGate?.await()
        stored = entity
    }

    override suspend fun get(): VolkareSessionEntity? = stored

    override suspend fun getUpdatedAt(): Long? = stored?.updatedAt
}
