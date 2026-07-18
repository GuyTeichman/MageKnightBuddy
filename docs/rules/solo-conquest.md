# Solo Conquest

Source: `Mage-Knight-Board-Game-Ultimate-Edition-Rule-Book-September-2018.pdf`, Scenario List, p.19.

This is the **v1 target scenario** for the Score Calculator.

## Overview

- Players: 1 (Solo)
- Length: Six rounds (3 days and 3 nights)
- Purpose: the standard solitaire scenario — conquer all cities. Also recommended as a way to learn the game before teaching others.

## Setup

- Map shape: Wedge
- Countryside tiles: 7
- Core city tiles: 2
- Core non-city tiles: 2
- Cities: the first city revealed is level 5, the second revealed is level 8
- Dummy Player: one standard Dummy Player (see `solo-scoring-overview.md`)
- Cards/Skills removed: the four competitive Spells (#109–112), and the one interactive Skill from the player's Skill deck

## Special rules

- When taking Tactics, the player always chooses first; the Dummy Player then takes one random card from those remaining.
- At the end of each Day or Night, both used Tactic cards are removed from the game — each Tactic card is picked exactly once during the whole scenario.

## Scenario end

When all cities are conquered, the player has one last turn (the Dummy Player does not get one). If not all cities are conquered by the Round limit, the scenario simply ends at Round 6.

## Scoring

1. **Base**: your Fame total.
2. **Standard Achievements Scoring** (see `solo-scoring-overview.md`) — summed directly, **no Titles awarded** (solo play, nothing to compare against).
3. **Scenario-specific bonuses**:
   - +10 points for each city conquered.
   - +15 points bonus if you conquered *all* cities.
   - +30 points for each Round finished before the 6-Round limit (i.e. you beat the clock).
   - +1 point for each card remaining in the Dummy Player's Deed deck that was not yet flipped this Round (a fuller deck = more time to spare).
   - +5 points if "End of the Round" had not yet been announced in your final Round.

## Outcome (Won/Lost)

Won iff both cities were conquered (i.e. the "conquered all cities" bonus above applies); Lost otherwise. This is derived from the same city-conquered tally used for scoring — no separate input needed. A score is always computed either way.

## Note: other scenarios playable solo

The rulebook notes any scenario can be played solo using setup modifications and special rules similar to Solo Conquest's — Solo Conquest is just the purpose-built, standard example. This is why the Score Calculator's domain model should treat "scenario" and "its scoring rules" as pluggable per-scenario, not hardcoded to Solo Conquest, even though only Solo Conquest is implemented in v1.
