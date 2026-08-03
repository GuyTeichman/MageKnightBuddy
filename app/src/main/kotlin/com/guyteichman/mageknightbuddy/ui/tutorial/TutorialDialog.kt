package com.guyteichman.mageknightbuddy.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

/**
 * The shared "show tutorial" top-bar action (issue #161): a graduation-cap icon that re-opens a
 * screen's tutorial pop-up. Deliberately a different icon from the field-help "?" ([HelpButton]) so
 * the two help affordances don't blur together. Sits next to the settings gear in a screen's
 * `TopAppBar` actions.
 */
@Composable
fun TutorialAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.School, contentDescription = "Show tutorial")
    }
}

/**
 * The triggerable, paginated tutorial pop-up (issue #161). A [Dialog] hosting a [HorizontalPager] of
 * the [tutorial]'s steps: swipe (or Back/Next) between them, a dots row shows progress, and the last
 * page's button becomes Done. [onDismiss] fires on any close (Done, the X, tapping outside, or the
 * back gesture) - the caller uses that single signal to both hide the dialog and mark the tutorial
 * seen.
 */
@Composable
fun TutorialDialog(tutorial: Tutorial, onDismiss: () -> Unit) {
    // pageCount is a lambda so the pager reads it lazily; steps is fixed here but that's the API.
    val pagerState = rememberPagerState(pageCount = { tutorial.steps.size })
    // Button-driven paging animates the pager, which is a suspend call - hence a coroutine scope.
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                // Header: the screen's overall tutorial title, plus an explicit close.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tutorial.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close tutorial")
                    }
                }

                Spacer(Modifier.size(8.dp))

                HorizontalPager(
                    state = pagerState,
                    // A min height keeps the dialog from resizing as steps of different lengths page by.
                    modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    val step = tutorial.steps[page]
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(text = step.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.size(16.dp))

                // Dots: the current step's dot is larger and primary-colored; the rest are muted.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(tutorial.steps.size) { index ->
                        val selected = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (selected) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    },
                                ),
                        )
                    }
                }

                Spacer(Modifier.size(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        enabled = pagerState.currentPage > 0,
                    ) {
                        Text("Back")
                    }
                    Spacer(Modifier.weight(1f))
                    val isLastStep = pagerState.currentPage == tutorial.steps.lastIndex
                    Button(
                        onClick = {
                            // On the final step the primary button finishes; otherwise it advances a page.
                            if (isLastStep) {
                                onDismiss()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                    ) {
                        Text(
                            text = if (isLastStep) "Done" else "Next",
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
