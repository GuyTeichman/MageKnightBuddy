package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyteichman.mageknightbuddy.data.SingleSlotAutosaveRepository
import kotlinx.coroutines.launch

/**
 * Base class behind every Dummy Player tab AI (turn/round) ViewModel: restores the autosaved
 * session of type [T] on creation, publishes it as [session], and funnels every mutation through
 * [mutate] so the isBusy double-tap guard and the post-mutation autosave are written exactly once,
 * here, instead of once per concrete ViewModel (see GitHub issue #151). Concrete subclasses
 * (DummyPlayerAiViewModel/VolkareAiViewModel/ProxyPlayerAiViewModel) contribute nothing but a
 * one-line `mutate { it.someMethod(args) }` per domain method they expose.
 *
 * @param T the plain-Kotlin domain session type (e.g. DummyPlayerSession). The Room entity type
 * behind [repository] is deliberately erased via star-projection (`SingleSlotAutosaveRepository<T, *>`)
 * - this class only ever calls [SingleSlotAutosaveRepository.restore]/[SingleSlotAutosaveRepository.save],
 * neither of which mentions the entity type parameter in its signature, so this class (and every
 * subclass) never needs to know or name it. `SingleSlotAutosaveRepository<T, *>` is Kotlin's
 * star-projection: since a concrete repository type like `DummyPlayerSessionRepository` is really
 * `SingleSlotAutosaveRepository<DummyPlayerSession, DummyPlayerSessionEntity>`, it's already a
 * subtype of `SingleSlotAutosaveRepository<DummyPlayerSession, *>` and can be passed straight to
 * the constructor here with no cast.
 */
abstract class AutosaveSessionViewModel<T>(
    private val repository: SingleSlotAutosaveRepository<T, *>,
) : ViewModel() {

    var session: T? by mutableStateOf(null)
        private set

    // Guards mutate{} against a double-tap firing two overlapping mutations - button click
    // handlers disable themselves on this flag, so it only needs to be read/written here.
    var isBusy: Boolean by mutableStateOf(false)
        private set

    init {
        // viewModelScope ties this coroutine's lifetime to the ViewModel - a one-shot restore run
        // once per ViewModel instance, same as every original XAiViewModel's init block.
        viewModelScope.launch {
            session = repository.restore()
        }
    }

    /**
     * Runs [transform] against the current [session], publishes the result as the new [session],
     * and autosaves it through [repository]. A no-op if there's no session yet (nothing restored,
     * or restore is still in flight) or if a mutation is already in progress. Every mutating
     * method on a concrete subclass is exactly one call to this: `mutate { it.playTurn() }`.
     */
    protected suspend fun mutate(transform: (T) -> T) {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.let(transform) ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }
}
