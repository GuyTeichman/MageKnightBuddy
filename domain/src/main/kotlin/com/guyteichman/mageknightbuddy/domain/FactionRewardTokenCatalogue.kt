package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

/**
 * The full set of transcribed [FactionRewardToken]s, loaded once from `faction-reward-tokens.json`
 * in this module's resources - the same pattern [TokenCatalogue] / [RuinTokenCatalogue] use
 * (ADR-0007), kept as its own catalogue because a reward token's shape differs from an enemy's or a
 * ruin's (see [FactionRewardToken]'s doc comment).
 */
object FactionRewardTokenCatalogue {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = "/faction-reward-tokens.json"

    /** All tokens in the catalogue. `by lazy` parses the JSON on first access and caches it. */
    val tokens: List<FactionRewardToken> by lazy { load() }

    /** Looks up a token by its [FactionRewardToken.id], or null if no such token exists. */
    fun byId(id: String): FactionRewardToken? = tokensById[id]

    private val tokensById: Map<String, FactionRewardToken> by lazy { tokens.associateBy { it.id } }

    private fun load(): List<FactionRewardToken> {
        val stream = FactionRewardTokenCatalogue::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: error("Faction reward token catalogue resource not found at $RESOURCE_PATH")
        val text = stream.bufferedReader().use { it.readText() }
        return Json.decodeFromString<List<FactionRewardToken>>(text)
    }
}
