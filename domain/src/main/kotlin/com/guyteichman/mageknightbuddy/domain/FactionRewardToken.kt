package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One transcribed **faction reward token** - the held reward you earn (typically by defeating a
 * faction or possessed enemy) from one of the four faction piles: the Shades of Tezla Elementalist
 * and Dark Crusader factions, and the Apocalypse Dragon Apocalypse Cult and Council of the Void
 * factions (see `docs/rules/faction-reward-tokens.md` and `CONTEXT.md`'s "Faction Reward Token").
 *
 * A sibling type to [EnemyToken]/[RuinToken], not a variant: a reward token prints no
 * Armor/Attack/Fame block, only a one-off effect. So the shape it *does* share with [EnemyToken] is
 * exactly the pile-building trio - [pile] (which face-down stack it belongs to), [copies] (how many
 * the box holds; every reward token comes in 2), and [expansion] (which [Token Set][EnemyPickerSession]
 * toggle gates it) - and nothing else.
 *
 * [effectText] is the token's printed effect, carried as **free reference text shown but never
 * resolved** (ADR-0006), exactly like [RuinToken.reward]: the Enemy Picker draws the token and shows
 * you what it does, but the player applies the effect themselves. The universal *"may be discarded
 * during interactions for 1 Fame, or 3 Influence"* line printed on **every** reward token is
 * deliberately *not* stored here (it's identical across all 24 tokens); it's documented once in the
 * rules doc and shown as a fixed footer by the UI, so [effectText] holds only what differs per token.
 *
 * `@Serializable` so the whole catalogue decodes straight into `List<FactionRewardToken>` from
 * `faction-reward-tokens.json`, the same JSON-in-`domain/`-resources approach [EnemyToken] uses
 * (ADR-0007).
 */
@Serializable
data class FactionRewardToken(
    val id: String,
    val expansion: Expansion,
    val pile: TokenPileId,
    val name: String,
    val effectText: String,
    val copies: Int = 2,
)
