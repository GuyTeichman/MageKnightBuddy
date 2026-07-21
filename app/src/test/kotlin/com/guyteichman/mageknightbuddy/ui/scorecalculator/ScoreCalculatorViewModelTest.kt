package com.guyteichman.mageknightbuddy.ui.scorecalculator

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.data.toDomain
import com.guyteichman.mageknightbuddy.domain.AgainstTheApocalypseScoringInput
import com.guyteichman.mageknightbuddy.domain.AgainstTheDragonScoringInput
import com.guyteichman.mageknightbuddy.domain.AgainstTheHorsemenScoringInput
import com.guyteichman.mageknightbuddy.domain.ApocalypseIsHereScoringInput
import com.guyteichman.mageknightbuddy.domain.CombatLevel
import com.guyteichman.mageknightbuddy.domain.FirstReconnaissanceScoringInput
import com.guyteichman.mageknightbuddy.domain.ForTheCouncilScoringInput
import com.guyteichman.mageknightbuddy.domain.FracturedLandsScoringInput
import com.guyteichman.mageknightbuddy.domain.HiddenValleyScoringInput
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.LifeAndDeathScoringInput
import com.guyteichman.mageknightbuddy.domain.LostRelicScoringInput
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.RealmOfTheDeadScoringInput
import com.guyteichman.mageknightbuddy.domain.ReputationTrackSpace
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.SoloConquestScoringInput
import com.guyteichman.mageknightbuddy.domain.VolkaresQuestScoringInput
import com.guyteichman.mageknightbuddy.domain.VolkaresReturnScoringInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class ScoreCalculatorViewModelTest {

    @Test
    fun `save persists a ScoringSession built from the current field values`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.knight = Knight.WOLFHAWK
        viewModel.playerName = "Guy"
        viewModel.fame = "50"
        viewModel.spellsInDeck = "2"
        viewModel.advancedActionsInDeck = "1"
        viewModel.city1Conquered = true
        viewModel.city2Conquered = true
        viewModel.roundsFinishedEarly = "1"
        viewModel.cardsRemainingInDummyDeck = "4"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.SoloConquest, saved.scenario)
        assertEquals(Knight.WOLFHAWK, saved.knight)
        assertEquals("Guy", saved.playerName)
        // 50 fame + 5 achievements (2*2+1) + (2*10 + 15 all-cities) cities
        // + 1*30 rounds + 4 dummy cards + 5 end-of-round-not-announced = 129
        assertEquals(129, saved.score)
        assertEquals(Outcome.WON, saved.outcome)
        val input = assertIs<SoloConquestScoringInput>(saved.input)
        assertEquals(2, input.citiesConquered)
    }

    @Test
    fun `save maps a blank Player name to null`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.save()

        assertEquals(null, fakeDao.inserted.single().toDomain().playerName)
    }

    @Test
    fun `save builds a FirstReconnaissanceScoringInput when First Reconnaissance is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.FirstReconnaissance.id
        viewModel.fame = "20"
        viewModel.cityRevealed = true
        viewModel.roundsFinishedEarly = "1"
        viewModel.cardsRemainingInDummyDeck = "3"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.FirstReconnaissance, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 20 fame + 10 city revealed + 30 rounds + 3 dummy cards + 5 end-of-round-not-announced = 68
        assertEquals(68, saved.score)
        val input = assertIs<FirstReconnaissanceScoringInput>(saved.input)
        assertEquals(true, input.cityRevealed)
    }

    @Test
    fun `save builds a HiddenValleyScoringInput when The Hidden Valley is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.HiddenValley.id
        viewModel.fame = "5"
        viewModel.highPriestessDefeated = true

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.HiddenValley, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 5 fame + 20 High Priestess defeated + 5 end-of-round-not-announced (default true, so 0) = 25
        assertEquals(25, saved.score)
        val input = assertIs<HiddenValleyScoringInput>(saved.input)
        assertEquals(true, input.highPriestessDefeated)
    }

    @Test
    fun `save builds a RealmOfTheDeadScoringInput when The Realm of the Dead is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.RealmOfTheDead.id
        viewModel.fame = "5"
        viewModel.graveyardsSealed = 2
        viewModel.necromancerDefeated = true

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.RealmOfTheDead, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 5 fame + 2*5 graveyards + 10 necromancer + 10 both achieved = 35
        assertEquals(35, saved.score)
        val input = assertIs<RealmOfTheDeadScoringInput>(saved.input)
        assertEquals(2, input.graveyardsSealed)
        assertEquals(true, input.necromancerDefeated)
    }

    @Test
    fun `save builds a ForTheCouncilScoringInput from the selected Reputation track space, negative modifier included`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.ForTheCouncil.id
        viewModel.questPoints = "12"
        viewModel.reputationTrackSpaceName = ReputationTrackSpace.MINUS_3.name

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.ForTheCouncil, saved.scenario)
        assertEquals(Outcome.LOST, saved.outcome)
        // 12 quest points - 3 reputation modifier = 9
        assertEquals(9, saved.score)
        val input = assertIs<ForTheCouncilScoringInput>(saved.input)
        assertEquals(ReputationTrackSpace.MINUS_3, input.reputationTrackSpace)
    }

    @Test
    fun `save builds an AgainstTheDragonScoringInput when Against the Dragon is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.AgainstTheDragon.id
        viewModel.fame = "30"
        viewModel.headsDefeated = 4
        viewModel.roundsFinishedEarly = "1"
        viewModel.cardsRemainingInDummyDeck = "4"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.AgainstTheDragon, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 30 fame + 4*5 heads + 15 all-heads + 30 rounds + 4 dummy cards + 5 end-of-round-not-announced = 104
        assertEquals(104, saved.score)
        val input = assertIs<AgainstTheDragonScoringInput>(saved.input)
        assertEquals(4, input.headsDefeated)
    }

    @Test
    fun `save builds an AgainstTheHorsemenScoringInput when Against the Horsemen is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.AgainstTheHorsemen.id
        viewModel.fame = "20"
        viewModel.horsemenDefeated = 3
        viewModel.cardsRemainingInDummyDeck = "2"

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.AgainstTheHorsemen, saved.scenario)
        assertEquals(Outcome.LOST, saved.outcome)
        // 20 fame + 3*4 horsemen (no all-four bonus) + 2 dummy cards = 34
        assertEquals(34, saved.score)
        val input = assertIs<AgainstTheHorsemenScoringInput>(saved.input)
        assertEquals(3, input.horsemenDefeated)
    }

    @Test
    fun `save builds an ApocalypseIsHereScoringInput when Apocalypse is Here is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.ApocalypseIsHere.id
        viewModel.fame = "40"
        viewModel.horsemenDefeated = 2
        viewModel.headsDefeated = 4
        viewModel.roundsFinishedEarly = "1"
        viewModel.cardsRemainingInDummyDeck = "3"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.ApocalypseIsHere, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 40 fame + 2*3 horsemen + 4*5 heads + 15 all-heads + 30 rounds + 3 dummy cards + 5 end-of-round = 119
        assertEquals(119, saved.score)
        val input = assertIs<ApocalypseIsHereScoringInput>(saved.input)
        assertEquals(2, input.horsemenDefeated)
        assertEquals(4, input.headsDefeated)
    }

    @Test
    fun `save builds a FracturedLandsScoringInput when The Fractured Lands is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.FracturedLands.id
        viewModel.fame = "12"
        viewModel.questPoints = "5"

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.FracturedLands, saved.scenario)
        // The Fractured Lands has no lose condition - always Won.
        assertEquals(Outcome.WON, saved.outcome)
        // 12 fame + 5 quest points (Greatest Quester) = 17
        assertEquals(17, saved.score)
        val input = assertIs<FracturedLandsScoringInput>(saved.input)
        assertEquals(5, input.questPoints)
    }

    @Test
    fun `save builds a LifeAndDeathScoringInput when Life and Death is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.LifeAndDeath.id
        viewModel.fame = "25"
        viewModel.tezlaSpiritDefeated = true
        viewModel.darkTezlaDefeated = true
        viewModel.roundsFinishedEarly = "1"
        viewModel.cardsRemainingInDummyDeck = "2"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.LifeAndDeath, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 25 fame + 2*10 avatars + 15 both-avatars + 30 rounds + 2 dummy cards + 5 end-of-round = 97
        assertEquals(97, saved.score)
        val input = assertIs<LifeAndDeathScoringInput>(saved.input)
        assertEquals(true, input.tezlaSpiritDefeated)
        assertEquals(true, input.darkTezlaDefeated)
    }

    @Test
    fun `save builds a LostRelicScoringInput when The Lost Relic is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.LostRelic.id
        viewModel.fame = "18"
        viewModel.relicPiecesFound = 2
        viewModel.cardsRemainingInDummyDeck = "4"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.LostRelic, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 18 fame + 2*5 relic pieces + 10 all-pieces + 4 dummy cards + 5 end-of-round = 47
        assertEquals(47, saved.score)
        val input = assertIs<LostRelicScoringInput>(saved.input)
        assertEquals(2, input.relicPiecesFound)
    }

    @Test
    fun `save builds an AgainstTheApocalypseScoringInput mapping the ziggurat and pyramid switches to 1 or 0`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.AgainstTheApocalypse.id
        viewModel.fame = "35"
        viewModel.destroyedSiteTokens = "2"
        viewModel.zigguratFloorConquered = true
        viewModel.pyramidFloorConquered = true
        viewModel.roundsFinishedEarly = "1"
        viewModel.cardsRemainingInDummyDeck = "3"
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.AgainstTheApocalypse, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 35 fame + 2*3 destroyed sites + 2*5 sites conquered (flat, not tiered by floor) + 15 victorious
        // + 30 rounds + 3 dummy cards + 5 end-of-round = 104
        assertEquals(104, saved.score)
        val input = assertIs<AgainstTheApocalypseScoringInput>(saved.input)
        assertEquals(1, input.zigguratFloorsConquered)
        assertEquals(1, input.pyramidFloorsConquered)
    }

    @Test
    fun `save builds a VolkaresQuestScoringInput when Volkare's Quest is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.VolkaresQuest.id
        viewModel.fame = "32"
        viewModel.volkareCitiesConquered = 2
        viewModel.combatLevel = CombatLevel.HEROIC
        viewModel.raceLevel = RaceLevel.TIGHT
        viewModel.volkareDefeated = true
        viewModel.cardsRemainingInVolkaresDeck = "5"

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.VolkaresQuest, saved.scenario)
        assertEquals(Outcome.WON, saved.outcome)
        // 32 fame + 2*5 cities + Volkare combat bonus: (40 Heroic base + 2*5 cards) * 3/2 Tight = 75
        // total = 32 + 10 + 75 = 117
        assertEquals(117, saved.score)
        val input = assertIs<VolkaresQuestScoringInput>(saved.input)
        assertEquals(2, input.citiesConquered)
        assertEquals(CombatLevel.HEROIC, input.combatLevel)
        assertEquals(RaceLevel.TIGHT, input.raceLevel)
    }

    @Test
    fun `save builds a VolkaresReturnScoringInput when Volkare's Return is selected`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))

        viewModel.scenarioId = Scenario.VolkaresReturn.id
        viewModel.fame = "27"
        viewModel.cityConquered = true
        viewModel.volkareDefeated = false
        viewModel.combatLevel = CombatLevel.LEGENDARY
        viewModel.raceLevel = RaceLevel.THRILLING
        viewModel.cardsRemainingInVolkaresDeck = "8"

        viewModel.save()

        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.VolkaresReturn, saved.scenario)
        assertEquals(Outcome.LOST, saved.outcome)
        // 27 fame + 20 city conquered + 0 Volkare combat bonus (not defeated) = 47
        assertEquals(47, saved.score)
        val input = assertIs<VolkaresReturnScoringInput>(saved.input)
        assertEquals(true, input.cityConquered)
        assertEquals(false, input.volkareDefeated)
    }

    @Test
    fun `save resets the wizard back to its defaults after persisting`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))
        viewModel.pageIndex = 5
        viewModel.scenarioId = Scenario.ForTheCouncil.id
        viewModel.knight = Knight.WOLFHAWK
        viewModel.playerName = "Guy"
        viewModel.fame = "50"
        viewModel.city1Conquered = true
        viewModel.endOfRoundAnnounced = false

        viewModel.save()

        // The just-saved session must reflect the pre-reset field values, not the defaults below.
        val saved = fakeDao.inserted.single().toDomain()
        assertEquals(Scenario.ForTheCouncil, saved.scenario)
        assertEquals("Guy", saved.playerName)

        // The wizard itself must already be back to a blank first page - otherwise the player is
        // dropped back onto the just-submitted Result page next time they open this tab (issue #87).
        assertEquals(0, viewModel.pageIndex)
        assertEquals(Scenario.SoloConquest, viewModel.scenario)
        assertEquals(Knight.entries.first(), viewModel.knight)
        assertEquals("", viewModel.playerName)
        assertEquals("0", viewModel.fame)
        assertEquals(false, viewModel.city1Conquered)
        assertEquals(true, viewModel.endOfRoundAnnounced)
    }

    @Test
    fun `reset clears all fields back to their defaults`() = runTest {
        val fakeDao = FakeScoringSessionDao()
        val viewModel = ScoreCalculatorViewModel(SavedStateHandle(), ScoringSessionRepository(fakeDao))
        viewModel.pageIndex = 5
        viewModel.knight = Knight.WOLFHAWK
        viewModel.playerName = "Guy"
        viewModel.fame = "50"
        viewModel.city1Conquered = true
        viewModel.endOfRoundAnnounced = false
        viewModel.scenarioId = Scenario.ForTheCouncil.id
        viewModel.cityRevealed = true
        viewModel.highPriestessDefeated = true
        viewModel.graveyardsSealed = 2
        viewModel.necromancerDefeated = true
        viewModel.reputationTrackSpaceName = ReputationTrackSpace.NEGATIVE_X.name

        viewModel.reset()

        assertEquals(0, viewModel.pageIndex)
        assertEquals(Scenario.SoloConquest, viewModel.scenario)
        assertEquals(Knight.entries.first(), viewModel.knight)
        assertEquals("", viewModel.playerName)
        assertEquals("0", viewModel.fame)
        assertEquals(false, viewModel.city1Conquered)
        assertEquals(true, viewModel.endOfRoundAnnounced)
        assertEquals(false, viewModel.cityRevealed)
        assertEquals(false, viewModel.highPriestessDefeated)
        assertEquals(0, viewModel.graveyardsSealed)
        assertEquals(false, viewModel.necromancerDefeated)
        assertEquals(ReputationTrackSpace.CENTER.name, viewModel.reputationTrackSpaceName)
    }
}
