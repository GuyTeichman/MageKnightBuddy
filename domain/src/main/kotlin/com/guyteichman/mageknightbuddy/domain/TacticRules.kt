package com.guyteichman.mageknightbuddy.domain

/**
 * Decides which [TacticRemovalRule] applies to a Round's Tactic Selection, given the mode
 * (Volkare vs. Standard/Proxy Player), solo vs. coop, and - for coop, non-Volkare only - which
 * [Scenario] is being played. See `docs/context-dummy-player.md`'s "Tactic Selection" entry and
 * ADR-0008 (docs/adr/0008-reuse-scenario-for-tactic-rule-lookup.md) for the rules sweep and the
 * decision to key this off the existing [Scenario] type without adding new entries for it.
 *
 * Solo is a deliberately accepted simplification: every solo scenario removes both picks every
 * Round *except* For the Council, which only removes once (end of the first Day) - this function
 * ignores that one exception and always returns [TacticRemovalRule.EveryRound] with
 * [RemovalTarget.BOTH] for solo, since solo gets no Scenario picker in the UI at all.
 *
 * [scenario] is only read for Standard/Proxy Player coop mode. [Scenario.VolkaresReturn],
 * [Scenario.VolkaresQuest], and [Scenario.SoloConquestChallenge] must never reach the coop
 * branch below - the first two are Volkare-only (excluded from the coop Scenario picker
 * entirely, since Volkare's own rule is fixed and doesn't depend on [Scenario]), and the third
 * has no documented coop variant anywhere in the rulebook (also excluded from the picker). Each
 * throws here as a defensive "should never happen" guard, not a real UI path.
 */
fun tacticRemovalRule(isVolkare: Boolean, isSolo: Boolean, scenario: Scenario): TacticRemovalRule {
    // Volkare is checked first: its rule is fixed and doesn't follow the general solo/coop split
    // below (its own pick is never removed, even in solo - PLAYER_ONLY, not BOTH). Checking
    // isSolo first here previously short-circuited Volkare solo into the wrong BOTH rule.
    if (isVolkare) return if (isSolo) TacticRemovalRule.EveryRound(RemovalTarget.PLAYER_ONLY) else TacticRemovalRule.Never
    if (isSolo) return TacticRemovalRule.EveryRound(RemovalTarget.BOTH)

    return when (scenario) {
        Scenario.SoloConquest -> TacticRemovalRule.EveryRound(RemovalTarget.PLAYER_ONLY)

        Scenario.FirstReconnaissance,
        Scenario.ForTheCouncil,
        Scenario.AgainstTheApocalypse,
        -> TacticRemovalRule.FirstDayOnly(RemovalTarget.PLAYER_ONLY)

        Scenario.AgainstTheHorsemen,
        Scenario.AgainstTheDragon,
        Scenario.FracturedLands,
        -> TacticRemovalRule.FirstTwoRounds(RemovalTarget.PLAYER_ONLY)

        Scenario.ApocalypseIsHere,
        Scenario.RealmOfTheDead,
        Scenario.HiddenValley,
        Scenario.LifeAndDeath,
        Scenario.LostRelic,
        -> TacticRemovalRule.Never

        Scenario.VolkaresReturn, Scenario.VolkaresQuest, Scenario.SoloConquestChallenge ->
            throw IllegalArgumentException(
                "$scenario has no coop Tactic-removal rule - it must be excluded from the coop Scenario picker (see ADR-0008)",
            )
    }
}

/**
 * Whether [rule] fires at the end of this specific Round, and if so, which pick(s) it targets -
 * `null` means nothing fires. [round] and [startsAtNight] are the session's own existing fields
 * (see [DummyPlayerSession.isDay]'s sibling `isDayRound` helper) - the "first Day" Round number
 * depends on whether the scenario started at night, so it's derived here rather than assumed to
 * always be Round 1.
 */
fun tacticRemovalTarget(rule: TacticRemovalRule, round: Int, startsAtNight: Boolean): RemovalTarget? {
    val firstDayRound = if (startsAtNight) 2 else 1
    // `when` over a `sealed interface` is exhaustive: the compiler requires every implementing
    // type (TacticRemovalRule.EveryRound/FirstDayOnly/FirstTwoRounds/Never) to have its own
    // branch here, with no `else` needed - if a new removal shape is ever added, this fails to
    // compile until it's handled here too.
    return when (rule) {
        is TacticRemovalRule.EveryRound -> rule.target
        is TacticRemovalRule.FirstDayOnly -> if (round == firstDayRound) rule.target else null
        is TacticRemovalRule.FirstTwoRounds -> if (round <= 2) rule.target else null
        TacticRemovalRule.Never -> null
    }
}

/** Who drafts a Tactic card first this Round. */
enum class PickOrder { PLAYER_FIRST, DUMMY_FIRST }

/**
 * The real player always picks first in Solo (every mode) and in Volkare's coop mode - the one
 * exception among every coop scenario this app models, where every other coop scenario has the
 * Dummy Player/Proxy Player pick first instead. See `docs/context-dummy-player.md`'s "Tactic
 * Selection" entry.
 */
fun tacticPickOrder(isVolkare: Boolean, isSolo: Boolean): PickOrder =
    if (isVolkare || isSolo) PickOrder.PLAYER_FIRST else PickOrder.DUMMY_FIRST
