package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.VolkareSession

/**
 * Backs Volkare mode's AI (turn/round) screen: restores the saved [VolkareSession] on creation and
 * mutates it via [playTurn]/[endRound]/[toggleCityRevealed], autosaving through
 * [AutosaveSessionViewModel] after every mutation - the Volkare counterpart to
 * [DummyPlayerAiViewModel].
 */
class VolkareAiViewModel(repository: VolkareSessionRepository) :
    AutosaveSessionViewModel<VolkareSession>(repository) {

    /** Plays one Volkare turn - reveals a card, or logs Frenzy/sets QuestLost on an empty deck - and autosaves. */
    suspend fun playTurn() = mutate { it.playTurn() }

    /** Advances to the next Round (a tracking convenience only - see [VolkareSession.endRound]) and autosaves. */
    suspend fun endRound() = mutate { it.endRound() }

    /** Flips the City Revealed flag (Volkare's Return only - see `CONTEXT.md`'s "City Revealed" entry) and autosaves. */
    suspend fun toggleCityRevealed() = mutate { it.toggleCityRevealed() }

    companion object {
        fun factory(repository: VolkareSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { VolkareAiViewModel(repository) }
        }
    }
}
