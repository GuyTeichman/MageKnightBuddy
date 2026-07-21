package com.guyteichman.mageknightbuddy.domain

// Same city count as base Solo Conquest (docs/rules/solo-conquest-challenge.md says this
// scenario "uses all the same rules as the base game's Solo Conquest ... except where
// indicated"; the city count isn't one of the exceptions).
private const val TOTAL_CITIES_IN_SOLO_CONQUEST_CHALLENGE = 2

// Mage Knight has exactly 4 card colors (see CardColor: RED, GREEN, BLUE, WHITE), so "distinct
// colors" tallies (crystals held, Advanced Actions in deck) can never exceed 4.
private const val TOTAL_CARD_COLORS = 4

// The Wound/Shield/Unit-level thresholds each Knight's additional objective checks against
// (docs/rules/solo-conquest-challenge.md, "Outcome" section). Named per-Knight so the
// `outcome()` when-block below reads like the rulebook table it mirrors.
private const val ARYTHEA_WOUND_CARD_THRESHOLD = 10
private const val GOLDYX_SPELL_THRESHOLD = 4
private const val GOLDYX_CRYSTAL_COLOR_THRESHOLD = 4
private const val NOROWAS_UNIT_LEVEL_THRESHOLD = 10
private const val TOVAK_SHIELD_THRESHOLD = 4
private const val WOLFHAWK_SHIELD_THRESHOLD = 4
private const val KRANG_DISTINCT_FAME_VALUE_THRESHOLD = 4
private const val BRAEVALAR_ADVANCED_ACTION_COLOR_THRESHOLD = 4
private const val CORAL_ARTIFACT_THRESHOLD = 3
private const val CORAL_CRYSTAL_THRESHOLD = 4

/**
 * Everything the player enters at the end of a Solo Conquest Challenge session
 * (docs/rules/solo-conquest-challenge.md): the same raw tallies base Solo Conquest needs
 * (see [SoloConquestScoringInput]) plus [knight], since the Challenge variant's Achievements
 * Scoring overrides and additional victory objective both depend on which Knight is played
 * (see `CONTEXT.md`'s **Knight** entry). The Knight-specific fields below (wound cards on
 * Units, crystal color diversity, Puppet Master tokens, Braevalar's deck/terrain checks) only
 * matter for the one Knight each is named after; they default to a neutral 0/false so tests
 * and callers for other Knights can ignore them.
 */
data class SoloConquestChallengeScoringInput(
    val knight: Knight,
    val fame: Int,
    val standardAchievements: StandardAchievements,
    val citiesConquered: Int,
    val roundsFinishedEarly: Int,
    val cardsRemainingInDummyDeck: Int,
    val endOfRoundAnnounced: Boolean,
    val questPoints: Int,
    // Arythea: Wound cards attached to Units, on top of standardAchievements.woundsInDeck -
    // her objective counts wounds "in your deck and on your Units" together.
    val woundCardsOnUnits: Int = 0,
    // Goldyx: how many distinct crystal colors (0-4) have at least one crystal in Inventory.
    val distinctCrystalColorsInInventory: Int = 0,
    // Krang: highest Fame value among Enemy tokens held from the Puppet Master Skill.
    val puppetMasterHighestFameValue: Int = 0,
    // Krang: how many distinct Fame values are represented among those held tokens.
    val puppetMasterDistinctFameValues: Int = 0,
    // Braevalar: whether every Basic Action card is still in the deck (none thrown away).
    val allBasicActionsInDeck: Boolean = false,
    // Braevalar: how many distinct colors (0-4) have at least one Advanced Action in deck.
    val distinctAdvancedActionColorsInDeck: Int = 0,
    // Braevalar: normal Move cost at Night of the space the game ended on (Mountains = 5,
    // Lakes = 2) - scored directly as bonus Fame.
    val finalSpaceMoveCostAtNight: Int = 0,
) {
    // init runs on every construction (including copy()), so an out-of-range tally can never
    // reach the scoring math below - it fails fast at the point the bad value was created.
    init {
        require(citiesConquered in 0..TOTAL_CITIES_IN_SOLO_CONQUEST_CHALLENGE) {
            "citiesConquered must be between 0 and $TOTAL_CITIES_IN_SOLO_CONQUEST_CHALLENGE, was $citiesConquered"
        }
        require(distinctCrystalColorsInInventory in 0..TOTAL_CARD_COLORS) {
            "distinctCrystalColorsInInventory must be between 0 and $TOTAL_CARD_COLORS, " +
                "was $distinctCrystalColorsInInventory"
        }
        require(distinctAdvancedActionColorsInDeck in 0..TOTAL_CARD_COLORS) {
            "distinctAdvancedActionColorsInDeck must be between 0 and $TOTAL_CARD_COLORS, " +
                "was $distinctAdvancedActionColorsInDeck"
        }
    }
}

/**
 * Scoring engine for the Solo Conquest Challenge scenario (docs/rules/solo-conquest-challenge.md):
 * the Apocalypse Dragon expansion's knight-specific variant of Solo Conquest. Scoring follows
 * base Solo Conquest (Fame + Standard Achievements, no Titles, + the Solo Conquest scenario
 * bonuses - see [SoloConquestScoring]) except each Knight overrides specific Achievements
 * Scoring formulas, and Outcome adds a per-Knight objective on top of conquering both cities.
 */
object SoloConquestChallengeScoring {
    /** Total score for the session: the sum of every line in [breakdown]. */
    fun score(input: SoloConquestChallengeScoringInput): Int =
        breakdown(input).sumOf { it.value }

    /**
     * Itemized score breakdown, per docs/rules/solo-conquest-challenge.md's "Scoring" section:
     * Fame, then either the six Standard Achievements (with this Knight's formula overrides
     * applied) or, for Krang, a single "Puppet Master" line that replaces all six, then
     * Greatest Quester and the shared Solo Conquest scenario bonuses.
     */
    fun breakdown(input: SoloConquestChallengeScoringInput): List<ScoreLineItem> {
        val allCitiesBonus =
            if (input.citiesConquered == TOTAL_CITIES_IN_SOLO_CONQUEST_CHALLENGE) 15 else 0
        val endOfRoundBonus = if (!input.endOfRoundAnnounced) 5 else 0

        // listOf + `+` to concatenate: build the six-category block for every Knight except
        // Krang (whose Puppet Master formula "replaces the standard categories entirely" per
        // the rulebook table), so the two shapes stay easy to tell apart at a glance.
        val achievementsLines = if (input.knight == Knight.KRANG) {
            listOf(ScoreLineItem("Puppet Master", puppetMasterFame(input)))
        } else {
            achievementCategoryLines(input) + braevalarMoveCostLine(input)
        }

        return listOf(ScoreLineItem("Fame", input.fame)) +
            achievementsLines +
            listOf(
                ScoreLineItem("Greatest Quester", input.questPoints),
                ScoreLineItem("Cities Conquered", input.citiesConquered * 10),
                ScoreLineItem("All Cities Conquered", allCitiesBonus),
                ScoreLineItem("Rounds Finished Early", input.roundsFinishedEarly * 30),
                ScoreLineItem("Dummy Player's Deck", input.cardsRemainingInDummyDeck),
                ScoreLineItem("End of Round", endOfRoundBonus),
            )
    }

    /**
     * Win/Loss check (docs/rules/solo-conquest-challenge.md, "Outcome" section): Won iff both
     * cities were conquered *and* this Knight's additional objective is met; Lost otherwise.
     * A score is always produced either way (see [score]).
     */
    fun outcome(input: SoloConquestChallengeScoringInput): Outcome {
        val bothCitiesConquered =
            input.citiesConquered == TOTAL_CITIES_IN_SOLO_CONQUEST_CHALLENGE
        return if (bothCitiesConquered && knightObjectiveMet(input)) Outcome.WON else Outcome.LOST
    }

    /**
     * The six Standard Achievements, each using this Knight's override formula where the
     * rulebook table specifies one, and the normal [StandardAchievements] formula otherwise.
     */
    private fun achievementCategoryLines(input: SoloConquestChallengeScoringInput): List<ScoreLineItem> =
        listOf(
            ScoreLineItem("Greatest Knowledge", greatestKnowledge(input)),
            ScoreLineItem("Greatest Leader", greatestLeader(input)),
            ScoreLineItem("Greatest Adventurer", greatestAdventurer(input)),
            ScoreLineItem("Greatest Loot", greatestLoot(input)),
            ScoreLineItem("Greatest Conqueror", greatestConqueror(input)),
            ScoreLineItem("Greatest Beating", greatestBeating(input)),
        )

    /** Greatest Knowledge: Goldyx and Braevalar both raise the per-card Fame value. */
    private fun greatestKnowledge(input: SoloConquestChallengeScoringInput): Int {
        val a = input.standardAchievements
        // `when` used as an expression: each branch's value becomes this function's result.
        return when (input.knight) {
            Knight.GOLDYX -> 3 * a.spellsInDeck + a.advancedActionsInDeck
            Knight.BRAEVALAR -> 2 * a.spellsInDeck + 2 * a.advancedActionsInDeck
            else -> a.greatestKnowledge()
        }
    }

    /** Greatest Leader: Arythea counts Wounded Units at full level; Norowas doubles unwounded Units. */
    private fun greatestLeader(input: SoloConquestChallengeScoringInput): Int {
        val a = input.standardAchievements
        return when (input.knight) {
            // sumOf maps each UnitTally to a Fame value and adds them up.
            Knight.ARYTHEA -> a.units.sumOf { (it.healthyCount + it.woundedCount) * it.level }
            Knight.NOROWAS -> a.units.sumOf { it.healthyCount * 2 * it.level + it.woundedCount * (it.level / 2) }
            else -> a.greatestLeader()
        }
    }

    /** Greatest Adventurer: Wolfhawk doubles the per-Shield Fame value. */
    private fun greatestAdventurer(input: SoloConquestChallengeScoringInput): Int {
        val a = input.standardAchievements
        return when (input.knight) {
            Knight.WOLFHAWK -> 4 * a.shieldsOnAdventureSites
            else -> a.greatestAdventurer()
        }
    }

    /** Greatest Loot: Goldyx and Coral both raise the crystal rate to 1-per-crystal; Coral also doubles Artifacts. */
    private fun greatestLoot(input: SoloConquestChallengeScoringInput): Int {
        val a = input.standardAchievements
        return when (input.knight) {
            Knight.GOLDYX -> 2 * a.artifacts + a.crystalsInInventory
            Knight.CORAL -> 4 * a.artifacts + a.crystalsInInventory
            else -> a.greatestLoot()
        }
    }

    /** Greatest Conqueror: Tovak doubles the per-Shield Fame value. */
    private fun greatestConqueror(input: SoloConquestChallengeScoringInput): Int {
        val a = input.standardAchievements
        return when (input.knight) {
            Knight.TOVAK -> 4 * a.shieldsOnConquerSites
            else -> a.greatestConqueror()
        }
    }

    /** Greatest Beating: Arythea's Wound cards cost no Fame at all (normally -2 each). */
    private fun greatestBeating(input: SoloConquestChallengeScoringInput): Int =
        when (input.knight) {
            Knight.ARYTHEA -> 0
            else -> input.standardAchievements.greatestBeating()
        }

    /**
     * Braevalar's extra bonus line: Fame equal to the normal Move cost at Night of the space
     * he finishes the game on. Returns an empty list for every other Knight, so the line is
     * dropped entirely rather than showing a misleading "0" bonus that doesn't apply to them.
     */
    private fun braevalarMoveCostLine(input: SoloConquestChallengeScoringInput): List<ScoreLineItem> =
        if (input.knight == Knight.BRAEVALAR) {
            listOf(ScoreLineItem("Final Space Move Cost", input.finalSpaceMoveCostAtNight))
        } else {
            emptyList()
        }

    /**
     * Krang's replacement for all six Standard Achievements: the highest Fame value among his
     * held Puppet Master Enemy tokens, plus 2 Fame per distinct Fame value among them.
     */
    private fun puppetMasterFame(input: SoloConquestChallengeScoringInput): Int =
        input.puppetMasterHighestFameValue + 2 * input.puppetMasterDistinctFameValues

    /**
     * Each Knight's additional victory objective (docs/rules/solo-conquest-challenge.md,
     * "Outcome" table), checked against the same raw tallies already entered for scoring -
     * no separate input needed. Exhaustive `when` over the [Knight] enum, so adding a Knight
     * without an objective here is a compile error, not a silent gap.
     */
    private fun knightObjectiveMet(input: SoloConquestChallengeScoringInput): Boolean {
        val a = input.standardAchievements
        return when (input.knight) {
            Knight.ARYTHEA -> (a.woundsInDeck + input.woundCardsOnUnits) >= ARYTHEA_WOUND_CARD_THRESHOLD
            Knight.GOLDYX ->
                a.spellsInDeck >= GOLDYX_SPELL_THRESHOLD &&
                    input.distinctCrystalColorsInInventory >= GOLDYX_CRYSTAL_COLOR_THRESHOLD
            Knight.NOROWAS ->
                a.units.sumOf { (it.healthyCount + it.woundedCount) * it.level } >= NOROWAS_UNIT_LEVEL_THRESHOLD
            Knight.TOVAK -> a.shieldsOnConquerSites >= TOVAK_SHIELD_THRESHOLD
            Knight.WOLFHAWK -> a.shieldsOnAdventureSites >= WOLFHAWK_SHIELD_THRESHOLD
            Knight.KRANG -> input.puppetMasterDistinctFameValues >= KRANG_DISTINCT_FAME_VALUE_THRESHOLD
            Knight.BRAEVALAR ->
                input.allBasicActionsInDeck &&
                    input.distinctAdvancedActionColorsInDeck >= BRAEVALAR_ADVANCED_ACTION_COLOR_THRESHOLD
            Knight.CORAL ->
                a.artifacts >= CORAL_ARTIFACT_THRESHOLD && a.crystalsInInventory >= CORAL_CRYSTAL_THRESHOLD
        }
    }
}
