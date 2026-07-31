package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The mandatory catalogue-validation test ADR-0007 requires: it loads and checks the *entire*
 * shipped `sites.json` on every `make test`, converting what would otherwise be a runtime parse/typo
 * failure on the phone into a CI failure. It also spot-checks sites against values reasoned out by
 * hand from `docs/rules/sites.md` (not read off the JSON), so a wrong transcription is caught, not
 * just a malformed file. Mirrors `TokenCatalogueTest`.
 */
class SiteCatalogueTest {

    // The 21 in-scope map-features, grouped by expansion, per docs/rules/sites.md's attribution
    // table. Asserted as name sets so a site tagged with the wrong expansion is caught, not just a
    // wrong grand total.
    private val baseNames = setOf(
        "Crystal Mines", "Magical Glade", "Marauding Orcs", "Draconum", "Village", "Monastery",
        "Mage Tower", "Keep", "Monster Den", "Spawning Grounds", "Dungeon", "Tomb", "Ancient Ruins",
    )
    private val lostLegionNames = setOf("Walls", "Deep Mines", "Refugee Camp", "Maze", "Labyrinth")
    private val shadesNames = setOf("Hidden Valley Tile", "Necropolis Tile", "Graveyard Tile")

    // The app-side category grouping, per docs/rules/sites.md's "Category taxonomy" table. Asserted
    // per category so a mis-categorised site is caught.
    private val categoryRosters = mapOf(
        SiteCategory.RAMPAGING_ENEMY to setOf("Marauding Orcs", "Draconum"),
        SiteCategory.FORTIFIED_SITE to setOf("Keep", "Mage Tower"),
        SiteCategory.ADVENTURE_SITE to setOf(
            "Monster Den", "Spawning Grounds", "Dungeon", "Tomb", "Ancient Ruins", "Maze", "Labyrinth",
        ),
        SiteCategory.SETTLEMENT to setOf("Village", "Monastery", "Refugee Camp"),
        SiteCategory.RESOURCE_SITE to setOf("Crystal Mines", "Deep Mines", "Magical Glade"),
        SiteCategory.SPECIAL_TILE to setOf("Hidden Valley Tile", "Necropolis Tile", "Graveyard Tile"),
        SiteCategory.TERRAIN_FEATURE to setOf("Walls"),
    )

    // --- Structural validation (ADR-0007's "the whole catalogue parses and is internally consistent")

    @Test
    fun `the shipped catalogue parses and is non-empty`() {
        assertTrue(SiteCatalogue.sites.isNotEmpty(), "sites.json parsed to an empty list")
    }

    @Test
    fun `every site id is unique`() {
        val ids = SiteCatalogue.sites.map { it.id }
        assertEquals(
            ids.size,
            ids.toSet().size,
            "duplicate site id(s): ${ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}",
        )
    }

    @Test
    fun `every site has at least one section and no blank heading or body`() {
        SiteCatalogue.sites.forEach { site ->
            assertTrue(site.sections.isNotEmpty(), "${site.id} has no sections")
            site.sections.forEach { section ->
                assertTrue(section.heading.isNotBlank(), "${site.id} has a blank section heading")
                assertTrue(section.body.isNotBlank(), "${site.id} has a blank section body")
            }
        }
    }

    @Test
    fun `art filenames are unique and non-blank where present`() {
        // art is null until Sub-issue C (issue #235); once set it doubles as the asset filename base
        // (ADR-0007), so it must be unique. The domain test can't reach app/ assets, so it checks
        // uniqueness, not file existence - just as TokenCatalogue relies on unique ids.
        val arts = SiteCatalogue.sites.mapNotNull { it.art }
        arts.forEach { assertTrue(it.isNotBlank(), "a site has a blank (non-null) art filename") }
        assertEquals(arts.size, arts.toSet().size, "duplicate art filename(s): $arts")
    }

    // --- Component counts & rosters (the "count matches the physical component count" check)

    @Test
    fun `the catalogue has exactly the 21 in-scope map-features`() {
        assertEquals(21, SiteCatalogue.sites.size, "expected 21 in-scope sites")
        assertEquals(
            baseNames + lostLegionNames + shadesNames,
            SiteCatalogue.sites.map { it.name }.toSet(),
        )
    }

    @Test
    fun `each expansion's roster matches, and no other expansion is present`() {
        fun namesIn(expansion: SiteExpansion) =
            SiteCatalogue.sites.filter { it.expansion == expansion }.map { it.name }.toSet()

        assertEquals(baseNames, namesIn(SiteExpansion.BASE))
        assertEquals(lostLegionNames, namesIn(SiteExpansion.LOST_LEGION))
        assertEquals(shadesNames, namesIn(SiteExpansion.SHADES_OF_TEZLA))
        // Apocalypse Dragon sites are deferred (issue #238) - a site tagged with a not-yet-present
        // expansion should fail loudly, not slip in.
        assertEquals(
            setOf(SiteExpansion.BASE, SiteExpansion.LOST_LEGION, SiteExpansion.SHADES_OF_TEZLA),
            SiteCatalogue.sites.map { it.expansion }.toSet(),
        )
    }

    @Test
    fun `each category's roster matches`() {
        categoryRosters.forEach { (category, expectedNames) ->
            val actual = SiteCatalogue.sites.filter { it.category == category }.map { it.name }.toSet()
            assertEquals(expectedNames, actual, "category $category roster mismatch")
        }
        // Every category present is one we declared an expected roster for.
        assertEquals(categoryRosters.keys, SiteCatalogue.sites.map { it.category }.toSet())
    }

    // --- Spot-checks. Every expected value below is reasoned out by hand from docs/rules/sites.md,
    // not read off sites.json, so a wrong transcription is caught (per CLAUDE.md's TDD guidance).

    private fun headingsOf(id: String): List<String> =
        assertNotNull(SiteCatalogue.byId(id), "missing site $id").sections.map { it.heading }

    private fun bodyOf(id: String, heading: String): String {
        val site = assertNotNull(SiteCatalogue.byId(id), "missing site $id")
        val section = assertNotNull(
            site.sections.firstOrNull { it.heading == heading },
            "$id has no \"$heading\" section",
        )
        return section.body
    }

    @Test
    fun `Crystal Mines is a single-section base resource site`() {
        val mine = assertNotNull(SiteCatalogue.byId("crystal_mines"))
        assertEquals(SiteExpansion.BASE, mine.expansion)
        assertEquals(SiteCategory.RESOURCE_SITE, mine.category)
        assertEquals(listOf("Mining"), mine.sections.map { it.heading })
    }

    @Test
    fun `Mage Tower is a fortified base site with a While Conquered section`() {
        val tower = assertNotNull(SiteCatalogue.byId("mage_tower"))
        assertEquals(SiteCategory.FORTIFIED_SITE, tower.category)
        assertEquals(listOf("When Revealed", "While Unconquered", "While Conquered"), tower.sections.map { it.heading })
    }

    @Test
    fun `Keep's last section is Your Keep and explains the hand-limit bonus`() {
        assertEquals(
            listOf("When Revealed", "Unconquered Keep", "Other Players' Keeps", "Your Keep"),
            headingsOf("keep"),
        )
        assertTrue(bodyOf("keep", "Your Keep").contains("Hand limit"))
    }

    @Test
    fun `Village has the three interaction sections`() {
        assertEquals(listOf("Recruiting", "Healing", "Plundering"), headingsOf("village"))
    }

    @Test
    fun `Monastery can be burned for an Artifact`() {
        val monastery = assertNotNull(SiteCatalogue.byId("monastery"))
        assertEquals(SiteExpansion.BASE, monastery.expansion)
        assertTrue("Burning a Monastery" in monastery.sections.map { it.heading })
        assertTrue(bodyOf("monastery", "Burning a Monastery").contains("Artifact"))
    }

    @Test
    fun `the two rampaging enemies grant their printed Reputation rewards`() {
        assertEquals(SiteCategory.RAMPAGING_ENEMY, assertNotNull(SiteCatalogue.byId("marauding_orcs")).category)
        assertTrue(bodyOf("marauding_orcs", "Reward").contains("Reputation +1"))
        assertTrue(bodyOf("draconum", "Reward").contains("Reputation +2"))
    }

    @Test
    fun `Ancient Ruins offers an Altar worth 7 Fame`() {
        assertTrue("Altar" in headingsOf("ancient_ruins"))
        assertTrue(bodyOf("ancient_ruins", "Altar").contains("7 Fame"))
    }

    @Test
    fun `Maze fights a brown enemy while Labyrinth fights a Draconum for an extra Advanced Action`() {
        val maze = assertNotNull(SiteCatalogue.byId("maze"))
        assertEquals(SiteExpansion.LOST_LEGION, maze.expansion)
        assertEquals(SiteCategory.ADVENTURE_SITE, maze.category)
        assertTrue(bodyOf("maze", "Combat").contains("brown"), "Maze fights a brown enemy token")

        assertTrue(bodyOf("labyrinth", "Combat").contains("Draconum"), "Labyrinth fights a Draconum")
        // The Labyrinth's distinguishing reward: the Advanced Action on top of the Maze's rewards.
        assertTrue(bodyOf("labyrinth", "Reward").contains("Advanced Action"))
    }

    @Test
    fun `Walls is a Lost Legion terrain feature, not a site you occupy`() {
        val walls = assertNotNull(SiteCatalogue.byId("walls"))
        assertEquals(SiteExpansion.LOST_LEGION, walls.expansion)
        assertEquals(SiteCategory.TERRAIN_FEATURE, walls.category)
        assertEquals(listOf("Movement", "Combat", "Provoking"), walls.sections.map { it.heading })
    }

    @Test
    fun `Deep Mines lets you choose the crystal color`() {
        val deepMines = assertNotNull(SiteCatalogue.byId("deep_mines"))
        assertEquals(SiteExpansion.LOST_LEGION, deepMines.expansion)
        assertTrue(bodyOf("deep_mines", "Mining").contains("choose"))
    }

    @Test
    fun `the three Shades of Tezla tiles are special tiles that grant black mana`() {
        listOf("hidden_valley_tile", "necropolis_tile", "graveyard_tile").forEach { id ->
            val tile = assertNotNull(SiteCatalogue.byId(id))
            assertEquals(SiteExpansion.SHADES_OF_TEZLA, tile.expansion, "$id should be Shades of Tezla")
            assertEquals(SiteCategory.SPECIAL_TILE, tile.category, "$id should be a SPECIAL_TILE")
        }
        // Hidden Valley is the Magical-Glade twin (gold by Day, black by Night); Necropolis/Graveyard
        // are Eternal-Night tiles that grant black mana.
        assertTrue(bodyOf("hidden_valley_tile", "Imbued With Magic").contains("black mana"))
        assertTrue("Eternal Night" in headingsOf("necropolis_tile"))
        assertTrue(bodyOf("necropolis_tile", "Imbued With Magic").contains("black mana"))
        assertTrue(bodyOf("graveyard_tile", "Movement").contains("terrain type"))
    }
}
