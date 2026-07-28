package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

/**
 * The full set of transcribed [RuinToken]s, loaded once from `ruin-tokens.json` in this module's
 * resources - the same pattern [TokenCatalogue] uses for [EnemyToken] (ADR-0007), kept as a
 * separate catalogue because a Ruin token's shape is different (no armor/attack/fame block; see
 * [RuinToken]'s doc comment).
 */
object RuinTokenCatalogue {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = "/ruin-tokens.json"

    /** All tokens in the catalogue. `by lazy` parses the JSON on first access and caches it. */
    val tokens: List<RuinToken> by lazy { load() }

    /** Looks up a token by its [RuinToken.id], or null if no such token exists. */
    fun byId(id: String): RuinToken? = tokensById[id]

    private val tokensById: Map<String, RuinToken> by lazy { tokens.associateBy { it.id } }

    private fun load(): List<RuinToken> {
        val stream = RuinTokenCatalogue::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: error("Ruin token catalogue resource not found at $RESOURCE_PATH")
        val text = stream.bufferedReader().use { it.readText() }
        return Json.decodeFromString<List<RuinToken>>(text)
    }
}
