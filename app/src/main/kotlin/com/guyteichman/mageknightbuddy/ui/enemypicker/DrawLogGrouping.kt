package com.guyteichman.mageknightbuddy.ui.enemypicker

import com.guyteichman.mageknightbuddy.domain.DrawLogEntry

/**
 * One Draw Log row's worth of entries (issue #203, D18 - supersedes the older "one row per token"
 * D3): either a single drawn token, or a whole multi-token batch (shared [DrawLogEntry.batchId])
 * collapsed into one row. [logIndices] are chronological (oldest first) indices into
 * [com.guyteichman.mageknightbuddy.domain.EnemyPickerSession.drawLog].
 */
internal data class DrawLogGroup(val batchId: Long, val logIndices: List<Int>)

/**
 * Splits the flat, chronological Draw Log into per-batch groups (D18), newest batch first. Summon
 * Draw children ([DrawLogEntry.parentIndex] != null) are excluded - they're never shown as their
 * own top-level row, same as before #203. A batch's entries are always appended contiguously by
 * [com.guyteichman.mageknightbuddy.domain.EnemyPickerSession.draw], so `groupBy` (which preserves
 * each key's first-seen order, via its internal `LinkedHashMap`) reproduces chronological order.
 */
internal fun groupDrawLog(log: List<DrawLogEntry>): List<DrawLogGroup> =
    log.withIndex()
        .filter { it.value.parentIndex == null }
        .groupBy { it.value.batchId }
        .map { (batchId, entries) -> DrawLogGroup(batchId, entries.map { it.index }) }
        .asReversed()

/**
 * True once every member of [group] is defeated (D19) - a batch's collapsed row only moves to the
 * "Defeated" section once nothing in it still needs attention; it stays "on the board" while even
 * one member does.
 */
internal fun DrawLogGroup.allDefeated(log: List<DrawLogEntry>): Boolean = logIndices.all { log[it].defeated }
