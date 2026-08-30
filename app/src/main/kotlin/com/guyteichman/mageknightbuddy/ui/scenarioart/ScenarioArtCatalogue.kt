package com.guyteichman.mageknightbuddy.ui.scenarioart

import com.guyteichman.mageknightbuddy.domain.Scenario

/**
 * The licence a bundled scenario image is used under. Only [CC0] and [CC_BY] are allowed for
 * scenario art (decided in the issue #171 planning grill): CC0 needs no attribution (a credit is a
 * courtesy), CC-BY legally *requires* visible attribution - which is why every entry carries an
 * [ScenarioArtEntry.author] and [ScenarioArtEntry.sourceUrl] regardless, so issue #288 can wire the
 * CC-BY ones into Settings > Credits without re-sourcing them.
 */
enum class ArtLicense {
    CC0,
    CC_BY,
}

/**
 * One scenario's background art plus the attribution needed to use it. Kept app-side (not on the
 * pure-Kotlin [Scenario]) because art is presentation, mirroring `KnightShieldIcon`'s app-side
 * mapping - the `domain` module stays free of image/credit concerns.
 *
 * @param scenarioId the [Scenario.id] this art illustrates (a stable persistence key, so the mapping
 *   survives a scenario being renamed in the UI).
 * @param filename the image under `app/src/main/assets/scenario-art/`.
 * @param author who made the image, for the credit line.
 * @param sourceUrl where it came from, for the credit link and licence verification.
 * @param license the licence it's used under (see [ArtLicense]).
 */
data class ScenarioArtEntry(
    val scenarioId: String,
    val filename: String,
    val author: String,
    val sourceUrl: String,
    val license: ArtLicense,
)

/**
 * App-side catalogue mapping each [Scenario] to its background art. Empty for now (issue #285 is the
 * code-only foundation): every scenario is still unillustrated, so [artFor] returns null for all of
 * them and [ScenarioArt] shows its placeholder. Issue #288 fills [entries] in image-by-image as art
 * is sourced and license-verified - the two UI slices (scoreboard cards #286, picker #287) need no
 * further change as rows land, they just start showing real art in place of the placeholder.
 *
 * `object` is a Kotlin singleton, so this is a single shared catalogue with no instance to construct.
 */
object ScenarioArtCatalogue {
    /** Every illustrated scenario. Filled in incrementally by issue #288; empty at the foundation. */
    val entries: List<ScenarioArtEntry> = emptyList()

    // associateBy builds a scenarioId -> entry lookup once, so artFor is a map read rather than a
    // linear scan of entries on every call (this is read on every scoreboard row / picker item).
    private val byScenarioId: Map<String, ScenarioArtEntry> = entries.associateBy { it.scenarioId }

    /**
     * The art for [scenario], or null while it's still unillustrated - in which case the caller
     * ([ScenarioArt]) shows the placeholder. Keyed on the stable [Scenario.id], not the object.
     */
    fun artFor(scenario: Scenario): ScenarioArtEntry? = byScenarioId[scenario.id]
}
