package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * Which product a token comes from, used by the Enemy Picker's [Token Set][EnemyPickerSession] to
 * decide which [EnemyToken]s make up this game's piles (see `CONTEXT.md`'s "Token Set"). A
 * per-game setup choice, deliberately distinct from the future global [Settings] "which expansions
 * do I own" toggle.
 *
 * `@Serializable` so it can tag each [EnemyToken] in the JSON catalogue (ADR-0007). Only [BASE] has
 * tokens transcribed so far (the green-pile vertical slice, issue #178); the rest are listed ahead
 * of their tokens being added.
 */
@Serializable
enum class Expansion {
    BASE,
    LOST_LEGION,
    SHADES_OF_TEZLA,
    APOCALYPSE_DRAGON,
}
