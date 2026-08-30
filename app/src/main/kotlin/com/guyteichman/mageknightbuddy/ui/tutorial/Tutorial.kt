package com.guyteichman.mageknightbuddy.ui.tutorial

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One page of a screen's tutorial: a short heading plus the beginner-friendly explanation shown
 * beneath it. A [Tutorial] is an ordered list of these, paged through in the tutorial pop-up.
 *
 * `@Serializable` lets `Json` (see [parseTutorials]) build this straight from `tutorials.json`,
 * matching JSON object keys to constructor parameter names - no hand-written parsing.
 */
@Serializable
data class TutorialStep(val title: String, val body: String)

/**
 * The whole triggerable tutorial for one screen (issue #161): an overall [title] shown in the
 * pop-up's header, and the ordered [steps] the reader swipes through. Kept as plain presentation
 * content (no game rules logic), which is why it lives in `app/` assets rather than `domain/`.
 */
@Serializable
data class Tutorial(val title: String, val steps: List<TutorialStep>)

/**
 * The screen ids used as keys into the parsed tutorials map (and as the DataStore "seen" flag ids).
 * Kept as constants so a screen and its `tutorials.json` entry can't silently drift apart via a typo.
 */
object TutorialKeys {
    const val SETUP = "setup"
    const val DUMMY = "dummy"
    const val PROXY = "proxy"
    const val VOLKARE = "volkare"
    const val ENEMIES = "enemies"
}

/**
 * Decodes the raw JSON tutorial content into a lookup keyed by screen id (e.g. "setup", "dummy",
 * "proxy", "volkare"), one [Tutorial] per screen. Mirrors
 * [com.guyteichman.mageknightbuddy.ui.help.parseFieldHelp].
 */
fun parseTutorials(json: String): Map<String, Tutorial> =
    Json.decodeFromString<Map<String, Tutorial>>(json)

/**
 * Reads `tutorials.json` out of the app's bundled assets and parses it, so callers (see
 * `MageKnightBuddyApplication`) can load the tutorial content once at startup and pass the
 * resulting map down into the screens that host a tutorial pop-up.
 */
fun loadTutorials(context: Context): Map<String, Tutorial> {
    // `.use { }` is Kotlin's try-with-resources equivalent: it guarantees the reader is closed
    // after `readText()` runs, even if reading throws.
    val json = context.assets.open("tutorials.json").bufferedReader().use { it.readText() }
    return parseTutorials(json)
}
