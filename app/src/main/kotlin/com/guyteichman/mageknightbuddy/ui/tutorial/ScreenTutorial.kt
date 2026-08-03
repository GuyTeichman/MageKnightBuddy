package com.guyteichman.mageknightbuddy.ui.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.guyteichman.mageknightbuddy.data.TutorialProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    /** Closes the pop-up and records it as seen, so it won't auto-open on future visits. */
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
    val scope: CoroutineScope = rememberCoroutineScope()
    // rememberSaveable so an open pop-up survives a config change / process death (only a Boolean is
    // stored, so there's no parcelability hazard - contrast the SavedStateHandle data-object crash).
    val visibleState = rememberSaveable(tutorialKey) { mutableStateOf(false) }

    // Keyed on tutorialKey so it runs once per screen entry: auto-show only when never seen before.
    LaunchedEffect(tutorialKey) {
        if (tutorial != null && !progress.hasSeen(tutorialKey).first()) {
            visibleState.value = true
        }
    }

    // remember so the state object is stable across recompositions; the MutableState it wraps is the
    // saveable one above, so show()/dismiss() mutate observed state correctly.
    return remember(tutorialKey, tutorial, visibleState) {
        ScreenTutorialState(
            tutorial = tutorial,
            visibleState = visibleState,
            // Fire-and-forget: persisting the seen flag needn't block closing the dialog.
            onMarkSeen = { scope.launch { progress.markSeen(tutorialKey) } },
        )
    }
}
