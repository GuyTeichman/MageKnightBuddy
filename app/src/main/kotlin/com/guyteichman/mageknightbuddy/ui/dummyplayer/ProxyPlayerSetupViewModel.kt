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
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlinx.coroutines.launch

/**
 * Backs Proxy Player mode's setup fields: tracks the chosen [Knight] (or "Random") - the same
 * shape as [DummyPlayerSetupViewModel], since Proxy Player picks a Knight the same way standard
 * Dummy Player does (unlike Volkare, which has no Knight field at all - see
 * [VolkareSetupViewModel]). On Start, builds a fresh [ProxyPlayerSession] and autosaves it.
 */
@OptIn(SavedStateHandleSaveableApi::class)
class ProxyPlayerSetupViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProxyPlayerSessionRepository,
) : ViewModel() {

    // Defaults to the first Knight rather than leaving the picker blank, matching
    // DummyPlayerSetupViewModel's convention.
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

    /** Resolves "Random" immediately, mirroring [DummyPlayerSetupViewModel.pickRandom]. */
    fun pickRandom() {
        knight = Knight.entries.random()
        wasRandom = true
    }

    /** Builds a new session for the chosen Knight and autosaves it, overwriting any previously saved Proxy Player session. */
    suspend fun start() {
        repository.save(ProxyPlayerSession.start(knight = knight, wasRandom = wasRandom))
        // hasSavedSession is otherwise only set from the init-block check, which doesn't re-run
        // when returning to this screen via a nested-NavHost back-pop (the ViewModel survives
        // that pop) - so without this, Restore Game stays stale/disabled right after Start.
        hasSavedSession = true
    }

    companion object {
        fun factory(repository: ProxyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            // Registers the recipe for building this ViewModel; runs once per ViewModel instance -
            // the same indirection DummyPlayerSetupViewModel/VolkareSetupViewModel use to pass a
            // constructor argument through Compose's no-arg-only default viewModel() helper.
            initializer { ProxyPlayerSetupViewModel(createSavedStateHandle(), repository) }
        }
    }
}
