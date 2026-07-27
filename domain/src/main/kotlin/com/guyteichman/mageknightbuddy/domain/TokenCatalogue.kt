package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

/**
 * The full set of transcribed [EnemyToken]s, loaded once from the JSON catalogue that ships in this
 * module's resources (`enemy-tokens.json`), per ADR-0007. This is the source of truth the Enemy
 * Picker builds its piles from and the info window reads abilities from.
 *
 * Loading lives in `domain` (not `data`) precisely so a plain-JVM test can validate the whole
 * catalogue on every `make test` - see `TokenCatalogueTest`, the mandatory mitigation ADR-0007
 * requires for keeping compiler-uncheckable data honest.
 */
object TokenCatalogue {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = "/enemy-tokens.json"

    /**
     * All tokens in the catalogue. `by lazy` parses the JSON on first access and caches the result,
     * so repeated reads (pile builds, info lookups) don't re-parse. Throws if the resource is
     * missing or malformed - which is exactly what the validation test asserts never happens.
     */
    val tokens: List<EnemyToken> by lazy { load() }

    /** Looks up a token by its [EnemyToken.id], or null if no such token exists. */
    fun byId(id: String): EnemyToken? = tokensById[id]

    // Index built once for O(1) id lookups (the info window / Draw Log resolve ids constantly).
    private val tokensById: Map<String, EnemyToken> by lazy { tokens.associateBy { it.id } }

    private fun load(): List<EnemyToken> {
        // getResourceAsStream reads a file bundled on the classpath (works from a plain JVM test
        // and from the packaged app alike) - the pure-Kotlin equivalent of an Android asset.
        val stream = TokenCatalogue::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: error("Token catalogue resource not found at $RESOURCE_PATH")
        val text = stream.bufferedReader().use { it.readText() }
        // `Json` (the default format) rejects unknown keys by default, so a stray key in the JSON
        // fails the validation test loudly rather than being silently dropped - exactly what we want.
        return Json.decodeFromString<List<EnemyToken>>(text)
    }
}
