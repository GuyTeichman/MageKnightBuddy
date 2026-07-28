# ViewModel-backed state for the Score Calculator wizard, not rememberSaveable

The Score Calculator wizard lost all entered data whenever the player switched to another tab and back, because its fields used plain Compose `remember` state, which is scoped to composition and doesn't survive Navigation Compose disposing an inactive tab. `rememberSaveable` would have been the minimal fix for that bug alone.

We chose a heavier alternative instead: hoist all wizard state into a `ViewModel` (with `SavedStateHandle` for process-death survival). This is deliberately more than the tab-switching bug alone requires, because the same session was about to need Room persistence (saving a completed `ScoringSession` to the Scoreboard) — the ViewModel is the natural place to own that save action and talk to the repository, so it does double duty instead of becoming a second, separate refactor shortly after this one.

## Consequences

- A reader who only knows about the tab-switching bug might reasonably ask "why not just `rememberSaveable`?" — this is that answer.
- If persistence plans ever change and the wizard stops needing to save anywhere, this is more architecture than the tab-switching fix alone would justify.

## Update (issue #174): SavedStateHandle alone wasn't enough

`SavedStateHandle`'s Bundle survives ordinary backgrounding and OS-triggered process death, but not every "app was minimized" case — most notably, Android discards a task's saved instance state entirely when the user swipes the app away from Recents, treating that as an intentional close rather than a backgrounding. Players were losing in-progress wizard data "on occasion," matching that gap.

The fix layers the same single-slot Room autosave pattern already used by the Dummy Player tab's sessions and the Enemy Picker (`SingleSlotAutosaveRepository`, `ScoreCalculatorDraftRepository`) on top of the existing `SavedStateHandle` wiring, rather than replacing it: every wizard field write also autosaves a `Map<String, String>` draft to Room, and a ViewModel that starts with a genuinely empty `SavedStateHandle` (checked via `savedStateHandle.keys().isEmpty()` before any field is declared) restores from that draft. A `SavedStateHandle` that already has data (an ordinary tab switch or config change) is left untouched, since it's already the more current source of truth than a Room draft that autosaves asynchronously.
