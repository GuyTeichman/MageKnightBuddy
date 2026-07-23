# Architecture & Roadmap

## Tech stack

Native Android: Kotlin + Jetpack Compose. See [ADR-0001](../adr/0001-domain-logic-as-plain-kotlin-module.md) for why domain logic is isolated from Android in its own module.

## Module layout

- **`domain`** — plain Kotlin, no Android dependencies. Scenario definitions, scoring rule implementations, `Knight`, `ScoringSession`, and each Scenario's Outcome (win/loss) rule. This is the module that would move to Kotlin Multiplatform if iOS is ever built.
- **`data`** — persistence. Room, mapping the plain-Kotlin `ScoringSession` to/from a Room `@Entity` so `domain` stays Android-free (per [ADR-0001](../adr/0001-domain-logic-as-plain-kotlin-module.md)). DAO tests run on plain JVM via `BundledSQLiteDriver` — see [ADR-0003](../adr/0003-room-tests-via-bundled-sqlite-driver.md) — no emulator required.
- **`app`** — Compose UI, navigation between tabs, Android wiring, `ViewModel`s. Depends on `domain` and `data`.

## Tab roadmap

1. **Scoreboard** (start destination) — table of every saved `ScoringSession` on this device (Knight / Score / Outcome, most recent first). A FAB ("Score new scenario") navigates to the Score tab. Tapping a row pushes a full-screen category breakdown (its own nested `NavHost` scoped to this tab) — see Scoreboard flow below.
2. **Score** — the Score Calculator wizard. Solo scoring now covers every scenario with a working scoring engine in `domain/` (see `Scenario.entries`) - Solo Conquest, First Reconnaissance, For the Council, The Hidden Valley, The Realm of the Dead, Against the Dragon, Against the Horsemen, Apocalypse is Here, The Fractured Lands, Life and Death, The Lost Relic, Against the Apocalypse, Volkare's Quest, Volkare's Return, and Solo Conquest Challenge (whose Knight-specific "Challenge" pages only appear for the 4 Knights - Arythea, Goldyx, Krang, Braevalar - whose objective needs a field no other Knight's does; `wizardPagesFor` takes both `Scenario` and `Knight` for this one scenario).
3. **Dummy Player tab** — tracks the active player-simulation's deck and draws; buttons for "run turn" and "end round / new round". Hosts a 3-way mode selector (Standard / Volkare / Proxy Player), exactly one mode active per scenario, not three separate tabs. The standard **Dummy Player** mode (see `CONTEXT.md`) is implemented: a Knight-select setup screen, then an AI screen that plays turns and ends rounds against a `DummyPlayerSession`. **Volkare** (The Lost Legion's Volkare's Return/Volkare's Quest — see `CONTEXT.md`'s **Volkare Session** entry, [ADR-0004](../adr/0004-volkare-narrates-cards-not-simulates-board.md), and `docs/rules/volkares-return.md`/`volkares-quest.md`) is implemented too, with its own Scenario/Race Level/Wound-count setup fields and AI screen. **Proxy Player** (a more elaborate, interactive Dummy Player replacement introduced in Apocalypse Dragon, but usable in any solo/coop scenario, not just the ones that expansion added — see `CONTEXT.md`'s **Proxy Player Session** entry and `docs/rules/proxy-player.md`) is now implemented as well: like Volkare, it narrates rather than simulates, picks a Knight the same way standard Dummy Player does, and reuses (via a card type shared with `DummyPlayerSession`) the standard Dummy Player's round-end Advanced Action/Spell offer interaction; its AI screen additionally shows the current Objective Card and the computed movement-point total.

## Score Calculator flow

A **paginated wizard** — one page per scoring category, in rulebook order, with Next/Previous navigation. Every scenario is rendered by the same wizard screen/`ViewModel`, not a screen per scenario; `wizardPagesFor(scenario)` picks the ordered subset of pages that matches the selected scenario's `*ScoringInput` shape. State lives in a `ViewModel` (not `remember`), so it survives switching tabs and back — see [ADR-0002](../adr/0002-viewmodel-backed-wizard-state.md). The Solo Conquest flow (the original, still the most complete example) looks like:

1. **Setup** — select Scenario (dropdown, one entry per `Scenario.entries`) and Knight (played this game). Both stored as metadata on the `ScoringSession` — Knight isn't used in the Solo Conquest formula itself, but the domain model treats it as a first-class scoring input in general, since a future Apocalypse Dragon variant of Solo Conquest uses Knight to change the scoring rules. Also an optional **Player** name field (see `CONTEXT.md`) — free text, for telling multiple people's sessions apart later.
2. **Fame** (base score).
3. One page per Standard Achievements Scoring category — Greatest Knowledge, Greatest Leader, Greatest Adventurer, Greatest Loot, Greatest Conqueror, Greatest Beating (see `docs/rules/solo-scoring-overview.md`). No titles awarded in solo. Greatest Leader's Unit input is a per-level tally (count of healthy and wounded Units at each level 1–4), not a list of individual Units. Reused by every scenario that has a Standard Achievements section (all but For the Council).
4. **Greatest Quester** page (Quest Points → Fame) — Apocalypse Dragon variant category. Scored unconditionally for now, ahead of the Settings/expansion-toggle work (see `CONTEXT.md`'s Achievements Scoring and Settings entries for the "every expansion enabled by default" reasoning). Reused as-is by The Fractured Lands, whose Solo scoring is otherwise just Fame + Standard Achievements + this page.
5. Solo Conquest–specific bonus pages, one per bonus type — Cities Conquered, Rounds Finished Early, Dummy Player Status (cards remaining + whether "End of the Round" was announced) — see `docs/rules/solo-conquest.md`. Other scenarios substitute their own scenario-specific pages here instead (e.g. Heads Defeated for Against the Dragon, Avatars of Tezla Defeated for Life and Death) - see each scenario's `docs/rules/*.md` and the matching `*ScoringInput` in `domain/`.
6. **Result** page — computed total and Outcome (Won/Lost). Outcome is *derived* from the same tallies entered above per the scenario's victory condition (e.g. Solo Conquest: all cities conquered), never a separate manual input.
7. Tapping **Done** saves the `ScoringSession` (every raw tally entered, plus the computed total and Outcome) and navigates back to the Scoreboard tab, where the new row appears at the top.

Every field defaults to a sensible value (0 / false / first item) rather than starting blank — there is no required-input validation and Next is always enabled. Every field tied to a rulebook mechanic has a "?" button opening a dialog with beginner-friendly help text, sourced from a JSON file (`app/src/main/assets/field_help.json`) bundled with the app. Each entry always stores both the explanation and its rulebook page citation; whether the citation is shown is a single decision point in the rendering code, not a user-facing setting (a real toggle would need a Settings screen, which is still out of scope — see below). Shared field-widget composables (`NumberField`, `LabeledCheckbox`, `LabeledSwitch`, `LabeledDropdown`, `NumberPillPicker` for small fixed-range counts, `ReputationTrackPicker`, `UnitLevelRow`) live in `app/.../ui/components/`, reused across every scenario's pages.

This is a **post-game wizard only** — it does not track anything live during play. That's a deliberately separate, harder problem (would require mirroring full game state) and isn't in scope.

## Scoreboard flow

1. Lists every saved `ScoringSession` on this device as a table — Knight / Score / Outcome — most recent first.
2. A FAB ("Score new scenario") navigates to the Score tab to start a new wizard run.
3. Tapping a row pushes a full-screen breakdown (via a nested `NavHost` scoped to this tab, with a back arrow): two columns, Category and Score, one row per individual scoring rule (Fame, the six Standard Achievements categories, Greatest Quester, and the five Solo Conquest bonuses — 13 rows) plus a Total row.
4. Player name is stored on every `ScoringSession` already (see `CONTEXT.md`) but not yet shown on the Scoreboard table or breakdown — deferred until the shape of "compare multiple players" is actually decided.

## Explicitly out of scope for now

- Live/in-game tracking during the Score Calculator flow.
- The Settings screen itself (expansion/variant toggles, help-citation visibility) — Greatest Quester is scored unconditionally in the meantime; see `CONTEXT.md`.
- Surfacing Player name on the Scoreboard, and any actual multi-player comparison view.
- Global Scoreboard (stub) — see `CONTEXT.md`; not designed.
