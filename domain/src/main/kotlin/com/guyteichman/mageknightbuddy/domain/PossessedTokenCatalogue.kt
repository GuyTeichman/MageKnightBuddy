package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

/**
 * The full set of transcribed [PossessedToken]s, loaded once from `possessed-tokens.json` in this
 * module's resources - the same pattern [TokenCatalogue] / [RuinTokenCatalogue] use (ADR-0007), kept
 * a separate catalogue because a possessed token's shape is different (deltas, not a stat block; see
 * [PossessedToken]'s doc comment). The Enemy Picker builds the [TokenPileId.POSSESSED] pile from
 * these and resolves a composite's deltas by id when rendering the summed stats.
 */
object PossessedTokenCatalogue {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = "/possessed-tokens.json"

    /** All tokens in the catalogue. `by lazy` parses the JSON on first access and caches it. */
    val tokens: List<PossessedToken> by lazy { load() }

    /** Looks up a token by its [PossessedToken.id], or null if no such token exists. */
    fun byId(id: String): PossessedToken? = tokensById[id]

    private val tokensById: Map<String, PossessedToken> by lazy { tokens.associateBy { it.id } }

    private fun load(): List<PossessedToken> {
        val stream = PossessedTokenCatalogue::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: error("Possessed token catalogue resource not found at $RESOURCE_PATH")
        val text = stream.bufferedReader().use { it.readText() }
        return Json.decodeFromString<List<PossessedToken>>(text)
    }
}
