package com.guyteichman.mageknightbuddy.ui.sites

import com.guyteichman.mageknightbuddy.data.FavoriteSiteDao
import com.guyteichman.mageknightbuddy.data.FavoriteSiteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [FavoriteSiteDao] for app-module unit tests (mirrors the DummyPlayer tab's fake DAOs),
 * so [SitesViewModel] can be exercised without a real database. Backed by a [MutableStateFlow] set of
 * ids so [observeAll] emits live on every change, exactly like Room's generated Flow. The interface's
 * default `replaceAll` (deleteAll + addAll) is inherited as-is.
 */
class FakeFavoriteSiteDao : FavoriteSiteDao {
    private val ids = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun add(entity: FavoriteSiteEntity) {
        ids.value = ids.value + entity.siteId
    }

    override suspend fun remove(siteId: String) {
        ids.value = ids.value - siteId
    }

    override fun observeAll(): Flow<List<String>> = ids.map { it.sorted() }

    override suspend fun getAllOnce(): List<String> = ids.value.sorted()

    override suspend fun addAll(entities: List<FavoriteSiteEntity>) {
        ids.value = ids.value + entities.map { it.siteId }
    }

    override suspend fun deleteAll() {
        ids.value = emptySet()
    }
}
