package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * The element of an enemy [EnemyAttack], and equally the element a token can resist (see
 * [EnemyToken.resistances]). Mirrors the four attack/resistance types the Quick Reference Sheet
 * defines: plain physical plus the three magical elements. Cold Fire is its own element - it is
 * resisted only by having *both* Fire and Ice resistance (Quick Reference Sheet, "Both Fire and
 * Ice Resistance").
 *
 * `@Serializable` for the JSON catalogue (ADR-0007).
 */
@Serializable
enum class AttackElement {
    PHYSICAL,
    FIRE,
    ICE,
    COLD_FIRE,
}
