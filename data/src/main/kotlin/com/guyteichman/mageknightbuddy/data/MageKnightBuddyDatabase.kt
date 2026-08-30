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
    entities = [ScoringSessionEntity::class, DummyPlayerSessionEntity::class, VolkareSessionEntity::class, ProxyPlayerSessionEntity::class, EnemyPickerSessionEntity::class, ScoreCalculatorDraftEntity::class, FavoriteSiteEntity::class],
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
    // shape changed, column didn't" reasoning as the 3 -> 4 and 4 -> 5 bumps above. Bumped 10 -> 11:
    // added the new EnemyPickerSessionEntity table (enemy_picker_sessions) for the Enemy Picker's
    // autosaved pile state (issue #178, ADR-0006) - no hand-written migration,
    // fallbackToDestructiveMigration is fine pre-release, same as every prior bump. (v11 is still
    // unshipped: while finalising #178 its drawLogJson content was reshaped - DrawLogEntryDto's
    // stillInPlay field became defeated, and the token catalogue's ability model split into
    // offensive/defensive - so any pre-merge v11 test DB should be reinstalled clean rather than
    // carried over. No further bump, since v11 never shipped.) Bumped 11 -> 12: added the new
    // ScoreCalculatorDraftEntity table (score_calculator_drafts) for the Score Calculator wizard's
    // autosaved in-progress draft (issue #174) - no hand-written migration,
    // fallbackToDestructiveMigration is fine pre-release, same as every prior bump. Bumped 12 -> 13:
    // added tacticStateJson/isSolo columns to DummyPlayerSessionEntity, ProxyPlayerSessionEntity,
    // and VolkareSessionEntity, plus a scenario column to the first two (VolkareSessionEntity
    // already had its own, Return/Quest-only, scenario column) - for the Tactic Card draft
    // (issue #219, part of #179) - no hand-written migration, fallbackToDestructiveMigration is
    // fine pre-release, same as every prior bump. Bumped 13 -> 14: the Enemy Picker's pile lifecycle
    // became pile-correct (issue #251) - EnemyPickerSessionEntity's drawLogJson gained a
    // DrawLogEntryDto.ephemeral field, and its pilesJson `discardPile` now means *defeated-only* (an
    // undefeated drawn token is held on the board, out of both piles) rather than every drawn token.
    // The columns didn't change, but the JSON *semantics* did, so an old row would be misinterpreted
    // (its discarded-on-draw tokens would be re-drawable) - a destructive wipe avoids that, the same
    // "JSON content/semantics changed" reasoning as the 3 -> 4 / 4 -> 5 / 10 -> 11 bumps, and
    // fallbackToDestructiveMigration is fine pre-release as always. Bumped 14 -> 15: added the new
    // FavoriteSiteEntity table (favorite_sites) for the Sites tab's persisted favorites (issue #236).
    // A purely additive table, so a hand-written CREATE-TABLE migration would preserve existing data -
    // but per the issue's explicit decision we keep relying on fallbackToDestructiveMigration here,
    // accepting that installing this build wipes any on-device scoring history/sessions. (Note: unlike
    // the older bump comments above, the app *has* had a debug-signed release, so that wipe is a real
    // data loss the author signed off on, not the "no user data yet" situation those comments assume.)
    version = 15,
    exportSchema = false,
)
abstract class MageKnightBuddyDatabase : RoomDatabase() {
    // Room generates the DAO implementation for each abstract accessor below and wires it
    // to this database, so callers just call these functions to get a working DAO instance.
    abstract fun scoringSessionDao(): ScoringSessionDao
    abstract fun dummyPlayerSessionDao(): DummyPlayerSessionDao
    abstract fun volkareSessionDao(): VolkareSessionDao
    abstract fun proxyPlayerSessionDao(): ProxyPlayerSessionDao
    abstract fun enemyPickerSessionDao(): EnemyPickerSessionDao
    abstract fun scoreCalculatorDraftDao(): ScoreCalculatorDraftDao
    abstract fun favoriteSiteDao(): FavoriteSiteDao
}
