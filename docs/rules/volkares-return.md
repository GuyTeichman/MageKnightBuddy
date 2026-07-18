# Volkare's Return

Source: `Mage-Knight-Board-Game-Ultimate-Edition-Expansion-Rule-Books-September-2018.pdf` (The Lost Legion expansion), pages 12–14.

Two variants: **Epic** (p.12) and **Blitz** (p.14, shorter). Both are playable **solo or cooperative** (1 to 4 players) — Volkare himself replaces the Dummy Player in this scenario. Not a v1 target (v1 is Solo Conquest only) — captured here as reference for when this scenario gets implemented.

## Scenario difficulty (chosen before setup)

Difficulty here is two independent axes, chosen before setup, that scale everything else:

- **Combat Level** (Daring / Heroic / Legendary) — how tough Volkare and the city are.
- **Race Level** (Fair / Tight / Thrilling) — how much pressure/time-crunch Volkare exerts.

### Epic

| Combat level | City level (solo) | Volkare's level (solo) |
|---|---|---|
| Daring | 4 | 5 |
| Heroic | 6 | 8 |
| Legendary | 10 | 12 |

| Race level | Wounds in Volkare's deck (solo) | Indecisive Units (solo) |
|---|---|---|
| Fair | 18 | 1 |
| Tight | 15 | 2 |
| Thrilling | 12 | 3 |

(Values also given per 2/3/4-player counts in the source — solo column shown here since that's this app's current scope. Multiplayer values are in the full table on p.12 if needed later.)

### Blitz — same as Epic except

- Time limit: two days and two nights (4 rounds) instead of six.
- Fewer Countryside tiles; same map shape.
- All Blitz Conquest rules apply: one extra die/Unit, start with 2 Reputation and 1 Fame, +1 Fame per level gained.
- Its own (lower) difficulty table:

| Combat level | City level (solo) | Volkare's level (solo) |
|---|---|---|
| Daring | 3 | 4 |
| Heroic | 4 | 6 |
| Legendary | 5 | 8 |

| Race level | Wounds in Volkare's deck (solo) | Indecisive Units (solo) |
|---|---|---|
| Fair | 16 | 1 |
| Tight | 13 | 2 |
| Thrilling | 10 | 3 |

## Setup (Epic; Blitz is the same shape with fewer tiles)

- Map: Open, limited to 4 columns; Volkare's Camp is the leftmost starting tile.
- Volkare replaces the Dummy Player: he counts toward the player count for Source dice and the Unit offer (one extra die/Unit beyond real players).
- Cards/Skills: remove the four competitive Spells (109–112); use cooperative interactive Skills instead of competitive ones. In solo, one other Hero is chosen at random — add one of that Hero's Skills to the Common Skill offer after each of your level-ups.
- Volkare's deck: the 16 Basic Action cards of a Hero not in play, plus the 4 removed competitive Spells, plus Wounds per the Race Level table, shuffled together.
- Some Units in the Unit offer are "indecisive" (count per the Race Level table) and may join Volkare instead of the player.

## Scenario end (any of)

- Volkare enters the city before it's conquered → **players lose**.
- Volkare attacks an already-conquered city twice without defense → **players lose**.
- Round 6 (Epic) / Round 4 (Blitz) ends with Volkare's army not fully destroyed → **players lose**.
- Volkare's entire army is destroyed → **players win**.

### Outcome (Won/Lost)

Won iff Volkare was defeated — the same yes/no already needed for the Volkare combat bonus in scoring (step 4 below); Lost on any of the other three end conditions. No separate input needed.

## Scoring

The rulebook is explicit that the real goal is just to defeat Volkare — scoring is a secondary "how well did you do" measure, using the same shape as other cooperative scenarios:

1. Base: take Fame (in solo, just your own Fame).
2. Apply Standard Achievements Scoring — best player in each category, no Titles (in solo, just your own totals, same as Solo Conquest).
3. +20 points if the city was conquered.
4. If Volkare was defeated, compute a separate **Volkare combat bonus** and add it to the total from steps 1–3:
   - Start from 30 / 40 / 50 points for Daring / Heroic / Legendary Combat Level.
   - +2 points for each card still left in Volkare's deck.
   - Multiply *that bonus only* by 1 / 1.5 / 2 for Fair / Tight / Thrilling Race Level.

   The multiplier applies solely to this Volkare-combat bonus (base value + per-card bonus) — it does **not** multiply the Fame/Achievements/city-conquest total from steps 1–3.

## Not extracted here

Page 13 ("Course of the Game") describes Volkare's full movement and combat AI in detail — how he explores, paces toward the city, attacks players, retreats, etc. That's needed to *simulate* Volkare (analogous to the Dummy Player turn logic), not to *score* the scenario, so it's out of scope for this doc. Extract it separately if/when a Volkare-driving feature is built.
