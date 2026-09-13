package com.guyteichman.mageknightbuddy.domain

/**
 * Shared base for the module's JSON-backed catalogue objects (per ADR-0007): [TokenCatalogue],
 * [RuinTokenCatalogue], [PossessedTokenCatalogue], [FactionRewardTokenCatalogue] and [SiteCatalogue]
 * all load a list of items from a JSON file bundled in this module's resources, then index that list
 * by id for fast [byId] lookups. This class holds that shared load-and-index logic so each catalogue
 * only has to supply its resource path and how to decode/identify its own item type.
 *
 * Subclasses must implement [parse] and [idOf] rather than this class doing the decoding itself:
 * kotlinx serialization's `Json.decodeFromString<List<T>>(...)` needs a *reified* type parameter (the
 * actual class, not just `T` erased at runtime), which a generic function on an abstract class can't
 * provide - only a concrete subtype calling `decodeFromString` with its own real type can.
 */
abstract class JsonCatalogue<T>(private val resourcePath: String) {

    /**
     * All items in the catalogue, in file order. `by lazy` parses the JSON on first access and
     * caches the result, so repeated reads (pile builds, info lookups, list screens) don't re-parse.
     * Throws if the resource is missing or malformed - which is exactly what each catalogue's
     * validation test asserts never happens.
     */
    val all: List<T> by lazy { load() }

    // Index built once for O(1) id lookups (info windows / draw logs / search screens resolve ids
    // constantly). `associateBy` turns the list into a Map keyed by whatever idOf(item) returns.
    private val index: Map<String, T> by lazy { all.associateBy { idOf(it) } }

    /** Looks up an item by id, or null if no item with that id exists in the catalogue. */
    fun byId(id: String): T? = index[id]

    /**
     * Decodes this catalogue's JSON text into a list of [T]. Each subtype implements this as a
     * one-line `Json.decodeFromString(text)` call - see this class's doc comment for why the base
     * class can't do the decoding generically.
     */
    protected abstract fun parse(text: String): List<T>

    /** Returns the id [byId] should index [item] under (typically its `id` field). */
    protected abstract fun idOf(item: T): String

    private fun load(): List<T> {
        // getResourceAsStream reads a file bundled on the classpath (works from a plain JVM test and
        // from the packaged app alike) - the pure-Kotlin equivalent of an Android asset. `javaClass`
        // resolves to the calling subclass's runtime class (e.g. TokenCatalogue), which is fine here
        // since resourcePath is an absolute ("/...") classpath location.
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: error("Catalogue resource not found at $resourcePath")
        val text = stream.bufferedReader().use { it.readText() }
        return parse(text)
    }
}
