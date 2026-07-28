package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One transcribed hexagonal Ruin token (docs/rules/enemy-tokens.md's "Ruin tokens" section) - the
 * RUIN pile's counterpart to [EnemyToken]. Physically these are a different shape of component to
 * round enemy tokens and print no armor/attack/fame block; instead each is one of two kinds
 * (rulebook "Revealing Ruins"):
 *
 * - **Ancient Altar**: pay three mana of [altarColor] to gain 7 Fame; no combat.
 * - **Enemies With Treasure**: draw one token each from [firstPile] and [secondPile] (the same
 *   pile twice is possible - some tokens depict two draws from one colour) and fight both. The
 *   reward for defeating them both is printed on the token too, but isn't modelled here: the Enemy
 *   Picker only tracks pile/draw state, never rewards or Fame (ADR-0006), so that stays flavour
 *   text in the rules doc rather than a domain field.
 *
 * Exactly one of [altarColor] or ([firstPile] and [secondPile]) is set per token. This mirrors how
 * [EnemyAttack] tells a numeric attack from a Summon apart with nullable fields rather than a
 * sealed type: it keeps the JSON flat and reuses the same "catalogue-validation test enforces the
 * invariant" approach already established there, instead of teaching kotlinx-serialization a second
 * polymorphic shape just for this one type.
 *
 * `@Serializable` so the whole catalogue decodes straight into `List<RuinToken>`, the same
 * JSON-in-`domain/`-resources approach [EnemyToken] uses (ADR-0007).
 */
@Serializable
data class RuinToken(
    val id: String,
    val altarColor: ManaColor? = null,
    val firstPile: TokenPileId? = null,
    val secondPile: TokenPileId? = null,
) {
    /** Whether this token is an Ancient Altar (mana-for-Fame) rather than an Enemies With Treasure draw. */
    val isAltar: Boolean get() = altarColor != null
}
