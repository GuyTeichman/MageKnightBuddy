# The Fractured Lands

Source: `Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf`, pages 40–41.

An open-ended exploration/quest scenario with no definitive combat goal — teleportation via "folding corridors" is the signature mechanic. Not a v1 target (v1 is Solo Conquest only) — captured here as reference for when this scenario gets implemented.

## Overview

- Length: Four rounds (2 days and 2 nights).
- Purpose: explore, quest, and conquer without a definitive goal, taking advantage of the land's folded geometry to move around more easily.
- Uses the Quests rules and the Greatest Quester variant (see `solo-scoring-overview.md`).

## Setup

### Special rules (all variants)

- **Teleporting**: during your movement, you may teleport to any *safe* space with no site and the same terrain as your current space, spending 1 Move per revealed space of distance. Doesn't provoke rampaging enemies. Counts as "no site" for this purpose: the portal space, a space with a defeated rampaging enemy, and a space with a burned monastery. You may freely alternate between normal movement, teleporting, and revealing new tiles within one turn's movement.
- Revealing a new Map tile scores 1 Fame, and you may orient the tile in any direction before placing it.
- Optional Blitz variant: start with 1 Fame and 2(+1) Reputation, +1 Fame per level gained, one extra Source die and one extra Unit in the offer.

### Competitive

- Players: 2 to 4.
- Map Shape: Wedge, Wedge, or Open limited to 4 columns.
- Countryside tiles: 8, 9, or 12. Core city tiles: 2. Core non-city tiles: 2, 3, or 4.
- Cities: first revealed is level 1, second revealed is level 2.

### Cooperative

- Players: 2 to 4.
- Map Shape: Fully open.
- Countryside tiles: 9, 10, or 13. Core city tiles: 2. Core non-city tiles: 2, 3, or 4.
- Cities: first revealed is level 2, second revealed is level 4.
- Dummy Player: one standard Dummy Player. Cards/Skills: remove the four competitive Spells and the one competitive interactive Skill from each player's Skill tokens; use cooperative interactive Skill tokens if available.
- Tactics: Dummy Player takes a random Tactic first, then the players choose theirs; at the end of the first Day *and* Night, agree and remove one player-used Tactic from the game. Team Rules apply, everyone is one team.

### Solo

- Players: 1.
- Map Shape: Wedge.
- Countryside tiles: 7. Core city tiles: 1. Core non-city tiles: 2.
- City: level 2.
- Dummy Player: one standard Dummy Player. Cards/Skills: same removal/substitution as Cooperative.
- Tactics: player always chooses first; Dummy Player takes one random remaining card. At the end of each Day/Night, remove both used Tactic cards from the game.

## Scenario end (all variants)

At the end of the second day.

## Scoring

- **Competitive**: standard Achievements Scoring plus Greatest Quester scoring. City scoring is *not* applied.
- **Cooperative**: one team score = lowest Fame among all players as base, then standard Achievements Scoring plus Greatest Quester scoring, except only the highest-scoring player counts per category (no Titles). City scoring is *not* applied.
- **Solo**: base = your Fame, then standard Achievements Scoring plus Greatest Quester scoring, no Titles. City scoring is *not* applied.

Note: unlike every other scenario extracted so far, this one explicitly excludes city-conquest Fame from scoring — conquering cities is incidental here, not a scoring goal.

## Outcome (Won/Lost)

The rulebook defines no win/lose condition for this scenario — it has "no definitive goal." Only a score is computed; the rulebook itself gives no Outcome to derive.

App behavior: since there's no lose condition either, `FracturedLandsScoring.outcome()` scores every session a Won — playing it through to the end is treated as success, unlike every other scenario in this app's scope where Lost is a real possibility.
