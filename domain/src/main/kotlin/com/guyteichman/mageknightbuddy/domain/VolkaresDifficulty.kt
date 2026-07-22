package com.guyteichman.mageknightbuddy.domain

/**
 * How tough Volkare and the city are (docs/rules/volkares-return.md and docs/rules/volkares-quest.md,
 * both "Scenario difficulty"): one of the two independent difficulty axes chosen before setup,
 * shared by both Volkare scenarios. [combatBonusBase] is the starting value of the Volkare Combat
 * Bonus (Scoring, step 4) for that level - each scenario's own City Level / Volkare's Level setup
 * values still aren't modeled here (out of scope until a future feature needs them - see ADR-0004).
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
 * with no Double rounding surprises - e.g. Tight's 1.5x is 3/2. This level also drives the default
 * number of Wounds shuffled into Volkare's deck at setup - see [volkareWoundCount] below, which
 * varies by scenario as well as by this level (so it isn't a plain property on this enum).
 */
enum class RaceLevel(
    val combatBonusMultiplierNumerator: Int,
    val combatBonusMultiplierDenominator: Int,
) {
    FAIR(1, 1),
    TIGHT(3, 2),
    THRILLING(2, 1),
}

/**
 * Default number of Wounds shuffled into Volkare's deck at setup, by [Scenario] and [RaceLevel] -
 * docs/rules/volkares-return.md / volkares-quest.md's "Scenario difficulty" tables (solo column;
 * Epic variant for Volkare's Return, the only variant this app currently models - Blitz uses its
 * own, lower table and isn't modeled). The two scenarios share the same three [RaceLevel] labels
 * but scale to different wound counts, so this can't live as a plain [RaceLevel] property - it's a
 * per-(scenario, raceLevel) lookup instead. [VolkareSession.start] only takes this as a *default*:
 * the setup screen lets the player override it with a custom count, so nothing else in the domain
 * layer assumes this value was actually used.
 */
fun volkareWoundCount(scenario: Scenario, raceLevel: RaceLevel): Int = when (scenario) {
    Scenario.VolkaresReturn -> when (raceLevel) {
        RaceLevel.FAIR -> 18
        RaceLevel.TIGHT -> 15
        RaceLevel.THRILLING -> 12
    }
    Scenario.VolkaresQuest -> when (raceLevel) {
        RaceLevel.FAIR -> 20
        RaceLevel.TIGHT -> 16
        RaceLevel.THRILLING -> 12
    }
    else -> throw IllegalArgumentException(
        "Volkare's Wound count is only defined for Volkare's Return/Quest, got $scenario",
    )
}
