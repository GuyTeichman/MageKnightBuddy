package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * Identifies one face-down [Token Pile][EnemyPickerSession] the Enemy Picker draws from. These are
 * the uniform "draw one, discard it" piles sorted by token back (rulebook p.3, "seven face down
 * piles"): the six enemy colors plus the hexagonal ruin pile. Possessed-enemy and faction-token
 * piles are deliberately *not* here - they have different lifecycles (composite tokens / held
 * inventory) and are added in a later PR (see `CONTEXT.md`'s "Possessed Enemy"/"Faction Token").
 *
 * `@Serializable` so this can be a field on [EnemyToken] in the JSON catalogue (ADR-0007); the
 * enum name is what appears in the JSON.
 */
@Serializable
enum class TokenPileId {
    /** Marauding Orcs and other green enemies - the base game's simplest pile. */
    GREEN,
    GREY,
    VIOLET,
    BROWN,
    RED,
    WHITE,

    /** Hexagonal ruin tokens (altars/enemies drawn at ancient ruins), a non-enemy pile. */
    RUIN,
}
