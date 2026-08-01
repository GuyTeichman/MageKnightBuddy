# MageKnightBuddy

An Android companion app for the *Mage Knight* board game, written in native Kotlin with Jetpack Compose. It provides a solo score calculator, a Dummy Player turn tracker, and eventually an Apocalypse Dragon Proxy Player simulator. This is a personal project; v1 scope is the **Solo Conquest** scenario only.

## Build

The repo ships a `Makefile` wrapping the day-to-day dev loop — run `make help` to list targets, and `make doctor` first on a new machine to check that `JAVA_HOME`, `ANDROID_SDK`, and `AVD_NAME` were auto-detected correctly. See `CLAUDE.md` for the full build notes and `docs/design/architecture.md` for the module layout.

## License

Copyright (C) 2026 Guy Teichman

This project is licensed under the GNU Affero General Public License v3.0. See [LICENSE](./LICENSE) for the full text.

The Mage Knight rulebook and walkthrough PDFs included in this repository are the property of their respective publisher, are included for reference only, and are **not** covered by the project's license.
