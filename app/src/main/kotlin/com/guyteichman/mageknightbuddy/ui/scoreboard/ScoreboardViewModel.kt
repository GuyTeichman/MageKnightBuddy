package com.guyteichman.mageknightbuddy.ui.scoreboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import kotlinx.coroutines.flow.Flow

/**
 * Backs the Scoreboard tab: exposes the saved [ScoringSession] list from [repository] as a
 * [Flow] so the UI recomposes automatically whenever a session is added. Living in a
 * `ViewModel` (rather than composable `remember` state) means this list survives switching
 * tabs and coming back, the same rationale as ADR-0002's ViewModel-backed wizard state.
 */
class ScoreboardViewModel(repository: ScoringSessionRepository) : ViewModel() {
    // repository.getAll() already returns a Flow that emits a fresh list on every change to
    // the underlying storage, so there's nothing more to wire up here - just expose it as-is.
    val sessions: Flow<List<ScoringSession>> = repository.getAll()

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
