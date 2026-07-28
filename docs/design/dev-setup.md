# Development environment setup

Getting the `make`-based dev loop working on a fresh machine. **Run `make doctor`
first** — it self-diagnoses most of what's below (make version, whether `sh.exe`
is Git's, and whether `JAVA_HOME` / `ANDROID_SDK` / the AVD were detected).

Nothing in this repo hardcodes machine-specific paths: `JAVA_HOME` and the SDK
path are auto-detected (JBR from common Android Studio install spots; SDK from
`local.properties`'s `sdk.dir`), so the same checkout works on any machine once
the prerequisites below are in place.

## Prerequisites

1. **Git for Windows**, with `C:\Program Files\Git\usr\bin` on your **user PATH**
   (see [PATH setup](#path-setup-windows)). That directory holds the *real*
   `sh.exe`, `sed`, and `cygpath` the Makefile depends on — not the `Git\cmd`
   shim dir most installers put on PATH by default.
2. **GNU Make ≥ 4.0.** Install with:
   ```
   winget install --id ezwinports.make -e --scope user
   ```
   (4.4.1, native Win32, no admin rights.) **Do not use GnuWin32 make 3.81** —
   its `$(shell)` is broken on Windows, so the Makefile's auto-detection probes
   fail intermittently and return empty (issue #185). If `make doctor` reports a
   version starting with `3.`, this is your problem.
3. **Android Studio** — for the bundled JBR (a JDK 17+, which AGP needs). The
   Makefile scans common install paths for it; if yours is elsewhere, export
   `JAVA_HOME` yourself, e.g. `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
4. **Android SDK** — set `sdk.dir` in `local.properties` (Android Studio writes
   it for you on first open). The Makefile reads the SDK path from there, the
   same file Gradle reads.

## PATH setup (Windows)

Your **user** PATH should:

- **Contain `C:\Program Files\Git\usr\bin`, ahead of any other MSYS-based
  toolchain** (Anaconda/miniconda, MSYS2, Cygwin, Strawberry Perl, …). This makes
  Git's `sh.exe` and `cygpath` win, which is what stops `gradlew`'s classpath from
  being mangled into a `ClassNotFoundException` (see the `Makefile`'s
  `GIT_BIN_DIR`/`GRADLE` comments, around line 55, for the full mechanism).
- **Contain the ezwinports make dir** — winget adds
  `%LOCALAPPDATA%\Microsoft\WinGet\Packages\ezwinports.make_*\bin` automatically.
- **Not contain a second `make`.** If GnuWin32 is installed, remove
  `C:\Program Files (x86)\GnuWin32\bin` from PATH (or uninstall it:
  `winget uninstall --id GnuWin32.Make`) so make 4.4.1 is the only one found.

Prepend Git's `usr\bin` to the user PATH (restart your shell afterward — PATH
edits only take effect in new shells):

```powershell
$p = [Environment]::GetEnvironmentVariable('Path','User')
[Environment]::SetEnvironmentVariable('Path', 'C:\Program Files\Git\usr\bin;' + $p, 'User')
```

## First run

```
make doctor    # make >= 4.0, sh.exe is Git's, JAVA_HOME/ANDROID_SDK/AVD detected
make test      # unit tests (domain + data)
make build     # assemble + install the debug APK on a connected device/emulator
```

`make doctor` should print `(OK)` next to `make`, a Git `sh.exe` for `SHELL`, a
non-empty `GIT_BIN_DIR`, and `found` for adb/emulator.

## Troubleshooting

| Symptom | Cause & fix |
| --- | --- |
| `doctor` shows `make : 3.x - UNSUPPORTED` | GnuWin32 make 3.81. Install ezwinports make and make sure it wins on PATH. |
| `unterminated call to function 'shell'` | Old make hitting a construct 3.81 mishandles — upgrade to ≥ 4.0. |
| `CreateProcess(NULL, "", ...) failed` | `sh.exe` not found, or make 3.81's broken `$(shell)`. Fix PATH (`Git\usr\bin`) + make version. |
| `doctor` shows `GIT_BIN_DIR : EMPTY` | The sh/PATH probe failed — `C:\Program Files\Git\usr\bin` isn't effective on PATH. |
| `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain` | A foreign `cygpath` (Anaconda etc.) shadowed Git's. Put Git's `usr\bin` ahead of it on PATH. |
| `ANDROID_SDK` looks garbled / `MISSING on disk` | `sdk.dir` missing or unreadable in `local.properties`. Re-open the project in Android Studio, or set it by hand. |

## Manual fallback (no Makefile)

If `make` itself is unavailable, invoke Gradle directly — the `PATH` prefix is
what keeps `cygpath` from being shadowed:

```
PATH="/c/Program Files/Git/usr/bin:$PATH" JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew <task>
```

Adjust `JAVA_HOME` to wherever your Android Studio JBR actually lives.
