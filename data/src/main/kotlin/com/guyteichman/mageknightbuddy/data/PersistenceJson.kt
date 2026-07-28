package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.json.Json

/**
 * The single [Json] instance every session/scoring mapper in this module encodes and decodes its
 * JSON string columns with. Configured with `ignoreUnknownKeys = true` so a column written by an
 * *older* build - one carrying a field a newer build has since renamed or removed - still decodes
 * instead of throwing.
 *
 * This is the JSON-content counterpart to Room's schema migration. Room's own
 * `fallbackToDestructiveMigration` only reacts to *column* changes (a table/column added or dropped);
 * it is blind to a `String` column whose JSON *content* shape moved - which is exactly how a saved
 * Enemy Picker row with the old `stillInPlay` field crashed the app after that field was renamed to
 * `defeated` without a DB version bump (see MageKnightBuddyDatabase's version-history comment). Being
 * lenient here means such a change silently *drops* the unknown field rather than crashing on decode.
 *
 * Leniency alone would let an incompatible shape change slip by unnoticed and quietly discard shipped
 * users' data, so it is paired with two guards: [SingleSlotAutosaveRepository.restore] resets to a
 * fresh session if a row still fails to decode, and the golden-decode tests in `data/` fail loudly in
 * CI when a frozen, already-shipped payload stops decoding to its expected value - the point at which
 * a real migration (vs. an accepted destructive reset) has to be a conscious choice.
 */
internal val PersistenceJson: Json = Json {
    ignoreUnknownKeys = true
}
