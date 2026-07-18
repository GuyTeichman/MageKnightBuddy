# Domain logic as a plain Kotlin module, separate from Android

MageKnightBuddy is being built as a native Android app (Kotlin + Jetpack Compose) for personal use, with a possible future iOS port. The author has Java/Python/C++/Rust experience but no prior mobile development background. We considered starting with Kotlin Multiplatform + Compose Multiplatform from day one to avoid a future rewrite, but rejected it: it adds shared-module/target setup complexity on top of learning mobile development for the first time, for a platform (iOS) that isn't being built yet.

Instead, we build native Android now, but keep all scoring/domain logic (scenarios, scoring rules, Knight, Scoring Session) in a plain Kotlin module with zero Android SDK dependencies. The `app` module (Compose UI, Android lifecycle, persistence wiring) depends on this domain module, never the reverse.

## Consequences

- No Kotlin Multiplatform tooling overhead while learning Android development.
- If iOS support is wanted later, the domain module can be moved into a Kotlin Multiplatform `commonMain` source set with minimal changes, since it never referenced Android APIs. Only a new UI layer (SwiftUI or Compose Multiplatform) and platform wiring would need to be built — not a rewrite of the scoring rules themselves.
- Discipline is required: any Android import creeping into the domain module defeats the purpose of this decision.
