package com.guyteichman.mageknightbuddy.domain

/**
 * One drawn token recorded in the Enemy Picker's Draw Log (see `CONTEXT.md`'s "Draw Log"). Because
 * the picker discards a token the instant it is drawn and models no map, this log is the *only*
 * record of which enemy is still standing where - so entries are never removed, only appended and
 * flagged.
 *
 * [tokenId] points back into the [EnemyToken] catalogue (re-opening the entry shows that token's
 * full info). [batchId] groups tokens that were drawn together in one action (a stepper draw of
 * several, or - later - a multi-pile draw), so the UI can show them as one grouped entry.
 *
 * [defeated] tracks the drawn enemy's lifecycle: a freshly drawn enemy is **on the board**
 * (`defeated = false`) because it was just revealed, and is marked [defeated] once the player
 * defeats it - the common reveal→defeat flow is a single tap. Its free-text [note] ("keep, NE
 * tile") is the accompanying memory aid. Both are *purely* a memory aid: neither has any effect on
 * a [TokenPile] (a defeated enemy was already discarded the instant it was drawn), so draw odds are
 * identical whichever way this is set (ADR-0006). Kept as a plain domain data class (no
 * serialization annotations) per ADR-0001; the data layer has its own DTO mirror.
 */
data class DrawLogEntry(
    val tokenId: String,
    val pile: TokenPileId,
    val batchId: Long,
    val defeated: Boolean = false,
    val note: String = "",
)
