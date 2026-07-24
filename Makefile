# Run with GNU Make from Git Bash, or from any shell as long as
# "C:\Program Files\Git\usr\bin" (the real sh.exe) is on PATH - see the
# "Makefile / make" section in CLAUDE.md if `make` fails with a
# "CreateProcess" or "missing separator" error on Windows.

.DELETE_ON_ERROR:
.DEFAULT_GOAL := help

PACKAGE := com.guyteichman.mageknightbuddy
ACTIVITY := $(PACKAGE)/.MainActivity
SCREENSHOT_DIR := screenshots

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
ifeq ($(origin JAVA_HOME),undefined)
JAVA_HOME := $(shell \
	for p in \
		"$$PROGRAMFILES/Android/Android Studio/jbr" \
		"$$LOCALAPPDATA/Programs/Android Studio/jbr" \
		"$$LOCALAPPDATA"/Google/AndroidStudio*/jbr \
		/d/*/Android/android-studio/jbr \
		/c/*/Android/android-studio/jbr \
	; do [ -d "$$p" ] && { echo "$$p"; break; }; done 2>/dev/null)
endif

# Android SDK: read from local.properties, the same file Gradle itself reads
# (`sdk.dir=...`), so there's exactly one place to configure this per machine.
ifeq ($(origin ANDROID_SDK),undefined)
ANDROID_SDK := $(shell sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null | sed 's/[\]//g')
endif

# Which AVD `make emulator` boots. Defaults to the first AVD the SDK knows
# about; override if you keep more than one.
ifeq ($(origin AVD_NAME),undefined)
AVD_NAME := $(shell [ -x "$(ANDROID_SDK)/emulator/emulator" ] && "$(ANDROID_SDK)/emulator/emulator" -list-avds 2>/dev/null | head -n1)
endif

# Pin PATH to the same Git install providing $(SHELL) for this one invocation, so
# gradlew's internal `cygpath` call (used to convert POSIX paths to Windows form
# before invoking java.exe) can't be shadowed by some other MSYS-based toolchain
# (Anaconda, MSYS2, Cygwin, ...) that happens to sit earlier on the ambient PATH -
# that mismatch produces a garbled -classpath and a ClassNotFoundException.
# Uses the shell's own `dirname`, not Make's $(dir) function - $(dir) splits its
# argument on whitespace, which mangles a path like "C:/Program Files/Git/...".
# $(SHELL) itself is reported in Windows-drive form ("C:/Program Files/Git/usr/bin/sh.exe"),
# and a PATH entry in that form is silently ignored by MSYS's own exec search -
# it only recognizes POSIX-absolute entries ("/c/Program Files/..."), so without
# the sed conversion below the prepend is a no-op and lookup falls through to
# whichever shadowing toolchain is next on PATH (this is what broke `make build`
# for a user with Anaconda installed - its own cygpath got picked up instead).
GIT_BIN_DIR := $(shell dirname "$(SHELL)" | sed -E 's#^([A-Za-z]):#/\L\1#')

ifeq ($(strip $(JAVA_HOME)),)
GRADLE := PATH="$(GIT_BIN_DIR):$$PATH" ./gradlew
else
GRADLE := PATH="$(GIT_BIN_DIR):$$PATH" JAVA_HOME="$(JAVA_HOME)" ./gradlew
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

doctor: ## Check that JAVA_HOME/ANDROID_SDK/AVD were detected correctly on this machine
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
