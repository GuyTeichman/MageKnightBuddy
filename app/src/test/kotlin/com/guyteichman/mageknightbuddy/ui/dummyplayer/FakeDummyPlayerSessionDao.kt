package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionDao
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionEntity

class FakeDummyPlayerSessionDao : DummyPlayerSessionDao {
    private var stored: DummyPlayerSessionEntity? = null

    override suspend fun upsert(entity: DummyPlayerSessionEntity) {
        stored = entity
    }

    override suspend fun get(): DummyPlayerSessionEntity? = stored
}
