package com.guyteichman.mageknightbuddy.domain

/**
 * The live state of one face-down [Token Pile][TokenPileId] in an [EnemyPickerSession]: the
 * still-face-down [drawPile] (top = first element) and the [discardPile] of tokens already drawn
 * this game. Both are lists of [EnemyToken.id]s, with duplicates where the box holds several copies
 * of the same token.
 *
 * When [drawPile] empties, the picker **Replenishes** it - the discard is shuffled to become the
 * new draw pile (see `CONTEXT.md`'s "Replenish"). Under Draw with Replacement no discard ever
 * accumulates, so [discardPile] stays empty. Plain domain data class per ADR-0001.
 */
data class TokenPile(
    val drawPile: List<String>,
    val discardPile: List<String> = emptyList(),
)
