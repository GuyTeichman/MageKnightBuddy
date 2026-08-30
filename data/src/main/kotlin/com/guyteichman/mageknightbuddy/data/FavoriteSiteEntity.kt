package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table row marking one site as favorited on the Sites tab (issue #236). Unlike the tab's
 * session entities, favorites are a *set*, so each favorited site is one row keyed by its
 * [Site.id][com.guyteichman.mageknightbuddy.domain.Site.id] - the presence of a row means "this
 * site is favorited", its absence means "not". Favoriting is therefore an INSERT and unfavoriting a
 * DELETE (see [FavoriteSiteDao]); the primary key makes re-favoriting an already-favorite site a
 * harmless no-op rather than a duplicate row.
 *
 * The catalogue itself (name, art, rules) is static and lives in `domain`; this table stores only
 * *which* ids the player starred, so an id here that no longer names a catalogue site is harmless
 * (it simply matches nothing when the UI intersects favorites with the catalogue).
 */
@Entity(tableName = "favorite_sites")
data class FavoriteSiteEntity(
    @PrimaryKey val siteId: String,
)
