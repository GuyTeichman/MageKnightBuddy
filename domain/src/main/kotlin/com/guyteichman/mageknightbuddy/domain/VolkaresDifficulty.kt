package com.guyteichman.mageknightbuddy.domain

/**
 * How tough Volkare and the city are (docs/rules/volkares-return.md and docs/rules/volkares-quest.md,
 * both "Scenario difficulty"): one of the two independent difficulty axes chosen before setup,
 * shared by both Volkare scenarios - only the Combat Bonus base value matters for scoring; each
 * scenario's own City Level / Volkare's Level setup values aren't modeled here. [combatBonusBase]
 * is the starting value of the Volkare Combat Bonus (Scoring, step 4) for that level.
 */
enum class CombatLevel(val combatBonusBase: Int) {
    DARING(30),
    HEROIC(40),
    LEGENDARY(50),
}

/**
 * How much time pressure Volkare exerts (docs/rules/volkares-return.md and
 * docs/rules/volkares-quest.md, both "Scenario difficulty"): the other of the two independent
 * difficulty axes, shared by both Volkare scenarios ("Identical mechanic" per
 * docs/rules/volkares-quest.md's Scoring section). [combatBonusMultiplierNumerator] and
 * [combatBonusMultiplierDenominator] together represent this level's multiplier (1 / 1.5 / 2) on
 * the Volkare Combat Bonus as an exact fraction, so the multiplication can stay in integer math
 * with no Double rounding surprises - e.g. Tight's 1.5x is 3/2.
 */
enum class RaceLevel(
    val combatBonusMultiplierNumerator: Int,
    val combatBonusMultiplierDenominator: Int,
) {
    FAIR(1, 1),
    TIGHT(3, 2),
    THRILLING(2, 1),
}
