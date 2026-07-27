package com.guyteichman.mageknightbuddy.domain

/**
 * Immutable snapshot of the Enemy Picker's state (see `CONTEXT.md`'s "Enemy Picker"): the live
 * [piles], the [drawLog], and the two config choices that shape them - the [tokenSet] (which
 * expansions' tokens are in play) and [drawWithReplacement]. The app is authoritative over both the
 * randomness and what is left in each pile, so this whole object is persisted for the length of a
 * game (ADR-0006); it models no map, and a drawn token is discarded immediately.
 *
 * Follows the same shape as [VolkareSession]/[DummyPlayerSession]: a private constructor with a
 * companion-object [start]/[restore] pair, and every mutating method ([draw], [flagStillInPlay],
 * [reset]) returns a **new** session rather than mutating this one. Randomness is always injected
 * (`shuffle`) so tests are deterministic, mirroring [VolkareSession.start]'s explicit `deckOrder`.
 */
data class EnemyPickerSession private constructor(
    val tokenSet: Set<Expansion>,
    val drawWithReplacement: Boolean,
    val piles: Map<TokenPileId, TokenPile>,
    val drawLog: List<DrawLogEntry>,
) {
    /**
     * Draws [count] tokens from [pileId] in a single action, returning a new session with the
     * pile(s) and [drawLog] updated. All [count] new [DrawLogEntry]s share [batchId] so the UI can
     * group a stepper draw as one entry.
     *
     * Without replacement (the rules-correct default): each draw takes the top of the draw pile and
     * moves it to that pile's discard; if the draw pile is empty when a token is needed, the pile
     * **Replenishes** first (its discard is `shuffle`d into a new draw pile - see `CONTEXT.md`).
     * With replacement: each draw picks a `shuffle`-randomised token but leaves the pile untouched,
     * so piles never deplete and no discard accumulates.
     *
     * [shuffle] is the injected randomness (default: a real shuffle); it is used both to Replenish
     * and to pick under replacement. Draws never touch any pile other than [pileId], and never
     * touch existing [drawLog] entries' flags.
     */
    fun draw(
        pileId: TokenPileId,
        count: Int = 1,
        batchId: Long = System.currentTimeMillis(),
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
    ): EnemyPickerSession {
        require(count >= 1) { "count must be >= 1, was $count" }
        // `getValue` throws a clear error if the pile id isn't one this session was built with,
        // rather than silently returning null.
        var pile = piles.getValue(pileId)
        val newEntries = ArrayList<DrawLogEntry>(count)

        repeat(count) {
            val drawnId: String
            if (drawWithReplacement) {
                // With replacement: sample one token and leave the pile exactly as it was.
                require(pile.drawPile.isNotEmpty()) { "pile $pileId has no tokens to draw" }
                drawnId = shuffle(pile.drawPile).first()
            } else {
                // Replenish if the draw pile ran out: the discard becomes the new, shuffled pile.
                if (pile.drawPile.isEmpty()) {
                    require(pile.discardPile.isNotEmpty()) { "pile $pileId is completely empty" }
                    pile = TokenPile(drawPile = shuffle(pile.discardPile), discardPile = emptyList())
                }
                drawnId = pile.drawPile.first()
                // Drawn token leaves the draw pile and lands in the discard immediately (ADR-0006).
                pile = TokenPile(drawPile = pile.drawPile.drop(1), discardPile = pile.discardPile + drawnId)
            }
            newEntries += DrawLogEntry(tokenId = drawnId, pile = pileId, batchId = batchId)
        }

        // `piles + (pileId to pile)` returns a new map with just this pile replaced - every other
        // pile is carried over unchanged.
        return copy(piles = piles + (pileId to pile), drawLog = drawLog + newEntries)
    }

    /**
     * Sets the "still in play" flag and free-text [note] on the [drawLog] entry at [index]
     * (chronological order, since the log is append-only). Purely a memory aid: it changes only
     * that one log entry and touches **no** [TokenPile], so draw odds are unaffected (ADR-0006).
     */
    fun flagStillInPlay(index: Int, stillInPlay: Boolean = true, note: String = ""): EnemyPickerSession {
        val updated = drawLog[index].copy(stillInPlay = stillInPlay, note = note)
        // toMutableList().also { it[index] = ... } replaces one element without mutating the
        // original list (drawLog stays untouched; a fresh list is stored on the copy).
        return copy(drawLog = drawLog.toMutableList().also { it[index] = updated })
    }

    /**
     * Rebuilds every pile from [catalogue] and clears the [drawLog], keeping the current [tokenSet]
     * and [drawWithReplacement] - the config section's "Apply & Reset" and the standalone Reset
     * button both land here. Equivalent to [start] with this session's config.
     */
    fun reset(
        catalogue: List<EnemyToken>,
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
    ): EnemyPickerSession = start(catalogue, tokenSet, drawWithReplacement, shuffle)

    companion object {
        /**
         * Builds the initial [TokenPile] map from [catalogue]: every token whose [EnemyToken.pile]
         * exists and whose [EnemyToken.expansion] is in [tokenSet] is expanded into
         * [EnemyToken.copies] entries of its id, and each pile's combined list is `shuffle`d into
         * its draw pile (discard empty). Piles with no matching tokens are omitted.
         */
        private fun buildPiles(
            catalogue: List<EnemyToken>,
            tokenSet: Set<Expansion>,
            shuffle: (List<String>) -> List<String>,
        ): Map<TokenPileId, TokenPile> =
            catalogue
                .filter { it.expansion in tokenSet }
                // groupBy collects tokens into a map keyed by their pile; each value is the list of
                // tokens in that pile, which we then expand-by-copies and shuffle.
                .groupBy { it.pile }
                .mapValues { (_, tokens) ->
                    val ids = tokens.flatMap { token -> List(token.copies) { token.id } }
                    TokenPile(drawPile = shuffle(ids))
                }

        /**
         * Begins a fresh session. [tokenSet] defaults to base game only and [drawWithReplacement]
         * to false (the rules-correct default). [shuffle] defaults to a real shuffle; tests pass a
         * deterministic one (e.g. identity) the same way [VolkareSession.start] takes `deckOrder`.
         */
        fun start(
            catalogue: List<EnemyToken>,
            tokenSet: Set<Expansion> = setOf(Expansion.BASE),
            drawWithReplacement: Boolean = false,
            shuffle: (List<String>) -> List<String> = { it.shuffled() },
        ): EnemyPickerSession = EnemyPickerSession(
            tokenSet = tokenSet,
            drawWithReplacement = drawWithReplacement,
            piles = buildPiles(catalogue, tokenSet, shuffle),
            drawLog = emptyList(),
        )

        /**
         * Reconstructs a session from fully-persisted state (used by the data layer to restore a
         * saved game). Not for general use - [start] is the entry point for a new session.
         */
        fun restore(
            tokenSet: Set<Expansion>,
            drawWithReplacement: Boolean,
            piles: Map<TokenPileId, TokenPile>,
            drawLog: List<DrawLogEntry>,
        ): EnemyPickerSession = EnemyPickerSession(
            tokenSet = tokenSet,
            drawWithReplacement = drawWithReplacement,
            piles = piles,
            drawLog = drawLog,
        )
    }
}
