# Run with GNU Make. On Windows this needs a POSIX shell (sh.exe) reachable on
# PATH before make starts - Git for Windows provides one, and any PATH entry
# containing a Git sh.exe ("C:\Program Files\Git\bin" or "...\Git\usr\bin") is
# enough; the Makefile wires up the rest itself. See the "Windows `make` gotcha"
# section in CLAUDE.md if make fails with a "CreateProcess" or
# "missing separator" error. Run `make doctor` first - it reports on all of this.
#
# Known caveat: GNU Make 3.81's Windows build has a bug where `$(shell ...)`
# silently returns nothing under `-n`/`--dry-run` (it prints
# `process_begin: CreateProcess(NULL, "", ...) failed.` instead), so every
# auto-detected variable below looks empty in a `make -n` transcript. That is an
# artifact of `-n` alone; the same targets run correctly for real. Don't debug
# this Makefile through `make -n` on make 3.81.

.DELETE_ON_ERROR:
.DEFAULT_GOAL := help

PACKAGE := com.guyteichman.mageknightbuddy
ACTIVITY := $(PACKAGE)/.MainActivity
SCREENSHOT_DIR := screenshots

# --- Windows / MSYS shell wiring --------------------------------------------
# Two separate things go wrong on Windows when the Git install's *coreutils*
# directory ("...\Git\usr\bin", home to echo.exe, sed.exe, mkdir.exe,
# cygpath.exe, ...) isn't on PATH. Note that "...\Git\bin" - the directory most
# Git-for-Windows installs actually put on PATH - contains only sh/bash/git, so
# make can find a perfectly good shell there and still hit both of these:
#
#   1. Make optimizes a recipe line with no shell metacharacters into a direct
#      CreateProcess call instead of running it through $(SHELL). `echo` and
#      `mkdir` are shell builtins rather than .exe files, so such a line dies
#      with `process_begin: CreateProcess(NULL, echo ..., ...) failed.` unless
#      the real coreutils binaries are findable on PATH.
#   2. gradlew shells out to `cygpath` to convert its classpath from POSIX to
#      Windows form before invoking java.exe. If another MSYS-based toolchain
#      (Anaconda, MSYS2, Cygwin, Strawberry Perl, ...) sits earlier on PATH, its
#      incompatible cygpath is picked up, the -classpath string is mangled, and
#      java fails with `ClassNotFoundException: GradleWrapperMain` - an error
#      that reads as "class missing" but is really "classpath garbled".
#
# Rather than require a manual per-machine PATH edit, ask the shell make already
# resolved where its own /usr/bin lives and prepend that. `pwd -W` prints an MSYS
# path in Windows form, which is what make's own PATH search needs; it's an
# MSYS-only flag, so on Linux/macOS this expands to nothing and the export is
# skipped entirely. Backslashes because make's Windows PATH search wants them.
MSYS_USR_BIN := $(shell cd /usr/bin 2>/dev/null && pwd -W 2>/dev/null)
ifneq ($(strip $(MSYS_USR_BIN)),)
export PATH := $(subst /,\,$(MSYS_USR_BIN));$(PATH)
endif

# --- Machine-specific paths -------------------------------------------------
# These are auto-detected so the Makefile works across machines without
# editing it. Override any of them by exporting the env var yourself, or by
# passing it on the command line, e.g. `make emulator AVD_NAME=Pixel_9`.
# Uses $(origin) instead of plain `?=` so each $(shell ...) probe below runs
# exactly once instead of on every expansion of the variable.

# JDK bundled with Android Studio (AGP needs 17+; the system JDK often isn't).
# Checked in order: already-exported JAVA_HOME, then a few common Android
# Studio install spots. If none match, JAVA_HOME is left empty and `./gradlew`
# falls back to its own JDK resolution (works fine if the system JDK is 17+).
# `cd "$$p" && pwd` normalizes the hit to the shell's own path form: the
# $PROGRAMFILES/$LOCALAPPDATA candidates come from Windows env vars and are in
# Windows form ("C:\Program Files\..."), which would otherwise leave JAVA_HOME
# as a slash/backslash hybrid that gradlew's POSIX path handling chokes on.
ifeq ($(origin JAVA_HOME),undefined)
JAVA_HOME := $(shell \
	for p in \
		"$$PROGRAMFILES/Android/Android Studio/jbr" \
		"$$LOCALAPPDATA/Programs/Android Studio/jbr" \
		"$$LOCALAPPDATA"/Google/AndroidStudio*/jbr \
		/d/*/Android/android-studio/jbr \
		/c/*/Android/android-studio/jbr \
	; do [ -d "$$p" ] && { (cd "$$p" && pwd); break; }; done 2>/dev/null)
endif

# Android SDK: read from local.properties, the same file Gradle itself reads
# (`sdk.dir=...`), so there's exactly one place to configure this per machine.
# Android Studio writes that value Java-Properties-escaped, e.g.
# `sdk.dir=C\:\\Users\\me\\AppData\\Local\\Android\\Sdk`, so it has to be
# unescaped: `\\` -> `/` first (those are the path separators), then any leftover
# single `\` dropped (that's the `\:`). Doing it in one pass with a plain
# `s/[\]//g` would delete the separators too and yield `C:UsersmeAppData...`.
# The bracket form `[\]` rather than a bare `\\` is deliberate: make's Windows
# shell invocation mangles literal backslash runs in a $(shell ...) command, so
# `s|\\||g` reaches sed as an unterminated `s` command.
ifeq ($(origin ANDROID_SDK),undefined)
ANDROID_SDK := $(shell sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null | sed -e 's|[\][\]|/|g' -e 's|[\]||g')
endif

# Which AVD `make emulator` boots. Defaults to the first AVD the SDK knows
# about; override if you keep more than one.
ifeq ($(origin AVD_NAME),undefined)
AVD_NAME := $(shell [ -x "$(ANDROID_SDK)/emulator/emulator" ] && "$(ANDROID_SDK)/emulator/emulator" -list-avds 2>/dev/null | head -n1)
endif

# The cygpath shadowing described at the top of this file is already handled by
# the MSYS_USR_BIN prepend, which applies to every recipe rather than just this
# one, so all that's left here is pointing gradlew at the right JDK.
ifeq ($(strip $(JAVA_HOME)),)
GRADLE := ./gradlew
else
GRADLE := JAVA_HOME="$(JAVA_HOME)" ./gradlew
endif

# Full paths, not bare `adb`/`emulator` - platform-tools isn't guaranteed to
# be on PATH, and relying on PATH also makes recipes sensitive to which shell
# make invokes them through.
ADB := "$(ANDROID_SDK)/platform-tools/adb"
EMULATOR := "$(ANDROID_SDK)/emulator/emulator"

.PHONY: help doctor emulator devices avds build clean test lint launch reload stop uninstall logcat screenshot

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  %-12s %s\n", $$1, $$2}'

doctor: ## Check that the shell, JAVA_HOME, ANDROID_SDK and AVD were detected correctly on this machine
	@echo "make         : $(MAKE_VERSION)$(if $(filter 3.%,$(MAKE_VERSION)), - old enough that a make -n dry run wrongly reports every auto-detected value below as empty. A make 3.81 bug and not a real failure - the targets work when actually run.)"
	@echo "SHELL        : $(SHELL)"
	@echo "coreutils dir: $(if $(strip $(MSYS_USR_BIN)),$(MSYS_USR_BIN) (prepended to PATH for this run),n/a - not an MSYS shell)"
	@sh -c 'command -v sed >/dev/null && command -v cygpath >/dev/null' 2>/dev/null \
		&& echo "  -> sed/cygpath reachable" \
		|| echo "  -> BROKEN: sed and/or cygpath not on PATH. On Windows this means make did not find a Git-for-Windows sh.exe; put \"C:\Program Files\Git\usr\bin\" on PATH and re-run. Every other target will fail with 'process_begin: CreateProcess(NULL, \"\", ...)' until this is fixed."
	@echo "JAVA_HOME    : $(if $(strip $(JAVA_HOME)),$(JAVA_HOME),NOT FOUND - set it yourself, e.g. export JAVA_HOME=\"/c/Program Files/Android/Android Studio/jbr\")"
	@echo "ANDROID_SDK  : $(if $(strip $(ANDROID_SDK)),$(ANDROID_SDK),NOT FOUND - set sdk.dir in local.properties or export ANDROID_SDK)"
	@[ -d "$(ANDROID_SDK)" ] && echo "  -> exists" || echo "  -> MISSING on disk"
	@[ -x "$(ANDROID_SDK)/platform-tools/adb" ] && echo "adb          : found" || echo "adb          : NOT FOUND under $$ANDROID_SDK/platform-tools"
	@[ -x "$(ANDROID_SDK)/emulator/emulator" ] && echo "emulator     : found" || echo "emulator     : NOT FOUND under $$ANDROID_SDK/emulator"
	@echo "AVD_NAME     : $(if $(strip $(AVD_NAME)),$(AVD_NAME),NOT FOUND - create one in Android Studio's Device Manager, or run 'make avds' to list existing ones)"

## --- Device / emulator -------------------------------------------------

emulator: ## Start the emulator (software rendering avoids GPU black-window quirks on some machines)
	$(EMULATOR) -avd $(AVD_NAME) -gpu swiftshader_indirect

devices: ## List connected devices/emulators
	$(ADB) devices -l

avds: ## List AVDs known to this SDK
	$(EMULATOR) -list-avds

## --- Build / install ----------------------------------------------------

build: ## Assemble + install the debug APK on whatever device/emulator is connected
	$(GRADLE) installDebug

clean: ## Remove build outputs
	$(GRADLE) clean

test: ## Run the unit test suite (domain + data modules)
	$(GRADLE) test

lint: ## Run Android Lint
	$(GRADLE) lint

## --- Run / debug ---------------------------------------------------------

launch: ## Launch the app (assumes it's already installed)
	$(ADB) shell am start -n $(ACTIVITY)

reload: build launch ## Full reload after a code change: rebuild, install, relaunch

stop: ## Force-stop the app without uninstalling it
	$(ADB) shell am force-stop $(PACKAGE)

uninstall: ## Remove the app from the device/emulator
	$(ADB) uninstall $(PACKAGE)

logcat: ## Tail this app's logcat output only
	$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(PACKAGE))

screenshot: ## Capture a screenshot from the connected device into screenshots/
	@mkdir -p $(SCREENSHOT_DIR)
	$(ADB) exec-out screencap -p > "$(SCREENSHOT_DIR)/$$(date +%Y%m%d-%H%M%S).png"
	@echo "Saved to $(SCREENSHOT_DIR)/"
