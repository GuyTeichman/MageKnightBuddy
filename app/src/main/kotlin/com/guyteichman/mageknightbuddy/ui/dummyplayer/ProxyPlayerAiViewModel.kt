package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession

/**
 * Backs Proxy Player mode's AI (turn/round) screen: restores the saved [ProxyPlayerSession] on
 * creation and mutates it via [playTurn]/[resolveObjective]/[endRound], autosaving through
 * [AutosaveSessionViewModel] after every mutation - the Proxy Player counterpart to
 * [DummyPlayerAiViewModel]/[VolkareAiViewModel], including the same isBusy double-tap guard.
 *
 * The session itself is deliberately NOT kept in a SavedStateHandle: it already round-trips
 * through Room on every mutation (like [ProxyPlayerSetupViewModel.hasSavedSession]), so recreating
 * this ViewModel just re-restores it from there instead of duplicating that state.
 */
class ProxyPlayerAiViewModel(repository: ProxyPlayerSessionRepository) :
    AutosaveSessionViewModel<ProxyPlayerSession>(repository) {

    /** Plays one Proxy Player turn (docs/rules/proxy-player.md) and autosaves. */
    suspend fun playTurn() = mutate { it.playTurn() }

    /** Resolves the current Objective Card, discarding it and its Shields (docs/rules/proxy-player.md's "Resolution") and autosaves. */
    suspend fun resolveObjective() = mutate { it.resolveObjective() }

    /** Applies the round-prep offer interactions and autosaves - see [ProxyPlayerSession.endRound]. */
    suspend fun endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor) =
        mutate { it.endRound(advancedActionOfferColor, spellOfferColor) }

    companion object {
        fun factory(repository: ProxyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProxyPlayerAiViewModel(repository) }
        }
    }
}
