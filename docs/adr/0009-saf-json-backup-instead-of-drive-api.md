# Storage Access Framework + JSON for data backup, not the Google Drive API

Issue #121 asks for a way to sync the user's saved data to their own cloud storage (e.g. Google Drive) and restore it, since the app has no server of its own. After grilling the requirement down, the feature we're building is deliberately narrow: a manual **back up** and **restore** of the finished scoring history (the Scoreboard records), not continuous multi-device sync.

Two axes needed deciding — how the file reaches cloud storage, and what the file contains.

## How the file reaches storage

- **Google Drive REST API (native).** "One tap, straight to my Drive" with no picker. But it drags in a Google Cloud project, an OAuth consent screen, Play Services / credential libraries, and sign-in UI — a large, hard-to-reverse dependency footprint for a personal single-phone app, and one that only ever targets Drive.
- **Android Auto Backup.** Nearly free, but restore only happens on app reinstall and is opaque — it doesn't match "restore from there on demand".
- **Storage Access Framework (the system file picker).** On backup, `ACTION_CREATE_DOCUMENT` lets the user pick *where* the file goes; on restore, `ACTION_OPEN_DOCUMENT` lets them pick it back. Google Drive is just one of the document providers the picker offers (alongside local storage, Dropbox, OneDrive, …).

We chose **SAF**. The issue's "e.g. Google Drive" is illustrative, not a hard requirement, and SAF satisfies it — Drive is reachable through the picker — with **zero** Google APIs, no sign-in, and no new dependency at all. It keeps `domain/` and `data/` free of Android account/networking concerns (respecting [ADR-0001](0001-domain-logic-as-plain-kotlin-module.md)) and, as a bonus, works with any storage the user has a provider for, not only Drive.

## What the file contains

- **A raw copy of the SQLite database file.** Trivial to write. But `createDatabase` uses `fallbackToDestructiveMigration` and the schema version bumps often (see `MageKnightBuddyDatabase`); restoring an older `.db` into a newer app would be wiped by that destructive migration. In practice restore would only work within one exact app version.
- **A portable JSON export** with an explicit `formatVersion`, reusing the kotlinx-serialization DTOs already in `data/`.

We chose **JSON**. It survives schema changes (the codec can detect and refuse an incompatible file instead of silently losing data), it's human-inspectable, and it lets the export include only the durable records. Only finished `ScoringSession`s are exported — the single-slot in-progress autosaves (Dummy/Volkare/Proxy/Enemy/draft) are excluded, since those DTOs churn the most and a mid-game snapshot has little cross-device value.

## Consequences

- **No new dependency and no `domain`/`data` Android-account surface.** The encode/decode logic is a pure, JVM-tested object (`BackupCodec`) in `data/`; only the app module touches the SAF `Uri`/`ContentResolver`, in the Settings `ViewModel`. This is the same "keep the testable core Android-free" discipline as [ADR-0003](0003-room-tests-via-bundled-sqlite-driver.md).
- **The backup format is now a second thing to version, independent of the Room schema.** A breaking change to `BackupDocument`'s shape must bump `BackupCodec.FORMAT_VERSION`; `decode` refuses any file whose version exceeds the running build's, so a newer backup can never be silently mis-parsed onto an older app. This is versioned separately from the DB precisely so the two can evolve without dragging each other.
- **Restore replaces all local records** (guarded by a confirmation dialog showing the counts). Because the DB's `id` is a device-local autoincrement key — meaningless across installs — a merge would need a content-based identity; "replace" is the deterministic "revert to this snapshot" model, and is exactly right for the primary use case (a fresh install / new phone). The trade-off is that restoring an older backup over newer local records discards them; the confirmation dialog is the mitigation.
- **Decode never trusts the file:** malformed JSON, an unknown `formatVersion`, or a record naming a scenario/knight/outcome this build doesn't know all resolve to a typed failure result, and local data is only ever touched on a clean decode. A bad pick yields a friendly message, never a crash or a half-applied restore.
- **This un-defers the Settings screen** (previously listed out of scope in `docs/design/architecture.md`), but only minimally: the screen is built as a shell containing just Backup & Restore. The other deferred Settings items (expansion toggles, help-citation visibility) stay out of scope.
