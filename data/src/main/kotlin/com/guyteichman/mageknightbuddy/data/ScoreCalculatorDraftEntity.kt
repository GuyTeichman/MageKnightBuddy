package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table row persisting the Score Calculator wizard's in-progress field values, so an app
 * kill that discards the wizard's `SavedStateHandle` Bundle - e.g. the user swiping the app away
 * from Recents, which Android treats as an intentional close and does not retain state for (see
 * GitHub issue #174) - still has something durable to restore from. Single-slot autosave design
 * (fixed [SINGLETON_ID] primary key), same convention as [EnemyPickerSessionEntity] and the Dummy
 * Player tab's session entities (see GitHub issue #27, #151).
 *
 * [fieldsJson] holds every wizard field as a JSON object of field key -> stringified value (see
 * ScoreCalculatorDraftMapper): the wizard's own fields (`ScoreCalculatorViewModel.resettable`) are
 * already string/boolean/int/enum-name values keyed by a String key, so this mirrors that shape
 * directly instead of declaring a ~50-column table.
 */
@Entity(tableName = "score_calculator_drafts")
data class ScoreCalculatorDraftEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val fieldsJson: String,
    val updatedAt: Long,
) {
    companion object {
        /** The fixed primary key every saved row uses, enforcing the single-slot autosave design above. */
        const val SINGLETON_ID = 0
    }
}
