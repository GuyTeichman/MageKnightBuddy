package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One attack printed on an [EnemyToken]. Most enemies have exactly one; some have several, each
 * blocked or assigned separately (Quick Reference Sheet, "Multiple Attacks").
 *
 * A normal attack carries a [value] (its printed Attack number) and an [element]. A **Summon**
 * attack is different: instead of dealing damage it draws a token to fight in its place, so it has
 * no attack value ([value] is `null`) and names the pile it draws from in [summons] (Quick Reference
 * Sheet, "Summon"; see `CONTEXT.md`'s "Summon Draw"). Base-game summoners draw from the brown pile,
 * but the summoned pile is data, not a constant, since possessed/expansion summoners can draw from
 * other colours - which is exactly why it's a per-attack [TokenPileId] rather than a bare flag.
 *
 * Note there are **no per-attack modifiers** here: offensive abilities like Brutal/Swift/Poison are
 * whole-*token* traits ([OffensiveAbility] on [EnemyToken]) because the rules apply them to every
 * one of an enemy's attacks (Lost Legion, "Multiple Attacks"). The [element], however, genuinely is
 * per-attack - a multi-attack enemy can mix a physical and a magical attack - so it stays here.
 *
 * Invariant (enforced by the catalogue-validation test): exactly one of [value] / [summons] is set
 * - an attack either has a numeric value or is a summon, never both and never neither.
 *
 * `@Serializable` for the JSON catalogue (ADR-0007). [element] and [summons] default, so a plain
 * physical attack is just `{"value": 4}` and a summon is `{"summons": "BROWN"}`.
 */
@Serializable
data class EnemyAttack(
    val value: Int? = null,
    val element: AttackElement = AttackElement.PHYSICAL,
    val summons: TokenPileId? = null,
) {
    /** Whether this attack summons a replacement token instead of dealing its own damage. */
    val isSummon: Boolean get() = summons != null
}
