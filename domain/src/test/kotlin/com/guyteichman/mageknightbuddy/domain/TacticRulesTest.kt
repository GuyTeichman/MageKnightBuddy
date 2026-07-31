package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TacticRulesTest {

    @Test
    fun `solo always returns EveryRound BOTH, regardless of scenario`() {
        assertEquals(
            TacticRemovalRule.EveryRound(RemovalTarget.BOTH),
            tacticRemovalRule(isVolkare = false, isSolo = true, scenario = Scenario.SoloConquest),
        )
        assertEquals(
            TacticRemovalRule.EveryRound(RemovalTarget.BOTH),
            tacticRemovalRule(isVolkare = false, isSolo = true, scenario = Scenario.LostRelic),
        )
    }

    @Test
    fun `solo Volkare removes only the player's card, never Volkare's own, ignoring scenario`() {
        assertEquals(
            TacticRemovalRule.EveryRound(RemovalTarget.PLAYER_ONLY),
            tacticRemovalRule(isVolkare = true, isSolo = true, scenario = Scenario.VolkaresReturn),
        )
        assertEquals(
            TacticRemovalRule.EveryRound(RemovalTarget.PLAYER_ONLY),
            tacticRemovalRule(isVolkare = true, isSolo = true, scenario = Scenario.VolkaresQuest),
        )
    }

    @Test
    fun `coop Volkare never removes anything`() {
        assertEquals(
            TacticRemovalRule.Never,
            tacticRemovalRule(isVolkare = true, isSolo = false, scenario = Scenario.VolkaresQuest),
        )
    }

    @Test
    fun `coop Solo Conquest removes the player's card every round`() {
        assertEquals(
            TacticRemovalRule.EveryRound(RemovalTarget.PLAYER_ONLY),
            tacticRemovalRule(isVolkare = false, isSolo = false, scenario = Scenario.SoloConquest),
        )
    }

    @Test
    fun `coop scenarios that remove once at the end of the first Day only`() {
        for (scenario in listOf(Scenario.FirstReconnaissance, Scenario.ForTheCouncil, Scenario.AgainstTheApocalypse)) {
            assertEquals(
                TacticRemovalRule.FirstDayOnly(RemovalTarget.PLAYER_ONLY),
                tacticRemovalRule(isVolkare = false, isSolo = false, scenario = scenario),
                "expected FirstDayOnly for $scenario",
            )
        }
    }

    @Test
    fun `coop scenarios that remove across the first two rounds`() {
        for (scenario in listOf(Scenario.AgainstTheHorsemen, Scenario.AgainstTheDragon, Scenario.FracturedLands)) {
            assertEquals(
                TacticRemovalRule.FirstTwoRounds(RemovalTarget.PLAYER_ONLY),
                tacticRemovalRule(isVolkare = false, isSolo = false, scenario = scenario),
                "expected FirstTwoRounds for $scenario",
            )
        }
    }

    @Test
    fun `coop scenarios that never remove anything`() {
        val neverScenarios = listOf(
            Scenario.ApocalypseIsHere,
            Scenario.RealmOfTheDead,
            Scenario.HiddenValley,
            Scenario.LifeAndDeath,
            Scenario.LostRelic,
        )
        for (scenario in neverScenarios) {
            assertEquals(
                TacticRemovalRule.Never,
                tacticRemovalRule(isVolkare = false, isSolo = false, scenario = scenario),
                "expected Never for $scenario",
            )
        }
    }

    @Test
    fun `coop lookup throws for scenarios excluded from the coop Scenario picker`() {
        val excluded = listOf(Scenario.VolkaresReturn, Scenario.VolkaresQuest, Scenario.SoloConquestChallenge)
        for (scenario in excluded) {
            assertFailsWith<IllegalArgumentException>("expected throw for $scenario") {
                tacticRemovalRule(isVolkare = false, isSolo = false, scenario = scenario)
            }
        }
    }

    @Test
    fun `tacticRemovalTarget EveryRound fires on every round regardless of startsAtNight`() {
        val rule = TacticRemovalRule.EveryRound(RemovalTarget.BOTH)
        for (round in 1..6) {
            assertEquals(RemovalTarget.BOTH, tacticRemovalTarget(rule, round, startsAtNight = false))
            assertEquals(RemovalTarget.BOTH, tacticRemovalTarget(rule, round, startsAtNight = true))
        }
    }

    @Test
    fun `tacticRemovalTarget FirstDayOnly fires exactly at the first Day round, for both start parities`() {
        val rule = TacticRemovalRule.FirstDayOnly(RemovalTarget.PLAYER_ONLY)

        // startsAtNight = false: round 1 is the first Day.
        assertEquals(RemovalTarget.PLAYER_ONLY, tacticRemovalTarget(rule, round = 1, startsAtNight = false))
        assertEquals(null, tacticRemovalTarget(rule, round = 2, startsAtNight = false))

        // startsAtNight = true: round 1 is a Night, round 2 is the first Day.
        assertEquals(null, tacticRemovalTarget(rule, round = 1, startsAtNight = true))
        assertEquals(RemovalTarget.PLAYER_ONLY, tacticRemovalTarget(rule, round = 2, startsAtNight = true))
        assertEquals(null, tacticRemovalTarget(rule, round = 3, startsAtNight = true))
    }

    @Test
    fun `tacticRemovalTarget FirstTwoRounds fires on rounds 1 and 2 only, for both start parities`() {
        val rule = TacticRemovalRule.FirstTwoRounds(RemovalTarget.PLAYER_ONLY)

        for (startsAtNight in listOf(false, true)) {
            assertEquals(RemovalTarget.PLAYER_ONLY, tacticRemovalTarget(rule, round = 1, startsAtNight))
            assertEquals(RemovalTarget.PLAYER_ONLY, tacticRemovalTarget(rule, round = 2, startsAtNight))
            assertEquals(null, tacticRemovalTarget(rule, round = 3, startsAtNight))
        }
    }

    @Test
    fun `tacticRemovalTarget Never always returns null`() {
        assertEquals(null, tacticRemovalTarget(TacticRemovalRule.Never, round = 1, startsAtNight = false))
        assertEquals(null, tacticRemovalTarget(TacticRemovalRule.Never, round = 4, startsAtNight = true))
    }

    @Test
    fun `coopTacticScenarios is every Scenario except the 3 excluded from the coop picker`() {
        // Independent ground truth (not derived from tacticRemovalRule's own logic, per CLAUDE.md's
        // round-trip-assertion warning): every Scenario entry that has a real coop-branch row in
        // tacticRemovalRule's `when`, hand-listed from that function's table rather than computed
        // the same way coopTacticScenarios itself is computed.
        val expected = listOf(
            Scenario.SoloConquest,
            Scenario.FirstReconnaissance,
            Scenario.ForTheCouncil,
            Scenario.AgainstTheApocalypse,
            Scenario.AgainstTheHorsemen,
            Scenario.AgainstTheDragon,
            Scenario.FracturedLands,
            Scenario.ApocalypseIsHere,
            Scenario.RealmOfTheDead,
            Scenario.HiddenValley,
            Scenario.LifeAndDeath,
            Scenario.LostRelic,
        )

        assertEquals(expected.toSet(), coopTacticScenarios.toSet())
        // Excluded 3 must genuinely be absent, not just "expected happens to be missing them".
        for (excluded in listOf(Scenario.VolkaresReturn, Scenario.VolkaresQuest, Scenario.SoloConquestChallenge)) {
            assertEquals(false, excluded in coopTacticScenarios, "$excluded should not appear in coopTacticScenarios")
        }
    }

    @Test
    fun `tacticPickOrder is player-first for solo, dummy-first for coop, except Volkare coop is player-first too`() {
        assertEquals(PickOrder.PLAYER_FIRST, tacticPickOrder(isVolkare = false, isSolo = true))
        assertEquals(PickOrder.DUMMY_FIRST, tacticPickOrder(isVolkare = false, isSolo = false))
        assertEquals(PickOrder.PLAYER_FIRST, tacticPickOrder(isVolkare = true, isSolo = true))
        assertEquals(PickOrder.PLAYER_FIRST, tacticPickOrder(isVolkare = true, isSolo = false))
    }
}
