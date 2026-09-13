package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

// A supertype constructor call can't forward-reference a const declared inside the object body below
// it, so the literal lives here at file scope and the object's own RESOURCE_PATH const mirrors it.
private const val RUIN_TOKENS_RESOURCE_PATH = "/ruin-tokens.json"

/**
 * The full set of transcribed [RuinToken]s, loaded once from `ruin-tokens.json` in this module's
 * resources - the same pattern [TokenCatalogue] uses for [EnemyToken] (ADR-0007), kept as a
 * separate catalogue because a Ruin token's shape is different (no armor/attack/fame block; see
 * [RuinToken]'s doc comment). Shares its load/index/lookup machinery with the other catalogue
 * objects via [JsonCatalogue].
 */
object RuinTokenCatalogue : JsonCatalogue<RuinToken>(RUIN_TOKENS_RESOURCE_PATH) {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = RUIN_TOKENS_RESOURCE_PATH

    /** All tokens in the catalogue. Kept as the existing public name; delegates to [JsonCatalogue.all]. */
    val tokens: List<RuinToken> get() = all

    override fun parse(text: String): List<RuinToken> = Json.decodeFromString(text)

    override fun idOf(item: RuinToken): String = item.id
}
