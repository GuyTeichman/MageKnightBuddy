package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * Identifies one face-down [Token Pile][EnemyPickerSession] the Enemy Picker draws from. The first
 * seven are the uniform "draw one, discard it" piles sorted by token back (rulebook p.3, "seven face
 * down piles"): the six enemy colors plus the hexagonal ruin pile. The last four are the **faction
 * reward** piles (issue #252) - one per faction across two expansions.
 *
 * A faction reward token has a *different lifecycle* to an enemy: it's earned (typically by defeating
 * a faction/possessed enemy), **held** face up until used, then spent for its effect or discarded for
 * 1 Fame / 3 Influence (see `docs/rules/faction-reward-tokens.md` and `CONTEXT.md`'s "Faction Reward
 * Token"). The Enemy Picker still owns only the *randomness of the draw* here, not the held/spent
 * bookkeeping (the player tracks that, same as which enemies are still standing) - the pile-correct
 * held-vs-spent modelling is the follow-up in issue #251. Possessed-enemy piles are still deliberately
 * *not* here (composite two-token draws with a different shape); they arrive with issue #189.
 *
 * `@Serializable` so this can be a field on [EnemyToken]/[FactionRewardToken] in the JSON catalogue
 * (ADR-0007); the enum name is what appears in the JSON. Draw-log/display order follows
 * [entries] order, so the four reward piles sort after the enemy/ruin piles.
 */
@Serializable
enum class TokenPileId {
    /** Marauding Orcs and other green enemies - the base game's simplest pile. */
    GREEN,
    GREY,
    VIOLET,
    BROWN,
    RED,
    WHITE,

    /** Hexagonal ruin tokens (altars/enemies drawn at ancient ruins), a non-enemy pile. */
    RUIN,

    /** Shades of Tezla Elementalist faction reward tokens (held rewards, not enemies). */
    ELEMENTALIST_REWARDS,

    /** Shades of Tezla Dark Crusader faction reward tokens. */
    DARK_CRUSADER_REWARDS,

    /** Apocalypse Dragon "The Apocalypse Cult" faction reward tokens. */
    APOCALYPSE_CULT_REWARDS,

    /** Apocalypse Dragon "The Council of the Void" faction reward tokens. */
    COUNCIL_OF_VOID_REWARDS;

    /**
     * Whether this is one of the four faction reward piles (held rewards) rather than an enemy colour
     * or the RUIN pile. Used to give reward piles their own draw/held/spend UI treatment. An
     * exhaustive `when` (no `else`) so adding a future pile forces a decision here rather than
     * silently defaulting.
     */
    val isFactionReward: Boolean
        get() = when (this) {
            ELEMENTALIST_REWARDS, DARK_CRUSADER_REWARDS, APOCALYPSE_CULT_REWARDS, COUNCIL_OF_VOID_REWARDS -> true
            GREEN, GREY, VIOLET, BROWN, RED, WHITE, RUIN -> false
        }
}
