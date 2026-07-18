# Volkare's Quest

Source: `Mage-Knight-Board-Game-Ultimate-Edition-Expansion-Rule-Books-September-2018.pdf` (The Lost Legion expansion), pages 15–17.

Playable **solo or cooperative** (1 to 4 players) — Volkare himself replaces the Dummy Player, same as in Volkare's Return (see `volkares-return.md`), but here he's heading for a portal rather than a city. Not a v1 target (v1 is Solo Conquest only) — captured here as reference for when this scenario gets implemented.

## Overview

- Players: 1 to 4.
- Type: Solo or cooperative.
- Length: Six rounds (3 days and 3 nights).
- Purpose: avoid Volkare at first, then pursue and stop him before he enters the portal. Volkare is heading directly for the portal — his own quest, whatever it is — and the players must destroy his entire army before he gets there and moves once more.

## Scenario difficulty (chosen before setup)

Same two independent axes as Volkare's Return, with this scenario's own values:

- **Combat Level** (Daring / Heroic / Legendary) — how tough the city and Volkare's army are.
- **Race Level** (Fair / Tight / Thrilling) — Wounds in Volkare's deck and how many Units get scared off ("Fearful Units").

| Combat level | City level (solo) | Volkare's level (solo) |
|---|---|---|
| Daring | 3 | 8 |
| Heroic | 4 | 10 |
| Legendary | 4 | 14 |

| Race level | Wounds in Volkare's deck (solo) | Fearful Units (solo) |
|---|---|---|
| Fair | 20 | 1 |
| Tight | 16 | 2 |
| Thrilling | 12 | 3 |

(Solo column shown since that's this app's current scope — 2/3/4-player values are in the full table on p.15 if needed later.)

## Setup

- Map: predefined shape with Volkare's Camp on the opposite side from the portal tile. Tiles are face down except Volkare's Camp, the portal tile, and the three tiles adjacent to it.
- Countryside tiles: 8, 9, 11, or 12 (solo/2/3/4 players).
- Core city tiles: 2, 2, 3, or 3 (in addition to Volkare's Camp).
- Core non-city tiles: 4, 3, 4, or 3.
- City levels: per the Combat Level table above.
- Volkare replaces the Dummy Player: he counts toward the player count for Source dice and the Unit offer (one extra die/Unit beyond real players).
- Cards/Skills: remove the four competitive Spells (109–112); use cooperative interactive Skills instead of competitive ones. In solo, one other Hero is chosen at random — add one of that Hero's Skills to the Common Skill offer after each of your Level Ups.
- Volkare's deck: the 16 Basic Action cards of a Hero not in play, plus the 4 removed competitive Spells, plus Wounds per the Race Level table, shuffled together.
- Fearful Units: per the Race Level table, that many Unit-offer slots are marked at the start of the game with a crystal of a distinct basic color (colors don't matter, just which slots are marked); the first Units revealed each Round fill the marked slots first. Whenever a Wound is revealed from Volkare's deck, roll Volkare's mana die — if a marked slot of the rolled color still holds a Unit, remove that Unit from the game (scared off; unlike Volkare's Return, no token is added to Volkare's army when this happens). That slot stays empty until the next Round.

## Not extracted here

Pages 15–16 ("Course of the Game") describe Volkare's full movement toward the portal and his combat-triggering logic in detail — analogous to the Dummy Player's turn logic. That's needed to *simulate* Volkare, not to *score* the scenario, so it's out of scope for this doc. Extract it separately if/when a Volkare-driving feature is built.

## Scenario end (either)

- Volkare enters the portal and then performs another move → **players lose**.
- The entirety of Volkare's army is destroyed → **players win**. Every player may then take one last turn to finish scoring (optional — the rulebook also just says "rejoice in the victory" if you don't care about the score).

### Outcome (Won/Lost)

Won iff Volkare's entire army was destroyed before he could move again after entering the portal; Lost otherwise.

## Scoring

Same shape as Volkare's Return — defeating Volkare is the real goal, scoring is a secondary "how well did you do" measure:

1. Base: take the lowest Fame of all players (in solo, just your own Fame).
2. Apply Standard Achievements Scoring — best player in each category (most Wounds for Greatest Beating), no Titles.
3. +5 points for each conquered city. (Conquering cities isn't a goal of this scenario, unlike Volkare's Return's +20/city — see `volkares-return.md`.)
4. If Volkare was defeated, compute a separate **Volkare combat bonus** and add it to the total from steps 1–3:
   - Start from 30 / 40 / 50 points for Daring / Heroic / Legendary Combat Level.
   - +2 points for each card still left in Volkare's deck.
   - Multiply *that bonus only* by 1 / 1.5 / 2 for Fair / Tight / Thrilling Race Level.

   The multiplier applies solely to this Volkare-combat bonus (base value + per-card bonus) — it does **not** multiply the Fame/Achievements/city-conquest total from steps 1–3. Identical mechanic to Volkare's Return (see `volkares-return.md`), just with this scenario's own base-bonus/city-bonus numbers.
