package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

// A supertype constructor call can't forward-reference a const declared inside the object body below
// it, so the literal lives here at file scope and the object's own RESOURCE_PATH const mirrors it.
private const val SITES_RESOURCE_PATH = "/sites.json"

/**
 * The full set of transcribed [Site]s, loaded once from the JSON catalogue that ships in this
 * module's resources (`sites.json`), per ADR-0007. This is the source of truth the Sites tab
 * (issue #177) builds its browsable, searchable list from.
 *
 * Loading lives in `domain` (not `data`) precisely so a plain-JVM test can validate the whole
 * catalogue on every `make test` - see `SiteCatalogueTest`, the mandatory mitigation ADR-0007
 * requires for keeping compiler-uncheckable data honest. Mirrors [TokenCatalogue], and shares its
 * load/index/lookup machinery via [JsonCatalogue].
 */
object SiteCatalogue : JsonCatalogue<Site>(SITES_RESOURCE_PATH) {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = SITES_RESOURCE_PATH

    /** All sites in the catalogue. Kept as the existing public name; delegates to [JsonCatalogue.all]. */
    val sites: List<Site> get() = all

    // `Json` (the default format) rejects unknown keys by default, so a stray key in the JSON fails
    // the validation test loudly rather than being silently dropped.
    override fun parse(text: String): List<Site> = Json.decodeFromString(text)

    override fun idOf(item: Site): String = item.id
}
