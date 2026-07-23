package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.VolkareSession
import kotlinx.coroutines.launch

/**
 * Backs Volkare mode's AI (turn/round) screen: restores the saved [VolkareSession] on creation and
 * mutates it via [playTurn]/[endRound]/[toggleCityRevealed], autosaving through [repository] after
 * every mutation - the Volkare counterpart to [DummyPlayerAiViewModel].
 */
class VolkareAiViewModel(private val repository: VolkareSessionRepository) : ViewModel() {

    var session: VolkareSession? by mutableStateOf(null)
        private set

    // Guards the three mutating actions against a double-tap firing two overlapping mutations,
    // same pattern as DummyPlayerAiViewModel.isBusy.
    var isBusy: Boolean by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            session = repository.restore()
        }
    }

    /** Plays one Volkare turn - reveals a card, or logs Frenzy/sets QuestLost on an empty deck - and autosaves. */
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

    /** Advances to the next Round (a tracking convenience only - see [VolkareSession.endRound]) and autosaves. */
    suspend fun endRound() {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.endRound() ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    /** Flips the City Revealed flag (Volkare's Return only - see `CONTEXT.md`'s "City Revealed" entry) and autosaves. */
    suspend fun toggleCityRevealed() {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.toggleCityRevealed() ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    companion object {
        fun factory(repository: VolkareSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { VolkareAiViewModel(repository) }
        }
    }
}
