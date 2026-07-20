package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionDao
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionEntity
import kotlinx.coroutines.CompletableDeferred

class FakeDummyPlayerSessionDao : DummyPlayerSessionDao {
    private var stored: DummyPlayerSessionEntity? = null

    // Lets a test hold an in-flight upsert() suspended at a known point, standing in for Room's
    // real IO-dispatched suspension - null (the default) means upsert() completes immediately, as
    // every existing test expects.
    var upsertGate: CompletableDeferred<Unit>? = null

    override suspend fun upsert(entity: DummyPlayerSessionEntity) {
        upsertGate?.await()
        stored = entity
    }

    override suspend fun get(): DummyPlayerSessionEntity? = stored
}
