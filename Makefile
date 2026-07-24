JAVA_HOME := /c/Program Files/Android/Android Studio/jbr
ANDROID_SDK := C:/Users/USER/AppData/Local/Android/Sdk
AVD_NAME := MageKnightBuddy_API36
PACKAGE := com.guyteichman.mageknightbuddy
ACTIVITY := $(PACKAGE)/.MainActivity
GRADLE := JAVA_HOME="$(JAVA_HOME)" ./gradlew
# Full paths, not bare `adb`/`emulator` - platform-tools isn't on this machine's PATH,
# and relying on PATH also makes recipes sensitive to which shell make invokes them through.
ADB := "$(ANDROID_SDK)/platform-tools/adb"
EMULATOR := "$(ANDROID_SDK)/emulator/emulator"

.PHONY: emulator build install launch reload stop test uninstall logcat

## Start the emulator (software rendering - avoids the black-window GPU quirk on this machine)
emulator:
	$(EMULATOR) -avd $(AVD_NAME) -gpu swiftshader_indirect

## Assemble + install the debug APK on whatever device/emulator is connected
build:
	$(GRADLE) installDebug

## Launch the app (assumes it's already installed)
launch:
	$(ADB) shell am start -n $(ACTIVITY)

## Full reload after a code change: rebuild, install, relaunch
reload: build launch

## Quick relaunch without rebuilding - for when only running state needs to reset
stop:
	$(ADB) shell am force-stop $(PACKAGE)

## Run the unit test suite (domain + data modules)
test:
	$(GRADLE) test

## Remove the app from the device/emulator
uninstall:
	$(ADB) uninstall $(PACKAGE)

## Tail this app's logcat output only
logcat:
	$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(PACKAGE))
