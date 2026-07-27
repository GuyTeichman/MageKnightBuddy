package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One transcribed enemy/ruin token face - a single static record in the JSON catalogue (ADR-0007),
 * not a drawn instance. The Enemy Picker builds its piles by expanding each token into [copies]
 * identical entries (a pile physically holds several of the same token), so this describes a *type*
 * of token, and [copies] is how many of it the box contains.
 *
 * [id] is the stable key used everywhere a specific token is referenced - pile contents, the Draw
 * Log, and (doubling as the image filename base, ADR-0007) the token art asset. It must be unique
 * across the whole catalogue; the catalogue-validation test enforces that.
 *
 * [resistances] is the set of [AttackElement]s this token resists (empty for most green orcs);
 * [abilities] its whole-token specials; per-attack specials live inside each [EnemyAttack].
 *
 * `@Serializable` so the whole catalogue decodes straight into `List<EnemyToken>` (ADR-0007).
 */
@Serializable
data class EnemyToken(
    val id: String,
    val name: String,
    val pile: TokenPileId,
    val expansion: Expansion,
    val copies: Int,
    val armor: Int,
    val fame: Int,
    val attacks: List<EnemyAttack> = emptyList(),
    val resistances: Set<AttackElement> = emptySet(),
    val abilities: Set<EnemyAbility> = emptySet(),
)
