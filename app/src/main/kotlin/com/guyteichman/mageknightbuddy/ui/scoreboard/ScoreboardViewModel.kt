package com.guyteichman.mageknightbuddy.ui.scoreboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.data.StoredScoringSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Backs the Scoreboard tab: exposes the saved games from [repository] as a [Flow] so the UI
 * recomposes automatically whenever one is added or deleted, and deletes an individual game on
 * request (issue #304). Each item is a [StoredScoringSession] (session + its Room id), because
 * delete needs a stable per-row handle. Living in a `ViewModel` (rather than composable `remember`
 * state) means this list survives switching tabs and coming back, the same rationale as ADR-0002's
 * ViewModel-backed wizard state.
 */
class ScoreboardViewModel(private val repository: ScoringSessionRepository) : ViewModel() {
    // repository.getAll() already returns a Flow that emits a fresh list on every change to
    // the underlying storage, so there's nothing more to wire up here - just expose it as-is.
    val sessions: Flow<List<StoredScoringSession>> = repository.getAll()

    /**
     * Deletes the saved game with this Room [id] (from [StoredScoringSession.id]). Launched on
     * [viewModelScope] so the suspend delete runs off the caller's thread and is cancelled with the
     * ViewModel; the getAll [Flow] then re-emits without the removed row, refreshing the list.
     */
    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        /**
         * Builds a [ViewModelProvider.Factory] that constructs a [ScoreboardViewModel] with
         * [repository] already supplied. This indirection exists because [ScoreboardViewModel]
         * takes a constructor argument, and Compose's default `viewModel()` helper only knows
         * how to construct no-arg ViewModels on its own.
         */
        fun factory(repository: ScoringSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            // `initializer` registers the recipe for building this ViewModel type; it runs
            // once, the first time this ViewModel is requested, and the resulting instance is
            // then cached and reused across recompositions and tab navigation.
            initializer { ScoreboardViewModel(repository) }
        }
    }
}
