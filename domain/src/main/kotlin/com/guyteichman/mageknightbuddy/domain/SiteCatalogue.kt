package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

/**
 * The full set of transcribed [Site]s, loaded once from the JSON catalogue that ships in this
 * module's resources (`sites.json`), per ADR-0007. This is the source of truth the Sites tab
 * (issue #177) builds its browsable, searchable list from.
 *
 * Loading lives in `domain` (not `data`) precisely so a plain-JVM test can validate the whole
 * catalogue on every `make test` - see `SiteCatalogueTest`, the mandatory mitigation ADR-0007
 * requires for keeping compiler-uncheckable data honest. Mirrors [TokenCatalogue].
 */
object SiteCatalogue {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = "/sites.json"

    /**
     * All sites in the catalogue. `by lazy` parses the JSON on first access and caches the result,
     * so repeated reads don't re-parse. Throws if the resource is missing or malformed - which is
     * exactly what the validation test asserts never happens.
     */
    val sites: List<Site> by lazy { load() }

    /** Looks up a site by its [Site.id], or null if no such site exists. */
    fun byId(id: String): Site? = sitesById[id]

    // Index built once for O(1) id lookups.
    private val sitesById: Map<String, Site> by lazy { sites.associateBy { it.id } }

    private fun load(): List<Site> {
        // getResourceAsStream reads a file bundled on the classpath (works from a plain JVM test and
        // from the packaged app alike) - the pure-Kotlin equivalent of an Android asset.
        val stream = SiteCatalogue::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: error("Site catalogue resource not found at $RESOURCE_PATH")
        val text = stream.bufferedReader().use { it.readText() }
        // `Json` (the default format) rejects unknown keys by default, so a stray key in the JSON
        // fails the validation test loudly rather than being silently dropped.
        return Json.decodeFromString<List<Site>>(text)
    }
}
