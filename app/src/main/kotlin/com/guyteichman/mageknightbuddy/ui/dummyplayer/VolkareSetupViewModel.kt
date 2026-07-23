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
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareSession
import com.guyteichman.mageknightbuddy.domain.volkareWoundCount
import kotlinx.coroutines.launch

/**
 * Backs Volkare mode's setup fields (Scenario, Race Level, Wound count) - the Volkare counterpart
 * to [DummyPlayerSetupViewModel]. The setup screen hosts this alongside [DummyPlayerSetupViewModel]
 * and [ProxyPlayerSetupViewModel] side by side, switching which one drives Start/Restore based on
 * which mode is currently selected in `DummyPlayerScreen.kt`'s `DummyPlayerModeSelector`.
 */
@OptIn(SavedStateHandleSaveableApi::class)
class VolkareSetupViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: VolkareSessionRepository,
) : ViewModel() {

    var scenario: Scenario by savedStateHandle.saveable("scenario") { mutableStateOf(Scenario.VolkaresReturn) }
        private set
    var raceLevel: RaceLevel by savedStateHandle.saveable("raceLevel") { mutableStateOf(RaceLevel.FAIR) }
        private set

    // Starts at the (scenario, raceLevel) table default; pickScenario/pickRaceLevel reset it back
    // to that default whenever either pill changes, but setWoundCount (a direct edit) can diverge
    // from it - see woundCountIsCustom.
    var woundCount: Int by savedStateHandle.saveable("woundCount") { mutableStateOf(volkareWoundCount(scenario, raceLevel)) }
        private set

    // True once the player has typed a Wound count that no longer matches the current Race
    // Level's table value - the setup screen reads this to decide whether any RaceLevel pill
    // should still show as "selected" (see architecture note in the Volkare plan: "typing any
    // other number just quietly drops the highlight").
    var woundCountIsCustom: Boolean by savedStateHandle.saveable("woundCountIsCustom") { mutableStateOf(false) }
        private set

    // Deliberately NOT saved in savedStateHandle: reflects on-disk state, re-checked fresh every
    // time this ViewModel is created, same as DummyPlayerSetupViewModel.hasSavedSession.
    var hasSavedSession: Boolean by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            hasSavedSession = repository.restore() != null
        }
    }

    /** Picks Volkare's Return or Quest - resets the Wound count to that scenario's table default for the current Race Level. */
    fun pickScenario(selected: Scenario) {
        scenario = selected
        woundCount = volkareWoundCount(scenario, raceLevel)
        woundCountIsCustom = false
    }

    /** Picks a Race Level pill - resets the Wound count to its table value, clearing any custom override. */
    fun pickRaceLevel(selected: RaceLevel) {
        raceLevel = selected
        woundCount = volkareWoundCount(scenario, raceLevel)
        woundCountIsCustom = false
    }

    /** Directly edits the Wound count field; marks it custom unless the new value happens to match the current Race Level's table value. */
    fun changeWoundCount(value: Int) {
        woundCount = value
        woundCountIsCustom = value != volkareWoundCount(scenario, raceLevel)
    }

    /**
     * Builds a new Volkare session for the chosen [scenario]/[raceLevel]/[woundCount] and
     * autosaves it, overwriting any previously saved Volkare session - same "starting a new
     * session silently overwrites the old one" convention as [DummyPlayerSetupViewModel.start].
     */
    suspend fun start() {
        repository.save(VolkareSession.start(scenario = scenario, raceLevel = raceLevel, woundCount = woundCount))
        hasSavedSession = true
    }

    companion object {
        fun factory(repository: VolkareSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { VolkareSetupViewModel(createSavedStateHandle(), repository) }
        }
    }
}
