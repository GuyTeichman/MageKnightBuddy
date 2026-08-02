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
 * [APOCALYPSE_DRAGON] carries the three AD Site Description cards (Oasis, Ziggurat, Pyramid) added in
 * issue #238 - transcribed in `docs/rules/sites.md` from the AD rulebook, since the Quick Reference
 * Sheet predates that expansion. AD adds no other in-scope map-features: "Destroyed Sites" is a
 * token/mechanic rather than a Site Description card and is excluded (see `docs/rules/sites.md`).
 *
 * `@Serializable` so it can tag each [Site] in the JSON catalogue (ADR-0007).
 */
@Serializable
enum class SiteExpansion {
    BASE,
    LOST_LEGION,
    SHADES_OF_TEZLA,
    APOCALYPSE_DRAGON,
}
