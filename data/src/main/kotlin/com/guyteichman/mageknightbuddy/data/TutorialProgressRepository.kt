package com.guyteichman.mageknightbuddy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Remembers which per-screen tutorials the user has already seen (issue #161), so each one can
 * auto-show the first time its screen is visited and then stay quiet. The app's first piece of
 * persisted "seen this UI" state - deliberately its own tiny store, kept separate from the Room
 * scoring/session schema.
 */
interface TutorialProgressRepository {
    /** Emits whether the tutorial identified by [tutorialId] has been marked seen; false until it has. */
    fun hasSeen(tutorialId: String): Flow<Boolean>

    /** Records that [tutorialId] has now been seen, durably, so [hasSeen] reports true from here on. */
    suspend fun markSeen(tutorialId: String)

    /**
     * Fire-and-forget [markSeen] for callers with no long-lived scope of their own (the UI dismiss
     * handler): the write runs on this repository's process-lifetime scope, so it isn't cancelled if
     * the screen is disposed the instant after the tutorial closes - which would otherwise strand the
     * "seen" flag and re-auto-show the tutorial forever.
     */
    fun markSeenAsync(tutorialId: String)

    /**
     * Forgets every "seen" flag, so all per-screen tutorials auto-show again on their next visit.
     * Part of the app-wide "reset to default" (issue #304), which returns the app to a first-launch state.
     */
    suspend fun clear()
}

/**
 * [TutorialProgressRepository] backed by a Preferences [DataStore]: one boolean key per tutorial id.
 * The [DataStore] and [scope] are injected rather than built here so production can point them at the
 * app's data dir + a process-lifetime scope while tests use a temp file + the test scope (see
 * `TutorialProgressRepositoryTest`). [scope] must outlive any single screen for [markSeenAsync] to be
 * durable.
 */
class DataStoreTutorialProgressRepository(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
) : TutorialProgressRepository {

    // `map` transforms the DataStore's Flow<Preferences> into a Flow<Boolean>: for each emitted
    // snapshot, read this id's key and treat a missing key as "not seen yet" (?: false).
    override fun hasSeen(tutorialId: String): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[booleanPreferencesKey(tutorialId)] ?: false }

    // `edit` atomically reads-modifies-writes the whole preferences snapshot; we just set this id's key.
    override suspend fun markSeen(tutorialId: String) {
        dataStore.edit { preferences -> preferences[booleanPreferencesKey(tutorialId)] = true }
    }

    // Launched on the repository's own scope, not the caller's, so a disposed screen can't cancel it.
    override fun markSeenAsync(tutorialId: String) {
        scope.launch { markSeen(tutorialId) }
    }

    // edit { it.clear() } atomically drops every key in the store, so hasSeen falls back to its
    // "missing key -> false" default for all tutorials.
    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }
}

/**
 * Builds the production [TutorialProgressRepository], backed by a Preferences DataStore file
 * ("tutorial_progress") in the app's data dir and a process-lifetime IO scope shared by the DataStore
 * itself and [TutorialProgressRepository.markSeenAsync]. The "factory that looks like a constructor"
 * idiom (see [ScoreCalculatorDraftRepository]); a Preferences DataStore requires a single instance per
 * file per process, so call this exactly once - see `MageKnightBuddyApplication`.
 */
fun TutorialProgressRepository(context: Context): TutorialProgressRepository {
    // SupervisorJob so one failed write can't tear down the whole scope; IO since it's disk work.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
        // applicationContext so the long-lived DataStore never holds onto a shorter-lived Activity.
        context.applicationContext.preferencesDataStoreFile("tutorial_progress")
    }
    return DataStoreTutorialProgressRepository(dataStore, scope)
}
