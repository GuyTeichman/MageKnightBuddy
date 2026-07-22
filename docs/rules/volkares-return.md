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

## Course of the Game

Source: same PDF, p.13. Extracted for the Dummy Player tab's Volkare mode — see `CONTEXT.md`'s **Volkare Session** entry and [ADR-0004](../adr/0004-volkare-narrates-cards-not-simulates-board.md) for how much of this the app actually simulates (short answer: none of the board/combat parts below — only the card-triggered procedural instructions).

### Tactic selection

- Solo: the player always chooses first; Volkare then takes a random Tactic from those remaining. Coop: same, players choose first.
- At the end of each Day/Night, remove the Tactic card that was used from the game — except the one Volkare used, which is never removed.

### Volkare's turn

- Reveal the top card of Volkare's deck; it determines his action this turn.
- **Wound card**: Volkare makes camp and his army rests — he does not move. Roll Volkare's mana die. If a Unit-offer slot marked with a crystal of the rolled color still holds a Unit, remove it from the game (it joins Volkare's army — add one gray enemy token to it) and that slot stays empty until the next Round.
- **Non-Wound card (Action or Spell), any color**: look at the Source — if there's a die of the matching color, reroll one of them (during the day, if there's no die of that color but there is a gold die, reroll the gold one instead). Then Volkare moves, per whichever movement phase is currently active (below).

### Movement — Exploring phase (from the start of the game until the city tile is revealed)

- **Green, blue, or white Action card**: move Volkare one space in that color's direction, printed on the Volkare's Return scenario card. His figure ignores terrain and other map contents; if the move leads to an unexplored area, reveal the top tile from the stack first, then move him onto it; he never moves off the map shape.
- If a Hero figure occupies the space Volkare moves to, that Hero is attacked; unless completely defeated, Volkare stays on the new space and the Hero must withdraw (see "A player attacked by Volkare" below).
- **Green, blue, or white Spell card**: behaves as if two Action cards of that color were revealed consecutively (Volkare moves twice), but only one die is rerolled. A double move can trigger combat with two different Heroes if both are in the way.
- **Red Action card**: Volkare does not move. Instead he looks for a Mage Knight to fight — if any figure is on a neighboring space, he attacks it (the one with the most Fame if there's a choice, ties broken by earlier Round Order). Unlike movement-triggered combat, Volkare does not move onto the attacked player's space, and that player never has to withdraw afterward.
- **Red Spell card**: same as a red Action card, but if no one is adjacent, Volkare looks two spaces away instead. Even if he ends up attacking a player two spaces away, he returns to his original space once combat is over.

### Movement — Race for the City phase (once the city, i.e. the last tile, is revealed)

Works the same as Exploring, except:

- Volkare always moves in whichever way gets him closer to the city — never staying the same distance or moving away. Distance is measured directly, even through unrevealed tiles (he moves through virtual spaces as if a tile were there, without actually revealing new tiles anymore).
- If Volkare has a choice of two spaces that both get him closer, the card's color picks between them (whichever direction is closer to that color's printed direction).
- **Red Action or Spell cards now move him too** — handled exactly like a blue Action/Spell card, except a red Source die is rerolled instead of blue. Volkare no longer stops to look for players to fight in this phase.

### Movement — Battle for the City phase (Volkare is standing next to the city and has to move)

A non-Wound card reveal triggers an attack on the city:

- **City not yet conquered by players**: its gates open to Volkare, he marches in, and the scenario is lost.
- **City conquered, but players aren't present/willing to defend it yet**: Volkare destroys the city walls and plunders its vicinity instead of moving. The City card flips face down; from then on, no interaction (recruiting or its special option) is allowed there, and players who helped conquer it lose the hand-limit increase for being in/near it. If this happens again (city plundered, no one defends), Volkare takes over the city and wins.
- **City defended**: combat resolves per the standard Volkare-combat rules. If Volkare loses fewer tokens than there are real players, the defense failed and he takes the city (loss). If he loses at least one token per real player, he's fended off and must retreat to the space 2 spaces from the city, opposite the direction he attacked from; his next turn resumes the same reveal-a-card rules.

### Frenzy (out of cards)

- If Volkare's deck runs out before the scenario ends, discards are **not** reshuffled back in. On every subsequent turn, he plays as if a Spell card had been revealed (moves/attacks twice), treated as blue for choosing direction — but no die is rerolled while frenzied.

### End of the Round

- Volkare never announces End of Round. When a player announces it, other players get one more turn each; Volkare does not. His deck is never reshuffled — the next Round just keeps revealing one card on each of his turns.

### A player attacked by Volkare

If a single player is attacked outside a city (via a red card, or because Volkare entered their space), they choose:

- **Retreat**: no combat (and they don't take their turn in advance) — they just take Wounds to hand: 2 during the first Day/Night, 3 during the second, 4 during the third.
- **Fight**: they fight the entirety of Volkare's army and his token, per the standard Volkare-combat rules (including the choice of whether to attend that combat fully).

Whether fighting or retreating, if Volkare ends his turn on the player's space, that player must withdraw to any safe adjacent space except the one Volkare came from (moved there for free). If Volkare ends up elsewhere, no withdrawal is required.

### Attack or cooperative attack on Volkare

A player or players may also attack Volkare using the standard attacking-Volkare rules from elsewhere in this rulebook. Volkare is never moved or slowed down as a result. The scenario's intended flow is for the first fight with Volkare to happen as a (cooperative) defense of the city, though attacking him proactively is possible.

## Not modeled by this app

Actual combat resolution (fighting Volkare's army, the Retreat/Fight choice's consequences, city-defense combat outcomes) and full board/map state (Volkare's literal position, tile adjacency, Source dice contents, Unit-offer contents) stay entirely player-managed at the physical table. The Dummy Player tab's Volkare mode only narrates what a revealed card means procedurally — see `CONTEXT.md`'s **Volkare Session** entry — it never resolves combat or tracks the map.
