package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * A whole-token **defensive** special ability - one that governs how the enemy is *attacked* rather
 * than how it attacks (per-attack numbers live on [EnemyAttack]; offensive abilities live in
 * [OffensiveAbility]). Names and meanings come from the Quick Reference Sheet's "Enemy Token
 * Abilities > Defensive" section. Like every ability it applies to the enemy as a whole, so it is
 * held in a `Set` on [EnemyToken].
 *
 * Element resistances are deliberately *not* here - they are [EnemyToken.resistances], a
 * `Set<AttackElement>`, since "resists Fire" and "resists Ice" are the same ability parameterised by
 * element, which an enum entry can't carry. The one *valued* defensive ability, Defense (which
 * raises the first attacked enemy's Armor by a printed number), is likewise omitted until a
 * transcribed token needs it - see ADR-0007.
 *
 * `@Serializable` for the JSON catalogue (ADR-0007); the enum name is what appears in JSON.
 */
@Serializable
enum class DefensiveAbility {
    /** Only Siege attacks reach it in the Ranged & Siege phase (Quick Reference Sheet, Fortified). */
    FORTIFIED,

    /** Higher Armor value except when all its attacks are blocked (Quick Reference Sheet, Elusive). */
    ELUSIVE,

    /** Unaffected by any non-Attack/Block effects (Quick Reference Sheet, Arcane Immunity). */
    ARCANE_IMMUNITY,
}
