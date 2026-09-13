package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession

/**
 * Backs Proxy Player mode's setup fields: tracks the chosen Knight (or "Random") - the same
 * shape as [DummyPlayerSetupViewModel], since Proxy Player picks a Knight the same way standard
 * Dummy Player does (unlike Volkare, which has no Knight field at all - see
 * [VolkareSetupViewModel]). On Start, builds a fresh [ProxyPlayerSession] and autosaves it.
 * Everything but that [ProxyPlayerSession.start] call is shared with [DummyPlayerSetupViewModel] -
 * see [KnightModeSetupViewModel] for that shared logic.
 */
class ProxyPlayerSetupViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ProxyPlayerSessionRepository,
) : KnightModeSetupViewModel<ProxyPlayerSession>(
    savedStateHandle,
    repository,
    sessionFactory = { knight, wasRandom, startsAtNight, isSolo, scenario ->
        ProxyPlayerSession.start(
            knight = knight,
            wasRandom = wasRandom,
            startsAtNight = startsAtNight,
            isSolo = isSolo,
            scenario = scenario,
        )
    },
) {
    companion object {
        fun factory(repository: ProxyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            // Registers the recipe for building this ViewModel; runs once per ViewModel instance -
            // the same indirection DummyPlayerSetupViewModel/VolkareSetupViewModel use to pass a
            // constructor argument through Compose's no-arg-only default viewModel() helper.
            initializer { ProxyPlayerSetupViewModel(createSavedStateHandle(), repository) }
        }
    }
}
