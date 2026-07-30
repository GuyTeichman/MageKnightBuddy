package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * Which product a token comes from, used by the Enemy Picker's [Token Set][EnemyPickerSession] to
 * decide which [EnemyToken]s make up this game's piles (see `CONTEXT.md`'s "Token Set"). A
 * per-game setup choice, deliberately distinct from the future global [Settings] "which expansions
 * do I own" toggle.
 *
 * `@Serializable` so it can tag each [EnemyToken] in the JSON catalogue (ADR-0007).
 *
 * Shades of Tezla is split into **two** entries - one per faction ([SHADES_OF_TEZLA_ELEMENTALIST],
 * [SHADES_OF_TEZLA_DARK_CRUSADER]) - rather than one, because its enemy tokens come in two
 * factions whose enemies are chosen per scenario. This app only models the rulebook's "mix the
 * faction tokens into the regular piles" mode (Shades of Tezla rules, "Variants for Other
 * Scenarios"), so each faction is just another independently-selectable token set that shuffles
 * into the same six colour piles - like [LOST_LEGION]. The stricter "faction-only, separate piles
 * per faction" scenarios need per-faction piles the Token Set can't express yet, and are deferred
 * (issues #189/#190). [APOCALYPSE_DRAGON] is listed ahead of its tokens being transcribed.
 */
@Serializable
enum class Expansion {
    BASE,
    LOST_LEGION,
    SHADES_OF_TEZLA_ELEMENTALIST,
    SHADES_OF_TEZLA_DARK_CRUSADER,
    APOCALYPSE_DRAGON,
}
