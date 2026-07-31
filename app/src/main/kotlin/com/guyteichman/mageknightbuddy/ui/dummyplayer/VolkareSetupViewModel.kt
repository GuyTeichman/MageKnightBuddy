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

    // Stored as the scenario's stable String **id**, not the Scenario object itself: Scenario's
    // members are `data object`s (see domain/Scenario.kt), which are neither Parcelable nor
    // Serializable, so putting one straight into SavedStateHandle crashes the app the moment
    // Android parcels saved state on background ("Parcel: unknown type for value VolkaresReturn" -
    // issue #212). This mirrors ScoreCalculatorViewModel.scenarioId, which stores the id for the
    // same reason. Every other saveable field below is an enum/Int/Boolean (all Parcelable-safe).
    private var scenarioId: String by savedStateHandle.saveable("scenarioId") { mutableStateOf(Scenario.VolkaresReturn.id) }

    // Computed property (no backing field of its own): re-derives the Scenario from the stored id
    // on every read, and writes back through the id on set - so the picker still reads/writes a
    // Scenario while only a String ever reaches SavedStateHandle.
    var scenario: Scenario
        get() = Scenario.fromId(scenarioId)
        private set(value) { scenarioId = value.id }

    var raceLevel: RaceLevel by savedStateHandle.saveable("raceLevel") { mutableStateOf(RaceLevel.FAIR) }
        private set

    // Defaults to solo (unchecked toggle), mirroring DummyPlayerSetupViewModel.isSolo. No separate
    // Scenario field here - Volkare's own scenario above (Return/Quest) already drives Tactic
    // removal (see TacticRules.kt's isVolkare branch), unrelated to the coop-only Scenario picker
    // Standard/Proxy Player show.
    var isSolo: Boolean by savedStateHandle.saveable("isSolo") { mutableStateOf(true) }

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
     * [startsAtNight] comes from the shared setup screen's "Starts at night?" checkbox (default
     * false - most scenarios start at day).
     */
    suspend fun start(startsAtNight: Boolean = false) {
        repository.save(
            VolkareSession.start(
                scenario = scenario,
                raceLevel = raceLevel,
                woundCount = woundCount,
                startsAtNight = startsAtNight,
                isSolo = isSolo,
            ),
        )
        hasSavedSession = true
    }

    companion object {
        fun factory(repository: VolkareSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { VolkareSetupViewModel(createSavedStateHandle(), repository) }
        }
    }
}
