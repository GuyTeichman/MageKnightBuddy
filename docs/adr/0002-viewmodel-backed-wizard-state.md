# ViewModel-backed state for the Score Calculator wizard, not rememberSaveable

The Score Calculator wizard lost all entered data whenever the player switched to another tab and back, because its fields used plain Compose `remember` state, which is scoped to composition and doesn't survive Navigation Compose disposing an inactive tab. `rememberSaveable` would have been the minimal fix for that bug alone.

We chose a heavier alternative instead: hoist all wizard state into a `ViewModel` (with `SavedStateHandle` for process-death survival). This is deliberately more than the tab-switching bug alone requires, because the same session was about to need Room persistence (saving a completed `ScoringSession` to the Scoreboard) — the ViewModel is the natural place to own that save action and talk to the repository, so it does double duty instead of becoming a second, separate refactor shortly after this one.

## Consequences

- A reader who only knows about the tab-switching bug might reasonably ask "why not just `rememberSaveable`?" — this is that answer.
- If persistence plans ever change and the wizard stops needing to save anywhere, this is more architecture than the tab-switching fix alone would justify.
