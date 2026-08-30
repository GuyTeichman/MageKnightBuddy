package com.guyteichman.mageknightbuddy.ui.sites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.FavoriteSitesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Backs the Sites tab's favorites (issue #236): exposes the persisted favorite site ids from
 * [repository] as a live [Flow] the list/detail screens observe, and a [setFavorite] action that
 * writes a star toggle back to storage. This is the tab's first ViewModel - the catalogue itself is
 * static and still read directly - so it holds *only* the favorites, nothing about the catalogue.
 *
 * Follows the same repository-backed-Flow shape as [ScoreboardViewModel][com.guyteichman.mageknightbuddy.ui.scoreboard.ScoreboardViewModel]:
 * a plain [Flow] the UI collects with `collectAsState(initial = emptySet())`, rather than a hand-managed
 * StateFlow, since Room already re-emits on every change.
 */
class SitesViewModel(private val repository: FavoriteSitesRepository) : ViewModel() {
    /**
     * The set of favorited site ids, re-emitted whenever a star is toggled (Room drives the updates).
     * The screens intersect this with the static catalogue to decide which star is filled and which
     * sites pin to the top (see `searchedFilteredGrouped`).
     */
    val favorites: Flow<Set<String>> = repository.observeFavorites()

    /**
     * Stars ([favorite] = true) or unstars ([favorite] = false) the site with id [id]. Launched on
     * [viewModelScope] so the suspend write runs off the UI thread and is cancelled if the ViewModel
     * clears. Idempotent (see [FavoriteSitesRepository.setFavorite]), so the caller passes the desired
     * new state - which it already knows from the star it drew - without a read-back.
     */
    fun setFavorite(id: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(id, favorite) }
    }

    companion object {
        /**
         * Builds a [ViewModelProvider.Factory] that constructs a [SitesViewModel] with [repository]
         * already supplied - the same indirection as the other tabs' factories, needed because
         * Compose's `viewModel()` only constructs no-arg ViewModels on its own.
         */
        fun factory(repository: FavoriteSitesRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SitesViewModel(repository) }
        }
    }
}
