package com.guyteichman.mageknightbuddy.ui.sites

import com.guyteichman.mageknightbuddy.domain.Site

/**
 * Search/sort helpers for the Sites tab (issue #234). Kept as pure functions in their own file -
 * separate from the composables that call them - so this logic is unit-testable on a plain JVM
 * (see `SiteSearchTest`), the same split as the Enemy Picker's `DrawLogGrouping`.
 */

/**
 * True if [query] matches this site: a case-insensitive substring search across the site's [Site.name]
 * and every [com.guyteichman.mageknightbuddy.domain.SiteSection]'s heading and body. A blank (or
 * whitespace-only) query matches everything, so an empty search box shows the whole catalogue.
 *
 * Matching bodies as well as names is what lets a search like "conquered" surface every site with a
 * *While Conquered* section, even though none of them have that word in their name (issue #234's spec).
 */
internal fun Site.matchesQuery(query: String): Boolean {
    // Trim so stray spaces from the text field don't turn a blank box into a no-match.
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    if (name.lowercase().contains(needle)) return true
    // `any` short-circuits on the first matching section, so long catalogues stay cheap.
    return sections.any { section ->
        section.heading.lowercase().contains(needle) || section.body.lowercase().contains(needle)
    }
}

/**
 * The sites matching [query] (see [matchesQuery]), returned sorted alphabetically by name - the flat
 * ordering the v1 list uses. `sortedBy { it.name.lowercase() }` is a case-insensitive sort; site names
 * are ASCII, so no locale-aware collator is needed.
 */
internal fun List<Site>.searchedAndSorted(query: String): List<Site> =
    filter { it.matchesQuery(query) }.sortedBy { it.name.lowercase() }
