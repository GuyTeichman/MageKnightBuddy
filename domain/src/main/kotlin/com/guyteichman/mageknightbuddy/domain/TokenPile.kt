package com.guyteichman.mageknightbuddy.domain

/**
 * The live state of one face-down [Token Pile][TokenPileId] in an [EnemyPickerSession]: the
 * still-face-down [drawPile] (top = first element) and the [discardPile] of **defeated/discarded**
 * tokens. Both are lists of [EnemyToken.id]s, with duplicates where the box holds several copies of
 * the same token.
 *
 * Since issue #251 [discardPile] holds only tokens that have been **discarded** - a defeated enemy,
 * a spent faction reward, an ephemeral summon child - not every drawn token. A drawn-but-undefeated
 * token is held **on the board**: out of *both* lists, recorded only by its [DrawLogEntry] (see
 * [EnemyPickerSession]). So `drawPile + discardPile` is not the pile's full contents while tokens
 * stand on the board.
 *
 * When [drawPile] empties, the picker **Replenishes** it - the discard is shuffled to become the
 * new draw pile (see `CONTEXT.md`'s "Replenish"); on-board tokens are never reshuffled. If the
 * discard is empty too (everything drawn is on the board), the pile is genuinely empty until a token
 * is defeated back into the discard. Under Draw with Replacement no discard ever accumulates, so
 * [discardPile] stays empty. Plain domain data class per ADR-0001.
 */
data class TokenPile(
    val drawPile: List<String>,
    val discardPile: List<String> = emptyList(),
)
