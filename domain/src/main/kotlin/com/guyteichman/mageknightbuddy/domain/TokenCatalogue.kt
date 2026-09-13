package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.json.Json

// A supertype constructor call can't forward-reference a const declared inside the object body below
// it, so the literal lives here at file scope and the object's own RESOURCE_PATH const mirrors it.
private const val ENEMY_TOKENS_RESOURCE_PATH = "/enemy-tokens.json"

/**
 * The full set of transcribed [EnemyToken]s, loaded once from the JSON catalogue that ships in this
 * module's resources (`enemy-tokens.json`), per ADR-0007. This is the source of truth the Enemy
 * Picker builds its piles from and the info window reads abilities from.
 *
 * Loading lives in `domain` (not `data`) precisely so a plain-JVM test can validate the whole
 * catalogue on every `make test` - see `TokenCatalogueTest`, the mandatory mitigation ADR-0007
 * requires for keeping compiler-uncheckable data honest. The load/index/lookup machinery itself is
 * shared with the other catalogue objects via [JsonCatalogue].
 */
object TokenCatalogue : JsonCatalogue<EnemyToken>(ENEMY_TOKENS_RESOURCE_PATH) {
    /** Classpath location of the catalogue JSON, relative to this module's resources root. */
    const val RESOURCE_PATH = ENEMY_TOKENS_RESOURCE_PATH

    /** All tokens in the catalogue. Kept as the existing public name; delegates to [JsonCatalogue.all]. */
    val tokens: List<EnemyToken> get() = all

    // `Json` (the default format) rejects unknown keys by default, so a stray key in the JSON fails
    // the validation test loudly rather than being silently dropped - exactly what we want.
    override fun parse(text: String): List<EnemyToken> = Json.decodeFromString(text)

    override fun idOf(item: EnemyToken): String = item.id
}
