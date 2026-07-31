package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One transcribed hexagonal Ruin token (docs/rules/enemy-tokens.md's "Ruin tokens" section) - the
 * RUIN pile's counterpart to [EnemyToken]. Physically these are a different shape of component to
 * round enemy tokens and print no armor/attack/fame block; instead each is one of two kinds
 * (rulebook "Revealing Ruins"):
 *
 * - **Ancient Altar**: pay mana to gain Fame; no combat. [altarColors] says which mana, by a
 *   deliberate size convention (spelled out so no reader has to infer it):
 *     - **size 1** (base game): pay **three** mana of that one colour -> **7 Fame**.
 *     - **size 4** (Lost Legion): pay **one** mana of *each* of the four listed colours -> **10 Fame**.
 *   The Fame itself isn't stored - it's derivable from the size, and the Enemy Picker never scores
 *   it anyway (ADR-0006); the UI derives the prompt text from [altarColors].
 * - **Enemies With Treasure**: draw one token from each pile listed in [enemyPiles] (in order) and
 *   fight them all, then claim the printed [reward] if you defeat them. A pile may appear more than
 *   once - that means "draw that many from it" (e.g. base game's `ruin_green_green` = two draws from
 *   Green). This list shape maps straight onto [EnemyPickerSession.summon]'s `pileIds`, which is how
 *   the drawn enemies get attached to this ruin as a group. [reward] is flavour/reference text only:
 *   the picker tracks pile/draw state, never rewards (ADR-0006, amended to *display* the printed
 *   reward without tracking or scoring it).
 *
 * Exactly one of [altarColors] or [enemyPiles] is set per token (the other is null); the catalogue-
 * validation test ([RuinTokenCatalogueTest]) enforces that XOR plus the altar size convention. This
 * mirrors how [EnemyAttack] tells a numeric attack from a Summon apart with nullable fields rather
 * than a sealed type: it keeps the JSON flat and reuses the same "catalogue-validation test enforces
 * the invariant" approach, instead of teaching kotlinx-serialization a second polymorphic shape.
 *
 * [expansion] gates the token by the per-game Token Set exactly like [EnemyToken.expansion] does, so
 * a base-game session never shuffles in an expansion's ruins.
 *
 * `@Serializable` so the whole catalogue decodes straight into `List<RuinToken>`, the same
 * JSON-in-`domain/`-resources approach [EnemyToken] uses (ADR-0007).
 */
@Serializable
data class RuinToken(
    val id: String,
    val expansion: Expansion,
    val altarColors: List<ManaColor>? = null,
    val enemyPiles: List<TokenPileId>? = null,
    val reward: String? = null,
) {
    /** Whether this token is an Ancient Altar (mana-for-Fame) rather than an Enemies With Treasure draw. */
    val isAltar: Boolean get() = altarColors != null
}
