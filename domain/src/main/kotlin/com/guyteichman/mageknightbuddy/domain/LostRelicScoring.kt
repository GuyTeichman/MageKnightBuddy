package com.guyteichman.mageknightbuddy.domain

// Solo's setup always reveals exactly 2 core city tiles (docs/rules/lost-relic.md, Solo section:
// "Core city tiles: 2"), and the scenario's special rules put exactly one relic piece in every
// destroyed (core) city, so 2 is the maximum number of pieces obtainable in this variant.
private const val TOTAL_RELIC_PIECES_IN_SOLO_LOST_RELIC = 2

/**
 * Everything the player enters at the end of a solo Lost Relic session, matching the inputs
 * docs/rules/lost-relic.md's Solo "Scoring" section needs: base Fame, the six Standard
 * Achievements, how many relic pieces were found, Dummy deck cards left, and whether
 * "End of the Round" was already announced.
 */
data class LostRelicScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val relicPiecesFound: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
)

/**
 * Scoring engine for the solo variant of The Lost Relic (docs/rules/lost-relic.md, "Solo" >
 * "Scoring" and "Outcome"): Fame plus the Standard Achievements (no Titles - solo play has
 * nobody to compare against) plus this scenario's own bonuses for relic pieces found and the
 * shared Dummy-deck/End-of-Round bonuses.
 */
object LostRelicScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: LostRelicScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/lost-relic.md's "Solo" >
     * "Scoring" section: Fame, the six Standard Achievements, relic pieces found, the
     * all-pieces-found bonus, Dummy Player deck cards remaining, and the End of Round bonus.
     */
    fun breakdown(input: LostRelicScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // if/else as an expression, assigned straight to the val - Kotlin has no ternary operator.
        // +10 for finding *every* relic piece (rule: "+10 points if all pieces of the relic were found").
        val allRelicPiecesBonus =
            if (input.relicPiecesFound == TOTAL_RELIC_PIECES_IN_SOLO_LOST_RELIC) 10 else 0
        // +5 if "End of the Round" had not yet been announced in the final Round.
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("Relic Pieces Found", input.relicPiecesFound * 5),
            ScoreLineItem("All Relic Pieces Found", allRelicPiecesBonus),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/lost-relic.md, "Outcome" section): Won iff all parts of the
     * relic were collected, reusing the same tally used for the relic-pieces bonus above; Lost
     * otherwise. A score is always produced either way (see [score]).
     */
    fun outcome(input: LostRelicScoringInput): Outcome =
        if (input.relicPiecesFound == TOTAL_RELIC_PIECES_IN_SOLO_LOST_RELIC) Outcome.WON else Outcome.LOST
}
