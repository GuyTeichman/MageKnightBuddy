package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room data-access object for the Sites tab's favorites (issue #236). Favorites are modeled as a set
 * of rows (see [FavoriteSiteEntity]), so the operations are set operations: add a member, remove a
 * member, observe the whole set live, and (for backup/restore) snapshot or replace it wholesale.
 */
@Dao
interface FavoriteSiteDao {
    /**
     * Marks [entity]'s site as favorited. `IGNORE` on the primary-key conflict makes re-favoriting an
     * already-favorite site a silent no-op instead of a crash or a duplicate row - favoriting is
     * idempotent, which lets the UI fire it without first checking the current state.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entity: FavoriteSiteEntity)

    /** Unfavorites [siteId]. A no-op if it wasn't favorited (DELETE simply matches no rows). */
    @Query("DELETE FROM favorite_sites WHERE siteId = :siteId")
    suspend fun remove(siteId: String)

    /**
     * The favorited site ids as a live [Flow]: Room re-emits whenever the table changes, so the Sites
     * tab's ViewModel gets a push every time a star is toggled. Ordered by id so the stream is
     * deterministic (the order itself is irrelevant - callers treat it as a set).
     */
    @Query("SELECT siteId FROM favorite_sites ORDER BY siteId")
    fun observeAll(): Flow<List<String>>

    /** One-shot snapshot of the favorited ids (not a live Flow) - used to export favorites to a backup file. */
    @Query("SELECT siteId FROM favorite_sites ORDER BY siteId")
    suspend fun getAllOnce(): List<String>

    /** Bulk insert for restore, so a whole backup's worth of favorites goes in with one call. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAll(entities: List<FavoriteSiteEntity>)

    @Query("DELETE FROM favorite_sites")
    suspend fun deleteAll()

    /**
     * Replaces the entire favorites set with [entities] (used by "restore from backup"). @Transaction
     * makes the wipe + re-insert atomic, mirroring [ScoringSessionDao.replaceAll]: if the insert
     * fails the delete rolls back, so a failed restore can never leave favorites half-applied.
     */
    @Transaction
    suspend fun replaceAll(entities: List<FavoriteSiteEntity>) {
        deleteAll()
        addAll(entities)
    }
}
