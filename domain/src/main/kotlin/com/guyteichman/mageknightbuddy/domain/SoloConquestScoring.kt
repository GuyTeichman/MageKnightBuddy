package com.guyteichman.mageknightbuddy.domain

// Solo Conquest always reveals exactly 2 core city tiles (docs/rules/solo-conquest.md, Setup):
// the first is level 5, the second level 8. Both must fall for the "all cities" bonus/win.
private const val TOTAL_CITIES_IN_SOLO_CONQUEST = 2

/**
 * Everything the player enters at the end of a Solo Conquest session, matching the inputs
 * docs/rules/solo-conquest.md's Scoring section needs: base Fame, the six Standard Achievements,
 * how many cities fell, how many Rounds were finished early, Dummy deck cards left, whether
 * "End of the Round" was already announced, and quest points (Greatest Quester).
 */
data class SoloConquestScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val citiesConquered: Int,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
    val questPoints: Int,
) : ScoringInput

/**
 * One row of a score breakdown, e.g. ("Fame", 12) - the display-friendly unit every scenario's
 * `breakdown()` function returns. Shared across all scenario scoring objects in this package.
 */
data class ScoreLineItem(val label: String, val value: Int)

/**
 * Scoring engine for the Solo Conquest scenario (docs/rules/solo-conquest.md, "Scoring" and
 * "Outcome" sections): Fame plus the Standard Achievements (summed directly, no Titles - solo
 * play has nobody to compare against) plus this scenario's own bonuses for conquering cities,
 * finishing Rounds early, and so on.
 */
object SoloConquestScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: SoloConquestScoringInput): Int =
        // sumOf is a stdlib helper that maps each element with the lambda and adds up the
        // results in one step; `it` is the implicit name for each ScoreLineItem here.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown for Solo Conquest, one line per rule in
     * docs/rules/solo-conquest.md's "Scoring" section: Fame, the six Standard Achievements,
     * Greatest Quester, and the scenario-specific bonuses (cities conquered, all-cities bonus,
     * Rounds finished early, Dummy Player deck cards remaining, End of Round bonus).
     */
    fun breakdown(input: SoloConquestScoringInput): List<ScoreLineItem> {
        // if/else used as an expression (Kotlin has no separate ternary operator) - the result
        // is assigned straight to the val. +15 for conquering *all* cities (rule: "All Cities Conquered").
        val allCitiesBonus = if (input.citiesConquered == TOTAL_CITIES_IN_SOLO_CONQUEST) 15 else 0
        // `+` concatenates lists here: the shared Fame/Achievements block, this scenario's own
        // bonus lines, then the shared Rounds-Finished-Early/Dummy-Deck/End-of-Round trio.
        return standardScoreLines(input.fame, input.standardAchievements) +
            listOf(
                ScoreLineItem("Greatest Quester", input.questPoints),
                ScoreLineItem("Cities Conquered", input.citiesConquered * 10),
                ScoreLineItem("All Cities Conquered", allCitiesBonus),
            ) +
            dummyEndgameLines(input.roundsFinishedEarly, input.cardsRemainingInDummyDeck, input.endOfRoundAnnounced)
    }

    /**
     * Win/Loss check (docs/rules/solo-conquest.md, "Outcome" section): Won iff both cities were
     * conquered, reusing the same tally used for the cities-conquered bonus above; Lost
     * otherwise. A score is always produced either way (see [score]).
     */
    fun outcome(input: SoloConquestScoringInput): Outcome =
        if (input.citiesConquered == TOTAL_CITIES_IN_SOLO_CONQUEST) Outcome.WON else Outcome.LOST
}
