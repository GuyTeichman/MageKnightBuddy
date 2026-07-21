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

// Fame awarded per card still left in Volkare's deck, counted before the Race Level multiplier
// is applied (both scenarios' Scoring step 4).
private const val COMBAT_BONUS_PER_REMAINING_CARD = 2

/**
 * The Volkare Combat Bonus (both scenarios' Scoring step 4): this Combat Level's base value plus
 * 2 Fame per card still left in Volkare's deck, with that whole subtotal multiplied by this Race
 * Level's factor. Shared by [VolkaresQuestScoring] and [VolkaresReturnScoring] since both
 * scenarios compute it identically - only what feeds into [cardsRemaining] differs per scenario.
 */
fun CombatLevel.volkareCombatBonus(raceLevel: RaceLevel, cardsRemaining: Int): Int {
    val subtotal = combatBonusBase + COMBAT_BONUS_PER_REMAINING_CARD * cardsRemaining
    // Integer numerator/denominator arithmetic (rather than a Double multiplier) keeps this
    // exact - see RaceLevel's KDoc for why subtotal * numerator is always evenly divisible.
    return subtotal * raceLevel.combatBonusMultiplierNumerator / raceLevel.combatBonusMultiplierDenominator
}
