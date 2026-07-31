package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * Which *Mage Knight* product a [Site] first appeared in, used by the Sites tab to badge and (later)
 * group/filter the catalogue - see `docs/rules/sites.md`'s "Expansion attribution" table.
 *
 * This is deliberately a **separate enum from [Expansion]** (the Enemy Picker's Token Set uses that
 * one), for three reasons the sites domain doesn't share with tokens:
 * - Sites never split Shades of Tezla by faction - [Expansion] has `SHADES_OF_TEZLA_ELEMENTALIST` /
 *   `SHADES_OF_TEZLA_DARK_CRUSADER` because *tokens* come in two factions, but the three Shades
 *   location tiles (Hidden Valley / Necropolis / Graveyard) belong to no faction, so a single
 *   [SHADES_OF_TEZLA] value is the correct shape here.
 * - No site comes from the Krang expansion (its only addition is the "Flip Back" ability), so there
 *   is no `KRANG` value to carry.
 * - Reusing [Expansion] would couple the Sites tab to the Token Set's meaning ("which tokens make up
 *   this game's piles"), which is unrelated.
 *
 * [APOCALYPSE_DRAGON] is intentionally absent for now: its sites are deferred to issue #238, and a
 * value with no entries would break the catalogue-validation test's exact per-expansion counts. Add
 * it when those sites are transcribed.
 *
 * `@Serializable` so it can tag each [Site] in the JSON catalogue (ADR-0007).
 */
@Serializable
enum class SiteExpansion {
    BASE,
    LOST_LEGION,
    SHADES_OF_TEZLA,
}
