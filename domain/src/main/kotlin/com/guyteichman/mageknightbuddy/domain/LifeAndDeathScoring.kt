package com.guyteichman.mageknightbuddy.domain

// +10 points per Avatar (Tezla's Spirit / Dark Tezla) defeated (docs/rules/life-and-death.md, Solo > Scoring).
private const val AVATAR_DEFEATED_BONUS = 10

// +15 more on top of the per-Avatar bonus above if *both* Avatars were defeated.
private const val BOTH_AVATARS_DEFEATED_BONUS = 15

/**
 * Inputs for scoring a solo Life and Death session (docs/rules/life-and-death.md,
 * "Solo" > "Scoring"): Fame, the six Standard Achievements, whether each of the two named
 * Avatars of Tezla (Tezla's Spirit for the Elementalists, Dark Tezla for the Dark Crusaders)
 * was defeated, Rounds finished early, Dummy deck cards left, and whether "End of the Round"
 * had already been announced. Not a v1 target - kept here as reference for when it's
 * implemented (v1 is Solo Conquest only).
 */
data class LifeAndDeathScoringInput(
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val tezlaSpiritDefeated: Boolean,
    val darkTezlaDefeated: Boolean,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
)

/**
 * Scoring engine for the solo variant of Life and Death (docs/rules/life-and-death.md,
 * "Solo" > "Scoring" and "Outcome" sections): Fame plus the Standard Achievements (no Titles
 * in solo play - only one player, nothing to compare against) plus this scenario's own
 * bonuses for defeating the two Avatars of Tezla, finishing Rounds early, and so on.
 */
object LifeAndDeathScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: LifeAndDeathScoringInput): Int =
        // sumOf maps each ScoreLineItem to its value and adds the results together.
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, one line per rule in docs/rules/life-and-death.md's
     * "Solo" > "Scoring" section: Fame, the six Standard Achievements, the per-Avatar-defeated
     * bonus plus the extra bonus for defeating both, Rounds finished early, Dummy Player deck
     * cards remaining, and the End of Round bonus.
     */
    fun breakdown(input: LifeAndDeathScoringInput): List<ScoreLineItem> {
        val achievements = input.standardAchievements
        // listOf(...).count { it } counts how many of the two Booleans are true, i.e. how many
        // Avatars were defeated (0, 1, or 2) - used to scale the flat per-Avatar bonus below.
        val avatarsDefeatedCount = listOf(input.tezlaSpiritDefeated, input.darkTezlaDefeated).count { it }
        // if/else as an expression, assigned straight to the val (Kotlin has no ternary operator).
        val bothAvatarsBonus =
            if (input.tezlaSpiritDefeated && input.darkTezlaDefeated) BOTH_AVATARS_DEFEATED_BONUS else 0
        // +5 if "End of the Round" was not yet announced in the final Round.
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0
        return listOf(
            ScoreLineItem("Fame", input.fame),
            ScoreLineItem("Greatest Knowledge", achievements.greatestKnowledge()),
            ScoreLineItem("Greatest Leader", achievements.greatestLeader()),
            ScoreLineItem("Greatest Adventurer", achievements.greatestAdventurer()),
            ScoreLineItem("Greatest Loot", achievements.greatestLoot()),
            ScoreLineItem("Greatest Conqueror", achievements.greatestConqueror()),
            ScoreLineItem("Greatest Beating", achievements.greatestBeating()),
            ScoreLineItem("Avatars Defeated", avatarsDefeatedCount * AVATAR_DEFEATED_BONUS),
            ScoreLineItem("Both Avatars Defeated", bothAvatarsBonus),
            ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
            ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
            ScoreLineItem("End of Round", endOfRoundBonus),
        )
    }

    /**
     * Win/Loss check (docs/rules/life-and-death.md, "Outcome" section, which explicitly
     * applies "all variants"): Won iff both Avatars (both faction leaders) were defeated;
     * Lost otherwise. A score is always produced either way - the rulebook is explicit that
     * failing still lets you "count your score to see how good you were."
     */
    fun outcome(input: LifeAndDeathScoringInput): Outcome =
        if (input.tezlaSpiritDefeated && input.darkTezlaDefeated) Outcome.WON else Outcome.LOST
}
