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

    // Minimal fixture builder - category/expansion are irrelevant to search, so they're fixed.
    private fun site(id: String, name: String, sections: List<SiteSection> = listOf(SiteSection("Body", "text"))) =
        Site(
            id = id,
            name = name,
            category = SiteCategory.ADVENTURE_SITE,
            expansion = SiteExpansion.BASE,
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
}
