package com.guyteichman.mageknightbuddy.domain

// +20 points if the city was conquered (docs/rules/volkares-return.md, Scoring, step 3).
private const val CITY_CONQUERED_BONUS = 20

// +2 points per card still left in Volkare's deck when he was defeated (Scoring, step 4).
private const val POINTS_PER_CARD_LEFT_IN_VOLKARES_DECK = 2

/**
 * How tough Volkare and the city are (docs/rules/volkares-return.md, "Scenario difficulty"):
 * one of the two independent difficulty axes chosen before setup. [combatBonusBase] is the
 * starting value of the Volkare Combat Bonus (Scoring, step 4) for that level.
 */
enum class CombatLevel(val combatBonusBase: Int) {
    DARING(30),
    HEROIC(40),
    LEGENDARY(50),
}

/**
 * How much time pressure Volkare exerts (docs/rules/volkares-return.md, "Scenario difficulty"):
 * the other of the two independent difficulty axes. [combatBonusMultiplierNumerator] represents
 * this level's multiplier (1 / 1.5 / 2) on the Volkare Combat Bonus, expressed as a numerator
 * over a denominator of 2 so the multiplication can stay in exact integer math (no Double
 * rounding surprises) - e.g. Tight's 1.5x is numerator 3 over denominator 2.
 */
enum class RaceLevel(val combatBonusMultiplierNumerator: Int) {
    FAIR(2),
    TIGHT(3),
    THRILLING(4),
}

/**
 * Everything the player enters at the end of a Volkare's Return session, matching the inputs
 * docs/rules/volkares-return.md's Scoring section needs: base Fame, the six Standard
 * Achievements, whether the city was conquered, whether Volkare was defeated, the two
 * difficulty-axis settings chosen at setup, and how many cards were left in Volkare's deck
 * when he was defeated.
 */
data class VolkaresReturnScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val cityConquered: Boolean,
    val volkareDefeated: Boolean,
    val combatLevel: CombatLevel,
    val raceLevel: RaceLevel,
    val cardsRemainingInVolkareDeck: Int,
)

/**
 * Scoring engine for the Volkare's Return scenario (docs/rules/volkares-return.md, "Scoring"
 * and "Outcome" sections): Fame plus the Standard Achievements (summed directly, no Titles -
 * solo play has nobody to compare against) plus a bonus for conquering the city and, if Volkare
 * was defeated, a separate Volkare Combat Bonus scaled by the two difficulty settings.
 */
object VolkaresReturnScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: VolkaresReturnScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/volkares-return.md's "Scoring"
     * section: Fame, the six Standard Achievements, the City Conquered bonus, and the Volkare
     * Combat Bonus.
     */
    fun breakdown(input: VolkaresReturnScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // if/else as an expression, assigned straight to the val.
        val cityConqueredBonus = if (input.cityConquered) CITY_CONQUERED_BONUS else 0
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("City Conquered", cityConqueredBonus),
            ScoreLineItem("Volkare Combat Bonus", volkareCombatBonus(input)),
        )
    }

    /**
     * The Volkare Combat Bonus (docs/rules/volkares-return.md, Scoring, step 4): 0 unless
     * Volkare was defeated. When he was, start from the Combat Level's base value, add 2 per
     * card still left in his deck, then multiply *only that bonus* by the Race Level's
     * multiplier - the multiplier never touches Fame, Achievements, or the City Conquered bonus.
     */
    private fun volkareCombatBonus(input: VolkaresReturnScoringInput): Int {
        if (!input.volkareDefeated) return 0

        val bonusBeforeMultiplier =
            input.combatLevel.combatBonusBase +
                POINTS_PER_CARD_LEFT_IN_VOLKARES_DECK * input.cardsRemainingInVolkareDeck
        // The base bonus (30/40/50) and the per-card bonus (always 2 * an Int) are both even,
        // so their sum is always even - multiplying by the numerator (2/3/4) and dividing by 2
        // is therefore always an exact integer division, with no fractional Volkare Combat
        // Bonus ever silently truncated.
        return bonusBeforeMultiplier * input.raceLevel.combatBonusMultiplierNumerator / 2
    }

    /**
     * Win/Loss check (docs/rules/volkares-return.md, "Outcome" section): Won iff Volkare was
     * defeated; Lost on any of the scenario's other end conditions. Deliberately independent of
     * [VolkaresReturnScoringInput.cityConquered] - the city can be conquered yet Volkare still
     * not fully destroyed by the last Round, which is still a loss.
     */
    fun outcome(input: VolkaresReturnScoringInput): Outcome =
        if (input.volkareDefeated) Outcome.WON else Outcome.LOST
}
