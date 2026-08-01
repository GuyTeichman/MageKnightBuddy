package com.guyteichman.mageknightbuddy.domain

/**
 * Immutable snapshot of the Enemy Picker's state (see `CONTEXT.md`'s "Enemy Picker"): the live
 * [piles], the [drawLog], and the two config choices that shape them - the [tokenSet] (which
 * expansions' tokens are in play) and [drawWithReplacement]. The app is authoritative over both the
 * randomness and what is left in each pile, so this whole object is persisted for the length of a
 * game (ADR-0006); it models no map.
 *
 * A drawn token follows the pile-correct three-state lifecycle of issue #251: **in-pile → on-board
 * → discarded**. Drawing removes it from the draw pile and holds it **on the board** (out of both
 * piles, tracked only by its [DrawLogEntry]); [setDefeated] then moves it into that pile's discard
 * when it is defeated. Only the discard is reshuffled on a Replenish, so an undefeated token is
 * never re-drawn. On-board is *derived* from [drawLog] (undefeated, non-ephemeral entries), keeping
 * the log the sole record of what is standing where; the pile itself stores only the draw pile and
 * the discard. The one exception is an **ephemeral** Summon child ([DrawLogEntry.ephemeral]), which
 * is discarded the instant it is drawn (it never sits on the board).
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
     * pile and holds it **on the board** - out of both piles until it is defeated (issue #251), not
     * moved to the discard. If the draw pile is empty when a token is needed, the pile **Replenishes**
     * first (its discard is `shuffle`d into a new draw pile - see `CONTEXT.md`); if the discard is
     * *also* empty (everything drawn is still on the board), the pile is genuinely empty and this
     * throws. With replacement: each draw picks a `shuffle`-randomised token but leaves the pile
     * untouched, so piles never deplete and no discard accumulates. Both rules apply independently
     * per pile, even within one multi-pile batch - one pile replenishing has no effect on any other.
     *
     * [shuffle] is the injected randomness (default: a real shuffle); it is used both to Replenish
     * and to pick under replacement. Draws never touch a pile absent from [draws], and never touch
     * existing [drawLog] entries' flags.
     *
     * When [possessed] is true this is an Apocalypse Dragon **possessed** draw: every circular enemy
     * drawn is paired with one token pulled from the [TokenPileId.POSSESSED] pile (recorded on the
     * entry's [DrawLogEntry.possessedTokenId]), so each drawn enemy is a composite possessed enemy.
     * [draws] may then only name colour piles - never [TokenPileId.RUIN] or [TokenPileId.POSSESSED].
     * The circular enemy is held **on the board** like any draw (issue #251, tracked by its entry);
     * the possessed companion, which has no independent on-board/defeat tracking, is cycled straight
     * to the POSSESSED discard. See docs/rules/apocalypse-dragon.md.
     */
    fun draw(
        draws: Map<TokenPileId, Int>,
        batchId: Long = System.currentTimeMillis(),
        possessed: Boolean = false,
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
    ): EnemyPickerSession {
        require(draws.isNotEmpty()) { "draws must not be empty" }
        require(draws.values.all { it >= 1 }) { "every requested count must be >= 1, was $draws" }
        // A possessed draw pairs each *circular* enemy with a possessed token, so it may only target
        // the colour piles - never RUIN (a ruin is not an enemy) or POSSESSED (that pile is the
        // companion, never a primary draw target). See docs/rules/apocalypse-dragon.md.
        require(!possessed || draws.keys.none { it == TokenPileId.RUIN || it == TokenPileId.POSSESSED }) {
            "a possessed draw can only target circular enemy piles, was $draws"
        }

        var updatedPiles = piles
        val newEntries = ArrayList<DrawLogEntry>()

        // TokenPileId.entries (not draws.keys) fixes the order - see the KDoc above.
        for (pileId in TokenPileId.entries) {
            val count = draws[pileId] ?: continue
            // `getValue` throws a clear error if the pile id isn't one this session was built with,
            // rather than silently returning null.
            var pile = updatedPiles.getValue(pileId)

            repeat(count) {
                // discardImmediately = false: a normal draw holds the token on the board (issue #251).
                val (drawnId, next) = drawOneFrom(pileId, pile, shuffle, discardImmediately = false)
                pile = next
                // For a possessed draw, pair this circular enemy with a token drawn from the
                // POSSESSED pile (its own draw/replenish rules), recording its id on the entry so the
                // pair renders as one composite. POSSESSED is a different map key than the colour
                // pile we write back after the loop, so updating it here in-place is safe.
                //
                // discardImmediately = true: unlike the circular enemy above (held on the board),
                // the possessed companion has no independent on-board/defeat tracking - the
                // composite's #251 lifecycle runs through the *circular* token in its colour pile via
                // [setDefeated], which never touches the POSSESSED pile. So the companion is cycled
                // straight to the POSSESSED discard here (draw -> discard -> replenish), the same as
                // before #251; holding it on the board instead would strand it in neither pile and
                // eventually empty the POSSESSED pile.
                val possessedTokenId = if (possessed) {
                    val (pid, possessedNext) = drawOneFrom(
                        TokenPileId.POSSESSED, updatedPiles.getValue(TokenPileId.POSSESSED), shuffle,
                        discardImmediately = true,
                    )
                    updatedPiles = updatedPiles + (TokenPileId.POSSESSED to possessedNext)
                    pid
                } else {
                    null
                }
                newEntries += DrawLogEntry(
                    tokenId = drawnId, pile = pileId, batchId = batchId, possessedTokenId = possessedTokenId,
                )
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
     * [ephemeral] distinguishes the two callers of this machinery (issue #251). `true` (the default,
     * for a real **Summon ability**): the child is discarded the instant it is drawn - it just fills
     * the summoner's fight slot and is never independently defeated - so it goes straight to the
     * discard, exactly the pre-#251 behaviour. `false` (for a ruin **Enemies-With-Treasure** draw):
     * the child is a real, independently-defeatable enemy, so it is held **on the board** like a
     * normal [draw] until the player defeats it. The flag is recorded on each child's
     * [DrawLogEntry.ephemeral] so restore and [setDefeated] treat it consistently.
     *
     * Reuses the same per-pile draw/replenish rules [draw] uses (with/without replacement), via the
     * shared [drawOneFrom] helper.
     */
    fun summon(
        parentIndex: Int,
        pileIds: List<TokenPileId>,
        ephemeral: Boolean = true,
        batchId: Long = System.currentTimeMillis(),
        shuffle: (List<String>) -> List<String> = { it.shuffled() },
    ): EnemyPickerSession {
        require(parentIndex in drawLog.indices) { "parentIndex $parentIndex is out of range" }
        require(pileIds.isNotEmpty()) { "pileIds must not be empty" }

        var updatedPiles = piles
        val newEntries = ArrayList<DrawLogEntry>()
        for (pileId in pileIds) {
            // An ephemeral summon child discards on draw; a ruin enemy child is held on the board.
            val (drawnId, next) = drawOneFrom(pileId, updatedPiles.getValue(pileId), shuffle, discardImmediately = ephemeral)
            newEntries += DrawLogEntry(tokenId = drawnId, pile = pileId, batchId = batchId, parentIndex = parentIndex, ephemeral = ephemeral)
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
     * with/without-replacement rules [draw] and [summon] both need. With replacement: samples a token
     * and leaves [pile] untouched. Without: replenishes from a shuffled discard if the draw pile just
     * ran out, then removes the drawn token from the draw pile.
     *
     * [discardImmediately] decides where the drawn token goes (issue #251): `false` (a normal [draw]
     * or a ruin enemy) holds it **on the board** - removed from the draw pile but added to neither
     * pile - so it can't be re-drawn while it stands; `true` (an ephemeral [summon] child) moves it
     * straight to the discard, the pre-#251 behaviour. Returns the drawn token id alongside the
     * [pile] state after the draw.
     */
    private fun drawOneFrom(
        pileId: TokenPileId,
        pile: TokenPile,
        shuffle: (List<String>) -> List<String>,
        discardImmediately: Boolean,
    ): Pair<String, TokenPile> {
        if (drawWithReplacement) {
            require(pile.drawPile.isNotEmpty()) { "pile $pileId has no tokens to draw" }
            return shuffle(pile.drawPile).first() to pile
        }

        // Replenish if the draw pile is empty on entry: reshuffle the discard into a new draw pile.
        // If the discard is *also* empty, everything drawn is on the board - the pile is genuinely
        // empty and cannot be drawn (issue #251's new edge case; the UI disables the draw for it).
        val replenished = if (pile.drawPile.isEmpty()) {
            require(pile.discardPile.isNotEmpty()) { "pile $pileId is completely empty" }
            TokenPile(drawPile = shuffle(pile.discardPile), discardPile = emptyList())
        } else {
            pile
        }
        val drawnId = replenished.drawPile.first()
        val afterDraw = if (discardImmediately) {
            // Ephemeral summon child: to the discard immediately.
            TokenPile(drawPile = replenished.drawPile.drop(1), discardPile = replenished.discardPile + drawnId)
        } else {
            // On the board: only leaves the draw pile; added to neither pile until defeated.
            TokenPile(drawPile = replenished.drawPile.drop(1), discardPile = replenished.discardPile)
        }
        // Eager **Replenish** (issue #231, adapted for #251): the moment a draw empties the draw pile,
        // reshuffle the discard back so the pile never rests at 0 while it still has discardable
        // tokens - but *only* if the discard is non-empty. If both are empty (all drawn tokens are on
        // the board), the pile is legitimately empty and is left that way.
        val next = if (afterDraw.drawPile.isEmpty() && afterDraw.discardPile.isNotEmpty()) {
            TokenPile(drawPile = shuffle(afterDraw.discardPile), discardPile = emptyList())
        } else {
            afterDraw
        }
        return drawnId to next
    }

    /**
     * Sets the [defeated] flag and free-text [note] on the [drawLog] entry at [index] (chronological
     * order, since the log is append-only) - the one-tap Defeat action, and its inverse (for a
     * faction reward token this same action is "Spend"). Since issue #251 this is **not** a pure
     * memory aid: defeating an on-board token moves it into its pile's discard, and un-defeating
     * pulls it back out, so a Replenish reshuffles exactly the defeated tokens.
     *
     * The pile move is skipped - only the flag/note change - in the two cases where there is no
     * accumulated pile state to move the token in: under [drawWithReplacement] (piles never deplete
     * and no discard accumulates, so the flag is a memory aid again), and for an ephemeral Summon
     * child (already discarded when drawn, and never independently defeated). Stays pure (no
     * `shuffle`), so it is cleanly reversible.
     *
     * Un-defeat pulls the token back on the board from wherever it physically is: the discard if it's
     * still there, otherwise the draw pile (it may have been reshuffled back by a Replenish). If it is
     * in **neither** - it was reshuffled out *and* re-drawn, so all its copies are accounted for
     * elsewhere - un-defeat can't return it without duplicating a physical token, so this is a
     * **no-op** (the flag stays defeated). [canUndefeat] reports that case up front so the UI can
     * block the control rather than let it silently do nothing.
     */
    fun setDefeated(index: Int, defeated: Boolean = true, note: String = ""): EnemyPickerSession {
        val entry = drawLog[index]

        // Refuse an un-defeat we can't honour (token in neither pile), leaving flag + pile consistent.
        if (!defeated && entry.defeated && !drawWithReplacement && !entry.ephemeral && !canUndefeat(index)) {
            return this
        }

        val updatedEntry = entry.copy(defeated = defeated, note = note)
        // toMutableList().also { it[index] = ... } replaces one element without mutating the
        // original list (drawLog stays untouched; a fresh list is stored on the copy).
        val updatedLog = drawLog.toMutableList().also { it[index] = updatedEntry }

        // No pile effect for a replacement game or an ephemeral child - see the doc comment above.
        if (drawWithReplacement || entry.ephemeral) return copy(drawLog = updatedLog)

        val pile = piles[entry.pile] ?: return copy(drawLog = updatedLog)
        val updatedPile = when {
            // On the board -> discard (a fresh defeat).
            defeated && !entry.defeated -> pile.copy(discardPile = pile.discardPile + entry.tokenId)
            // Discard/draw -> on the board (un-defeat): pull one copy out of the discard if present,
            // else the draw pile (reshuffled back). The no-op guard above guarantees it's in one.
            !defeated && entry.defeated ->
                if (entry.tokenId in pile.discardPile) {
                    pile.copy(discardPile = pile.discardPile.toMutableList().also { it.removeAt(it.indexOf(entry.tokenId)) })
                } else {
                    pile.copy(drawPile = pile.drawPile.toMutableList().also { it.removeAt(it.indexOf(entry.tokenId)) })
                }
            // No lifecycle transition (e.g. a note-only edit): leave the pile alone.
            else -> pile
        }
        return copy(piles = piles + (entry.pile to updatedPile), drawLog = updatedLog)
    }

    /**
     * Whether the [drawLog] entry at [index] can be **un-defeated** - returned to the board (issue
     * #251). True for anything that isn't a blocked un-defeat: a non-defeated entry (nothing to undo),
     * a [drawWithReplacement] game or ephemeral child (the flag is pure memory there), or a defeated
     * token still present in its pile's discard or draw pile. **False** only when a defeated token is
     * in *neither* pile - reshuffled out and re-drawn - so un-defeating it would duplicate a physical
     * copy. The UI uses this to disable / warn on the un-defeat control instead of no-opping silently.
     */
    fun canUndefeat(index: Int): Boolean {
        val entry = drawLog[index]
        if (!entry.defeated || drawWithReplacement || entry.ephemeral) return true
        val pile = piles[entry.pile] ?: return false
        return entry.tokenId in pile.discardPile || entry.tokenId in pile.drawPile
    }

    /**
     * How many tokens drawn from [pileId] are currently **on the board** - undefeated, non-ephemeral
     * [drawLog] entries for that pile (issue #251). Derived from the log rather than stored, since the
     * log is the sole record of what is still standing (ADR-0006); the UI shows this alongside a
     * pile's remaining-to-draw count.
     */
    fun onBoardCount(pileId: TokenPileId): Int =
        drawLog.count { it.pile == pileId && !it.defeated && !it.ephemeral }

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
        possessedCatalogue: List<PossessedToken> = emptyList(),
        factionRewardCatalogue: List<FactionRewardToken> = emptyList(),
    ): EnemyPickerSession = start(catalogue, tokenSet, drawWithReplacement, shuffle, ruinCatalogue, possessedCatalogue, factionRewardCatalogue)

    companion object {
        /**
         * Builds the initial [TokenPile] map from the three catalogues: every [EnemyToken] whose
         * [EnemyToken.expansion] is in [tokenSet] is expanded into [EnemyToken.copies] entries of its
         * id and grouped into its [EnemyToken.pile]; every [FactionRewardToken] in [tokenSet] is
         * expanded the same way into its [FactionRewardToken.pile] (issue #252); and the [RuinToken]s
         * in [tokenSet] each contribute a single id (ruins are unique - no copy count) to the one
         * [TokenPileId.RUIN] pile; and the [PossessedToken]s in [tokenSet] are expanded by copy count
         * into the [TokenPileId.POSSESSED] pile (docs/rules/apocalypse-dragon.md). Each pile's
         * combined list is `shuffle`d into its draw pile (discard empty). Piles with no matching
         * tokens are omitted - including RUIN, POSSESSED and every reward pile, e.g. when their
         * catalogue isn't supplied or that expansion isn't ticked.
         */
        private fun buildPiles(
            catalogue: List<EnemyToken>,
            ruinCatalogue: List<RuinToken>,
            possessedCatalogue: List<PossessedToken>,
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
            // The POSSESSED pile expands each possessed token by its copy count, like enemy tokens
            // (possessed_03 has three copies).
            val possessedIds = possessedCatalogue
                .filter { it.expansion in tokenSet }
                .flatMap { token -> List(token.copies) { token.id } }

            // Enemy + faction reward piles merge cleanly (their pile ids are disjoint); then add the
            // RUIN and POSSESSED single-key piles only when non-empty, so an absent pile is omitted
            // just like any empty colour pile above.
            var piles = enemyPiles + factionPiles
            if (ruinIds.isNotEmpty()) piles = piles + (TokenPileId.RUIN to TokenPile(drawPile = shuffle(ruinIds)))
            if (possessedIds.isNotEmpty()) piles = piles + (TokenPileId.POSSESSED to TokenPile(drawPile = shuffle(possessedIds)))
            return piles
        }

        /**
         * Begins a fresh session. [tokenSet] defaults to base game only and [drawWithReplacement]
         * to false (the rules-correct default). [shuffle] defaults to a real shuffle; tests pass a
         * deterministic one (e.g. identity) the same way [VolkareSession.start] takes `deckOrder`.
         * [ruinCatalogue] populates the [TokenPileId.RUIN] pile, [possessedCatalogue] the
         * [TokenPileId.POSSESSED] pile, and [factionRewardCatalogue] the four faction reward piles
         * (issue #252); omit any (the default) to build a session without those piles.
         */
        fun start(
            catalogue: List<EnemyToken>,
            tokenSet: Set<Expansion> = setOf(Expansion.BASE),
            drawWithReplacement: Boolean = false,
            shuffle: (List<String>) -> List<String> = { it.shuffled() },
            ruinCatalogue: List<RuinToken> = emptyList(),
            possessedCatalogue: List<PossessedToken> = emptyList(),
            factionRewardCatalogue: List<FactionRewardToken> = emptyList(),
        ): EnemyPickerSession = EnemyPickerSession(
            tokenSet = tokenSet,
            drawWithReplacement = drawWithReplacement,
            piles = buildPiles(catalogue, ruinCatalogue, possessedCatalogue, factionRewardCatalogue, tokenSet, shuffle),
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
