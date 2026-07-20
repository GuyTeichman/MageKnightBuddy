package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table row representing a persisted [com.guyteichman.mageknightbuddy.domain.DummyPlayerSession].
 * Only autosave is supported (no named saves/history), so this is a single-slot table: every row
 * written here uses the same [SINGLETON_ID] primary key, meaning a new save always overwrites the
 * previous one instead of accumulating rows (see GitHub issue #27, which decided this fixed-id /
 * upsert-replace approach over auto-generated ids). [DummyPlayerSessionMapper] converts to/from
 * the domain session, and [DummyPlayerSessionDao] does the actual upsert/read.
 *
 * `@Entity` is a Room annotation marking this class as a database table (`tableName` names it);
 * Room generates the SQL schema and read/write code from the class shape at compile time.
 *
 * Fields that aren't simple primitives - the deck/discard pile ordering and the event log - are
 * stored as `...Json` `String` columns rather than native Room types, since Room can't map
 * `List`/sealed-class columns directly; see [DummyPlayerSessionMapper] for the JSON conversion.
 * Similarly, the crystal counts map (one `Int` per [com.guyteichman.mageknightbuddy.domain.CardColor])
 * is flattened into four separate columns here, since Room has no direct `Map` column support.
 */
@Entity(tableName = "dummy_player_sessions")
data class DummyPlayerSessionEntity(
    // `@PrimaryKey` marks the Room primary key column; defaulting it to the shared SINGLETON_ID
    // is what makes every write collide with the same row instead of inserting a new one.
    @PrimaryKey val id: Int = SINGLETON_ID,
    val knight: String,
    val wasRandom: Boolean,
    val deckOrderJson: String,
    val discardPileJson: String,
    val crystalsRed: Int,
    val crystalsGreen: Int,
    val crystalsBlue: Int,
    val crystalsWhite: Int,
    val round: Int,
    val roundEnded: Boolean,
    val logJson: String,
) {
    companion object {
        /** The fixed primary key every saved row uses, enforcing the single-slot autosave design above. */
        const val SINGLETON_ID = 0
    }
}
