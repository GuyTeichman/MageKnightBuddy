package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * The kind of map-feature a [Site] is. This is an **app-side grouping** that powers the Sites tab's
 * sort/group/filter controls (issue #237), not an official rulebook taxonomy - see the "Category
 * taxonomy" table in `docs/rules/sites.md` for the definitions and each site's assignment (which are
 * judgment calls open to revision).
 *
 * **Declaration order matters:** it's the order the section headers appear in when the list is grouped
 * by category (see `SiteSearch.searchedFilteredGrouped`), so keep it aligned with the taxonomy table.
 *
 * `@Serializable` so it can tag each [Site] in the JSON catalogue (ADR-0007).
 */
@Serializable
enum class SiteCategory {
    /** An enemy roaming the map, not a location you own (Marauding Orcs, Draconum). */
    RAMPAGING_ENEMY,

    /** A defended site you assault and then own (Keep, Mage Tower). */
    FORTIFIED_SITE,

    /** A site you spend an action to enter and fight for a reward (Dungeon, Tomb, Maze, ...). */
    ADVENTURE_SITE,

    /** A friendly site you interact at to recruit or heal (Village, Monastery, Refugee Camp). */
    SETTLEMENT,

    /** A site giving a passive start/end-of-turn benefit (Crystal Mines, Deep Mines, Magical Glade). */
    RESOURCE_SITE,

    /** A location tile placed over a map space (Hidden Valley, Necropolis, Graveyard). */
    SPECIAL_TILE,

    /** A map terrain feature, not a site you occupy (Walls). */
    TERRAIN_FEATURE,
}
