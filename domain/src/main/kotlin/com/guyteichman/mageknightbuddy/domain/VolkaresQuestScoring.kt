package com.guyteichman.mageknightbuddy.domain

// Fame awarded per conquered city (docs/rules/volkares-quest.md, Scoring step 3). Unlike Volkare's
// Return's +20/city, conquering cities isn't this scenario's goal, so the bonus is smaller.
private const val FAME_PER_CONQUERED_CITY = 5

// Fame awarded per card still in Volkare's deck, counted before the Race Level multiplier is
// applied (docs/rules/volkares-quest.md, Scoring step 4).
private const val VOLKARE_COMBAT_BONUS_PER_REMAINING_CARD = 2

// Solo setup always reveals exactly 2 core city tiles (docs/rules/volkares-quest.md, Setup:
// "Core city tiles: 2, 2, 3, or 3 [solo/2/3/4 players]"), so 2 is the maximum obtainable in Solo.
private const val TOTAL_CITIES_IN_SOLO_VOLKARES_QUEST = 2

/**
 * Everything the player enters at the end of a Volkare's Quest session, matching the inputs
 * docs/rules/volkares-quest.md's Scoring section needs: base Fame, the six Standard Achievements,
 * how many cities were conquered, which Combat/Race Level difficulty this session was played at,
 * whether Volkare was defeated, and how many cards were left in his deck when that happened.
 */
data class VolkaresQuestScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val citiesConquered: Int,
    val combatLevel: CombatLevel,
    val raceLevel: RaceLevel,
    val volkareDefeated: Boolean,
    val cardsRemainingInVolkaresDeck: Int,
) : ScoringInput {
    // init runs on every construction (including copy()), so an out-of-range tally can never
    // reach the scoring math below - it fails fast at the point the bad value was created.
    init {
        require(citiesConquered in 0..TOTAL_CITIES_IN_SOLO_VOLKARES_QUEST) {
            "citiesConquered must be between 0 and $TOTAL_CITIES_IN_SOLO_VOLKARES_QUEST, was $citiesConquered"
        }
    }
}

/**
 * Scoring engine for Volkare's Quest (docs/rules/volkares-quest.md, "Scoring" and "Outcome"
 * sections): Fame plus the Standard Achievements (no Titles in solo play) plus +5 Fame per
 * conquered city, plus - only if Volkare was defeated - a separate Volkare combat bonus scaled by
 * this session's Combat/Race Level difficulty. Defeating Volkare is the real goal here; scoring
 * is only a secondary "how well did you do" measure (same shape as Volkare's Return).
 */
object VolkaresQuestScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: VolkaresQuestScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown for Volkare's Quest, one line per rule in
     * docs/rules/volkares-quest.md's "Scoring" section: Fame, the six Standard Achievements,
     * cities conquered, and the Volkare combat bonus (zero if Volkare was not defeated).
     */
    fun breakdown(input: VolkaresQuestScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("Cities Conquered", input.citiesConquered * FAME_PER_CONQUERED_CITY),
            ScoreLineItem("Volkare Combat Bonus", volkareCombatBonus(input)),
        )
    }

    /**
     * Win/Loss check (docs/rules/volkares-quest.md, "Outcome" section): Won iff Volkare's entire
     * army was destroyed before he could move again after entering the portal; Lost otherwise.
     * Reuses the same flag that gates the Volkare combat bonus in [breakdown] - a score is always
     * produced either way.
     */
    fun outcome(input: VolkaresQuestScoringInput): Outcome =
        if (input.volkareDefeated) Outcome.WON else Outcome.LOST

    /**
     * The Volkare combat bonus (docs/rules/volkares-quest.md, Scoring step 4): zero unless
     * Volkare was defeated. Otherwise, this Combat Level's base value plus 2 Fame per card still
     * in his deck, with the whole subtotal multiplied by this Race Level's factor. The multiplier
     * applies only to this subtotal - never to the Fame/Achievements/cities total in [breakdown].
     */
    private fun volkareCombatBonus(input: VolkaresQuestScoringInput): Int {
        if (!input.volkareDefeated) return 0
        val subtotal = input.combatLevel.combatBonusBase +
            VOLKARE_COMBAT_BONUS_PER_REMAINING_CARD * input.cardsRemainingInVolkaresDeck
        // Integer numerator/denominator arithmetic (rather than a Double multiplier) keeps this
        // exact - see RaceLevel's KDoc for why subtotal * numerator is always evenly divisible.
        return subtotal * input.raceLevel.combatBonusMultiplierNumerator /
            input.raceLevel.combatBonusMultiplierDenominator
    }
}
