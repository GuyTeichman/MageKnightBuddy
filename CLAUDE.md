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

## Build

- Local system JDK is too old for the Android Gradle Plugin (needs JDK 17+). Use Android Studio's bundled JBR:
  `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew <task>`
- `./gradlew build` — full build.
- `./gradlew test` — unit tests (`domain`, `data`).
- Package: `com.guyteichman.mageknightbuddy` · minSdk 26 · target/compileSdk 36.
