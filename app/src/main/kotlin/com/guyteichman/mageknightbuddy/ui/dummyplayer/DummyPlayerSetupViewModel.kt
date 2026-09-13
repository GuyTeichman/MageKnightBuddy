package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession

/**
 * Backs the Dummy Player tab's setup screen: tracks the chosen Knight (or "Random") and, on
 * Start, builds a fresh [DummyPlayerSession] and autosaves it via [repository] so the AI screen
 * can restore it. Also tracks whether a previously saved session exists, to drive the
 * "Restore Game" button's enabled state. Everything but the [DummyPlayerSession.start] call is
 * shared with [ProxyPlayerSetupViewModel] - see [KnightModeSetupViewModel] for that shared logic.
 */
class DummyPlayerSetupViewModel(
    savedStateHandle: SavedStateHandle,
    repository: DummyPlayerSessionRepository,
) : KnightModeSetupViewModel<DummyPlayerSession>(
    savedStateHandle,
    repository,
    sessionFactory = { knight, wasRandom, startsAtNight, isSolo, scenario ->
        DummyPlayerSession.start(
            knight = knight,
            wasRandom = wasRandom,
            startsAtNight = startsAtNight,
            isSolo = isSolo,
            scenario = scenario,
        )
    },
) {
    companion object {
        fun factory(repository: DummyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            // Registers the recipe for building this ViewModel; runs once per ViewModel instance,
            // the same indirection ScoreboardViewModel/ScoreCalculatorViewModel use to pass a
            // constructor argument through Compose's no-arg-only default viewModel() helper.
            initializer { DummyPlayerSetupViewModel(createSavedStateHandle(), repository) }
        }
    }
}
