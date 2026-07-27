package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * A whole-token **offensive** special ability - one that modifies the enemy's attack(s). Names and
 * meanings come from the Quick Reference Sheet's "Enemy Token Abilities > Offensive" section.
 *
 * Crucially these are *token-level*, not per-attack: per the Lost Legion "Multiple Attacks" rule an
 * enemy's abilities apply to **every** attack it makes, so a two-attack Brutal enemy is Brutal on
 * both. That's why they live in a `Set` on [EnemyToken] rather than on individual [EnemyAttack]s
 * (which carry only [value][EnemyAttack.value] / [element][EnemyAttack.element] / summon). When a
 * summoner draws a token to fight in its place, that summoned token likewise applies *its own*
 * offensive abilities to its attack(s) - see `CONTEXT.md`'s "Summon Draw" and issue #191.
 *
 * `@Serializable` for the JSON catalogue (ADR-0007); the enum name is what appears in JSON.
 */
@Serializable
enum class OffensiveAbility {
    /** Needs twice as much Block as its Attack value to block (Quick Reference Sheet, Swift). */
    SWIFT,

    /** If unblocked, deals twice its Attack value in damage (Quick Reference Sheet, Brutal). */
    BRUTAL,

    /** A wounded figure takes a second Wound, into the discard pile (Quick Reference Sheet, Poison). */
    POISON,

    /** A wounded Unit is destroyed; a wounded Hero discards non-Wound cards (Quick Reference Sheet, Paralyze). */
    PARALYZE,

    /** May be reduced by spending Move points during the Block phase (Quick Reference Sheet, Cumbersome). */
    CUMBERSOME,

    /** Unblocked damage must all be assigned to the Hero, never Units (Quick Reference Sheet, Assassination). */
    ASSASSINATION,

    /** Gains +1 Armor per Unit its attacks wound / per Wound it deals (Quick Reference Sheet, Vampiric). */
    VAMPIRIC,
}
