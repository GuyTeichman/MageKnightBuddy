# Solo Conquest Challenge

Source: `Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf`, pages 40–42.

This is the scenario referenced in `CONTEXT.md`'s **Knight** entry: the Apocalypse Dragon expansion's knight-specific variant of Solo Conquest, where the Knight you play changes both the victory condition and several Achievements Scoring formulas. Not a v1 target — captured here as reference for when this scenario gets implemented.

## Overview

- Players: 1, Type: Solo, Length: Six rounds (3 days and 3 nights).
- Purpose: "A Solo Conquest scenario with additional victory conditions depending on which Hero you are playing."
- Uses all the same rules as the base game's Solo Conquest (see `solo-conquest.md`) **except where indicated below**.

## Additional setup

Some Heroes require specific Countryside Map tiles to be included (the rest of the Countryside tiles are chosen randomly as usual):

| Knight | Required Countryside tiles |
|---|---|
| Goldyx | The 3 base-game tiles with a Mage Tower (4, 9, 11) |
| Tovak | The 4 base-game tiles with exactly one Keep or Mage Tower (3, 4, 10, 11) |
| Wolfhawk | 1 base-game tile with a Dungeon + 1 with a Monster Den, plus the 2 Lost Legion tiles with a Maze (7 or 9, 6 or 10, 12, 14) |
| Braevalar | The 2 base-game tiles with a Monastery (5, 7) |
| Coral | 2 base-game tiles with a Dungeon + the 2 Apocalypse Dragon tiles with a Ziggurat (7, 9, 16, 17) |

(Arythea, Norowas, and Krang have no specific tile requirement.)

A specific Skill token is also set aside per Knight, for use in the "First Level Up" rule below:

| Knight | Set-aside Skill |
|---|---|
| Arythea | Power of Pain |
| Goldyx | Glittering Fortune |
| Norowas | Bonds of Loyalty |
| Tovak | Shield Mastery |
| Wolfhawk | Know Your Prey |
| Krang | Puppet Master |
| Braevalar | Shapeshift |
| Coral | Treasure Hunter |

## Additional special rules

- **First Level Up** (reaching level 2): instead of flipping two Skill tokens and choosing one, take the Skill token set aside during setup directly (the other flip goes to the Common Skills area). You still take one Advanced Action card as usual.
- **Subsequent Level Ups to even levels** (4, 6, 8, 10): you must take one of the two flipped Skill tokens — you may not take from the Common Skills area on these levels.

## Outcome (Won/Lost)

Won iff both cities were conquered **and** your Knight's additional objective (below) is met; Lost otherwise — a score is still counted either way. The objective check reads from the same raw tallies already entered for scoring (e.g. Arythea's objective is a Wound-card count, the same count that feeds her Greatest Beating override below) — no separate input needed.

| Knight | Additional objective |
|---|---|
| Arythea | At least 10 total Wound cards in your deck and on your Units |
| Goldyx | At least 4 Spells in your deck and 1 crystal of each color in your Inventory |
| Norowas | At least 10 total levels of your Units |
| Tovak | At least 4 Shield tokens that count towards Greatest Conqueror |
| Wolfhawk | At least 4 Shield tokens that count towards Greatest Adventurer |
| Krang | At least 4 Enemy tokens from the Puppet Master Skill with different Fame values |
| Braevalar | All Basic Action cards still in your deck (none thrown away), and at least one Advanced Action card of each color in your deck |
| Coral | At least 3 Artifacts in your deck and 4 crystals in your Inventory |

## Scoring

Score as normal Solo Conquest (Fame base + Standard Achievements Scoring, no Titles + the Solo Conquest scenario bonuses — see `solo-conquest.md`), **except** each Knight overrides specific Achievements Scoring formulas:

| Knight | Override |
|---|---|
| Arythea | Greatest Leader: score the **full** level of each Wounded Unit (not half). Greatest Beating: Wound cards cost **no** Fame (normally −2 each). |
| Goldyx | Greatest Knowledge: **3** Fame per Spell (not 2). Greatest Loot: **1** Fame per crystal (not per 2 crystals). |
| Norowas | Greatest Leader: **2** Fame per level of each *unwounded* Unit (not 1). |
| Tovak | Greatest Conqueror: **4** Fame per Shield token (not 2). |
| Wolfhawk | Greatest Adventurer: **4** Fame per Shield token (not 2). |
| Krang | Score Fame equal to the highest Fame value among Enemy tokens still held from the Puppet Master Skill, **plus** 2 Fame for each distinct Fame value among those tokens (including the one just scored). Replaces the standard categories entirely for Krang. |
| Braevalar | Greatest Knowledge: **2** Fame per Advanced Action (not 1). **Plus**: score Fame equal to the normal Move cost (at Night) of the space you finish the game on (Mountains = 5, Lakes = 2). |
| Coral | Greatest Loot: **4** Fame per Artifact (not 2), and **1** Fame per crystal (not per 2). |

This confirms the domain modeling decision in `CONTEXT.md`: scoring must be a function of `(session data, knight)`, not just session data — this scenario is the concrete case where Knight changes the formula, not just the record-keeping metadata.
