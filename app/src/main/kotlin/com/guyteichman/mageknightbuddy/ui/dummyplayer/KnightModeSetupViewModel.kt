package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.guyteichman.mageknightbuddy.data.SingleSlotAutosaveRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Scenario
import kotlinx.coroutines.launch

/**
 * Shared base for the Dummy Player tab's two Knight-based setup screens -
 * [DummyPlayerSetupViewModel] and [ProxyPlayerSetupViewModel]. Both track the same
 * knight/wasRandom/isSolo/scenario fields and the same "does a saved session already exist"
 * check, and differ only in which concrete session type Start builds ([T] here -
 * `DummyPlayerSession` or `ProxyPlayerSession`). That one difference is supplied by
 * [sessionFactory] rather than duplicated in each subclass. [VolkareSetupViewModel] deliberately
 * does NOT extend this: Volkare has no Knight to pick, so its fields don't overlap enough to
 * share this base.
 *
 * The star projection on [repository]'s second type argument (`SingleSlotAutosaveRepository<T,
 * *>`) is because this base class only ever calls [SingleSlotAutosaveRepository.save]/
 * [SingleSlotAutosaveRepository.restore], neither of which exposes the Room entity type - so this
 * class doesn't need to know or care what it is.
 */
@OptIn(SavedStateHandleSaveableApi::class)
abstract class KnightModeSetupViewModel<T>(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SingleSlotAutosaveRepository<T, *>,
    private val sessionFactory: (
        knight: Knight,
        wasRandom: Boolean,
        startsAtNight: Boolean,
        isSolo: Boolean,
        scenario: Scenario,
    ) -> T,
) : ViewModel() {

    // Defaults to the first Knight rather than leaving the picker blank, matching the Score
    // wizard's "every field starts at a sensible value" convention (see architecture.md).
    var knight: Knight by savedStateHandle.saveable("knight") { mutableStateOf(Knight.entries.first()) }
    var wasRandom: Boolean by savedStateHandle.saveable("wasRandom") { mutableStateOf(false) }

    // Defaults to solo (unchecked toggle) - most sessions played through this app so far are solo.
    var isSolo: Boolean by savedStateHandle.saveable("isSolo") { mutableStateOf(true) }

    // Stored as the scenario's stable String **id**, not the Scenario object itself: Scenario's
    // members are `data object`s (see domain/Scenario.kt), which are neither Parcelable nor
    // Serializable, so putting one straight into SavedStateHandle crashes the app the moment
    // Android parcels saved state on background ("Parcel: unknown type for value ..." - issue
    // #212). Mirrors VolkareSetupViewModel.scenarioId/ScoreCalculatorViewModel.scenarioId for the
    // same reason.
    private var scenarioId: String by savedStateHandle.saveable("scenarioId") { mutableStateOf(Scenario.SoloConquest.id) }

    // Computed property (no backing field of its own): re-derives the Scenario from the stored id
    // on every read, and writes back through the id on set - so the picker still reads/writes a
    // Scenario while only a String ever reaches SavedStateHandle. Only meaningful in coop mode
    // (mode != VOLKARE && !isSolo per issue #220) - the setup screen only shows this picker then.
    var scenario: Scenario
        get() = Scenario.fromId(scenarioId)
        set(value) { scenarioId = value.id }

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

    /**
     * Resolves "Random" immediately - rolls a Knight now rather than waiting for Start - so the
     * picker can show which Knight got picked right away, per the setup screen's design (#26).
     */
    fun pickRandom() {
        knight = Knight.entries.random()
        wasRandom = true
    }

    /**
     * Builds a new session for the chosen Knight (via [sessionFactory]) and autosaves it.
     * Overwrites any previously saved session, per #27's "starting a new session silently
     * overwrites the old one" design. [startsAtNight] comes from the shared setup screen's
     * "Starts at night?" checkbox (default false - most scenarios start at day).
     */
    suspend fun start(startsAtNight: Boolean = false) {
        repository.save(sessionFactory(knight, wasRandom, startsAtNight, isSolo, scenario))
        // hasSavedSession is otherwise only set from the init-block check, which doesn't re-run
        // when returning to this screen via a nested-NavHost back-pop (the ViewModel survives
        // that pop) - so without this, Restore Game stays stale/disabled right after Start.
        hasSavedSession = true
    }
}
