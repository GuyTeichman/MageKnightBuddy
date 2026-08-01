package com.guyteichman.mageknightbuddy.domain

/**
 * Immutable snapshot of the Enemy Picker's state (see `CONTEXT.md`'s "Enemy Picker"): the live
 * [piles], the [drawLog], and the two config choices that shape them - the [tokenSet] (which
 * expansions' tokens are in play) and [drawWithReplacement]. The app is authoritative over both the
 * randomness and what is left in each pile, so this whole object is persisted for the length of a
 * game (ADR-0006); it models no map, and a drawn token is discarded immediately.
 *
 * Follows the same shape as [VolkareSession]/[DummyPlayerSession]: a private constructor with a
 * companion-object [start]/[restore] pair, and every mutating method ([draw], [setDefeated],
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
     * Draws from one or more piles in a single action - [draws] maps each requested
     * [TokenPileId] to how many tokens to take from it - returning a new session with every
     * touched pile and [drawLog] updated. Every new [DrawLogEntry] across every pile shares one
     * [batchId], so the UI can group a whole multi-pile (or multi-count single-pile) draw as one
     * batch. A single-pile draw is just the degenerate case of a one-entry [draws] map.
     *
     * Piles are drawn from in [TokenPileId.entries] order (not [draws]' own iteration order) so
     * [drawLog] insertion order - and therefore display order - is deterministic regardless of how
     * the caller built the map; that enum order is itself the rulebook's enemy difficulty order.
     *
     * Without replacement (the rules-correct default): each draw takes the top of that pile's draw
     * pile and moves it to its discard; if the draw pile is empty when a token is needed, the pile
     * **Replenishes** first (its discard is `shuffle`d into a new draw pile - see `CONTEXT.md`).
     * With replacement: each draw picks a `shuffle`-randomised token but leaves the pile untouched,
     * so piles never deplete and no discard accumulates. Both rules apply independently per pile,
     * even within one multi-pile batch - one pile replenishing has no effect on any other.
     *
     * [shuffle] is the injected randomness (default: a real shuffle); it is used both to Replenish
     * and to pick under replacement. Draws never touch a pile absent from [draws], and never touch
     * existing [drawLog] entries' flags.
     */
    fun draw(
        draws: Map<TokenPileId, Int>,
        batchId: Long = System.currentTimeMillis(),
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
    ): EnemyPickerSession {
        require(draws.isNotEmpty()) { "draws must not be empty" }
        require(draws.values.all { it >= 1 }) { "every requested count must be >= 1, was $draws" }

        var updatedPiles = piles
        val newEntries = ArrayList<DrawLogEntry>()

        // TokenPileId.entries (not draws.keys) fixes the order - see the KDoc above.
        for (pileId in TokenPileId.entries) {
            val count = draws[pileId] ?: continue
            // `getValue` throws a clear error if the pile id isn't one this session was built with,
            // rather than silently returning null.
            var pile = updatedPiles.getValue(pileId)

            repeat(count) {
                val (drawnId, next) = drawOneFrom(pileId, pile, shuffle)
                pile = next
                newEntries += DrawLogEntry(tokenId = drawnId, pile = pileId, batchId = batchId)
            }

            // `updatedPiles + (pileId to pile)` returns a new map with just this pile replaced -
            // every other pile (including ones already updated earlier in this loop) is kept.
            updatedPiles = updatedPiles + (pileId to pile)
        }

        return copy(piles = updatedPiles, drawLog = drawLog + newEntries)
    }

    /**
     * The **Summon Draw** action (see `CONTEXT.md`'s "Summon Draw"): draws one token from each pile
     * in [pileIds] - a summoner token's own `EnemyAttack.summons` piles, resolved by the caller from
     * the catalogue, since this session never looks up [EnemyToken] itself (same separation [draw]
     * keeps). Every new entry is tagged [DrawLogEntry.parentIndex] = [parentIndex] (the summoner's
     * own chronological [drawLog] index) and shares one [batchId], so multiple summon slots on one
     * token are drawn and discarded together, matching the rulebook.
     *
     * A summoner can be re-engaged and summoned again - each call appends a *new* set of entries
     * rather than replacing the old ones (the log stays append-only, like [setDefeated]); the
     * previous children remain in [drawLog] but are superseded - see [currentChildrenOf].
     *
     * Reuses the same per-pile draw/replenish rules [draw] uses (with/without replacement, discard
     * on draw), via the shared [drawOneFrom] helper.
     */
    fun summon(
        parentIndex: Int,
        pileIds: List<TokenPileId>,
        batchId: Long = System.currentTimeMillis(),
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
    ): EnemyPickerSession {
        require(parentIndex in drawLog.indices) { "parentIndex $parentIndex is out of range" }
        require(pileIds.isNotEmpty()) { "pileIds must not be empty" }

        var updatedPiles = piles
        val newEntries = ArrayList<DrawLogEntry>()
        for (pileId in pileIds) {
            val (drawnId, next) = drawOneFrom(pileId, updatedPiles.getValue(pileId), shuffle)
            newEntries += DrawLogEntry(tokenId = drawnId, pile = pileId, batchId = batchId, parentIndex = parentIndex)
            updatedPiles = updatedPiles + (pileId to next)
        }

        return copy(piles = updatedPiles, drawLog = drawLog + newEntries)
    }

    /**
     * The [drawLog] indices of the *current* Summon Draw children of the entry at [parentIndex] -
     * the entries whose [DrawLogEntry.parentIndex] points here, filtered down to the most recent
     * shared [DrawLogEntry.batchId] among them (a re-summon appends a whole new batch rather than
     * replacing the old one - see [summon]). Empty if this entry has never been summoned from.
     */
    fun currentChildrenOf(parentIndex: Int): List<Int> {
        val children = drawLog.withIndex().filter { it.value.parentIndex == parentIndex }
        val latestBatchId = children.maxOfOrNull { it.value.batchId } ?: return emptyList()
        return children.filter { it.value.batchId == latestBatchId }.map { it.index }
    }

    /**
     * Draws one token from [pile] (named [pileId] only for its error messages), following the same
     * with/without-replacement rules [draw] and [summon] both need: with replacement, samples a
     * token and leaves [pile] untouched; without, replenishes from a shuffled discard if the draw
     * pile just ran out, then moves the drawn token from draw pile to discard immediately (ADR-0006).
     * Returns the drawn token id alongside the [pile] state after the draw.
     */
    private fun drawOneFrom(
        pileId: TokenPileId,
        pile: TokenPile,
        shuffle: (List<String>) -> List<String>,
    ): Pair<String, TokenPile> {
        if (drawWithReplacement) {
            require(pile.drawPile.isNotEmpty()) { "pile $pileId has no tokens to draw" }
            return shuffle(pile.drawPile).first() to pile
        }

        // Defensive replenish if the draw pile is *already* empty on entry: normally the eager
        // reshuffle below keeps that from ever happening, but a session restored from state
        // persisted under the old lazy semantics can still carry an empty draw pile - so handle it.
        val replenished = if (pile.drawPile.isEmpty()) {
            require(pile.discardPile.isNotEmpty()) { "pile $pileId is completely empty" }
            TokenPile(drawPile = shuffle(pile.discardPile), discardPile = emptyList())
        } else {
            pile
        }
        val drawnId = replenished.drawPile.first()
        // Drawn token leaves the draw pile and lands in the discard immediately (ADR-0006).
        val afterDraw = TokenPile(drawPile = replenished.drawPile.drop(1), discardPile = replenished.discardPile + drawnId)
        // Eager **Replenish** (issue #231): the moment that draw empties the pile, reshuffle the
        // discard back into a fresh draw pile so a pile never rests at 0 remaining. Distribution is
        // identical to reshuffling lazily on the next draw (the just-drawn token is in the discard
        // either way); this only means the UI never displays an empty pile.
        val next = if (afterDraw.drawPile.isEmpty() && afterDraw.discardPile.isNotEmpty()) {
            TokenPile(drawPile = shuffle(afterDraw.discardPile), discardPile = emptyList())
        } else {
            afterDraw
        }
        return drawnId to next
    }

    /**
     * Sets the [defeated] flag and free-text [note] on the [drawLog] entry at [index] (chronological
     * order, since the log is append-only) - the one-tap Defeat action, and its inverse. Purely a
     * memory aid: it changes only that one log entry and touches **no** [TokenPile], so draw odds
     * are unaffected (ADR-0006 - a defeated enemy was already discarded when it was drawn).
     */
    fun setDefeated(index: Int, defeated: Boolean = true, note: String = ""): EnemyPickerSession {
        val updated = drawLog[index].copy(defeated = defeated, note = note)
        // toMutableList().also { it[index] = ... } replaces one element without mutating the
        // original list (drawLog stays untouched; a fresh list is stored on the copy).
        return copy(drawLog = drawLog.toMutableList().also { it[index] = updated })
    }

    /**
     * Rebuilds every pile from [catalogue] (and [ruinCatalogue] for the RUIN pile) and clears the
     * [drawLog], keeping the current [tokenSet] and [drawWithReplacement] - the config section's
     * "Apply & Reset" and the standalone Reset button both land here. Equivalent to [start] with this
     * session's config.
     */
    fun reset(
        catalogue: List<EnemyToken>,
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
        ruinCatalogue: List<RuinToken> = emptyList(),
        factionRewardCatalogue: List<FactionRewardToken> = emptyList(),
    ): EnemyPickerSession = start(catalogue, tokenSet, drawWithReplacement, shuffle, ruinCatalogue, factionRewardCatalogue)

    companion object {
        /**
         * Builds the initial [TokenPile] map from the three catalogues: every [EnemyToken] whose
         * [EnemyToken.expansion] is in [tokenSet] is expanded into [EnemyToken.copies] entries of its
         * id and grouped into its [EnemyToken.pile]; every [FactionRewardToken] in [tokenSet] is
         * expanded the same way into its [FactionRewardToken.pile] (issue #252); and the [RuinToken]s
         * in [tokenSet] each contribute a single id (ruins are unique - no copy count) to the one
         * [TokenPileId.RUIN] pile. Each pile's combined list is `shuffle`d into its draw pile (discard
         * empty). Piles with no matching tokens are omitted - including RUIN and every reward pile,
         * e.g. when no [ruinCatalogue]/[factionRewardCatalogue] is supplied or that faction's
         * expansion isn't ticked.
         */
        private fun buildPiles(
            catalogue: List<EnemyToken>,
            ruinCatalogue: List<RuinToken>,
            factionRewardCatalogue: List<FactionRewardToken>,
            tokenSet: Set<Expansion>,
            shuffle: (List<String>) -> List<String>,
        ): Map<TokenPileId, TokenPile> {
            val enemyPiles = catalogue
                .filter { it.expansion in tokenSet }
                // groupBy collects tokens into a map keyed by their pile; each value is the list of
                // tokens in that pile, which we then expand-by-copies and shuffle.
                .groupBy { it.pile }
                .mapValues { (_, tokens) ->
                    val ids = tokens.flatMap { token -> List(token.copies) { token.id } }
                    TokenPile(drawPile = shuffle(ids))
                }

            // Faction reward piles build identically to the enemy piles above (gate by expansion,
            // group by pile, expand copies, shuffle) - a reward token carries the same pile/copies/
            // expansion trio. Their pile ids are disjoint from the enemy colours and RUIN, so the
            // maps merge cleanly.
            val factionPiles = factionRewardCatalogue
                .filter { it.expansion in tokenSet }
                .groupBy { it.pile }
                .mapValues { (_, tokens) ->
                    val ids = tokens.flatMap { token -> List(token.copies) { token.id } }
                    TokenPile(drawPile = shuffle(ids))
                }

            // The RUIN pile comes from its own catalogue (a different token shape); one copy per ruin.
            val ruinIds = ruinCatalogue.filter { it.expansion in tokenSet }.map { it.id }
            // `+ (RUIN to ...)` only when there's something to put there, so an empty RUIN pile is
            // omitted just like any other empty pile above.
            val nonRuin = enemyPiles + factionPiles
            return if (ruinIds.isEmpty()) nonRuin
            else nonRuin + (TokenPileId.RUIN to TokenPile(drawPile = shuffle(ruinIds)))
        }

        /**
         * Begins a fresh session. [tokenSet] defaults to base game only and [drawWithReplacement]
         * to false (the rules-correct default). [shuffle] defaults to a real shuffle; tests pass a
         * deterministic one (e.g. identity) the same way [VolkareSession.start] takes `deckOrder`.
         * [ruinCatalogue] populates the [TokenPileId.RUIN] pile and [factionRewardCatalogue] the four
         * faction reward piles (issue #252); omit either (the default) to build a session without
         * those piles at all.
         */
        fun start(
            catalogue: List<EnemyToken>,
            tokenSet: Set<Expansion> = setOf(Expansion.BASE),
            drawWithReplacement: Boolean = false,
            shuffle: (List<String>) -> List<String> = { it.shuffled() },
            ruinCatalogue: List<RuinToken> = emptyList(),
            factionRewardCatalogue: List<FactionRewardToken> = emptyList(),
        ): EnemyPickerSession = EnemyPickerSession(
            tokenSet = tokenSet,
            drawWithReplacement = drawWithReplacement,
            piles = buildPiles(catalogue, ruinCatalogue, factionRewardCatalogue, tokenSet, shuffle),
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
