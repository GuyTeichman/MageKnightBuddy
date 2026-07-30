# Full Cooperation / Blitz Cooperation

Source: `Mage-Knight-Board-Game-Ultimate-Edition-Rule-Book-September-2018.pdf`, Scenario List, p.20.

The base game's cooperative counterpart to Solo Conquest (`solo-conquest.md`) — same "conquer all cities" goal, played as a team with a standard Dummy Player instead of solo. Not a v1 target (v1 is Solo Conquest only) — captured here because issue #179's Tactic-selection work needs it as ground truth: this app's `Scenario.SoloConquest` entry (displayName "Conquest") identifies this scenario family regardless of player mode — see [ADR-0008](adr/0008-reuse-scenario-for-tactic-rule-lookup.md).

## Overview

- **Full Cooperation**: 2–3 players, six rounds (3 days and 3 nights).
- **Blitz Cooperation**: 2–3 players, four rounds (2 days and 2 nights) — otherwise identical rules and setup shape to Full Cooperation, just shorter (fewer Countryside tiles, lower city levels, and the standard Blitz starting bonuses: 1 Fame, 2(+1) Reputation, +1 Fame per level gained, one extra Source die and Unit in the offer).
- Purpose: "Standard cooperative scenario — the ultimate challenge for those who want to beat the game together." Team-based: one shared score, rewarded (or penalized) by the team's weakest link and best achievements.

## Setup

- Map Shape: Fully open.
- Countryside tiles: 8 or 10 (Full); 7 or 8 (Blitz).
- Core city tiles: 3 or 4 (one more than players) (Full); 2 or 3, equal to the number of players (Blitz).
- Core non-city tiles: 2 or 3, equal to the number of players (both).
- Cities: level 5, except the final one revealed is level 8 (2 players) or 11 (3 players) (Full); level 5 and 8 (2 players), or 5, 8, and 11 (3 players), in the order revealed (Blitz).
- Dummy Player: one standard Dummy Player (see `dummy-player.md`).
- Cards/Skills removed: the four competitive Spells (#109–112), and the one interactive Skill from each player's Skill deck (marked with the interactive-Skill icon).

## Tactic selection

- When taking Tactics, the Dummy Player takes a random Tactic first, then the real players choose theirs.
- At the end of **each** Day or Night — not just the first — the players agree and remove one of the Tactics *used by the players* (never the one the Dummy Player used) from the game. (This stops mattering on the scenario's last Day/Night, since there's no further Round left for the removal to affect.)
- Team Rules apply (see the base rulebook's General Principles section), with everyone treated as one team.

This is a genuinely different removal shape from every Apocalypse Dragon/Shades of Tezla-sourced cooperative scenario this app also models (`docs/context-dummy-player.md`'s **Tactic Selection** entry) — those remove either zero, one, or two player-used cards total across the whole game, never once per Round. Full/Blitz Cooperation is the only cooperative scenario found so far whose removal recurs every Round, matching Solo Conquest's own solo-mode cadence but applied to the player's card instead of both cards.

## Scenario end

When all cities are conquered, all players except the Dummy Player have one last turn.

## Scoring

Not extracted here — not a v1 scoring target (v1 is Solo Conquest only), and this doc exists solely to support issue #179's Tactic-selection work. Extract scoring separately if this scenario's Score Calculator support is ever built.
