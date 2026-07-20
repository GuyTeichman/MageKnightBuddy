# For the Council

Source: `Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf`, pages 24–25.

A short scenario built entirely around Quests: the Council of the Void tests you, and your score is driven by quest points, Reputation, and Titles rather than combat/conquest.

## Overview

- Length: Three rounds (2 days and 1 night).
- Purpose: a short scenario that uses Quests. Quest scoring, Reputation, and Titles determine your score.
- Uses the Quests rules (Quest cards, Quest Points — see `CONTEXT.md`) throughout.

## Setup (all variants)

To set up the Countryside tile stack: shuffle all Countryside tiles with a village on them and randomly select one to be the top tile of the stack; then shuffle the remaining Countryside tiles together and randomly select the rest needed for the scenario. This guarantees at least one village is on the map at the start.

### Competitive

- Players: 2 to 4.
- Map Shape: Wedge, Wedge, or Open limited to 4 columns (2/3/4 players).
- Countryside tiles: 6, 7, or 9.
- Core city tiles: 1. Core non-city tiles: 2, 3, or 4.
- City: friendly — each player puts one Shield token on it, but no-one is its leader.

### Cooperative

- Players: 2 to 4.
- Map Shape: Fully open.
- Countryside tiles: 7, 9, or 11.
- Core city tiles: 1. Core non-city tiles: 2, 3, or 4.
- City: friendly, same as Competitive.
- Dummy Player: one standard Dummy Player.
- Cards/Skills: remove the four competitive Spells; remove the one competitive interactive Skill from each player's Skill tokens; use cooperative interactive Skill tokens if available.
- When taking Tactics, the Dummy Player takes a random Tactic first, then the players choose theirs. At the end of the first Day, agree and remove one of the Tactics used by the players (not the Dummy Player) from the game. Team Rules apply, except everyone is one team.

### Solo

- Players: 1.
- Map Shape: Wedge.
- Countryside tiles: 6.
- Core city tiles: 1. Core non-city tiles: 2.
- City: friendly — both the player and the Dummy Player put one Shield token on it, no-one is its leader.
- Dummy Player: one standard Dummy Player.
- Cards/Skills: same removal/substitution as Cooperative.
- When taking Tactics, the player always chooses first; the Dummy Player then takes one random card from those remaining. At the end of the first Day, remove both used Tactic cards from the game.

## Scenario end (all variants)

At the end of the second day.

## Reputation vs. Reputation modifier

These two terms below are **not** the same number - both matter for this scenario's scoring, so don't conflate them:

- **Reputation** is the raw position of your Shield token on the Reputation track (e.g. "+2 Reputation") - this is what the Outcome thresholds below check. The board itself doesn't print these position numbers; they're only ever used in rules text as a count of steps from the center "0" space.
- **Reputation modifier** is the (usually smaller) value *printed* at that track position, per the base rulebook's Reputation track illustration (`Mage-Knight-Board-Game-Ultimate-Edition-Rule-Book-September-2018.pdf`, p.2, p.7) - this is what actually gets added to (or subtracted from) your score below. E.g. Reputation +2 prints a +1 modifier; Reputation -2 prints a -1 modifier.

All 13 spaces, center-out (`ReputationTrackSpace` in `domain/` encodes this table exactly):

| Position | -6 | -5 | -4 | -3 | -2 | -1 | 0 | +1 | +2 | +3 | +4 | +5 | +6 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Modifier | X | -5 | -3 | -2 | -1 | -1 | 0 | +1 | +1 | +2 | +3 | +5 | X |

The two "X" spaces (positions -6/+6 here - the board prints no number there, this app just needs *a* consistent internal label) are their own case (see the -10 quest-point line below), not a modifier value - a Shield token can't sit on a numbered space and an X space at once. The app's Reputation step of the Score wizard shows this whole table as one tappable row - pick the space your token is actually on, and both numbers are read off automatically.

## Scoring

### Competitive

Success condition: all players have a Reputation of +1 or higher. A score is always computed regardless.

Base = quest points. In addition:
- Most quest points → +3 quest points for Greatest Quester (+1 if tied).
- Standard Titles apply, but *not* their qualifying criteria — e.g. score the Greatest Knowledge title (+3, or +1 if tied) without scoring per-Spell/per-Advanced-Action Fame.
- Each player scores (or loses) quest points equal to their Reputation modifier. A Shield token on the Reputation track's X space scores −10 quest points.
- Highest Reputation → +3 quest points for Greatest Esteem (+1 if tied).
- Highest Fame → +3 quest points for Greatest Renown (+1 if tied).

### Cooperative

Success condition: all players have a Reputation of +1 or higher — win/lose as a team.

One team score. Each player's score = their quest points, plus (or minus) quest points equal to their Reputation modifier (−10 if their Shield token is on the X space). The team score is the **lowest** of these per-player scores.

### Solo

Success condition: Reputation of +2 or higher.

Score = quest points, plus (or minus) quest points equal to your Reputation modifier (−10 if your Shield token is on the X space).

## Outcome (Won/Lost)

- Competitive/Cooperative: Won iff every player ended with Reputation +1 or higher.
- Solo: Won iff you ended with Reputation +2 or higher.

A score is always computed either way.
