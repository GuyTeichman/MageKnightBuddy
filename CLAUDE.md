# MageKnightBuddy

Android companion app for the *Mage Knight* board game: a solo score calculator (v1 scope: Solo Conquest scenario only), a Dummy Player turn tracker, and eventually an Apocalypse Dragon Proxy Player simulator. Personal project for the author's own Android phone — native Kotlin + Jetpack Compose, possible iOS port later (see ADR-0001).

## Where things live

- `CONTEXT.md` — domain glossary (Scenario, Knight, Achievements Scoring, Quest Point, etc). Check it before introducing a new domain term, and update it the moment a term gets resolved or sharpened — don't let it drift out of sync with the code.
- `docs/design/architecture.md` — module layout, tab roadmap, Score Calculator flow, and what's explicitly out of scope right now.
- `docs/design/workflow.md` — the GitHub issue → branch → PR → CI → merge loop, including the standing authorization for Claude to push branches and open PRs when pointed at a specific issue.
- `docs/adr/` — architecture decision records. Only hard-to-reverse, non-obvious, real-trade-off decisions get one (see `docs/adr/0001-*.md` for the template in use).
- `docs/rules/` — rules extracted from the rulebook PDFs (scoring formulas, Dummy/Volkare/Proxy Player mechanics, etc.), with page citations back to the source. Treat these as ground truth instead of re-reading the PDFs each time; if a rule looks wrong *or ambiguous* here, verify against the PDF and fix the doc — don't silently code around it, and don't resolve the ambiguity only in your head while leaving the doc as-is. When extracting a rule, check whether its wording leans on a convention defined elsewhere in the rulebook (e.g. "shuffle the deck" assuming the reader already knows a general step from an earlier page) and spell that convention out in the doc rather than leaving it implicit — an implicit cross-reference is exactly how `docs/rules/dummy-player.md`'s "the deck is then reshuffled" got misread as "just the undrawn cards" instead of "deck + discard pile combined," which was never caught because the doc, the implementation, and the tests all shared that same narrow reading.
- `domain/` — pure Kotlin, zero Android dependencies. Keep it that way. This is the module that would move to Kotlin Multiplatform if iOS ever happens — any Android import here defeats the point.
- `data/` — persistence (Room, once implemented), depends on `domain`.
- `app/` — Compose UI, depends on `domain` and `data`.

## Development approach

- **Test-driven development for anything with real logic** — scoring formulas, session state, persistence. Write a failing test first, then implement. Use the `tdd` skill for this. Pure Compose UI scaffolding (screens with no logic yet) doesn't need this rigor.
- **TDD only catches a mismatch between the test and the implementation — it cannot catch a misunderstanding they both share**, which happens whenever the test and the code are written from the same narrow reading of a spec/issue. Two concrete habits that guard against this:
  - For a state-mutating method, build its test's starting state by calling the class's *own prior methods* (e.g. `playTurn()` enough times to get a realistic discard pile) rather than hand-constructing a convenient state via a factory/`start(...)` override that happens to leave some field empty or default. A shortcut precondition silently avoids exercising whatever that method does with that field.
  - Assert on every field the method's own doc comment says it changes, not just the one the test's name is about. If a doc comment doesn't already say what the method does to *each* field it touches, write that down first — an unstated field is the one most likely to be wrong.
  - This is exactly how `DummyPlayerSession.endRound()` shuffled the deck without ever merging the discard pile back into it: every test for `endRound` built its session via `start(deckOrder = ...)` directly, so the discard pile was empty in all of them, and the method's own doc comment (at the time) never stated what should happen to `discardPile`.
  - For a round-trip test (serialize then deserialize, or any encode/decode pair), don't assert only `assertEquals(original, roundTripped)`. That's self-referential — `original` is whatever the domain method actually produced, bug or not, so the assertion can only ever confirm "the round-trip preserved whatever the code produced," never "the code produced the right thing." Add explicit, independently-derived expected values (reasoned out by hand from the rules doc, not read off the code's own output) alongside the round-trip check.
  - This is exactly why `DummyPlayerSessionMapperTest`'s round-trip tests never caught `endRound()`'s discard-pile bug (issue #148) for its entire life: `assertEquals(session, roundTripped)` was true for the buggy output just as it would've been for correct output — the test could never have told the difference (issue #150).
- Domain logic must be testable without an emulator — that's the reason it's isolated in a pure Kotlin module. If a `domain` class needs an Android SDK type to test, it belongs in the wrong module.
- Stay in scope: v1 is Solo Conquest only. Don't build out other scenarios, the Dummy Player tab, Settings/expansion toggles, or the Proxy Player simulator unless explicitly asked — they're deliberately stubbed in the docs, not forgotten.
- Before calling any rule-derived logic done — scoring formulas, Dummy/Volkare/Proxy Player mechanics, anything else with a `docs/rules/*.md` counterpart — cross-check it against that doc rather than trusting memory of the rulebook or of an issue's paraphrase of it.
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

The repo ships a `Makefile` wrapping the day-to-day dev loop. Run `make help` to list targets; run `make doctor` first on any new machine — it reports the `make` version and resolved shell, and whether `JAVA_HOME`, `ANDROID_SDK`, and `AVD_NAME` were auto-detected correctly. Every other target depends on those being right.

- `make build` — assemble + install the debug APK on whatever device/emulator is connected.
- `make test` — unit tests (`domain`, `data`).
- `make lint` — Android Lint.
- `make clean` — `./gradlew clean`.
- `make emulator` — boot the AVD (software rendering — avoids a GPU black-window quirk seen on some machines).
- `make launch` / `make reload` / `make stop` / `make uninstall` — run and iterate on the installed app (`reload` = build + launch in one step).
- `make screenshot` — grab a screenshot from the connected device/emulator into `screenshots/` (gitignored, timestamped filename).
- `make logcat` — tail this app's logcat only.
- `make devices` / `make avds` — list connected devices / AVDs known to the SDK.

`JAVA_HOME` and `ANDROID_SDK` are auto-detected per machine (JBR by scanning common Android Studio install paths; SDK from `local.properties`'s `sdk.dir` — the same value Gradle itself reads), so the Makefile needs no editing between machines. Override either by exporting the env var or passing it inline, e.g. `make build JAVA_HOME=...` or `make emulator AVD_NAME=Other_Avd`.

### Windows `make` gotcha

Run `make doctor` first — it now reports the `make` version, the `SHELL` Make resolved, and whether `sed`/`cygpath` are reachable, which covers every failure mode below.

GNU Make's Windows port only routes recipes through a real POSIX shell if it can find `sh.exe` on `PATH` *before* Make starts — setting `SHELL` inside the Makefile itself is too late, since Make locks in whether recipes get shell-wrapped at all during its own startup probe. Without that, it tries to exec each recipe line's first word directly (e.g. `echo`) and fails with a `CreateProcess` error. So *some* directory holding a Git-for-Windows `sh.exe` must be on `PATH` — either `C:\Program Files\Git\bin` or `C:\Program Files\Git\usr\bin` (the `Git\cmd` shim dir that most installs put on `PATH` is **not** one of them). From an interactive Git Bash session this is automatic.

Finding a shell isn't the end of it, because `Git\bin` holds only `sh`/`bash`/`git` — the actual coreutils (`echo.exe`, `sed.exe`, `mkdir.exe`, `cygpath.exe`, ...) live one directory over in `Git\usr\bin`. Two things break when that directory isn't on `PATH`:

- Make optimizes any recipe line with no shell metacharacters into a direct `CreateProcess` call rather than running it through `$(SHELL)`. `echo`/`mkdir` are shell builtins, not `.exe` files, so those lines fail with `process_begin: CreateProcess(NULL, echo ..., ...) failed.` — even though Make found a perfectly good shell.
- `./gradlew` is a POSIX script that shells out to `cygpath` to convert its classpath from POSIX to Windows form before invoking `java.exe`. If some *other* MSYS-based toolchain (Anaconda, MSYS2, Cygwin, Strawberry Perl, ...) sits earlier on `PATH` and ships its own incompatible `cygpath`, that gets picked up, silently mangles the path, and `java` fails with `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain` — a "can't find the class" error that's actually "the classpath string was garbage."

The Makefile handles both itself rather than depending on a per-machine `PATH` edit: it asks the shell Make already resolved where its own `/usr/bin` is (`cd /usr/bin && pwd -W`, giving e.g. `C:/Program Files/Git/usr/bin`) and prepends that to the exported `PATH` for every recipe. `pwd -W` is MSYS-only, so on Linux/macOS the probe comes back empty and the prepend is skipped. Windows-drive form (with backslashes) is deliberate — that's what Make's own Win32 `PATH` search wants; note that MSYS's *internal* exec search is the opposite, recognizing only POSIX-absolute entries (`/c/Program Files/...`), so a `PATH` entry hand-written as `C:/Program Files/...` for `sh` to consume would be silently ignored.

- `make` itself: `winget install --id ezwinports.make -e --scope user` (GNU Make 4.4.1, native Win32 build, no admin rights needed) is the recommended install. **On this machine the `make` actually on `PATH` is GnuWin32 GNU Make 3.81** (`C:\Program Files (x86)\GnuWin32\bin\make.exe`) — it works fine for real runs, but it has a bug where `$(shell ...)` returns nothing under `-n`/`--dry-run`, printing `process_begin: CreateProcess(NULL, "", ...) failed.` instead. That makes every auto-detected variable look empty in a `make -n` transcript. **Never diagnose this Makefile with `make -n`** — the emptiness is an artifact of `-n`, not a real failure. `make doctor` flags this when it detects a 3.x make.
- Manual fallback without the Makefile: `PATH="/c/Program Files/Git/usr/bin:$PATH" JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew <task>` — the `PATH` prefix matters, see above. (`JAVA_HOME` is per-machine; `make doctor` prints the auto-detected value.)
- `local.properties`' `sdk.dir` is written Java-Properties-escaped by Android Studio (`sdk.dir=C\:\\Users\\me\\...`), so the Makefile has to unescape `\\` → `/` before dropping the leftover `\:`. Doing it in one pass deletes the separators too and yields `C:UsersmeAppData...`. It also has to spell the backslash as the bracket class `[\]` rather than `\\`, because Make's Windows shell invocation mangles literal backslash runs inside a `$(shell ...)` command.
- A `#` can never be used as a `sed` delimiter inside a Makefile — `#` starts a Make comment, truncating the line mid-expression.

- Package: `com.guyteichman.mageknightbuddy` · minSdk 26 · target/compileSdk 36.
