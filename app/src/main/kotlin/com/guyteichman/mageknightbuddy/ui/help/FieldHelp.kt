package com.guyteichman.mageknightbuddy.ui.help

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The beginner-friendly help text shown by a wizard field's "?" dialog: the rule explanation
 * itself, plus a citation back to the rulebook page it came from.
 *
 * `@Serializable` is a kotlinx.serialization annotation - it lets `Json` (below) build this class
 * directly from the bundled `field_help.json`, matching JSON object keys to constructor parameter
 * names, without any hand-written parsing code.
 */
@Serializable
data class FieldHelp(val text: String, val source: String)

/**
 * Decodes the raw JSON help content into a lookup map keyed by field/category name (e.g. "Fame",
 * "Greatest Loot"), one [FieldHelp] entry per rulebook mechanic.
 */
fun parseFieldHelp(json: String): Map<String, FieldHelp> =
    Json.decodeFromString<Map<String, FieldHelp>>(json)

/**
 * Reads `field_help.json` out of the app's bundled assets and parses it, so callers (see
 * `MageKnightBuddyApplication`) can load the wizard's help content once at startup and pass the
 * resulting map down into [com.guyteichman.mageknightbuddy.ui.scorecalculator.ScoreCalculatorScreen],
 * which wires each page's "?" button to the matching entries via [FieldHelp].
 */
fun loadFieldHelp(context: Context): Map<String, FieldHelp> {
    // `.use { }` is Kotlin's try-with-resources equivalent: it guarantees the stream/reader is
    // closed after `readText()` runs, even if reading throws.
    val json = context.assets.open("field_help.json").bufferedReader().use { it.readText() }
    return parseFieldHelp(json)
}
