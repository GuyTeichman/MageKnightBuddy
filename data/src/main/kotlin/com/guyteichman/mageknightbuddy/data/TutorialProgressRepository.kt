package com.guyteichman.mageknightbuddy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}

/**
 * [TutorialProgressRepository] backed by a Preferences [DataStore]: one boolean key per tutorial id.
 * The [DataStore] is injected rather than built here so production can point it at the app's data
 * dir while tests point it at a temp file (see `TutorialProgressRepositoryTest`).
 */
class DataStoreTutorialProgressRepository(
    private val dataStore: DataStore<Preferences>,
) : TutorialProgressRepository {

    // `map` transforms the DataStore's Flow<Preferences> into a Flow<Boolean>: for each emitted
    // snapshot, read this id's key and treat a missing key as "not seen yet" (?: false).
    override fun hasSeen(tutorialId: String): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[booleanPreferencesKey(tutorialId)] ?: false }

    // `edit` atomically reads-modifies-writes the whole preferences snapshot; we just set this id's key.
    override suspend fun markSeen(tutorialId: String) {
        dataStore.edit { preferences -> preferences[booleanPreferencesKey(tutorialId)] = true }
    }
}

/**
 * Builds the production [TutorialProgressRepository], backed by a Preferences DataStore file
 * ("tutorial_progress") in the app's data dir. The "factory that looks like a constructor" idiom
 * (see [ScoreCalculatorDraftRepository]); a Preferences DataStore requires a single instance per
 * file per process, so call this exactly once - see `MageKnightBuddyApplication`.
 */
fun TutorialProgressRepository(context: Context): TutorialProgressRepository =
    DataStoreTutorialProgressRepository(
        // applicationContext so the long-lived DataStore never holds onto a shorter-lived Activity.
        PreferenceDataStoreFactory.create {
            context.applicationContext.preferencesDataStoreFile("tutorial_progress")
        },
    )
