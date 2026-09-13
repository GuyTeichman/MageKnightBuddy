package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

// A supertype constructor call can't forward-reference a const declared inside the object body below
// it, so the literal lives here at file scope and the object's own RESOURCE_PATH const mirrors it.
private const val FACTION_REWARD_TOKENS_RESOURCE_PATH = "/faction-reward-tokens.json"

/**
 * The full set of transcribed [FactionRewardToken]s, loaded once from `faction-reward-tokens.json`
 * in this module's resources - the same pattern [TokenCatalogue] / [RuinTokenCatalogue] use
 * (ADR-0007), kept as its own catalogue because a reward token's shape differs from an enemy's or a
 * ruin's (see [FactionRewardToken]'s doc comment). Shares its load/index/lookup machinery with the
 * other catalogue objects via [JsonCatalogue].
 */
object FactionRewardTokenCatalogue : JsonCatalogue<FactionRewardToken>(FACTION_REWARD_TOKENS_RESOURCE_PATH) {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = FACTION_REWARD_TOKENS_RESOURCE_PATH

    /** All tokens in the catalogue. Kept as the existing public name; delegates to [JsonCatalogue.all]. */
    val tokens: List<FactionRewardToken> get() = all

    override fun parse(text: String): List<FactionRewardToken> = Json.decodeFromString(text)

    override fun idOf(item: FactionRewardToken): String = item.id
}
