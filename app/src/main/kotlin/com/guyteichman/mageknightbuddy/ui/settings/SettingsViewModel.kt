package com.guyteichman.mageknightbuddy.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.AppReset
import com.guyteichman.mageknightbuddy.data.BackupCodec
import com.guyteichman.mageknightbuddy.data.BackupDecodeResult
import com.guyteichman.mageknightbuddy.data.FavoriteSitesRepository
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A one-shot message for the Settings screen to surface in a snackbar (backup done, restore failed,
 * etc.). Once shown, the screen calls [SettingsViewModel.messageShown] to clear it.
 */
data class SettingsMessage(val text: String)

/**
 * The confirmation prompt shown before a restore actually overwrites anything: restoring replaces
 * all local records (the decision in issue #121), so the user first sees how many local games
 * ([localCount]) will be swapped for how many from the backup ([backupCount]).
 */
data class RestorePrompt(val localCount: Int, val backupCount: Int)

/** Everything the Settings screen renders that isn't a direct button press. */
data class SettingsUiState(
    val message: SettingsMessage? = null,
    val restorePrompt: RestorePrompt? = null,
    // True while the "reset app to default?" confirm dialog is open (issue #304).
    val resetPrompt: Boolean = false,
)

/**
 * Drives the Settings screen's Backup & Restore actions (issue #121, ADR-0009). An
 * [AndroidViewModel] because reading/writing the user-chosen backup file needs the app's
 * `ContentResolver` (resolved from the SAF `Uri`); the actual JSON encode/decode is delegated to
 * the pure, JVM-tested [BackupCodec], and all record I/O goes through [repository]. Restore is
 * deliberately two-step - [prepareRestoreFrom] only decodes + validates and raises a
 * [RestorePrompt]; nothing is overwritten until [confirmRestore].
 */
class SettingsViewModel(
    application: Application,
    private val repository: ScoringSessionRepository,
    private val favoritesRepository: FavoriteSitesRepository,
    private val appReset: AppReset,
) : AndroidViewModel(application) {

    // Backing MutableStateFlow kept private so only this ViewModel mutates it; the screen observes
    // the read-only [uiState]. update {} applies a copy() atomically, avoiding lost updates if two
    // coroutines touch it at once.
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Sessions + favorite-site ids decoded by prepareRestoreFrom, held until the user confirms (or
    // cancels) the overwrite. Not persisted across process death - if that happens mid-prompt, the
    // user just re-picks the file (ScoringSession isn't Parcelable anyway).
    private var pendingRestore: PendingRestore? = null

    /** The decoded backup awaiting the user's confirm/cancel (see [pendingRestore]). */
    private data class PendingRestore(val sessions: List<ScoringSession>, val favoriteSiteIds: List<String>)

    /** Serializes the whole history and writes it to [uri] (the SAF "create document" result). */
    fun backUpTo(uri: Uri) {
        viewModelScope.launch {
            // Reading + JSON-encoding the history is CPU work (a decode+encode per record), so run it
            // off the main thread on Dispatchers.Default; the file write below is IO-bound instead.
            val (count, json) = withContext(Dispatchers.Default) {
                val sessions = repository.exportAll()
                // Favorites ride along in the same backup file (issue #236); the count message stays
                // session-based (favorites are secondary, and empty for most users).
                val favoriteSiteIds = favoritesRepository.exportAll()
                sessions.size to BackupCodec.encode(sessions, favoriteSiteIds, Instant.now())
            }
            val wrote = withContext(Dispatchers.IO) { writeText(uri, json) }
            _uiState.update {
                it.copy(
                    message = SettingsMessage(
                        if (wrote) "Backed up $count ${games(count)}"
                        else "Backup failed - couldn't write that file",
                    ),
                )
            }
        }
    }

    /**
     * Reads and decodes [uri] (the SAF "open document" result) but does NOT touch local data: on a
     * good file it raises a [RestorePrompt] for confirmation; on a bad one it just reports why.
     */
    fun prepareRestoreFrom(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { readText(uri) }
            if (text == null) {
                _uiState.update { it.copy(message = SettingsMessage("Couldn't read that file")) }
                return@launch
            }
            // Parsing is CPU work (a decode per record), so keep it off the main thread too.
            when (val result = withContext(Dispatchers.Default) { BackupCodec.decode(text) }) {
                is BackupDecodeResult.Success -> {
                    pendingRestore = PendingRestore(result.sessions, result.favoriteSiteIds)
                    val localCount = repository.exportAll().size
                    _uiState.update {
                        it.copy(restorePrompt = RestorePrompt(localCount = localCount, backupCount = result.sessions.size))
                    }
                }
                BackupDecodeResult.Malformed ->
                    _uiState.update { it.copy(message = SettingsMessage("That file isn't a MageKnightBuddy backup")) }
                is BackupDecodeResult.UnsupportedVersion ->
                    _uiState.update { it.copy(message = SettingsMessage("That backup was made by a newer version of the app")) }
            }
        }
    }

    /**
     * Applies the pending restore raised by [prepareRestoreFrom], replacing all local records *and*
     * the favorites set (both were decoded from the same backup - issue #236).
     */
    fun confirmRestore() {
        val pending = pendingRestore ?: return
        viewModelScope.launch {
            repository.replaceAll(pending.sessions)
            favoritesRepository.replaceAll(pending.favoriteSiteIds)
            pendingRestore = null
            _uiState.update {
                it.copy(
                    restorePrompt = null,
                    message = SettingsMessage("Restored ${pending.sessions.size} ${games(pending.sessions.size)}"),
                )
            }
        }
    }

    /** Dismisses the restore prompt without overwriting anything. */
    fun cancelRestore() {
        pendingRestore = null
        _uiState.update { it.copy(restorePrompt = null) }
    }

    /** Opens the "reset app to default?" confirmation. Nothing is wiped until [confirmReset]. */
    fun requestReset() {
        _uiState.update { it.copy(resetPrompt = true) }
    }

    /** Dismisses the reset confirmation without wiping anything. */
    fun cancelReset() {
        _uiState.update { it.copy(resetPrompt = false) }
    }

    /**
     * Wipes every saved game, in-progress session, favorite, and seen-tutorial flag via [AppReset],
     * returning the app to a fresh-install state (issue #304), then reports it in a snackbar. The
     * Scoreboard and Sites tabs update live (Room Flows re-emit empty); a game a screen is holding in
     * memory right now survives until that screen is re-entered (see [AppReset]).
     */
    fun confirmReset() {
        viewModelScope.launch {
            appReset.resetToDefault()
            _uiState.update {
                it.copy(resetPrompt = false, message = SettingsMessage("App reset to default"))
            }
        }
    }

    /** Called by the screen once a [SettingsMessage] has been shown, so it isn't shown again. */
    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    // Writes [text] as UTF-8 to [uri], returning false (rather than throwing) if the stream can't be
    // opened or the write fails - the caller turns that into a user-facing "backup failed" message.
    // "wt" (write + truncate) matters: a plain "w" doesn't truncate on many SAF providers, so
    // overwriting a longer previous backup would leave stale trailing bytes and corrupt the file.
    // SecurityException (a revoked/withdrawn Uri grant) is caught alongside IOException so a bad
    // target never crashes the app, only reports the failure.
    private fun writeText(uri: Uri, text: String): Boolean = try {
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            true
        } ?: false
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }

    // Reads [uri]'s whole content as a UTF-8 string, or null if it can't be opened/read. Catches
    // SecurityException too (e.g. a cloud provider whose grant was revoked), so restoring a bad pick
    // reports "Couldn't read that file" rather than crashing.
    private fun readText(uri: Uri): String? = try {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }

    // "game" vs "games" so the snackbars read naturally for a count of 1.
    private fun games(count: Int) = if (count == 1) "game" else "games"

    companion object {
        /**
         * Builds the [ViewModelProvider.Factory] with [repository] supplied. The Application comes
         * from [ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY], which Compose's
         * `viewModel()` puts in the creation extras for us - that's what an [AndroidViewModel] needs.
         */
        fun factory(
            repository: ScoringSessionRepository,
            favoritesRepository: FavoriteSitesRepository,
            appReset: AppReset,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!,
                    repository = repository,
                    favoritesRepository = favoritesRepository,
                    appReset = appReset,
                )
            }
        }
    }
}
