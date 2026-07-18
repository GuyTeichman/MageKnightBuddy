# Architecture & Roadmap

## Tech stack

Native Android: Kotlin + Jetpack Compose. See [ADR-0001](../adr/0001-domain-logic-as-plain-kotlin-module.md) for why domain logic is isolated from Android in its own module.

## Module layout

- **`domain`** — plain Kotlin, no Android dependencies. Scenario definitions, scoring rule implementations, `Knight`, `ScoringSession`. This is the module that would move to Kotlin Multiplatform if iOS is ever built.
- **`data`** — persistence. Stores completed `ScoringSession`s locally (planned: Room) so the player can review past games and stats (e.g. "I win more with Arythea, but Wounds cost me more Fame than with Tovak"). Not implemented in this docs-only pass.
- **`app`** — Compose UI, navigation between tabs, Android wiring. Depends on `domain` and `data`.

## Tab roadmap

1. **Score Calculator** (current focus) — Solo Conquest only for v1.
2. Additional solo scenarios (later) — same engine, new `Scenario` + scoring-rule implementations per scenario.
3. **Dummy Player tab** (later) — tracks the Dummy Player's deck and draws; buttons for "run Dummy's turn" and "end round / new round".
4. **Proxy Player tab** (later, deferred) — Apocalypse Dragon's more complex solo-simulation mechanic. Not designed yet.

## Score Calculator flow (v1: Solo Conquest)

1. Select scenario (only Solo Conquest available in v1).
2. Select Knight (played this game) — stored as metadata on the `ScoringSession`; not used in the Solo Conquest formula itself, but the domain model treats it as a first-class input to scoring in general, since a future Apocalypse Dragon variant of Solo Conquest uses Knight to change the scoring rules.
3. Step through scoring categories in rulebook order, entering final tallies:
   - Fame (base score)
   - Standard Achievements Scoring categories (Knowledge, Leader, Adventurer, Loot, Conqueror, Beating) — see `docs/rules/solo-scoring-overview.md`. No titles awarded in solo.
   - Solo Conquest–specific bonuses — see `docs/rules/solo-conquest.md`.
4. Show computed total.
5. Save the `ScoringSession` to history.

This is a **post-game wizard only** — it does not track anything live during play. That's a deliberately separate, harder problem (would require mirroring full game state) and isn't in scope.

## Explicitly out of scope for now

- Any scenario other than Solo Conquest.
- Live/in-game tracking during the Score Calculator flow.
- Dummy Player tab implementation.
- Proxy Player tab (Apocalypse Dragon) — not even designed yet.
- Actual Room/persistence implementation — noted as a requirement here, not built in this pass.
