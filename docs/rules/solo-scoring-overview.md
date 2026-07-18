# Solo Scoring — General Overview

Sources:
- `Mage-Knight-Board-Game-Ultimate-Edition-Rule-Book-September-2018.pdf`, "Cooperative and Solo Scenarios" (p.14) and "Standard Achievements Scoring" (p.15).
- `Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf`, "Ziggurat / Pyramid" (p.9) and the "Greatest Quester" variant rule (p.43).

## The Dummy Player

Solo and cooperative scenarios use one automated **Dummy Player** to keep the pace of the game realistic (a real player's cadence would otherwise be too slow/fast without an opponent applying pressure).

Setup: a random unused Hero is chosen as the Dummy Player's identity. Its Deed deck (16 Basic Action cards) is shuffled; it starts with 3 crystals matching the colors of the dots on its Hero card.

Each Dummy Player turn: flip 3 cards from its Deed deck to its discard pile. If the last card flipped is a color the Dummy has no crystal of, its turn ends. If it does have a crystal of that color, flip that many *additional* cards (color of those extra cards doesn't matter), then its turn ends. When its deck runs empty, it announces End of Round.

Some scenario scoring (e.g. Solo Conquest, see `solo-conquest.md`) counts how many cards remain unflipped in the Dummy Player's deck at game end — a fuller deck means the player finished with time to spare.

## Achievements Scoring

A Scoring Session's full set of scoring categories is its **Achievements Scoring**: the fixed **Standard Achievements Scoring** six, plus whichever optional variant categories are enabled (currently just Greatest Quester — see below). Which variants are enabled will eventually be a Settings choice; see `CONTEXT.md`.

### Standard Achievements Scoring

The fixed six scoring categories used by virtually every scenario (source: p.15) — always in play, no Settings involved. Each category has its own Fame formula:

| Category | Formula |
|---|---|
| **Greatest Knowledge** | 2 Fame per Spell in deck + 1 Fame per Advanced Action in deck |
| **Greatest Leader** | Fame = total level of all Units (Wounded Units count as half their level, rounded down) |
| **Greatest Adventurer** | 2 Fame per Shield token on an adventure site |
| **Greatest Loot** | 2 Fame per Artifact (in deck or on Units) + 1 Fame per 2 crystals in Inventory |
| **Greatest Conqueror** | 2 Fame per Shield token on a keep, mage tower, or monastery |
| **Greatest Beating** | −2 Fame per Wound card in deck (Wounds on Units don't count) |

In **multiplayer**, whoever scores highest in a category also gets a **Title** bonus (usually +3 Fame, "Greatest X"; +1/-1 if tied). Titles require comparing against other players.

#### New sites from the Apocalypse Dragon expansion

The Apocalypse Dragon expansion adds two new **adventure sites**: **Ziggurat** and **Pyramid**. Per the rulebook (p.9), a Shield token on a ziggurat/pyramid floor provides 2 Fame "towards the Greatest Adventurer title" during final scoring — the same formula as any other adventure site, just a new site type that counts towards it.

Note this is **Greatest Adventurer**, not Greatest Conqueror: ziggurats/pyramids are adventure sites you can optionally enter (like a dungeon or tomb), not fortified sites you conquer. The expansion does not add any new site types to Greatest Conqueror's keep/mage tower/monastery list.

### Greatest Quester (Apocalypse Dragon variant, optional)

Source: `Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf`, p.43 (Variant Rules).

The Apocalypse Dragon expansion introduces **Quest cards** — optional objective cards usable in any scenario — and a seventh Achievements Scoring category built on top of them, **Greatest Quester**:

| Category | Formula |
|---|---|
| **Greatest Quester** | 1 Fame per quest point scored (from completed Quest card steps) |

Like the other six categories, Greatest Quester's Title (+3 Fame to whoever scored the most quest points, +1 if tied) only applies in multiplayer. It's an optional category — it only applies when the scenario/session is using Quest cards, unlike the base six categories, which are always in play.

## Solo play: no Titles

In solo scenarios there is only one player, so there's nothing to compare against — **no Titles are awarded**. The rulebook states this explicitly for Solo Conquest ("apply standard Achievements scoring, except no titles are awarded"), and it applies to solo play generally: each category — the base six, plus Greatest Quester if Quest cards are in use — is simply summed and added to the score, with no highest-scorer bonus logic.

This means the Score Calculator's solo scoring engine never needs cross-player comparison — it's a straight sum of category formulas plus whatever scenario-specific bonuses apply (see e.g. `solo-conquest.md`).
