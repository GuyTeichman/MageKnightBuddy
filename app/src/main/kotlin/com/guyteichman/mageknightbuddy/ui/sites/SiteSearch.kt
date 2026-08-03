package com.guyteichman.mageknightbuddy.ui.sites

import com.guyteichman.mageknightbuddy.domain.Site
import com.guyteichman.mageknightbuddy.domain.SiteCategory
import com.guyteichman.mageknightbuddy.domain.SiteExpansion

/**
 * Search/sort/filter/group helpers for the Sites tab (issues #234 and #237). Kept as pure functions
 * in their own file - separate from the composables that call them - so this logic is unit-testable
 * on a plain JVM (see `SiteSearchTest`), the same split as the Enemy Picker's `DrawLogGrouping`.
 */

/**
 * How the Sites list is divided into sections (issue #237). [NONE] is the flat, alphabetical v1
 * default; [CATEGORY] / [EXPANSION] insert a header per [SiteCategory] / [SiteExpansion]. Stored as
 * the list screen's `rememberSaveable` group state - an enum is Parcelable-safe, so it survives
 * rotation and process death without a custom Saver.
 */
enum class SiteGrouping { NONE, CATEGORY, EXPANSION }

/**
 * One rendered section of the Sites list: a group [key] and the name-sorted [sites] under it.
 *
 * [key] is the enum the group is keyed on - a [SiteCategory] or a [SiteExpansion] - or `null` for the
 * single group returned when grouping is [SiteGrouping.NONE] (a flat list with no header). Keeping the
 * enum here rather than a display string keeps this logic UI-string-free and unit-testable; the UI
 * turns [key] into a header label (see `SitesScreen`). `Enum<*>` is the common supertype of the two
 * possible key enums.
 */
data class SiteGroup(val key: Enum<*>?, val sites: List<Site>)

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
 * The sites matching [query] (see [matchesQuery]), returned sorted alphabetically by name - the flat,
 * ungrouped ordering (the same shape [searchedFilteredGrouped] produces for [SiteGrouping.NONE], minus
 * the filters). Kept as a focused unit for the search/sort tests, which exercise matching and ordering
 * in isolation from grouping. `sortedBy { it.name.lowercase() }` is a case-insensitive sort; site
 * names are ASCII, so no locale-aware collator is needed.
 */
internal fun List<Site>.searchedAndSorted(query: String): List<Site> =
    filter { it.matchesQuery(query) }.sortedBy { it.name.lowercase() }

/**
 * The sites passing the expansion and category filters (issue #237). Each axis is an OR within
 * itself and an AND across the two: a site is kept when its expansion is in [expansions] *and* its
 * category is in [categories]. An **empty set means "no constraint"** for that axis, so passing
 * `emptySet()` for both returns the whole list unchanged - that's the unfiltered default the filter
 * sheet starts in. Input order is preserved (sorting happens later in the pipeline).
 */
internal fun List<Site>.filteredBy(
    expansions: Set<SiteExpansion>,
    categories: Set<SiteCategory>,
): List<Site> = filter { site ->
    (expansions.isEmpty() || site.expansion in expansions) &&
        (categories.isEmpty() || site.category in categories)
}

/**
 * The full Sites-list pipeline (issue #237): **search → filter → sort → group**. Applies the search
 * [query] ([matchesQuery]), narrows by the [expansions] / [categories] filters ([filteredBy]), sorts
 * the survivors alphabetically by name, then splits them into [SiteGroup]s per [grouping]:
 * - [SiteGrouping.NONE] → a single `null`-keyed group (or *no* groups when nothing matched, so the UI
 *   shows its empty state rather than an empty header-less section).
 * - [SiteGrouping.CATEGORY] / [SiteGrouping.EXPANSION] → one group per distinct key, ordered by the
 *   enum's declaration order (`ordinal`) - release order for expansions, the curated `SiteCategory`
 *   order for categories. Keys with no matching sites are naturally absent (`groupBy` only emits keys
 *   that occur), so empty sections never render. Sites keep their name-sorted order within each group.
 */
internal fun List<Site>.searchedFilteredGrouped(
    query: String,
    expansions: Set<SiteExpansion> = emptySet(),
    categories: Set<SiteCategory> = emptySet(),
    grouping: SiteGrouping = SiteGrouping.NONE,
): List<SiteGroup> {
    val matched = filter { it.matchesQuery(query) }
        .filteredBy(expansions, categories)
        .sortedBy { it.name.lowercase() }
    return when (grouping) {
        // A single header-less group; empty result → no groups at all (empty-state case).
        SiteGrouping.NONE -> if (matched.isEmpty()) emptyList() else listOf(SiteGroup(null, matched))
        // groupBy preserves each site's (already name-sorted) order within a bucket; we then reorder
        // the buckets themselves by the key enum's ordinal so headers follow declaration order.
        SiteGrouping.CATEGORY -> matched.groupBy { it.category }
            .entries.sortedBy { it.key.ordinal }
            .map { SiteGroup(it.key, it.value) }
        SiteGrouping.EXPANSION -> matched.groupBy { it.expansion }
            .entries.sortedBy { it.key.ordinal }
            .map { SiteGroup(it.key, it.value) }
    }
}
