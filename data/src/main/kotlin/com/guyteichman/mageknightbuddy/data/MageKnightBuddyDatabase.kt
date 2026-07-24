package com.guyteichman.mageknightbuddy.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's single Room database: one physical SQLite file (or in-memory instance in tests,
 * see ADR-0003) holding both the score-calculator and Dummy-Player session tables.
 *
 * This class only declares *what* the database contains - Room generates the actual
 * implementation. Obtain a real instance via [createDatabase]; tests build their own directly
 * against this class using Room's in-memory builder.
 */
// The @Database annotation is Room's entry point: it tells the Room compiler (KSP) which
// entities (tables) belong to this database and what version the schema is at, so it can
// generate the actual RoomDatabase implementation (a class named `MageKnightBuddyDatabase_Impl`)
// behind the scenes. `exportSchema = false` skips writing that schema to a JSON file on disk,
// since this project isn't tracking schema history for migrations yet.
@Database(
    entities = [ScoringSessionEntity::class, DummyPlayerSessionEntity::class, VolkareSessionEntity::class, ProxyPlayerSessionEntity::class],
    // Bumped 2 -> 3: ScoringSessionEntity's ~22 wide columns collapsed into a single inputJson
    // column (see ScoringInputDto). Bumped 3 -> 4: ScoringInputDto.ForTheCouncil's own shape
    // changed (reputationModifier/shieldOnXSpace/reputation -> one reputationTrackPosition).
    // Bumped 4 -> 5: reputationTrackPosition (an invented index the physical board never prints)
    // replaced with reputationTrackSpaceName (the enum name), since the Outcome check was wrongly
    // keyed off that invented index instead of the modifier the board actually shows - see
    // ReputationTrackSpace. Room's column-level schema didn't change (inputJson is still just a
    // String column), but the JSON *content* inside it did, and kotlinx.serialization fails to
    // decode old-shaped JSON by default. Any ScoringInputDto shape change needs a version bump for
    // this same reason, even when no entity/column actually changed. Bumped 5 -> 6: added the new
    // VolkareSessionEntity table (volkare_sessions), plus an updatedAt column on the existing
    // DummyPlayerSessionEntity table - both needed so the setup screen's "Restore Game" flow can
    // compare recency between a Dummy Player session and a Volkare session (issue #129). No
    // hand-written migration - the app has never been published, so fallbackToDestructiveMigration
    // (see createDatabase()) is fine pre-release. Bumped 6 -> 7: DummyPlayerSessionEntity's
    // deckOrderJson/discardPileJson/logJson shape changed (CardColor -> CardIdentity, to support
    // dual-color Advanced Action cards) - destructive migration, no real user data (see
    // docs/adr/0005-shared-advanced-action-card-type-for-dual-color-cards.md). Bumped 7 -> 8: added
    // the new ProxyPlayerSessionEntity table (proxy_player_sessions) - see
    // docs/rules/proxy-player.md - plus a startsAtNight column on VolkareSessionEntity, for the
    // setup screen's new "Starts at night?" checkbox. No hand-written migration -
    // fallbackToDestructiveMigration (see createDatabase()) is fine pre-release, same as every
    // prior bump. Bumped 8 -> 9: added the same startsAtNight column to DummyPlayerSessionEntity
    // and ProxyPlayerSessionEntity, for parity across all 3 Dummy Player tab modes. Bumped 9 -> 10:
    // ProxyPlayerEventDto.ObjectiveResolved dropped its resolution field (the Proxy Player screen's
    // Explored/Completed buttons were merged into one, since the two outcomes have identical
    // tracked-state effect - see docs/rules/proxy-player.md's "Resolution") - same "JSON content
    // shape changed, column didn't" reasoning as the 3 -> 4 and 4 -> 5 bumps above.
    version = 10,
    exportSchema = false,
)
abstract class MageKnightBuddyDatabase : RoomDatabase() {
    // Room generates the DAO implementation for each abstract accessor below and wires it
    // to this database, so callers just call these functions to get a working DAO instance.
    abstract fun scoringSessionDao(): ScoringSessionDao
    abstract fun dummyPlayerSessionDao(): DummyPlayerSessionDao
    abstract fun volkareSessionDao(): VolkareSessionDao
    abstract fun proxyPlayerSessionDao(): ProxyPlayerSessionDao
}
