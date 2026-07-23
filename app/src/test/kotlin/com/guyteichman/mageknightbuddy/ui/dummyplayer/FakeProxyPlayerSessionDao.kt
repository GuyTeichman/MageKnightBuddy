package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionDao
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionEntity
import kotlinx.coroutines.CompletableDeferred

/** The Proxy Player-mode counterpart to [FakeDummyPlayerSessionDao]/[FakeVolkareSessionDao] - see either's doc comment. */
class FakeProxyPlayerSessionDao : ProxyPlayerSessionDao {
    private var stored: ProxyPlayerSessionEntity? = null

    // Lets a test hold an in-flight upsert() suspended at a known point, standing in for Room's
    // real IO-dispatched suspension - null (the default) means upsert() completes immediately, as
    // every existing test expects.
    var upsertGate: CompletableDeferred<Unit>? = null

    override suspend fun upsert(entity: ProxyPlayerSessionEntity) {
        upsertGate?.await()
        stored = entity
    }

    override suspend fun get(): ProxyPlayerSessionEntity? = stored

    override suspend fun getUpdatedAt(): Long? = stored?.updatedAt
}
