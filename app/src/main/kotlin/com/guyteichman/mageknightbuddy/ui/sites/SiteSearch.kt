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
 * [key] is the enum the group is keyed on - a [SiteCategory] or a [SiteExpansion] - or `null` for a
 * header-less group. Keeping the enum here rather than a display string keeps this logic
 * UI-string-free and unit-testable; the UI turns [key] into a header label (see `SitesScreen`).
 * `Enum<*>` is the common supertype of the two possible key enums.
 *
 * [isFavorites] marks the single leading "★ Favorites" group (issue #236) that pinned favorites are
 * pulled into. It is a separate flag rather than another [key] enum because "Favorites" isn't one of
 * the grouping axes - it's an always-on top section orthogonal to [SiteGrouping]. The favorites group
 * carries a `null` [key] (its header text is fixed, not derived from an enum); every other group has
 * `isFavorites = false`. So the three shapes are: `(null, false)` = flat ungrouped list,
 * `(category/expansion, false)` = a grouped section, `(null, true)` = the favorites section.
 */
data class SiteGroup(val key: Enum<*>?, val sites: List<Site>, val isFavorites: Boolean = false)

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
 * The full Sites-list pipeline (issues #237, #236): **search → filter → sort → pin favorites → group**.
 * Applies the search [query] ([matchesQuery]), narrows by the [expansions] / [categories] filters
 * ([filteredBy]), sorts the survivors alphabetically by name, pulls the [favorites] into a single
 * leading "★ Favorites" group, then splits the rest into [SiteGroup]s per [grouping]:
 * - A favorited site (its id in [favorites]) that survived search+filter is moved into the leading
 *   `isFavorites` group (name-sorted, spanning all categories/expansions) and removed from the normal
 *   grouping below - so it appears exactly once, at the top, in every [grouping] mode. The favorites
 *   group therefore *also* obeys search and filters (a favorite that didn't survive them isn't in it),
 *   and is omitted entirely when no favorite survived. [favorites] defaults empty → no favorites group,
 *   i.e. the pre-#236 behaviour.
 * - [SiteGrouping.NONE] → a single `null`-keyed group for the non-favorites (or *no* such group when
 *   nothing is left, so the UI shows its empty state rather than an empty header-less section).
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
    favorites: Set<String> = emptySet(),
): List<SiteGroup> {
    val matched = filter { it.matchesQuery(query) }
        .filteredBy(expansions, categories)
        .sortedBy { it.name.lowercase() }
    // partition splits the (already name-sorted) survivors into favorites and the rest in one pass,
    // preserving order in both halves - so the favorites block stays alphabetical and the remainder
    // feeds the normal grouping below.
    val (favorited, rest) = matched.partition { it.id in favorites }
    // The leading ★ section, only when at least one favorite survived search+filter (listOfNotNull
    // drops it otherwise, so an empty favorites section never renders).
    val favoritesGroup = if (favorited.isEmpty()) null else SiteGroup(key = null, sites = favorited, isFavorites = true)
    val restGroups = when (grouping) {
        // A single header-less group; empty remainder → no group at all (empty-state case).
        SiteGrouping.NONE -> if (rest.isEmpty()) emptyList() else listOf(SiteGroup(null, rest))
        // groupBy preserves each site's (already name-sorted) order within a bucket; we then reorder
        // the buckets themselves by the key enum's ordinal so headers follow declaration order.
        SiteGrouping.CATEGORY -> rest.groupBy { it.category }
            .entries.sortedBy { it.key.ordinal }
            .map { SiteGroup(it.key, it.value) }
        SiteGrouping.EXPANSION -> rest.groupBy { it.expansion }
            .entries.sortedBy { it.key.ordinal }
            .map { SiteGroup(it.key, it.value) }
    }
    return listOfNotNull(favoritesGroup) + restGroups
}
