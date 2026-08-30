package com.guyteichman.mageknightbuddy.ui.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.guyteichman.mageknightbuddy.data.TutorialProgressRepository
import kotlinx.coroutines.flow.first

/**
 * Drives one screen's tutorial pop-up (issue #161): whether it's currently open, how to open it (the
 * top-bar [TutorialAction]), and what to do when it closes. Created by [rememberScreenTutorialState],
 * which also handles the auto-show-once-on-first-visit behavior.
 *
 * [tutorial] is null when the screen's key has no entry in the loaded content, in which case there's
 * simply nothing to show and [show] is a no-op - so a screen can wire this in unconditionally.
 */
@Stable
class ScreenTutorialState(
    val tutorial: Tutorial?,
    private val visibleState: MutableState<Boolean>,
    private val onMarkSeen: () -> Unit,
) {
    /** Whether the pop-up should currently be shown; observed by Compose so the dialog follows it. */
    val isVisible: Boolean get() = visibleState.value

    /** Opens the pop-up on demand (the top-bar tutorial icon). No-op if this screen has no tutorial. */
    fun show() {
        if (tutorial != null) visibleState.value = true
    }

    /** Closes the pop-up and records it as seen (durably), so it won't auto-open on future visits. */
    fun dismiss() {
        visibleState.value = false
        onMarkSeen()
    }
}

/**
 * Wires up [ScreenTutorialState] for the screen identified by [tutorialKey]: reads its [Tutorial]
 * from [tutorials], and on first entry auto-opens the pop-up unless [progress] already has it marked
 * seen. "Seen" is recorded on dismiss (not merely on show - see [ScreenTutorialState.dismiss]), so
 * backgrounding mid-read doesn't burn the single auto-show.
 */
@Composable
fun rememberScreenTutorialState(
    tutorialKey: String,
    tutorials: Map<String, Tutorial>,
    progress: TutorialProgressRepository,
): ScreenTutorialState {
    val tutorial = tutorials[tutorialKey]
    // rememberSaveable so an open pop-up survives a config change / process death (only Booleans are
    // stored, so there's no parcelability hazard - contrast the SavedStateHandle data-object crash).
    val visibleState = rememberSaveable(tutorialKey) { mutableStateOf(false) }
    // The auto-show is a genuine one-shot: once resolved (whether it opened or not), it must not fire
    // again on re-entry or a config change - otherwise a just-dismissed tutorial can pop back up when
    // the recreated LaunchedEffect re-reads `hasSeen` before the async "seen" write has flushed.
    var autoShowResolved by rememberSaveable(tutorialKey) { mutableStateOf(false) }

    LaunchedEffect(tutorialKey) {
        if (!autoShowResolved) {
            autoShowResolved = true
            if (tutorial != null && !progress.hasSeen(tutorialKey).first()) {
                visibleState.value = true
            }
        }
    }

    // remember so the state object is stable across recompositions; the MutableState it wraps is the
    // saveable one above, so show()/dismiss() mutate observed state correctly.
    return remember(tutorialKey, tutorial, visibleState) {
        ScreenTutorialState(
            tutorial = tutorial,
            visibleState = visibleState,
            // Durable fire-and-forget: the repo persists on its own process-lifetime scope, so this
            // survives the screen being disposed the instant after the dialog closes.
            onMarkSeen = { progress.markSeenAsync(tutorialKey) },
        )
    }
}
