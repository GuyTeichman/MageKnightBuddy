# Room persistence tests run on plain JVM via BundledSQLiteDriver, not Robolectric or instrumented tests

`architecture.md` already named Room as the planned persistence technology for the `data` module. The open question was how to test it under this project's TDD requirement (`CLAUDE.md`: "Test-driven development for anything with real logic — scoring formulas, session state, persistence") without an emulator or device for every test run — the way Room DAO tests have traditionally required, via Robolectric or `androidTest`.

Room's KMP-ready line (stable since 2.7.0; we use 2.8.4, the current stable release) supports `androidx.sqlite:sqlite-bundled`'s `BundledSQLiteDriver`, which runs Room's underlying SQLite entirely on the JVM. We chose this over Robolectric or instrumented tests: `data` module tests run exactly like `domain`'s — fast, in `src/test`, no emulator — keeping the TDD loop uniform across both modules.

Note: Room 3.0 exists but is Alpha only (`3.0.0-alpha01`, under a new `androidx.room3` Maven group) as of this writing — not used here. 2.8.4 already has everything this ADR needs.

## Consequences

- Known gotcha, verified empirically (not just from docs): a plain (non-KMP) Android library module resolves the Android target of every Room/sqlite artifact by default — even in `src/test`, which still runs on a JVM. The Android target of `room-runtime` doesn't even expose the context-free `Room.databaseBuilder<T>(name)` overload (only `Room.databaseBuilder(context, ...)`), and the Android target of `sqlite-bundled` doesn't ship JVM native binaries. Fixed by substituting three coordinates — `androidx.room:room-runtime`, `androidx.sqlite:sqlite-bundled`, and the transitive `androidx.sqlite:sqlite` — with their `-jvm` variants, on **both** the unit test's `compileConfiguration` and `runtimeConfiguration` (compile-only substitution isn't enough; the KSP-generated `_Impl` class still needs the matching runtime classes to link against). See `data/build.gradle.kts`.
- This only works because Room's JVM- and Android-target artifacts are ABI-compatible KMP variants of the same library version — confirmed by running the actual DAO test, not just by reading documentation, since a real risk existed that KSP's Android-compiled generated code wouldn't link against JVM-target internals.
