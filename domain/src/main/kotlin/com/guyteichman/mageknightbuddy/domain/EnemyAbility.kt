package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * A *token-level* special ability, i.e. one that describes the whole enemy rather than a single
 * one of its attacks (per-attack modifiers live in [AttackModifier] instead). Names and meanings
 * come from the Quick Reference Sheet's "Enemy Token Abilities > Defensive" section, plus Vampiric
 * (listed there under Offensive, but it modifies the enemy's own Armor, so it belongs here). The
 * info window ("?" on a drawn token) renders these with their rulebook descriptions.
 *
 * Element resistances are *not* here - they are [EnemyToken.resistances], a `Set<AttackElement>`,
 * since "resists Fire" and "resists Ice" are the same ability parameterised by element. The one
 * *valued* defensive ability, Defense (which raises the first attacked enemy's Armor by a printed
 * number), is deliberately omitted until a transcribed token needs it, since an enum entry can't
 * carry that per-token value - see ADR-0007's "the schema will grow" note.
 *
 * `@Serializable` for the JSON catalogue (ADR-0007); the enum name is what appears in JSON.
 */
@Serializable
enum class EnemyAbility {
    /** Only Siege attacks reach it in the Ranged & Siege phase (Quick Reference Sheet, Fortified). */
    FORTIFIED,

    /** Ignores all site fortifications (Quick Reference Sheet, Unfortified). */
    UNFORTIFIED,

    /** Higher Armor value except when all its attacks are blocked (Quick Reference Sheet, Elusive). */
    ELUSIVE,

    /** Unaffected by any non-Attack/Block effects (Quick Reference Sheet, Arcane Immunity). */
    ARCANE_IMMUNITY,

    /** Gains +1 Armor per Unit its attacks wound / per Wound it deals (Quick Reference Sheet, Vampiric). */
    VAMPIRIC,
}
