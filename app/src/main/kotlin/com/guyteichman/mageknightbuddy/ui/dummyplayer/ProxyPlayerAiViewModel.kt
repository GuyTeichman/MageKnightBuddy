package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlinx.coroutines.launch

/**
 * Backs Proxy Player mode's AI (turn/round) screen: restores the saved [ProxyPlayerSession] on
 * creation and mutates it via [playTurn]/[resolveObjective]/[endRound], autosaving through
 * [repository] after every mutation - the Proxy Player counterpart to
 * [DummyPlayerAiViewModel]/[VolkareAiViewModel], including the same [isBusy] double-tap guard.
 *
 * The session itself is deliberately NOT kept in a SavedStateHandle: it already round-trips
 * through Room on every mutation (like [ProxyPlayerSetupViewModel.hasSavedSession]), so recreating
 * this ViewModel just re-restores it from there instead of duplicating that state.
 */
class ProxyPlayerAiViewModel(private val repository: ProxyPlayerSessionRepository) : ViewModel() {

    var session: ProxyPlayerSession? by mutableStateOf(null)
        private set

    // Guards playTurn/resolveObjective/endRound against a double-tap firing two overlapping
    // mutations - the button click handlers disable themselves on this flag, so it only needs to
    // be read/written here.
    var isBusy: Boolean by mutableStateOf(false)
        private set

    init {
        // viewModelScope ties this coroutine's lifetime to the ViewModel, matching the one-shot
        // restore pattern in DummyPlayerAiViewModel's init block.
        viewModelScope.launch {
            session = repository.restore()
        }
    }

    /** Plays one Proxy Player turn (docs/rules/proxy-player.md) and autosaves. */
    suspend fun playTurn() {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.playTurn() ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    /** Resolves the current Objective Card as Explored or Completed (docs/rules/proxy-player.md's "Resolution") and autosaves. */
    suspend fun resolveObjective(resolution: ProxyPlayerObjectiveResolution) {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.resolveObjective(resolution) ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    /** Applies the round-prep offer interactions and autosaves - see [ProxyPlayerSession.endRound]. */
    suspend fun endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor) {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.endRound(advancedActionOfferColor, spellOfferColor) ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    companion object {
        fun factory(repository: ProxyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProxyPlayerAiViewModel(repository) }
        }
    }
}
