package com.guyteichman.mageknightbuddy.domain

/**
 * One drawn token recorded in the Enemy Picker's Draw Log (see `CONTEXT.md`'s "Draw Log"). Because
 * the picker holds an undefeated token *on the board* (out of both piles) and models no map, this
 * log is the *only* record of which enemy is still standing where - so entries are never removed,
 * only appended and flagged.
 *
 * [tokenId] points back into the [EnemyToken] catalogue (re-opening the entry shows that token's
 * full info). [batchId] groups tokens that were drawn together in one action (a stepper draw of
 * several, or - later - a multi-pile draw), so the UI can show them as one grouped entry.
 *
 * [defeated] tracks the drawn enemy's lifecycle (issue #251): a freshly drawn enemy is **on the
 * board** (`defeated = false`), held out of both the draw pile and the discard; it is marked
 * [defeated] once the player defeats it - the common reveal→defeat flow is a single tap - at which
 * point [EnemyPickerSession.setDefeated] moves it into its [TokenPile]'s discard (so it can later
 * be reshuffled on a Replenish). So unlike before #251, this flag is *not* a pure memory aid: it
 * governs whether the token is on the board or in the discard (ADR-0006, amended by #251). Its
 * free-text [note] ("keep, NE tile") is the accompanying memory aid. Kept as a plain domain data
 * class (no serialization annotations) per ADR-0001; the data layer has its own DTO mirror.
 *
 * [parentIndex] is set only for a **Summon Draw** child (see `CONTEXT.md`'s "Summon Draw"): the
 * chronological [EnemyPickerSession.drawLog] index of the summoner entry this token was drawn for.
 * A summoner can be re-engaged and re-summon a fresh child; every summon appends a new entry rather
 * than overwriting the old one (the log is append-only), so several entries can share one
 * [parentIndex] - [EnemyPickerSession.currentChildrenOf] resolves which of them is current (the
 * most recent shared [batchId]) rather than this field alone.
 *
 * [ephemeral] distinguishes the two kinds of child that both carry a [parentIndex] (issue #251): a
 * **true Summon-ability child** is `ephemeral = true` - it is discarded the instant it is drawn and
 * never independently defeated (it just fills the summoner's fight slot), so it goes straight to the
 * discard rather than on the board. A ruin **Enemies-With-Treasure** child reuses the same summon
 * machinery but is `ephemeral = false`: it is a real, independently-defeatable enemy that stays on
 * the board until beaten, exactly like a normal draw. Normal (parentless) draws are always
 * `ephemeral = false`.
 */
data class DrawLogEntry(
    val tokenId: String,
    val pile: TokenPileId,
    val batchId: Long,
    val defeated: Boolean = false,
    val note: String = "",
    val parentIndex: Int? = null,
    val ephemeral: Boolean = false,
)
