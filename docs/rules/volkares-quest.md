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

## Course of the Game

Source: same PDF, p.16. Extracted for the Dummy Player tab's Volkare mode — see `CONTEXT.md`'s **Volkare Session** entry and [ADR-0004](../adr/0004-volkare-narrates-cards-not-simulates-board.md) for how much of this the app actually simulates (short answer: none of the board/combat parts below — only the card-triggered procedural instructions).

### Tactic selection

Same as Volkare's Return — see `volkares-return.md`'s "Tactic selection".

### Volkare's turn

- Reveal the top card of Volkare's deck; it determines his action this turn.
- **Wound card**: Volkare makes camp — he does not move. Roll Volkare's mana die. If a Unit-offer slot marked with the rolled color still holds a Unit, remove it from the game (it's scared off; **unlike Volkare's Return, no army token is added**). That slot stays empty until the next Round.
- **Non-Wound card (Action or Spell), any color**: reroll a matching-color Source die (or the gold die by day, if none of that color exists) — same rule as Volkare's Return.
- **Green, blue, or white Action card**: move Volkare one space in that color's direction, printed on the Volkare's Quest scenario card, always choosing whichever of up to two equally-valid directions gets him closer to the portal. Distance is measured directly, even through unrevealed tiles — in this scenario Volkare walks over unexplored tiles without exploring them (unlike Volkare's Return). If his move would lead off the map shape, he instead moves one space closer to the portal.
  - **Exception**: once Volkare is adjacent to the portal space, he always moves onto it on any non-Wound reveal, regardless of card color. If he's already on the portal space, revealing a green/blue/white card means he enters it and wins (see Scenario end).
- **Green, blue, or white Spell card**: same as two consecutive Action cards of that color (double move, only one die rerolled) — the two moves can end up in different directions if the first one reveals the portal or would lead off the map.
- **Red Action or Spell card**: Volkare does not move. He attacks the highest-Fame adjacent Mage Knight instead (ties broken by earlier Round Order) — same targeting rule as Volkare's Return. **Unlike Volkare's Return, there is no later phase where red cards start moving him** — this rule holds for the entire scenario.

### Frenzy (deck exhausted)

Not defined by the rulebook — Volkare's Quest is designed so his deck is always long enough to reach the portal under correct play ("Volkare should always have enough cards to win in this scenario. If he fails to reach the portal and perform his final move, you probably made a mistake"). This app treats an exhausted deck as Volkare having already reached the portal and made his final move — an automatic loss (see Scenario end) — rather than inventing an undefined Frenzy-style continuation.

### End of the Round

Same as Volkare's Return — see `volkares-return.md`'s "End of the Round": Volkare never announces it, gets no extra turn when a player does, and his deck is never reshuffled.

### A player attacked by Volkare

Same Retreat/Fight choice and Wound counts as Volkare's Return — see `volkares-return.md`'s "A player attacked by Volkare".

### Attack or cooperative attack on Volkare

Same as Volkare's Return, with one addition: after such an assault, players return to the space they attacked from (Volkare is not moved). Cooperative assault is explicitly recommended here, but requires good synchronization since only players adjacent to Volkare's figure at the moment of the assault may join.

### Combat outcome

- If all of Volkare's troops are eliminated (regardless of who initiated combat), the players win.
- If Volkare loses at least twice as many tokens from his army as there are players in the game, he's slowed down: his Round Order token turns face down, and next time it's his turn to play, he just flips it back up and his turn is over (no card reveal). Excess kills beyond that threshold don't carry over to future turns.

## Not modeled by this app

Same boundary as Volkare's Return — see that doc's equivalent section. This app narrates card-triggered implications only; it never simulates the map, Volkare's literal position, or combat resolution.

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
