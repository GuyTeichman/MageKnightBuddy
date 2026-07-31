package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One transcribed *Mage Knight* map-feature description - a single static record in the JSON
 * catalogue (ADR-0007), mirroring the physical **Site Description card** (name + art + "what you can
 * do here and how"). The Sites tab (issue #177) browses these; the catalogue owns no game state.
 *
 * A site's rules are printed as several titled paragraphs (e.g. a Keep's "When Revealed" /
 * "Unconquered Keep" / "Your Keep"), so the body is modeled as an ordered list of [SiteSection]s
 * rather than one blob - the tab renders them in order and the search matches across all of them.
 *
 * @property id Stable unique key used everywhere a specific site is referenced, and (doubling as the
 *   image filename base, ADR-0007) the site art asset. The catalogue-validation test enforces
 *   uniqueness.
 * @property name Display title (e.g. "Mage Tower").
 * @property category App-side grouping for the future sort/filter (see [SiteCategory]).
 * @property expansion Which product this site first appeared in (see [SiteExpansion]).
 * @property art Filename of the site's art asset in `app/src/main/assets/site-art/`, or `null` until
 *   Sub-issue C (issue #235) supplies it - the UI renders a placeholder while it's absent, so this
 *   catalogue can ship before the art. When non-null it must be unique across the catalogue (the
 *   validation test enforces that).
 * @property sections The site's titled rule paragraphs, in printed order (never empty).
 *
 * `@Serializable` so the whole catalogue decodes straight into `List<Site>` (ADR-0007).
 */
@Serializable
data class Site(
    val id: String,
    val name: String,
    val category: SiteCategory,
    val expansion: SiteExpansion,
    val sections: List<SiteSection>,
    val art: String? = null,
)

/**
 * One titled paragraph of a [Site]'s rules - e.g. a Village's "Plundering" heading and its body
 * text. Kept as a first-class type (rather than a `Map<String, String>`) so the printed order is
 * preserved and both fields are searched.
 *
 * `@Serializable` so it decodes as part of the [Site] catalogue (ADR-0007).
 */
@Serializable
data class SiteSection(
    val heading: String,
    val body: String,
)
