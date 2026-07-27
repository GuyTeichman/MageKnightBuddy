package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * A modifier attached to a *single* [EnemyAttack] (as opposed to a whole-enemy [EnemyAbility]).
 * Names and meanings come from the Quick Reference Sheet's "Enemy Token Abilities > Offensive"
 * section. An attack can carry several at once (e.g. a Brutal Poison attack), hence
 * [EnemyAttack.modifiers] is a `Set`.
 *
 * `@Serializable` for the JSON catalogue (ADR-0007); the enum name is what appears in JSON.
 */
@Serializable
enum class AttackModifier {
    /** Needs twice as much Block as its Attack value to block (Quick Reference Sheet, Swift). */
    SWIFT,

    /** If unblocked, deals twice its Attack value in damage (Quick Reference Sheet, Brutal). */
    BRUTAL,

    /** A wounded Unit/Hero gets a second Wound (into the discard pile) (Quick Reference Sheet, Poison). */
    POISON,

    /** A wounded Unit is destroyed; a wounded Hero must discard non-Wound cards (Quick Reference Sheet, Paralyze). */
    PARALYZE,

    /** Unblocked damage cannot be assigned to Units, only the Hero (Quick Reference Sheet, Assassination). */
    ASSASSINATION,

    /** May be reduced by spending Move points in the Block phase (Quick Reference Sheet, Cumbersome). */
    CUMBERSOME,

    /**
     * Instead of attacking, at the start of the Block phase a random brown token is drawn to
     * replace this enemy for the Block/Assign-Damage phases (Quick Reference Sheet, Summon). In the
     * Enemy Picker this is a Summon Draw - an explicit action, not an automatic reveal (see
     * `CONTEXT.md`'s "Summon Draw"); not exercised by the green pile.
     */
    SUMMON,
}
