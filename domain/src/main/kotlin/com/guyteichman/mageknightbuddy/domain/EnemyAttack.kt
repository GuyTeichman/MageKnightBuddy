package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One attack printed on an [EnemyToken]. Most enemies have exactly one; some have several, each
 * blocked or assigned separately (Quick Reference Sheet, "Multiple Attacks"). [value] is the
 * printed Attack number, [element] its damage type, and [modifiers] any per-attack special rules
 * (Brutal, Swift, ...). A Summon attack (see [AttackModifier.SUMMON]) has no meaningful [value]
 * since it draws a replacement instead - it carries `value = 0` by convention.
 *
 * `@Serializable` for the JSON catalogue (ADR-0007). [element] and [modifiers] default so the
 * common case (a plain physical attack) can be written as just `{"value": 4}` in JSON.
 */
@Serializable
data class EnemyAttack(
    val value: Int,
    val element: AttackElement = AttackElement.PHYSICAL,
    val modifiers: Set<AttackModifier> = emptySet(),
)
