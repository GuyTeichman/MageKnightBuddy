package com.guyteichman.mageknightbuddy.domain

/**
 * Everything the player enters at the end of a solo The Fractured Lands session, matching what
 * docs/rules/the-fractured-lands.md's "Scoring" > "Solo" section needs: base Fame, the six
 * Standard Achievements, and quest points (Greatest Quester). Unlike Solo Conquest and The
 * Hidden Valley, this scenario has no cities-conquered field - the rulebook explicitly excludes
 * city scoring here ("City scoring is not applied") - and no Rounds-finished-early, Dummy-deck,
 * or End-of-Round fields either, since the Solo scoring rule doesn't mention any of those bonuses.
 */
data class FracturedLandsScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val questPoints: Int,
) : ScoringInput

/**
 * Scoring engine for the Solo variant of The Fractured Lands
 * (docs/rules/the-fractured-lands.md, "Scoring" > "Solo" and "Outcome" sections): Fame plus the
 * Standard Achievements (summed directly, no Titles - solo play has nobody to compare against)
 * plus Greatest Quester. This scenario has no lose condition at all (see [outcome]).
 */
object FracturedLandsScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: FracturedLandsScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown for The Fractured Lands, one line per rule in
     * docs/rules/the-fractured-lands.md's "Scoring" > "Solo" section: Fame, the six Standard
     * Achievements, and Greatest Quester. Notably shorter than Solo Conquest/Hidden Valley's
     * breakdowns - this scenario's Solo scoring rule doesn't add any further bonuses.
     */
    fun breakdown(input: FracturedLandsScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("Greatest Quester", input.questPoints),
        )
    }

    /**
     * The Fractured Lands has no lose condition (docs/rules/the-fractured-lands.md, "Outcome"
     * section: "no definitive goal... there's no Outcome to derive"), unlike every other
     * scenario in this app's scope. Rather than leave Outcome undefined, every session is scored
     * a Won for having played it through to the end - only [score] actually differentiates one
     * session from another here.
     */
    fun outcome(input: FracturedLandsScoringInput): Outcome = Outcome.WON
}
