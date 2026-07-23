# Proxy Player

Source: `Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf`, "Proxy Player", pp.16–19.

A more elaborate, interactive drop-in replacement for the standard Dummy Player (see `dummy-player.md`), introduced in the Apocalypse Dragon expansion but usable as the Dummy Player substitute in any Solo/Cooperative scenario, not just ones the expansion added. Their Hero moves around the map, conquers fortified/adventure sites, interacts (recruits Units/learns Advanced Actions/learns Spells), and explores — everything a real player's Hero does, driven by a fixed procedure instead of a human choosing.

## App scope: narrate, not simulate

This app tracks the Proxy Player's *deck, discard pile, crystals, current objective card, and its shield-token count* — everything the movement-point formula needs — and computes that formula for the player each turn. It does **not** track the Hero's map position, tile layout, or other players' positions, and therefore never decides *which* site the Proxy Player moves to, conquers, or interacts with, nor whether movement provokes a rampaging enemy or a fortified site along the way. Those are the physical Proxy Player reference card's job, resolved by the player at the table — the same hands-off relationship Volkare mode has with the board (see [ADR-0004](../adr/0004-volkare-narrates-cards-not-simulates-board.md)). The full movement/conquering/interacting procedure is still extracted below in detail, since it's the source material for an in-app "movement rules" help button, even though the app itself never executes it.

Each turn, the player reports back one of exactly three outcomes — **Nothing** (still traveling; the objective card and its shields persist to next turn), **Explored**, or **Completed** — see "Resolution" below.

## At the start of the game

Set up the Proxy Player as you would a normal Dummy Player (`dummy-player.md`'s Setup: random unused Hero, that Hero's 16-card Basic Action deck shuffled, starting crystals per the Hero's card dots) **plus** their Hero figure — placed on the map like a real player's. The Proxy Player card (a physical reference card, not tracked by the app) summarizes their objectives, movement, and actions.

## When picking Tactics

Same as a normal Dummy Player.

## Mana dice and Units

Unlike a normal Dummy Player, **a Proxy Player counts as a real player** for the number of mana dice in the Source and the number of Units in the Unit offer — setup fact only, nothing for the app to track.

## The Proxy Player's turn

If their Hero figure is not on the map, place it on the portal space.

If their Deed deck is empty: they announce End of the Round and their turn is over — other players get one more turn each, then the Round ends. (Same empty-deck rule as the standard Dummy Player.)

If their Deed deck is not empty:

- **If they have an objective card**: place a Shield token on it. Then flip 3 cards from their deck onto their discard pile (as many as available if fewer than 3 remain).
- **If they don't have an objective card** (first turn of a Round, or after completing/discarding the previous one): flip 1 card and place it next to their Hero card as their new objective card, then flip 2 more cards onto their discard pile (as many as available if fewer than 2 remain). The last flipped card may be the objective card itself, if it was the only card left in the deck.

In either case, check the color of the **last** card flipped (the new top of the discard pile — or the objective card itself, per the note above):

- If the Proxy Player has **no** crystals of that color, their turn's flip is done.
- If they have crystals of that color, flip one additional card per matching crystal (color of these extra cards doesn't matter and doesn't chain further).
- **Dual-color Advanced Action cards** (see below) count as matching if the Proxy Player has crystals of *either* of the card's two colors — this app-specific ruling isn't stated in the rulebook (dual-color cards are from a separate product); flip one extra card per matching crystal of whichever of the two colors the Proxy Player holds *more* of (the higher count, not both added together — it's still one physical card being flipped, not two).

If their Hero figure ends the turn on the portal space, remove it from the map.

## Objective (movement target, by objective card color)

The Proxy Player moves toward a site determined by their objective card's color:

- **Green** → the closest unconquered adventure site. A partially-conquered site (e.g. a ziggurat) without the Proxy Player's own Shield token on it still counts as unconquered.
- **Red** → the closest unconquered fortified site or non-destroyed monastery.
- **White** → the closest site where a Unit can currently be recruited, or an Advanced Action (in the Unit offer) or a Spell (matching a crystal they hold) can currently be learned. An unconquered fortified site or another player's keep doesn't count.
- **Blue** → the closest site satisfying any of the green/red/white conditions above that is *further from the portal* than the Proxy Player's Hero currently is.
- If already on the target site, or they reach it this turn: stop moving and resolve it (conquer / burn / interact — see "Resolution").
- If no valid site or path exists for any of the above: move toward the closest space they can explore from instead; if already there or they reach it, stop and explore. If exploring is no longer possible either, fall back to the closest green/red/white-condition site regardless of portal distance.
- **Dual-color Advanced Action cards** count as both colors for this targeting rule — choose the closest site valid for either objective color.

## Movement points

Sum of:

- **+2** if the objective card is an Advanced Action or one of the Hero's own unique Basic Action cards (the 2 portrait cards per Hero — see "Unique Basic Action cards" below); **+1** otherwise (a generic Basic Action).
- **+1** per Shield token currently on the objective card.
- **+1** if there's a mana die of the objective card's color in the Source, or a gold die and it's day (a basic-color die is preferred over gold if both are available). Immediately reroll that die afterward.

Movement rules (for the physical resolution, not app-tracked):

- Must move by the most direct path toward the objective; terrain move cost is irrelevant.
- May not move into a space with another Hero, a lake, or a mountain — factor this into what counts as the "closest" site/path.
- If movement would provoke or pass through a rampaging enemy, stop and fight it instead of continuing (unless a valid path avoids the enemy while still being equally direct — simply ending movement adjacent to an enemy is not itself a provocation).
- If movement would pass through an unconquered fortified site or another player's keep, stop and conquer it instead of continuing (unless an equally-direct path avoids it).
- Ties in "closest" are the resolving player's choice, except: never choose a path that forces a later fight/conquest if an alternative avoids it entirely; if forced regardless of path, move as far as possible before the forced fight/conquest.
- Whenever a choice must be made during the Proxy Player's turn (destination ties, resolution details), the player with the lowest Fame decides (ties broken by later Round order).

## Resolution

Whatever the objective card's color, the turn ends in exactly one of three outcomes:

### Nothing (still traveling)

The objective card and its accumulated Shield tokens persist unchanged into the Proxy Player's next turn.

### Explored

Reveal one new map tile (player's choice of placement if there's a choice, decided before seeing the tile). Discard the objective card and its Shield tokens. No other tracked-state effect.

### Completed

However the objective was actually resolved at the table — conquering an adventure site (ruins: discard its lowest-Fame enemy token, or all tokens for other adventure sites; mark conquered with a Shield token), conquering a fortified site (discard the lowest-Fame enemy token in a city, or all enemy tokens for a non-city; mark conquered), burning a monastery (mark it destroyed), fighting a rampaging enemy that blocked the path (discard the enemy token), or interacting (recruit the lowest-Influence-cost Unit/Advanced Action/Spell available, preferring a Unit over either — see "Interacting" below) — the app treats all of these identically: discard the objective card and its Shield tokens. **Interacting never changes the Proxy Player's own deck or crystals** — see below.

### Interacting, in detail (reference only — not app-tracked)

- Acquires the lowest-Influence-cost Unit, Advanced Action (6 Influence), or Spell (7 Influence) available at the site.
- Unit beats Advanced Action or Spell on a tie in eligibility.
- May not learn a Spell without a matching crystal in Inventory for one of the offered Spells.
- Learning a Unit: discard the lowest-Influence-cost Unit of a matching type from the Unit offer (tie: choose).
- Learning an Advanced Action: **choose and discard** an Advanced Action from the Unit offer — it does not join the Proxy Player's own deck.
- Learning a Spell: **choose and discard** a Spell from the Spell offer matching one of their crystals — the crystal itself is *not* discarded, but no new crystal is gained either.

These three are offer-bookkeeping only. The only way Advanced Actions or crystals ever enter the Proxy Player's own tracked deck/Inventory is the round-end step below — same mechanism as the standard Dummy Player.

## When preparing a new Round

If the Proxy Player has an objective card, discard it and its Shield tokens first. Then perform the standard Dummy Player's round-end step (`dummy-player.md`'s "End of Round: round-prep offer interactions") unchanged: the lowest Advanced Action offer card joins their deck (then reshuffle), and the lowest Spell offer card grants them one crystal of its color. **This is the only path by which a dual-color Advanced Action card (see below) can enter either a standard Dummy Player's or a Proxy Player's deck.**

## Unique Basic Action cards

Each Hero's starting 16-card Basic Action deck (the same one used to set up a standard Dummy Player) has exactly 2 of its generic cards replaced by that Hero's own unique versions (portrait cards) — the deck stays 16 cards total. A unique card counts as an Advanced Action for the movement-point bonus (+2, not +1) despite being a Basic Action.

| Knight | Unique card colors |
|---|---|
| Tovak | Blue, Red |
| Goldyx | Blue, Green |
| Norowas | White, Green |
| Wolfhawk | Blue, White |
| Arythea | White, Red |
| Krang | Red, Green |
| Braevalar | Blue, Green |
| Coral | White, Red |

## Dual-color Advanced Action cards

From a separate dual-color-cards product (not the base Apocalypse Dragon expansion), 4 Advanced Action cards each count as *two* colors instead of one:

| Card | Colors |
|---|---|
| Power of Crystals | Green, Blue |
| Chilling Stare | Blue, White |
| Explosive Bolt | Red, White |
| Rush of Adrenaline | Red, Green |

They only ever enter a deck via the round-end Advanced Action offer step (see above), for either the standard Dummy Player or the Proxy Player. Effects:

- **Movement targeting** (see "Objective" above): counts as both colors — closest site valid for either objective color.
- **Crystal-chain deck flip** (see "The Proxy Player's turn" above): matches if the Proxy Player holds crystals of *either* color, extending the chain by the *higher* of the two colors' crystal counts (not their sum) — an app-specific ruling, not stated in the rulebook.
- **Movement-point bonus**: no different from any other Advanced Action — always +2 regardless of color(s).

## Using a Proxy Player in a scenario

Works well in a Cooperative or Solo Conquest-family scenario out of the box. In other scenarios, some conditions may need adjustment — e.g. in Against the Apocalypse, a destroyed site shouldn't count as a valid objective, and the table may choose not to count ziggurats/pyramids as valid objectives either. Not app-enforced; a reminder for the table.

## Not extracted here

The exact tie-breaking and path-legality judgment calls ("most direct path," what counts as "further from the portal," precise adjacency to lakes/mountains/other Heroes) require the physical board state this app doesn't track — reference the physical Proxy Player card and the "Movement" section above at the table. Standard Achievements Scoring and scenario-level scoring/setup already live in each scenario's own `docs/rules/*.md`.
