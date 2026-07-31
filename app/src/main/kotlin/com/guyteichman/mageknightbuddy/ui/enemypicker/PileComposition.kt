package com.guyteichman.mageknightbuddy.ui.enemypicker

/**
 * One identity's worth of a pile's remaining tokens (issue #231): the [tokenId], its resolved
 * [displayName], and how many copies of it are still face-down in the draw pile ([count]).
 */
internal data class PileTokenGroup(val tokenId: String, val displayName: String, val count: Int)

/**
 * A face-down pile summarised for the "view draw pile" dialog (issue #231): its remaining tokens
 * [grouped by identity][groups] (alphabetical by display name) plus the [total] still in the pile.
 * Deliberately *unordered composition* - it never exposes draw order, so it can't reveal the next
 * token and undermine the picker's face-down secrecy (see `CONTEXT.md`'s "Enemy Picker").
 */
internal data class PileComposition(val groups: List<PileTokenGroup>, val total: Int)

/**
 * Groups a [drawPile]'s token ids into a [PileComposition]: identical ids collapse into one
 * [PileTokenGroup] carrying a copy count, sorted alphabetically (case-insensitively) by the display
 * name [nameOf] resolves for each id, with the id as a stable tie-break. [total] is just the pile's
 * size. Pure and catalogue-agnostic - the caller injects [nameOf] (the enemy/ruin catalogue lookup),
 * which keeps this testable without Android or the real catalogues.
 */
internal fun composePile(drawPile: List<String>, nameOf: (String) -> String): PileComposition {
    val groups = drawPile
        // groupingBy{it}.eachCount() tallies how many times each id appears -> Map<id, count>,
        // without materialising the intermediate per-id lists a plain groupBy would build.
        .groupingBy { it }
        .eachCount()
        .map { (id, count) -> PileTokenGroup(tokenId = id, displayName = nameOf(id), count = count) }
        // Case-insensitive alphabetical by name; tokenId breaks ties so ordering is deterministic
        // even for two distinct tokens that happen to share a display name.
        .sortedWith(compareBy({ it.displayName.lowercase() }, { it.tokenId }))
    return PileComposition(groups = groups, total = drawPile.size)
}
