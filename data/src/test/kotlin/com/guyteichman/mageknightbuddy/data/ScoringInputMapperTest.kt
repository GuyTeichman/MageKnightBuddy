package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.AgainstTheApocalypseScoringInput
import com.guyteichman.mageknightbuddy.domain.AgainstTheDragonScoringInput
import com.guyteichman.mageknightbuddy.domain.AgainstTheHorsemenScoringInput
import com.guyteichman.mageknightbuddy.domain.ApocalypseIsHereScoringInput
import com.guyteichman.mageknightbuddy.domain.FirstReconnaissanceScoringInput
import com.guyteichman.mageknightbuddy.domain.ForTheCouncilScoringInput
import com.guyteichman.mageknightbuddy.domain.FracturedLandsScoringInput
import com.guyteichman.mageknightbuddy.domain.HiddenValleyScoringInput
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.LifeAndDeathScoringInput
import com.guyteichman.mageknightbuddy.domain.LostRelicScoringInput
import com.guyteichman.mageknightbuddy.domain.RealmOfTheDeadScoringInput
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.ScoringInput
import com.guyteichman.mageknightbuddy.domain.SoloConquestChallengeScoringInput
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.StandardAchievements
import com.guyteichman.mageknightbuddy.domain.UnitTally
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tests the JSON-column mapping between every [ScoringInput] variant and its mirroring
 * [ScoringInputDto] variant - the layer that lets [ScoringSessionEntity] persist all 5 scenarios'
 * differently-shaped inputs through a single `inputJson` string column instead of one wide,
 * flattened set of Room columns (see [DummyPlayerEventDto] for the precedent this follows).
 */
class ScoringInputMapperTest {

    private val achievements = StandardAchievements(
        spellsInDeck = 3,
        advancedActionsInDeck = 2,
        units = listOf(
            UnitTally(level = 1, healthyCount = 1, woundedCount = 2),
            UnitTally(level = 2, healthyCount = 3, woundedCount = 0),
            UnitTally(level = 3, healthyCount = 0, woundedCount = 1),
            UnitTally(level = 4, healthyCount = 2, woundedCount = 2),
        ),
        shieldsOnAdventureSites = 5,
        artifacts = 4,
        crystalsInInventory = 7,
        shieldsOnConquerSites = 6,
        woundsInDeck = 3,
    )

    @Test
    fun `SoloConquestScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = SoloConquestScoringInput(
            fame = 72,
            standardAchievements = achievements,
            citiesConquered = 2,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 8,
            endOfRoundAnnounced = false,
            questPoints = 9,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `FirstReconnaissanceScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = FirstReconnaissanceScoringInput(
            fame = 20,
            standardAchievements = achievements,
            cityRevealed = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `ForTheCouncilScoringInput round-trips through ScoringInputDto and JSON, including a negative modifier`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 12,
            reputationTrackSpace = ReputationTrackSpace.MINUS_3,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `ForTheCouncilScoringInput round-trips an X space too`() {
        val input = ForTheCouncilScoringInput(
            questPoints = 12,
            reputationTrackSpace = ReputationTrackSpace.NEGATIVE_X,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `HiddenValleyScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = HiddenValleyScoringInput(
            fame = 15,
            standardAchievements = achievements,
            highPriestessDefeated = true,
            roundsFinishedEarly = 0,
            cardsRemainingInDummyDeck = 5,
            endOfRoundAnnounced = true,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `RealmOfTheDeadScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = RealmOfTheDeadScoringInput(
            fame = 18,
            standardAchievements = achievements,
            graveyardsSealed = 2,
            necromancerDefeated = true,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `AgainstTheDragonScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = AgainstTheDragonScoringInput(
            fame = 30,
            standardAchievements = achievements,
            headsDefeated = 3,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 4,
            endOfRoundAnnounced = false,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `AgainstTheHorsemenScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = AgainstTheHorsemenScoringInput(
            fame = 25,
            standardAchievements = achievements,
            horsemenDefeated = 4,
            roundsFinishedEarly = 0,
            cardsRemainingInDummyDeck = 2,
            endOfRoundAnnounced = true,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `ApocalypseIsHereScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = ApocalypseIsHereScoringInput(
            fame = 45,
            standardAchievements = achievements,
            horsemenDefeated = 2,
            headsDefeated = 4,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 6,
            endOfRoundAnnounced = false,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `FracturedLandsScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = FracturedLandsScoringInput(
            fame = 15,
            standardAchievements = achievements,
            questPoints = 8,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `LifeAndDeathScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = LifeAndDeathScoringInput(
            fame = 35,
            standardAchievements = achievements,
            tezlaSpiritDefeated = true,
            darkTezlaDefeated = false,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 3,
            endOfRoundAnnounced = false,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `LostRelicScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = LostRelicScoringInput(
            fame = 22,
            standardAchievements = achievements,
            relicPiecesFound = 2,
            cardsRemainingInDummyDeck = 5,
            endOfRoundAnnounced = true,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `AgainstTheApocalypseScoringInput round-trips through ScoringInputDto and JSON`() {
        val input = AgainstTheApocalypseScoringInput(
            fame = 28,
            standardAchievements = achievements,
            destroyedSiteTokens = 3,
            zigguratFloorsConquered = 1,
            pyramidFloorsConquered = 1,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 5,
            endOfRoundAnnounced = false,
        )

        assertRoundTrips(input)
    }

    @Test
    fun `SoloConquestChallengeScoringInput round-trips through ScoringInputDto and JSON, Knight-specific fields included`() {
        val input = SoloConquestChallengeScoringInput(
            knight = Knight.BRAEVALAR,
            fame = 45,
            standardAchievements = achievements,
            citiesConquered = 2,
            roundsFinishedEarly = 1,
            cardsRemainingInDummyDeck = 5,
            endOfRoundAnnounced = false,
            questPoints = 6,
            allBasicActionsInDeck = true,
            distinctAdvancedActionColorsInDeck = 3,
            finalSpaceMoveCostAtNight = 5,
        )

        assertRoundTrips(input)
    }

    /** Round-trips [input] through [ScoringInput.toDto]/[ScoringInputDto.toDomain] and a JSON string, in both directions. */
    private fun assertRoundTrips(input: ScoringInput) {
        val dto = input.toDto()
        assertEquals(input, dto.toDomain())

        val json = Json.encodeToString(dto)
        val decoded = Json.decodeFromString<ScoringInputDto>(json)
        assertEquals(input, decoded.toDomain())
    }
}
