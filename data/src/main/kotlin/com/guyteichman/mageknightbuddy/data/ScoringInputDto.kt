package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.UnitTally], nested inside
 * [StandardAchievementsDto]. Kept in `data/` so the domain module stays free of serialization
 * annotations/dependencies (see [DummyPlayerEventDto] for the same pattern applied elsewhere).
 */
@Serializable
data class UnitTallyDto(val level: Int, val healthyCount: Int, val woundedCount: Int)

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.StandardAchievements].
 * Nested inside every [ScoringInputDto] variant that has a Standard Achievements section
 * (every scenario except [ScoringInputDto.ForTheCouncil]).
 */
@Serializable
data class StandardAchievementsDto(
    val spellsInDeck: Int,
    val advancedActionsInDeck: Int,
    val units: List<UnitTallyDto>,
    val shieldsOnAdventureSites: Int,
    val artifacts: Int,
    val crystalsInInventory: Int,
    val shieldsOnConquerSites: Int,
    val woundsInDeck: Int,
)

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.ScoringInput] - one
 * `@SerialName`-tagged variant per scenario's `*ScoringInput` data class, encoded to a single
 * JSON string column ([ScoringSessionEntity.inputJson]) instead of the ~22 wide, flattened
 * columns this replaced. Each `@SerialName` value is stored in the database, so it must stay
 * stable once released - it is independent from [com.guyteichman.mageknightbuddy.domain.Scenario.id]
 * even though the two happen to use the same names today.
 */
@Serializable
sealed interface ScoringInputDto {
    @Serializable
    @SerialName("solo_conquest")
    data class SoloConquest(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val citiesConquered: Int,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
        val questPoints: Int,
    ) : ScoringInputDto

    @Serializable
    @SerialName("first_reconnaissance")
    data class FirstReconnaissance(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val cityRevealed: Boolean,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto

    @Serializable
    @SerialName("for_the_council")
    data class ForTheCouncil(
        val questPoints: Int,
        val reputationModifier: Int,
        val shieldOnXSpace: Boolean,
        val reputation: Int,
    ) : ScoringInputDto

    @Serializable
    @SerialName("hidden_valley")
    data class HiddenValley(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val highPriestessDefeated: Boolean,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto

    @Serializable
    @SerialName("realm_of_the_dead")
    data class RealmOfTheDead(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val graveyardsSealed: Int,
        val necromancerDefeated: Boolean,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto
}
