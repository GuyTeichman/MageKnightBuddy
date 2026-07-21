package com.guyteichman.mageknightbuddy.domain

// Solo scenario-end threshold (docs/rules/against-the-apocalypse.md, "Scenario end" > "Solo"):
// the game ends once you're holding at least this many Destroyed Site tokens in your Inventory.
private const val MIN_DESTROYED_SITE_TOKENS_FOR_VICTORY = 2

// ...and at least this many floors conquered of *each* of the ziggurat and the pyramid
// (tracked as two separate tallies below, since the win condition needs both, not just a sum).
private const val MIN_FLOORS_CONQUERED_PER_STRUCTURE = 1

/**
 * Everything the player enters at the end of a Solo Against the Apocalypse session, matching
 * the inputs docs/rules/against-the-apocalypse.md's "Scoring" > "Solo" section needs: base
 * Fame, the six Standard Achievements, Destroyed Site tokens held in Inventory, ziggurat and
 * pyramid floors conquered (kept as two separate tallies since the win condition requires at
 * least one floor of *each* structure, not just a combined total), how many Rounds were
 * finished early, Dummy deck cards left, and whether "End of the Round" was already announced.
 */
data class AgainstTheApocalypseScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val destroyedSiteTokens: Int,
    val zigguratFloorsConquered: Int,
    val pyramidFloorsConquered: Int,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
)

/**
 * Scoring engine for the Solo variant of Against the Apocalypse
 * (docs/rules/against-the-apocalypse.md, "Scoring" > "Solo" and "Outcome"): Fame plus the
 * Standard Achievements (summed directly, no Titles - solo play has nobody to compare against)
 * plus this scenario's own bonuses for Destroyed Site tokens, ziggurat/pyramid floors
 * conquered, victory, finishing Rounds early, and so on.
 */
object AgainstTheApocalypseScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: AgainstTheApocalypseScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/against-the-apocalypse.md's
     * "Scoring" > "Solo" section: Fame, the six Standard Achievements, Destroyed Site tokens,
     * ziggurat/pyramid floors conquered, the victory bonus, Rounds finished early, Dummy
     * Player deck cards remaining, and the End of Round bonus.
     */
    fun breakdown(input: AgainstTheApocalypseScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // Solo scoring is a flat 5 points per *site* you conquered a floor of (0, 1, or 2 sites
        // total - one ziggurat, one pyramid), not scaled by which floor number (1/2/3) was
        // reached - that tiered-by-floor-number formula is Competitive's rule, not Solo's.
        val sitesConquered =
            listOf(input.zigguratFloorsConquered, input.pyramidFloorsConquered).count { it >= 1 }
        // +15 Fame if the scenario's Solo win condition was met (see isVictorious below).
        val victoryBonus = if (isVictorious(input)) 15 else 0
        // +5 if "End of the Round" had not yet been announced in the final Round.
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0
        // listOf builds an immutable, ordered list in one expression; each entry below is a
        // rulebook-mandated scoring category or bonus, in the order the rulebook lists them.
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("Destroyed Sites", input.destroyedSiteTokens * 3),
            ScoreLineItem("Ziggurat/Pyramid Floors Conquered", sitesConquered * 5),
            ScoreLineItem("Victorious", victoryBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/against-the-apocalypse.md, "Outcome" section): Won iff every
     * Solo scenario-end condition was met - at least 2 Destroyed Site tokens in Inventory, and
     * at least one floor conquered of *both* the ziggurat and the pyramid; Lost otherwise. A
     * score is always produced either way (see [score]).
     */
    fun outcome(input: AgainstTheApocalypseScoringInput): Outcome =
        if (isVictorious(input)) Outcome.WON else Outcome.LOST

    // Shared win-condition check reused by both the +15 "Victorious" score bonus and outcome()
    // above - the rulebook ties both to the same "all scenario-end conditions were met" idea.
    private fun isVictorious(input: AgainstTheApocalypseScoringInput): Boolean =
        input.destroyedSiteTokens >= MIN_DESTROYED_SITE_TOKENS_FOR_VICTORY &&
            input.zigguratFloorsConquered >= MIN_FLOORS_CONQUERED_PER_STRUCTURE &&
            input.pyramidFloorsConquered >= MIN_FLOORS_CONQUERED_PER_STRUCTURE
}
