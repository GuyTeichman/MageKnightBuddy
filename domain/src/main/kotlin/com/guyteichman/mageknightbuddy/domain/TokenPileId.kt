package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * Identifies one face-down [Token Pile][EnemyPickerSession] the Enemy Picker draws from. The first
 * seven are the uniform "draw one, discard it" piles sorted by token back (rulebook p.3, "seven face
 * down piles"): the six enemy colors plus the hexagonal ruin pile. Then the Apocalypse Dragon
 * [POSSESSED] pile (issue #189), and the four **faction reward** piles (issue #252) - one per faction
 * across two expansions.
 *
 * A faction reward token has a *different lifecycle* to an enemy: it's earned (typically by defeating
 * a faction/possessed enemy), **held** face up until used, then spent for its effect or discarded for
 * 1 Fame / 3 Influence (see `docs/rules/faction-reward-tokens.md` and `CONTEXT.md`'s "Faction Reward
 * Token"). Since issue #251 that held-vs-spent state is pile-correct: a drawn reward is held on the
 * board (out of both piles) until "spent" (the defeat action) moves it into the discard - the same
 * lifecycle as a defeated enemy (see [EnemyPickerSession]).
 *
 * A [POSSESSED] token is likewise not an enemy on its own: it is paired with a circular enemy drawn
 * from one of the colour piles to form one composite possessed enemy, so that pile is only ever drawn
 * as the companion of a `possessed` [EnemyPickerSession.draw] (see docs/rules/apocalypse-dragon.md).
 *
 * `@Serializable` so this can be a field on [EnemyToken]/[FactionRewardToken] in the JSON catalogue
 * (ADR-0007); the enum name is what appears in the JSON. Draw-log/display order follows [entries]
 * order, so POSSESSED and the four reward piles sort after the enemy/ruin piles.
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

    /**
     * Apocalypse Dragon possessed enemy tokens (see docs/rules/apocalypse-dragon.md). Unlike the
     * other piles, a token drawn here is never an enemy on its own - it is paired with a circular
     * enemy drawn from one of the colour piles to form one composite possessed enemy, so this pile
     * is only ever drawn as the companion of a `possessed` [EnemyPickerSession.draw], never on its own.
     */
    POSSESSED,

    /** Shades of Tezla Elementalist faction reward tokens (held rewards, not enemies). */
    ELEMENTALIST_REWARDS,

    /** Shades of Tezla Dark Crusader faction reward tokens. */
    DARK_CRUSADER_REWARDS,

    /** Apocalypse Dragon "The Apocalypse Cult" faction reward tokens. */
    APOCALYPSE_CULT_REWARDS,

    /** Apocalypse Dragon "The Council of the Void" faction reward tokens. */
    COUNCIL_OF_VOID_REWARDS;

    /**
     * Whether this is one of the four faction reward piles (held rewards) rather than an enemy colour,
     * the RUIN pile, or the POSSESSED pile. Used to give reward piles their own draw/held/spend UI
     * treatment. An exhaustive `when` (no `else`) so adding a future pile forces a decision here
     * rather than silently defaulting.
     */
    val isFactionReward: Boolean
        get() = when (this) {
            ELEMENTALIST_REWARDS, DARK_CRUSADER_REWARDS, APOCALYPSE_CULT_REWARDS, COUNCIL_OF_VOID_REWARDS -> true
            GREEN, GREY, VIOLET, BROWN, RED, WHITE, RUIN, POSSESSED -> false
        }
}
