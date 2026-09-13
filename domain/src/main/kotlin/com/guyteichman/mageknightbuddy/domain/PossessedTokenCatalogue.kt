package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

// A supertype constructor call can't forward-reference a const declared inside the object body below
// it, so the literal lives here at file scope and the object's own RESOURCE_PATH const mirrors it.
private const val POSSESSED_TOKENS_RESOURCE_PATH = "/possessed-tokens.json"

/**
 * The full set of transcribed [PossessedToken]s, loaded once from `possessed-tokens.json` in this
 * module's resources - the same pattern [TokenCatalogue] / [RuinTokenCatalogue] use (ADR-0007), kept
 * a separate catalogue because a possessed token's shape is different (deltas, not a stat block; see
 * [PossessedToken]'s doc comment). The Enemy Picker builds the [TokenPileId.POSSESSED] pile from
 * these and resolves a composite's deltas by id when rendering the summed stats. Shares its
 * load/index/lookup machinery with the other catalogue objects via [JsonCatalogue].
 */
object PossessedTokenCatalogue : JsonCatalogue<PossessedToken>(POSSESSED_TOKENS_RESOURCE_PATH) {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = POSSESSED_TOKENS_RESOURCE_PATH

    /** All tokens in the catalogue. Kept as the existing public name; delegates to [JsonCatalogue.all]. */
    val tokens: List<PossessedToken> get() = all

    override fun parse(text: String): List<PossessedToken> = Json.decodeFromString(text)

    override fun idOf(item: PossessedToken): String = item.id
}
