package com.guyteichman.mageknightbuddy.ui.enemypicker

import com.guyteichman.mageknightbuddy.domain.AttackElement
import com.guyteichman.mageknightbuddy.domain.AttackModifier
import com.guyteichman.mageknightbuddy.domain.EnemyAbility

/**
 * User-facing display text for the enemy info window ("?" on a drawn token). Kept in the UI layer,
 * not the domain, since these are player-facing descriptions - the wording is condensed from the
 * Quick Reference Sheet's "Enemy Token Abilities" section. (Ability *icons* extracted from the same
 * sheet are a later art follow-up; text stands in until then.)
 */

/** Short human title for an [AttackElement], e.g. for an attack chip. */
internal fun AttackElement.displayName(): String = when (this) {
    AttackElement.PHYSICAL -> "Physical"
    AttackElement.FIRE -> "Fire"
    AttackElement.ICE -> "Ice"
    AttackElement.COLD_FIRE -> "Cold Fire"
}

/** Title + one-line rules description for a whole-token [EnemyAbility]. */
internal fun EnemyAbility.describe(): Pair<String, String> = when (this) {
    EnemyAbility.FORTIFIED -> "Fortified" to "Only Siege attacks can be used against it in the Ranged & Siege phase."
    EnemyAbility.UNFORTIFIED -> "Unfortified" to "Ignores all site fortifications."
    EnemyAbility.ELUSIVE -> "Elusive" to "Uses the higher Armor value unless all of its attacks are blocked."
    EnemyAbility.ARCANE_IMMUNITY -> "Arcane Immunity" to "Unaffected by any non-Attack/Block effects."
    EnemyAbility.VAMPIRIC -> "Vampiric" to "Gains +1 Armor for each Unit it wounds and each Wound it deals."
}

/** Title + one-line rules description for a per-attack [AttackModifier]. */
internal fun AttackModifier.describe(): Pair<String, String> = when (this) {
    AttackModifier.SWIFT -> "Swift" to "Needs twice as much Block as its Attack value to block."
    AttackModifier.BRUTAL -> "Brutal" to "If unblocked, deals twice its Attack value in damage."
    AttackModifier.POISON -> "Poison" to "A wounded figure takes a second Wound (into the discard pile)."
    AttackModifier.PARALYZE -> "Paralyze" to "A wounded Unit is destroyed; a wounded Hero discards non-Wound cards."
    AttackModifier.ASSASSINATION -> "Assassination" to "Unblocked damage must all be assigned to the Hero, never Units."
    AttackModifier.CUMBERSOME -> "Cumbersome" to "May be reduced by spending Move points during the Block phase."
}
