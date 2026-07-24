package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession

/**
 * Backs the Dummy Player tab's AI (turn/round) screen: restores the saved [DummyPlayerSession] on
 * creation and mutates it via [playTurn]/[endRound], autosaving through [AutosaveSessionViewModel]
 * after every mutating action - per issue #35's autosave requirement.
 *
 * The session itself is deliberately NOT kept in a SavedStateHandle: it already round-trips
 * through Room on every mutation (like [DummyPlayerSetupViewModel.hasSavedSession]), so recreating
 * this ViewModel just re-restores it from there instead of duplicating that state.
 */
class DummyPlayerAiViewModel(repository: DummyPlayerSessionRepository) :
    AutosaveSessionViewModel<DummyPlayerSession>(repository) {

    /**
     * Flips the Dummy Player's next turn (or announces End of Round, if its deck is empty) and
     * autosaves. Suspend, not self-launching - matching [DummyPlayerSetupViewModel.start], the
     * caller (the screen) supplies its own coroutine scope, e.g. tied to a button click.
     */
    suspend fun playTurn() = mutate { it.playTurn() }

    /**
     * Applies the round-prep offer interactions (see docs/rules/dummy-player.md) and autosaves.
     * [advancedActionOfferColor] is the card removed from the Advanced Action offer this
     * round-prep - single- or dual-color, see [CardIdentity] - and [spellOfferColor] is the color
     * of the card removed from the Spell offer (Spells are never dual-color). The End Round dialog
     * is what asks the player to supply them. Callable at any time, even mid-round:
     * [DummyPlayerSession.endRound] doesn't require [DummyPlayerSession.roundEnded] to be true
     * first.
     */
    suspend fun endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor) =
        mutate { it.endRound(advancedActionOfferColor, spellOfferColor) }

    companion object {
        fun factory(repository: DummyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { DummyPlayerAiViewModel(repository) }
        }
    }
}
