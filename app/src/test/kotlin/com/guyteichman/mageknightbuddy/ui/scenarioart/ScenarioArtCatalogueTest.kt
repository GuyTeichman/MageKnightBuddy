package com.guyteichman.mageknightbuddy.ui.scenarioart

import com.guyteichman.mageknightbuddy.domain.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the app-side scenario-art catalogue's integrity (issue #285).
 *
 * The catalogue is empty at the foundation stage, so the per-entry checks are vacuously green now -
 * they become load-bearing the moment issue #288 adds rows, catching a typo'd scenario id or a
 * duplicate at test time rather than as a blank card at runtime. Pure JVM (no assets/Robolectric);
 * the sibling `ScenarioArtAssetsTest` covers the "does the file exist" half.
 */
class ScenarioArtCatalogueTest {

    private val validIds = Scenario.entries.map { it.id }.toSet()

    @Test
    fun `every entry maps to a real scenario id`() {
        val unknown = ScenarioArtCatalogue.entries.map { it.scenarioId }.filterNot { it in validIds }
        assertTrue("catalogue references unknown scenario ids: $unknown", unknown.isEmpty())
    }

    @Test
    fun `no scenario or filename is listed twice`() {
        val ids = ScenarioArtCatalogue.entries.map { it.scenarioId }
        assertEquals("a scenario is illustrated more than once", ids.size, ids.toSet().size)
        val files = ScenarioArtCatalogue.entries.map { it.filename }
        assertEquals("an image is referenced by more than one scenario", files.size, files.toSet().size)
    }

    @Test
    fun `every entry has non-blank attribution fields`() {
        val bad = ScenarioArtCatalogue.entries.filter {
            it.scenarioId.isBlank() || it.filename.isBlank() || it.workTitle.isBlank() ||
                it.author.isBlank() || it.sourceUrl.isBlank()
        }
        assertTrue("catalogue entries with a blank field: $bad", bad.isEmpty())
    }

    @Test
    fun `artFor resolves by scenario id and is null for the unillustrated`() {
        // Stays correct as data lands: null iff no row claims the scenario, else the row matches by id.
        Scenario.entries.forEach { scenario ->
            val entry = ScenarioArtCatalogue.artFor(scenario)
            if (entry == null) {
                assertTrue(
                    "artFor returned null but a catalogue row claims ${scenario.id}",
                    ScenarioArtCatalogue.entries.none { it.scenarioId == scenario.id },
                )
            } else {
                assertEquals(scenario.id, entry.scenarioId)
            }
        }
    }
}
