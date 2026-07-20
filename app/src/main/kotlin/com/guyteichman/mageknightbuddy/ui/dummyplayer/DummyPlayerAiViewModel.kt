package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import kotlinx.coroutines.launch

/**
 * Backs the Dummy Player tab's AI (turn/round) screen: restores the saved [DummyPlayerSession] on
 * creation and mutates it via [playTurn]/[endRound], autosaving through [repository] after every
 * mutating action - per issue #35's autosave requirement.
 *
 * The session itself is deliberately NOT kept in a SavedStateHandle: it already round-trips
 * through Room on every mutation (like [DummyPlayerSetupViewModel.hasSavedSession]), so recreating
 * this ViewModel just re-restores it from there instead of duplicating that state.
 */
class DummyPlayerAiViewModel(private val repository: DummyPlayerSessionRepository) : ViewModel() {

    var session: DummyPlayerSession? by mutableStateOf(null)
        private set

    // Guards playTurn/endRound against a double-tap firing two overlapping mutations - the button
    // click handlers disable themselves on this flag, so it only needs to be read/written here.
    var isBusy: Boolean by mutableStateOf(false)
        private set

    init {
        // viewModelScope ties this coroutine's lifetime to the ViewModel, matching the one-shot
        // restore pattern in DummyPlayerSetupViewModel's init block.
        viewModelScope.launch {
            session = repository.restore()
        }
    }

    /**
     * Flips the Dummy Player's next turn (or announces End of Round, if its deck is empty) and
     * autosaves. Suspend, not self-launching - matching [DummyPlayerSetupViewModel.start], the
     * caller (the screen) supplies its own coroutine scope, e.g. tied to a button click.
     */
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

    /**
     * Applies the round-prep offer interactions (see docs/rules/dummy-player.md) and autosaves.
     * [advancedActionOfferColor] and [spellOfferColor] are the colors of the cards removed from
     * those offers this round-prep - the End Round dialog is what asks the player to supply them.
     * Callable at any time, even mid-round: [DummyPlayerSession.endRound] doesn't require
     * [DummyPlayerSession.roundEnded] to be true first.
     */
    suspend fun endRound(advancedActionOfferColor: CardColor, spellOfferColor: CardColor) {
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
        fun factory(repository: DummyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { DummyPlayerAiViewModel(repository) }
        }
    }
}
