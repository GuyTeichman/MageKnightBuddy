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
        // The ReputationTrackSpace enum's name (see domain) - the space itself is the only thing
        // that needs persisting, since its modifier is looked up from it directly.
        val reputationTrackSpaceName: String,
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

    @Serializable
    @SerialName("against_the_dragon")
    data class AgainstTheDragon(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val headsDefeated: Int,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto

    @Serializable
    @SerialName("against_the_horsemen")
    data class AgainstTheHorsemen(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val horsemenDefeated: Int,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto

    @Serializable
    @SerialName("apocalypse_is_here")
    data class ApocalypseIsHere(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val horsemenDefeated: Int,
        val headsDefeated: Int,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto

    @Serializable
    @SerialName("the_fractured_lands")
    data class FracturedLands(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val questPoints: Int,
    ) : ScoringInputDto

    @Serializable
    @SerialName("life_and_death")
    data class LifeAndDeath(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val tezlaSpiritDefeated: Boolean,
        val darkTezlaDefeated: Boolean,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto

    @Serializable
    @SerialName("lost_relic")
    data class LostRelic(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val relicPiecesFound: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto

    @Serializable
    @SerialName("against_the_apocalypse")
    data class AgainstTheApocalypse(
        val fame: Int,
        val standardAchievements: StandardAchievementsDto,
        val destroyedSiteTokens: Int,
        val zigguratFloorsConquered: Int,
        val pyramidFloorsConquered: Int,
        val roundsFinishedEarly: Int,
        val cardsRemainingInDummyDeck: Int,
        val endOfRoundAnnounced: Boolean,
    ) : ScoringInputDto
}
