package com.guyteichman.mageknightbuddy.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.BackupCodec
import com.guyteichman.mageknightbuddy.data.BackupDecodeResult
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
) : AndroidViewModel(application) {

    // Backing MutableStateFlow kept private so only this ViewModel mutates it; the screen observes
    // the read-only [uiState]. update {} applies a copy() atomically, avoiding lost updates if two
    // coroutines touch it at once.
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Sessions decoded by prepareRestoreFrom, held until the user confirms (or cancels) the
    // overwrite. Not persisted across process death - if that happens mid-prompt, the user just
    // re-picks the file (ScoringSession isn't Parcelable anyway).
    private var pendingRestore: List<ScoringSession>? = null

    /** Serializes the whole history and writes it to [uri] (the SAF "create document" result). */
    fun backUpTo(uri: Uri) {
        viewModelScope.launch {
            val sessions = repository.exportAll()
            val json = BackupCodec.encode(sessions, Instant.now())
            val wrote = withContext(Dispatchers.IO) { writeText(uri, json) }
            _uiState.update {
                it.copy(
                    message = SettingsMessage(
                        if (wrote) "Backed up ${sessions.size} ${games(sessions.size)}"
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
            when (val result = BackupCodec.decode(text)) {
                is BackupDecodeResult.Success -> {
                    pendingRestore = result.sessions
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

    /** Applies the pending restore raised by [prepareRestoreFrom], replacing all local records. */
    fun confirmRestore() {
        val sessions = pendingRestore ?: return
        viewModelScope.launch {
            repository.replaceAll(sessions)
            pendingRestore = null
            _uiState.update {
                it.copy(
                    restorePrompt = null,
                    message = SettingsMessage("Restored ${sessions.size} ${games(sessions.size)}"),
                )
            }
        }
    }

    /** Dismisses the restore prompt without overwriting anything. */
    fun cancelRestore() {
        pendingRestore = null
        _uiState.update { it.copy(restorePrompt = null) }
    }

    /** Called by the screen once a [SettingsMessage] has been shown, so it isn't shown again. */
    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    // Writes [text] as UTF-8 to [uri], returning false (rather than throwing) if the stream can't be
    // opened or the write fails - the caller turns that into a user-facing "backup failed" message.
    private fun writeText(uri: Uri, text: String): Boolean = try {
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            true
        } ?: false
    } catch (e: IOException) {
        false
    }

    // Reads [uri]'s whole content as a UTF-8 string, or null if it can't be opened/read.
    private fun readText(uri: Uri): String? = try {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    } catch (e: IOException) {
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
        fun factory(repository: ScoringSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!,
                    repository = repository,
                )
            }
        }
    }
}
