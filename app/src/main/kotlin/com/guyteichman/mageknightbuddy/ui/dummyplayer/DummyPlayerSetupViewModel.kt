package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlinx.coroutines.launch

/**
 * Backs the Dummy Player tab's setup screen: tracks the chosen [Knight] (or "Random") and, on
 * Start, builds a fresh [DummyPlayerSession] and autosaves it via [repository] so the AI screen
 * can restore it. Also tracks whether a previously saved session exists, to drive the
 * "Restore Game" button's enabled state.
 */
@OptIn(SavedStateHandleSaveableApi::class)
class DummyPlayerSetupViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: DummyPlayerSessionRepository,
) : ViewModel() {

    // Defaults to the first Knight rather than leaving the picker blank, matching the Score
    // wizard's "every field starts at a sensible value" convention (see architecture.md).
    var knight: Knight by savedStateHandle.saveable("knight") { mutableStateOf(Knight.entries.first()) }
    var wasRandom: Boolean by savedStateHandle.saveable("wasRandom") { mutableStateOf(false) }

    // Deliberately NOT saved in savedStateHandle: this reflects on-disk state, so it's re-checked
    // fresh via repository.restore() every time this ViewModel is created rather than cached.
    var hasSavedSession: Boolean by mutableStateOf(false)
        private set

    init {
        // viewModelScope ties this coroutine's lifetime to the ViewModel, so it's automatically
        // cancelled if the ViewModel is cleared before the one-shot check below completes.
        viewModelScope.launch {
            hasSavedSession = repository.restore() != null
        }
    }

    /** Picks a specific Knight directly, clearing the "(Random)" badge. */
    fun pickKnight(selected: Knight) {
        knight = selected
        wasRandom = false
    }

    /**
     * Resolves "Random" immediately - rolls a Knight now rather than waiting for Start - so the
     * picker can show which Knight got picked right away, per the setup screen's design (#26).
     */
    fun pickRandom() {
        knight = Knight.entries.random()
        wasRandom = true
    }

    /**
     * Builds a new session for the chosen Knight and autosaves it. Overwrites any previously
     * saved session, per #27's "starting a new session silently overwrites the old one" design.
     */
    suspend fun start() {
        repository.save(DummyPlayerSession.start(knight = knight, wasRandom = wasRandom))
        // hasSavedSession is otherwise only set from the init-block check, which doesn't re-run
        // when returning to this screen via a nested-NavHost back-pop (the ViewModel survives
        // that pop) - so without this, Restore Game stays stale/disabled right after Start.
        hasSavedSession = true
    }

    companion object {
        fun factory(repository: DummyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            // Registers the recipe for building this ViewModel; runs once per ViewModel instance,
            // the same indirection ScoreboardViewModel/ScoreCalculatorViewModel use to pass a
            // constructor argument through Compose's no-arg-only default viewModel() helper.
            initializer { DummyPlayerSetupViewModel(createSavedStateHandle(), repository) }
        }
    }
}
