package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table row representing a persisted [com.guyteichman.mageknightbuddy.domain.VolkareSession].
 * Mirrors [DummyPlayerSessionEntity]'s design exactly: only autosave is supported, so this is a
 * single-slot table where every row written here uses the same [SINGLETON_ID] primary key (a new
 * save always overwrites the previous one, see GitHub issue #27). [VolkareSessionMapper] converts
 * to/from the domain session, and [VolkareSessionDao] does the actual upsert/read.
 *
 * [scenario] holds [com.guyteichman.mageknightbuddy.domain.Scenario.id] (either Volkare's Return
 * or Volkare's Quest - see [VolkareSessionMapper]) and [raceLevel] holds the
 * [com.guyteichman.mageknightbuddy.domain.RaceLevel] enum's name, both as plain `String` columns
 * for the same reason [DummyPlayerSessionEntity.knight] is: Room maps enums/sealed types to
 * columns via their `String` name, not natively.
 *
 * [deckOrderJson], [discardPileJson], and [logJson] hold the deck order, discard pile, and event
 * log as JSON string columns, since Room can't map `List`/sealed-class columns directly - see
 * [VolkareSessionMapper] for the JSON conversion (via [VolkareCardDto]/[VolkareEventDto]).
 *
 * [updatedAt] mirrors [DummyPlayerSessionEntity.updatedAt] - see that property's doc comment.
 */
@Entity(tableName = "volkare_sessions")
data class VolkareSessionEntity(
    // `@PrimaryKey` marks the Room primary key column; defaulting it to the shared SINGLETON_ID
    // is what makes every write collide with the same row instead of inserting a new one.
    @PrimaryKey val id: Int = SINGLETON_ID,
    val scenario: String,
    val raceLevel: String,
    val deckOrderJson: String,
    val discardPileJson: String,
    val round: Int,
    val cityRevealed: Boolean,
    val lost: Boolean,
    val logJson: String,
    val updatedAt: Long,
    val startsAtNight: Boolean = false,
) {
    companion object {
        /** The fixed primary key every saved row uses, enforcing the single-slot autosave design above. */
        const val SINGLETON_ID = 0
    }
}
