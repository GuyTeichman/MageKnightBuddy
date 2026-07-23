package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table row representing a persisted [com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession].
 * Mirrors [VolkareSessionEntity]/[DummyPlayerSessionEntity]'s design exactly: single-slot autosave
 * via the fixed [SINGLETON_ID] primary key (see GitHub issue #27). [ProxyPlayerSessionMapper]
 * converts to/from the domain session, and [ProxyPlayerSessionDao] does the actual upsert/read.
 *
 * Deck/discard pile/objective card/event log are `...Json` `String` columns, same reason as
 * [VolkareSessionEntity]'s equivalents (Room can't map `List`/sealed-class/nullable-sealed-class
 * columns directly). [objectiveCardJson] is nullable - `null` means no current Objective Card
 * (see `CONTEXT.md`'s **Objective Card** entry). Crystal counts flatten into 4 named columns,
 * matching [DummyPlayerSessionEntity]'s convention (Room has no direct `Map` column support).
 */
@Entity(tableName = "proxy_player_sessions")
data class ProxyPlayerSessionEntity(
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
    val objectiveCardJson: String?,
    val objectiveShields: Int,
    val logJson: String,
    val updatedAt: Long,
) {
    companion object {
        /** The fixed primary key every saved row uses, enforcing the single-slot autosave design above. */
        const val SINGLETON_ID = 0
    }
}
