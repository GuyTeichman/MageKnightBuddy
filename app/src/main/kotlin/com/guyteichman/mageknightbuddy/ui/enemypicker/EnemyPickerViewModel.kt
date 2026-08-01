package com.guyteichman.mageknightbuddy.ui.enemypicker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.EnemyPickerSessionRepository
import com.guyteichman.mageknightbuddy.domain.EnemyPickerSession
import com.guyteichman.mageknightbuddy.domain.EnemyToken
import com.guyteichman.mageknightbuddy.domain.Expansion
import com.guyteichman.mageknightbuddy.domain.FactionRewardToken
import com.guyteichman.mageknightbuddy.domain.FactionRewardTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.PossessedToken
import com.guyteichman.mageknightbuddy.domain.PossessedTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.RuinToken
import com.guyteichman.mageknightbuddy.domain.RuinTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.TokenCatalogue
import com.guyteichman.mageknightbuddy.domain.TokenPileId
import kotlinx.coroutines.launch

/**
 * Backs the Enemy Picker screen: restores the autosaved [EnemyPickerSession] on creation and
 * mutates it via [draw]/[flag]/[reset]/[applyConfig], autosaving after each change.
 *
 * Unlike the Dummy Player tab's modes (which reuse [com.guyteichman.mageknightbuddy.ui.dummyplayer.AutosaveSessionViewModel]),
 * the Enemy Picker has *no setup screen* - there is nothing to choose before playing - so when
 * nothing has been saved yet it auto-[start][EnemyPickerSession.start]s a default session (base
 * game, without replacement) instead of leaving [session] null. That different lifecycle is why
 * this ViewModel doesn't extend the restore-only base, but it repeats the same [isBusy] double-tap
 * guard + autosave shape.
 *
 * [catalogue], [ruinCatalogue], [possessedCatalogue] and [factionRewardCatalogue] are injected
 * (defaults: the real [TokenCatalogue] / [RuinTokenCatalogue] / [PossessedTokenCatalogue] /
 * [FactionRewardTokenCatalogue]) so pile builds/resets are
 * testable with fixture catalogues.
 */
class EnemyPickerViewModel(
    private val repository: EnemyPickerSessionRepository,
    private val catalogue: List<EnemyToken> = TokenCatalogue.tokens,
    private val ruinCatalogue: List<RuinToken> = RuinTokenCatalogue.tokens,
    private val possessedCatalogue: List<PossessedToken> = PossessedTokenCatalogue.tokens,
    private val factionRewardCatalogue: List<FactionRewardToken> = FactionRewardTokenCatalogue.tokens,
) : ViewModel() {

    var session: EnemyPickerSession? by mutableStateOf(null)
        private set

    var isBusy: Boolean by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            // Restore the saved game, or create + persist a fresh default one on first ever launch.
            session = repository.restore()
                ?: EnemyPickerSession.start(
                    catalogue,
                    ruinCatalogue = ruinCatalogue,
                    possessedCatalogue = possessedCatalogue,
                    factionRewardCatalogue = factionRewardCatalogue,
                ).also { repository.save(it) }
        }
    }

    /**
     * Draws from every pile in [draws] (mapped to how many tokens to take from it) as one batch and
     * autosaves. When [possessed] is true (the Apocalypse Dragon possess toggle), each drawn circular
     * enemy is paired with a token from the possessed pile - see [EnemyPickerSession.draw].
     */
    suspend fun draw(draws: Map<TokenPileId, Int>, possessed: Boolean = false) =
        mutate { it.draw(draws, possessed = possessed) }

    /** Sets the defeated flag / [note] on the Draw Log entry at [index] and autosaves. */
    suspend fun setDefeated(index: Int, defeated: Boolean, note: String = "") =
        mutate { it.setDefeated(index, defeated, note) }

    /**
     * The Summon Draw action (issue #191, `CONTEXT.md`'s "Summon Draw"): resolves the Draw Log
     * entry at [parentIndex] to its [EnemyToken] in [catalogue] and draws one token from every pile
     * its Summon attacks name, then autosaves. A no-op if the entry's token has no Summon attack
     * (shouldn't happen - the UI only shows the Summon action when it does) or can't be resolved.
     */
    suspend fun summon(parentIndex: Int) = mutate { current ->
        val entry = current.drawLog.getOrNull(parentIndex) ?: return@mutate current
        val token = catalogue.find { it.id == entry.tokenId } ?: return@mutate current
        val pileIds = token.attacks.filter { it.isSummon }.mapNotNull { it.summons }
        if (pileIds.isEmpty()) current else current.summon(parentIndex, pileIds)
    }

    /**
     * The Enemies-With-Treasure draw (issue #201): resolves the Draw Log entry at [parentIndex] to
     * its [RuinToken] in [ruinCatalogue] and draws one enemy from each pile the ruin names (in
     * order), attaching them as children of the ruin - the same [EnemyPickerSession.summon]
     * machinery a Summon attack uses. A no-op if the entry isn't a resolvable Enemies-With-Treasure
     * ruin (an altar has no [RuinToken.enemyPiles]; the UI only offers this on treasure ruins).
     */
    suspend fun drawRuinEnemies(parentIndex: Int) = mutate { current ->
        val entry = current.drawLog.getOrNull(parentIndex) ?: return@mutate current
        val ruin = ruinCatalogue.find { it.id == entry.tokenId } ?: return@mutate current
        val pileIds = ruin.enemyPiles ?: return@mutate current
        // ephemeral = false: a ruin's Enemies-With-Treasure are real, independently-defeatable
        // enemies held on the board until beaten - not discard-on-draw summon children (issue #251).
        current.summon(parentIndex, pileIds, ephemeral = false)
    }

    /** Rebuilds every pile and clears the Draw Log, keeping the current config, then autosaves. */
    suspend fun reset() = mutate { it.reset(catalogue, ruinCatalogue = ruinCatalogue, possessedCatalogue = possessedCatalogue, factionRewardCatalogue = factionRewardCatalogue) }

    /**
     * Commits staged config edits (the "Apply & Reset" action): rebuilds the piles for [tokenSet]
     * and [drawWithReplacement] from scratch, clearing the Draw Log, then autosaves. Config is
     * committed all at once so changing two expansions doesn't trigger two reset prompts (see
     * `CONTEXT.md`'s "Token Set").
     */
    suspend fun applyConfig(tokenSet: Set<Expansion>, drawWithReplacement: Boolean) =
        mutate { EnemyPickerSession.start(catalogue, tokenSet, drawWithReplacement, ruinCatalogue = ruinCatalogue, possessedCatalogue = possessedCatalogue, factionRewardCatalogue = factionRewardCatalogue) }

    /**
     * Runs [transform] against the current [session], publishes the result, and autosaves it - a
     * no-op if there's no session yet or a mutation is already running. Mirrors
     * [com.guyteichman.mageknightbuddy.ui.dummyplayer.AutosaveSessionViewModel.mutate].
     */
    private suspend fun mutate(transform: (EnemyPickerSession) -> EnemyPickerSession) {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.let(transform) ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    companion object {
        fun factory(repository: EnemyPickerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { EnemyPickerViewModel(repository) }
        }
    }
}
