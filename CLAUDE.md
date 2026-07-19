# MageKnightBuddy

Android companion app for the *Mage Knight* board game: a solo score calculator (v1 scope: Solo Conquest scenario only), a Dummy Player turn tracker, and eventually an Apocalypse Dragon Proxy Player simulator. Personal project for the author's own Android phone — native Kotlin + Jetpack Compose, possible iOS port later (see ADR-0001).

## Where things live

- `CONTEXT.md` — domain glossary (Scenario, Knight, Achievements Scoring, Quest Point, etc). Check it before introducing a new domain term, and update it the moment a term gets resolved or sharpened — don't let it drift out of sync with the code.
- `docs/design/architecture.md` — module layout, tab roadmap, Score Calculator flow, and what's explicitly out of scope right now.
- `docs/design/workflow.md` — the GitHub issue → branch → PR → CI → merge loop, including the standing authorization for Claude to push branches and open PRs when pointed at a specific issue.
- `docs/adr/` — architecture decision records. Only hard-to-reverse, non-obvious, real-trade-off decisions get one (see `docs/adr/0001-*.md` for the template in use).
- `docs/rules/` — scoring rules extracted from the rulebook PDFs, with page citations back to the source. Treat these as ground truth for scoring logic instead of re-reading the PDFs each time; if a rule looks wrong here, verify against the PDF and fix the doc, don't silently code around it.
- `domain/` — pure Kotlin, zero Android dependencies. Keep it that way. This is the module that would move to Kotlin Multiplatform if iOS ever happens — any Android import here defeats the point.
- `data/` — persistence (Room, once implemented), depends on `domain`.
- `app/` — Compose UI, depends on `domain` and `data`.

## Development approach

- **Test-driven development for anything with real logic** — scoring formulas, session state, persistence. Write a failing test first, then implement. Use the `tdd` skill for this. Pure Compose UI scaffolding (screens with no logic yet) doesn't need this rigor.
- Domain logic must be testable without an emulator — that's the reason it's isolated in a pure Kotlin module. If a `domain` class needs an Android SDK type to test, it belongs in the wrong module.
- Stay in scope: v1 is Solo Conquest only. Don't build out other scenarios, the Dummy Player tab, Settings/expansion toggles, or the Proxy Player simulator unless explicitly asked — they're deliberately stubbed in the docs, not forgotten.
- Before calling scoring logic done, cross-check the formula against the matching `docs/rules/*.md` file rather than trusting memory of the rulebook.
- `main` is branch-protected: work happens on a branch, merges via PR, and requires the `test` and `build` CI checks (`.github/workflows/ci.yml`) to pass. See `docs/design/workflow.md` for the full issue → PR → merge loop.

## Commenting standard

The default Claude Code behavior is "no comments unless the WHY is non-obvious." This project overrides that: the author is still building Kotlin fluency (understands rough structure, not always a function's purpose or an idiom's effect) and works on this in bursts with gaps between sessions, so comments earn their keep here more than usual.

- Every public class/object/function gets a short KDoc-style summary: what it is/does and why it exists. Keep it brief — a sentence or two, not a spec.
- Add inline comments narrating non-obvious steps inside function bodies, especially the first time a Kotlin or Compose idiom appears in a file that isn't self-evident to someone who reads code structurally but doesn't know Kotlin deeply (e.g. `copy()`, `associateWith`, sealed-interface dispatch via `when`, scope functions, `remember`/state hoisting/`LaunchedEffect`).
- Applies uniformly across `domain/`, `data/`, and `app/`.
- Forward-only for existing code: comment a file when you're already touching it for another reason, don't go out of your way otherwise. The one exception is the tracked retroactive pass below.
- Test files: descriptive backtick-named test functions already document intent — no extra KDoc needed there.

### Retroactive pass

Existing pre-standard code is being brought up to this standard via dedicated, separately-tracked passes, one module at a time (`domain/` → `data/` → `app/`), each split into several small, thematically-grouped PRs sized for one sitting of review.

## Build

- Local system JDK is too old for the Android Gradle Plugin (needs JDK 17+). Use Android Studio's bundled JBR:
  `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew <task>`
- `./gradlew build` — full build.
- `./gradlew test` — unit tests (`domain`, `data`).
- Package: `com.guyteichman.mageknightbuddy` · minSdk 26 · target/compileSdk 36.
