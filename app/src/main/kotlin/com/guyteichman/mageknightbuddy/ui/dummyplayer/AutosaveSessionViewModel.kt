package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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

    // In-memory stack of pre-mutation session snapshots, most-recent last, powering Undo (issue
    // #62). Each snapshot is just a reference to an already-existing immutable session object, so
    // holding the whole history is cheap and undo needs no reconstruction - reverting is popping
    // the object that was current before the action (this also sidesteps endRound()'s
    // non-reversible reshuffle: the pre-shuffle deck is retained, never recomputed). Deliberately
    // in-memory only, not persisted: it's wiped when this ViewModel is recreated (navigating back
    // to setup, or process death), which is fine because undo is a live-session misclick tool and
    // autosave already preserves the current state across those. mutableStateListOf makes reads of
    // it (via [canUndo]) observable to Compose, so the Undo button enables/disables reactively.
    private val undoStack = mutableStateListOf<T>()

    /** Whether there's a prior action to revert - drives the Undo button's enabled state. */
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

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
            val current = session ?: return
            val next = transform(current)
            // Push the pre-mutation snapshot before publishing the new one, so Undo can restore
            // exactly what was on screen before this action (issue #62).
            undoStack.add(current)
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    /**
     * Reverts the most recent mutation by restoring the session snapshot taken just before it, and
     * autosaves that restored state so Room and the visible session never diverge (issue #62's
     * autosave interaction). A no-op if there's nothing left to undo or a mutation is already in
     * flight. Undo itself is not undoable and pushes nothing onto the stack - it only pops - so the
     * stack shrinks back toward the state this screen was entered in and no further.
     */
    suspend fun undo() {
        if (isBusy || undoStack.isEmpty()) return
        isBusy = true
        try {
            // removeAt(lastIndex) pops the most-recent snapshot (the stack's top), matching the
            // most-recent-action-first order the button reverts in.
            val previous = undoStack.removeAt(undoStack.lastIndex)
            session = previous
            repository.save(previous)
        } finally {
            isBusy = false
        }
    }
}
