package com.guyteichman.mageknightbuddy.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The persistence-facing entry point the app uses to read and toggle the Sites tab's favorites
 * (issue #236), hiding the Room [FavoriteSiteDao] and the entity<->id conversion behind a
 * set-of-ids API. The rest of the app deals only in site-id [String]s; this is the only place
 * outside the DAO that needs to know [FavoriteSiteEntity] exists.
 */
class FavoriteSitesRepository(private val dao: FavoriteSiteDao) {
    /**
     * The favorited site ids as a live [Flow]. `map` here transforms each emitted ordered List into a
     * Set without collecting the flow itself - callers still get a live, auto-updating stream, just of
     * a Set (order is irrelevant, and a Set makes the UI's "is this favorited?" check an O(1) `in` test).
     */
    fun observeFavorites(): Flow<Set<String>> =
        dao.observeAll().map { it.toSet() }

    /**
     * Adds or removes [id] from the favorites, per [favorite]. Idempotent in both directions (see
     * [FavoriteSiteDao.add]/[FavoriteSiteDao.remove]), so the UI can call it from a star toggle without
     * first reading the current state.
     */
    suspend fun setFavorite(id: String, favorite: Boolean) {
        if (favorite) dao.add(FavoriteSiteEntity(id)) else dao.remove(id)
    }

    /** One-shot sorted snapshot of the favorited ids, for writing a backup file (see BackupCodec). */
    suspend fun exportAll(): List<String> = dao.getAllOnce()

    /**
     * Replaces the whole favorites set with [ids] - the "restore from backup" operation. Atomic (see
     * [FavoriteSiteDao.replaceAll]); any prior favorites are discarded.
     */
    suspend fun replaceAll(ids: List<String>) =
        dao.replaceAll(ids.map { FavoriteSiteEntity(it) })
}
