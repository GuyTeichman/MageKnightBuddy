package com.guyteichman.mageknightbuddy.domain

/**
 * Which Tactic pick(s) a Round-end removal targets, when a [TacticRemovalRule] fires - the
 * Dummy Player's/Volkare's/Proxy Player's own pick is never removed alone; either both picks go
 * (Solo's universal rule, and Volkare Solo's rule) or only the real player's does (every
 * Cooperative scenario that removes anything at all). See `docs/context-dummy-player.md`'s
 * "Tactic Selection" entry for the rules sweep this table comes from.
 */
enum class RemovalTarget { BOTH, PLAYER_ONLY }

/**
 * How often a scenario/mode permanently removes a used Tactic card from its pile, verified
 * against the physical rulebook PDFs (not just this repo's docs/rules Markdown extractions) for
 * issue #179. Four shapes cover every scenario this app models - see `TacticRules.kt` for the
 * lookup that decides which shape applies and, each Round, whether it actually fires.
 */
sealed interface TacticRemovalRule {
    /** Fires every Round, e.g. Solo's universal rule and Full/Blitz Cooperation's coop rule. */
    data class EveryRound(val target: RemovalTarget) : TacticRemovalRule

    /** Fires only at the end of the scenario's first Day Round, never again. */
    data class FirstDayOnly(val target: RemovalTarget) : TacticRemovalRule

    /** Fires at the end of Rounds 1 and 2 only (always exactly one Day, one Night), never again. */
    data class FirstTwoRounds(val target: RemovalTarget) : TacticRemovalRule

    /** Never fires - e.g. Apocalypse is Here's coop mode, Volkare's coop mode. */
    data object Never : TacticRemovalRule
}
