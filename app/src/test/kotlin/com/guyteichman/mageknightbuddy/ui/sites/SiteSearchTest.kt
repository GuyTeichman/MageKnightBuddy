package com.guyteichman.mageknightbuddy.ui.sites

import com.guyteichman.mageknightbuddy.domain.Site
import com.guyteichman.mageknightbuddy.domain.SiteCatalogue
import com.guyteichman.mageknightbuddy.domain.SiteCategory
import com.guyteichman.mageknightbuddy.domain.SiteExpansion
import com.guyteichman.mageknightbuddy.domain.SiteSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Sites tab's search/sort logic ([matchesQuery] / [searchedAndSorted]).
 *
 * The pure-logic cases use small hand-built [Site] fixtures rather than the real catalogue, so they
 * stay meaningful and non-brittle as sites get added (issue #238). A couple of catalogue-integration
 * cases at the end pin the actual product behaviour ("searching a section word surfaces that site")
 * using membership, not counts, so they survive catalogue growth too.
 */
class SiteSearchTest {

    // Minimal fixture builder. category/expansion default to fixed values for the search-only tests
    // (where they're irrelevant), but are overridable for the filter/group tests below.
    private fun site(
        id: String,
        name: String,
        category: SiteCategory = SiteCategory.ADVENTURE_SITE,
        expansion: SiteExpansion = SiteExpansion.BASE,
        sections: List<SiteSection> = listOf(SiteSection("Body", "text")),
    ) =
        Site(
            id = id,
            name = name,
            category = category,
            expansion = expansion,
            sections = sections,
        )

    @Test
    fun `blank query returns every site, sorted alphabetically by name`() {
        // Deliberately built out of alphabetical order to prove the sort, not the input order.
        val sites = listOf(site("k", "Keep"), site("a", "Ancient Ruins"), site("d", "Dungeon"))

        val result = sites.searchedAndSorted("")

        assertEquals(listOf("Ancient Ruins", "Dungeon", "Keep"), result.map { it.name })
    }

    @Test
    fun `a whitespace-only query is treated as blank and matches every site`() {
        val sites = listOf(site("a", "Ancient Ruins"), site("b", "Village"))

        assertEquals(2, sites.searchedAndSorted("   ").size)
    }

    @Test
    fun `name matching is case-insensitive`() {
        val keep = site("k", "Keep")

        assertTrue(keep.matchesQuery("keep"))
        assertTrue(keep.matchesQuery("KEEP"))
        assertTrue(keep.matchesQuery("KeEp"))
    }

    @Test
    fun `matches a substring appearing inside the name`() {
        val sites = listOf(site("cm", "Crystal Mines"), site("dm", "Deep Mines"), site("v", "Village"))

        // "mine" is a substring of both mines' names but not Village's.
        assertEquals(listOf("Crystal Mines", "Deep Mines"), sites.searchedAndSorted("mine").map { it.name })
    }

    @Test
    fun `matches text in a section heading even when the name does not`() {
        val keep = site("k", "Keep", sections = listOf(SiteSection("While Conquered", "You own it.")))
        val village = site("v", "Village", sections = listOf(SiteSection("Recruiting", "Recruit units.")))

        // "conquered" appears only in the Keep's heading, in neither site's name.
        assertTrue(keep.matchesQuery("conquered"))
        assertFalse(village.matchesQuery("conquered"))
    }

    @Test
    fun `matches text in a section body even when the name and heading do not`() {
        val orcs = site("o", "Marauding Orcs", sections = listOf(SiteSection("Reward", "Gain Reputation +1.")))

        // "reputation" is only in the body - not the name, not the heading.
        assertTrue(orcs.matchesQuery("reputation"))
    }

    @Test
    fun `a query matching nothing returns an empty list`() {
        val sites = listOf(site("a", "Ancient Ruins"), site("k", "Keep"))

        assertTrue(sites.searchedAndSorted("zzzzz").isEmpty())
    }

    // --- Catalogue-integration behaviour (stable facts, asserted by membership not count) ---

    @Test
    fun `searching a While-Conquered word surfaces conquerable sites but not a plain settlement`() {
        val names = SiteCatalogue.sites.searchedAndSorted("conquered").map { it.name }

        // Keep and Mage Tower have "While Conquered" sections; Village has no conquered section.
        assertTrue("Keep" in names)
        assertTrue("Mage Tower" in names)
        assertFalse("Village" in names)
    }

    @Test
    fun `searching an enemy name matches both its own site and other sites referencing it`() {
        val names = SiteCatalogue.sites.searchedAndSorted("draconum").map { it.name }

        // Draconum matches by name; Labyrinth matches because its combat text names the Draconum.
        assertTrue("Draconum" in names)
        assertTrue("Labyrinth" in names)
    }

    @Test
    fun `catalogue results come back alphabetically sorted`() {
        val names = SiteCatalogue.sites.searchedAndSorted("mine").map { it.name }

        assertEquals(names.sortedBy { it.lowercase() }, names)
    }

    // --- Filtering ([filteredBy]) : empty set = no constraint, OR within an axis, AND across axes ---

    @Test
    fun `an empty filter on both axes leaves every site`() {
        val sites = listOf(
            site("a", "A", expansion = SiteExpansion.BASE),
            site("b", "B", expansion = SiteExpansion.LOST_LEGION),
        )

        assertEquals(sites, sites.filteredBy(expansions = emptySet(), categories = emptySet()))
    }

    @Test
    fun `an expansion filter keeps only sites in the selected expansions (OR within the axis)`() {
        val base = site("a", "A", expansion = SiteExpansion.BASE)
        val lost = site("b", "B", expansion = SiteExpansion.LOST_LEGION)
        val shades = site("c", "C", expansion = SiteExpansion.SHADES_OF_TEZLA)
        val sites = listOf(base, lost, shades)

        val result = sites.filteredBy(
            expansions = setOf(SiteExpansion.BASE, SiteExpansion.LOST_LEGION),
            categories = emptySet(),
        )

        // Base OR Lost Legion - Shades is dropped.
        assertEquals(listOf(base, lost), result)
    }

    @Test
    fun `a category filter keeps only sites in the selected categories`() {
        val keep = site("k", "Keep", category = SiteCategory.FORTIFIED_SITE)
        val dungeon = site("d", "Dungeon", category = SiteCategory.ADVENTURE_SITE)
        val sites = listOf(keep, dungeon)

        val result = sites.filteredBy(
            expansions = emptySet(),
            categories = setOf(SiteCategory.FORTIFIED_SITE),
        )

        assertEquals(listOf(keep), result)
    }

    @Test
    fun `the two filter axes combine with AND`() {
        val baseAdventure = site("ba", "BA", category = SiteCategory.ADVENTURE_SITE, expansion = SiteExpansion.BASE)
        val baseFort = site("bf", "BF", category = SiteCategory.FORTIFIED_SITE, expansion = SiteExpansion.BASE)
        val lostAdventure = site("la", "LA", category = SiteCategory.ADVENTURE_SITE, expansion = SiteExpansion.LOST_LEGION)
        val sites = listOf(baseAdventure, baseFort, lostAdventure)

        val result = sites.filteredBy(
            expansions = setOf(SiteExpansion.BASE),
            categories = setOf(SiteCategory.ADVENTURE_SITE),
        )

        // Only the site that is BASE *and* an ADVENTURE_SITE survives both axes.
        assertEquals(listOf(baseAdventure), result)
    }

    // --- Grouping ([searchedFilteredGrouped]) ---

    @Test
    fun `NONE grouping returns a single null-keyed group, name-sorted`() {
        val sites = listOf(site("k", "Keep"), site("a", "Ancient Ruins"), site("d", "Dungeon"))

        val groups = sites.searchedFilteredGrouped(query = "", grouping = SiteGrouping.NONE)

        assertEquals(1, groups.size)
        assertEquals(null, groups.single().key)
        assertEquals(listOf("Ancient Ruins", "Dungeon", "Keep"), groups.single().sites.map { it.name })
    }

    @Test
    fun `NONE grouping on an empty result returns no groups (not one empty group)`() {
        val sites = listOf(site("a", "Ancient Ruins"))

        assertTrue(sites.searchedFilteredGrouped(query = "zzz", grouping = SiteGrouping.NONE).isEmpty())
    }

    @Test
    fun `CATEGORY grouping orders headers by enum order and omits empty categories`() {
        // Deliberately built out of enum order and with two categories only (others absent).
        val settlementZ = site("z", "Zed", category = SiteCategory.SETTLEMENT)
        val rampaging = site("r", "Orcs", category = SiteCategory.RAMPAGING_ENEMY)
        val settlementA = site("a", "Abbey", category = SiteCategory.SETTLEMENT)
        val sites = listOf(settlementZ, rampaging, settlementA)

        val groups = sites.searchedFilteredGrouped(query = "", grouping = SiteGrouping.CATEGORY)

        // RAMPAGING_ENEMY (ordinal 0) precedes SETTLEMENT (ordinal 3); FORTIFIED/ADVENTURE/... omitted.
        assertEquals(listOf(SiteCategory.RAMPAGING_ENEMY, SiteCategory.SETTLEMENT), groups.map { it.key })
        // Within the settlement group, sites are name-sorted (Abbey before Zed).
        assertEquals(listOf("Abbey", "Zed"), groups.last().sites.map { it.name })
    }

    @Test
    fun `EXPANSION grouping orders headers by enum order (release order)`() {
        val shades = site("s", "S", expansion = SiteExpansion.SHADES_OF_TEZLA)
        val base = site("b", "B", expansion = SiteExpansion.BASE)
        val ad = site("a", "A", expansion = SiteExpansion.APOCALYPSE_DRAGON)
        val sites = listOf(shades, base, ad)

        val groups = sites.searchedFilteredGrouped(query = "", grouping = SiteGrouping.EXPANSION)

        assertEquals(
            listOf(SiteExpansion.BASE, SiteExpansion.SHADES_OF_TEZLA, SiteExpansion.APOCALYPSE_DRAGON),
            groups.map { it.key },
        )
    }

    @Test
    fun `search, filter and grouping all apply together`() {
        val baseAdvMine = site("m", "Crystal Mine", category = SiteCategory.ADVENTURE_SITE, expansion = SiteExpansion.BASE)
        val baseFortMine = site("f", "Fortified Mine", category = SiteCategory.FORTIFIED_SITE, expansion = SiteExpansion.BASE)
        val lostAdvMine = site("l", "Lost Mine", category = SiteCategory.ADVENTURE_SITE, expansion = SiteExpansion.LOST_LEGION)
        val baseAdvKeep = site("k", "Keep", category = SiteCategory.ADVENTURE_SITE, expansion = SiteExpansion.BASE)
        val sites = listOf(baseAdvMine, baseFortMine, lostAdvMine, baseAdvKeep)

        val groups = sites.searchedFilteredGrouped(
            query = "mine",                                  // drops Keep
            expansions = setOf(SiteExpansion.BASE),          // drops Lost Mine
            categories = setOf(SiteCategory.ADVENTURE_SITE), // drops Fortified Mine
            grouping = SiteGrouping.CATEGORY,
        )

        // Only Crystal Mine survives all three; one ADVENTURE_SITE group holding it.
        assertEquals(listOf(SiteCategory.ADVENTURE_SITE), groups.map { it.key })
        assertEquals(listOf("Crystal Mine"), groups.single().sites.map { it.name })
    }
}
