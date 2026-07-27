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
 * [catalogue] is injected (default: the real [TokenCatalogue]) so pile builds/resets are testable
 * with a fixture catalogue.
 */
class EnemyPickerViewModel(
    private val repository: EnemyPickerSessionRepository,
    private val catalogue: List<EnemyToken> = TokenCatalogue.tokens,
) : ViewModel() {

    var session: EnemyPickerSession? by mutableStateOf(null)
        private set

    var isBusy: Boolean by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            // Restore the saved game, or create + persist a fresh default one on first ever launch.
            session = repository.restore()
                ?: EnemyPickerSession.start(catalogue).also { repository.save(it) }
        }
    }

    /** Draws [count] tokens from [pileId] as one batch and autosaves. */
    suspend fun draw(pileId: TokenPileId, count: Int = 1) = mutate { it.draw(pileId, count) }

    /** Sets the defeated flag / [note] on the Draw Log entry at [index] and autosaves. */
    suspend fun setDefeated(index: Int, defeated: Boolean, note: String = "") =
        mutate { it.setDefeated(index, defeated, note) }

    /** Rebuilds every pile and clears the Draw Log, keeping the current config, then autosaves. */
    suspend fun reset() = mutate { it.reset(catalogue) }

    /**
     * Commits staged config edits (the "Apply & Reset" action): rebuilds the piles for [tokenSet]
     * and [drawWithReplacement] from scratch, clearing the Draw Log, then autosaves. Config is
     * committed all at once so changing two expansions doesn't trigger two reset prompts (see
     * `CONTEXT.md`'s "Token Set").
     */
    suspend fun applyConfig(tokenSet: Set<Expansion>, drawWithReplacement: Boolean) =
        mutate { EnemyPickerSession.start(catalogue, tokenSet, drawWithReplacement) }

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
