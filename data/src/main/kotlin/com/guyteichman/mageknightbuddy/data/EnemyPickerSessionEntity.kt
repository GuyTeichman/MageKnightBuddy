package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table row representing a persisted
 * [com.guyteichman.mageknightbuddy.domain.EnemyPickerSession]. Mirrors the other session entities'
 * single-slot autosave design (fixed [SINGLETON_ID] primary key, see GitHub issue #27): the Enemy
 * Picker's pile state must survive app restarts for the length of a whole game (ADR-0006), so it is
 * autosaved here rather than held only in a `ViewModel`.
 *
 * [drawWithReplacement] is a plain column. [tokenSetJson] holds the active
 * [Expansion][com.guyteichman.mageknightbuddy.domain.Expansion] set as a JSON string array of enum
 * names; [pilesJson] holds the pile map (pile-name -> [TokenPileDto]) and [drawLogJson] the Draw
 * Log ([DrawLogEntryDto] list) - all as JSON columns since Room maps neither `Map`/`List` nor
 * sealed/enum-keyed structures directly (see [EnemyPickerSessionMapper]).
 *
 * [updatedAt] mirrors the other entities' timestamp - see [DummyPlayerSessionEntity.updatedAt].
 */
@Entity(tableName = "enemy_picker_sessions")
data class EnemyPickerSessionEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val drawWithReplacement: Boolean,
    val tokenSetJson: String,
    val pilesJson: String,
    val drawLogJson: String,
    val updatedAt: Long,
) {
    companion object {
        /** The fixed primary key every saved row uses, enforcing the single-slot autosave design above. */
        const val SINGLETON_ID = 0
    }
}
