# Dummy Player

Source: `Mage-Knight-Board-Game-Ultimate-Edition-Rule-Book-September-2018.pdf`, "Cooperative and Solo Scenarios" → "Dummy Player", p.15.

The Dummy Player is the automated pacing opponent used by every cooperative and solo scenario (including Solo Conquest — see `solo-conquest.md`, which already references "cards remaining in the Dummy Player's deck"). This is a base, scenario-agnostic mechanic, extracted here once so scenario docs don't need to re-derive it.

## Setup (deck composition)

- After players choose their Heroes, randomly choose one of the Heroes **not** in the game to be the Dummy Player. Take only that Hero's Hero card, Round Order token, and starting deck of 16 Basic Action cards.
- Give the Dummy Player 3 crystals in its Inventory, matching the 3 colored dots on the bottom of its Hero card (one crystal per dot — e.g. Goldyx's dots are green, green, blue, so the Dummy starts with 2 green + 1 blue crystal). Only the Dummy Player starts with crystals this way — real players start with none.

  This base rulebook only ever shows Goldyx's dots as its worked example — it doesn't print a full per-Knight table. The dots for every other Knight this app models were sourced by visually reading their Hero cards from other materials (research tickets #31, #66, #70): official card photos for Wolfhawk, Arythea, and Coral (Expansion/Apocalypse Dragon rulebooks); a third-party solo-play companion tool's hard-coded values for Tovak, Norowas, Krang, and Braevalar, independently cross-checked against clean card art from a Tabletop Simulator mod. Every one of the 8 Knights below is now confirmed from at least one direct visual source:

  Coral's dots were corrected to White, White, Red by issue #89 (was previously recorded as Blue, Blue, Red). A first re-verification attempt, sampling pixel colors from the embedded thumbnail image on p.5 of `Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf` ("Coral – New Playable Hero" component list), misread the two white dots as blue — likely a color-cast or compression artifact in that low-resolution embedded image, since a direct photo of the physical Hero card (the actual authoritative source, not a scan of a scan) clearly shows two pale/white dots and one dark red dot, not blue. Trust the physical-card photo over the PDF pixel-sampling if the two ever disagree again.

  | Knight | Dots |
  |---|---|
  | Tovak | Red, Blue, Blue |
  | Goldyx | Green, Green, Blue |
  | Norowas | Green, White, White |
  | Wolfhawk | White, White, Blue |
  | Arythea | Red, Red, White |
  | Krang | Red, Red, Green |
  | Braevalar | Green, Blue, Blue |
  | Coral | White, White, Red |

  See `domain/src/main/kotlin/.../DummyPlayerSession.kt`'s `STARTING_CRYSTAL_DOTS` for where this table is actually consumed.
- Shuffle that Hero's Deed deck (the 16 Basic Action cards). That shuffled deck is the Dummy Player's deck.
- The Dummy Player does **not** count toward how many mana dice or Units are used in the scenario.

**No Wounds are added to the deck at setup.** Unlike Volkare's deck in Volkare's Return (16 Basic Actions + 4 competitive Spells + a Race-Level-dependent number of Wounds — see `volkares-return.md`), the standard Dummy Player's starting deck is exactly the 16 Basic Action cards and nothing else. The deck's composition can still change during play, but only through the round-prep offer interactions below (which add Advanced Action cards) — nothing in the base rule ever adds Wounds to it.

## Tactics

- The scenario description determines how the Dummy Player picks its Tactic card each Round (e.g. Solo Conquest: the player always picks first, and the Dummy Player then takes a random card from what's left — see `solo-conquest.md`).
- The Tactic card gives the Dummy Player no benefit; it only fixes their position in Round Order, arranged by Tactic number as usual.

## Turn procedure

On the Dummy Player's turn:

- **If its deck is empty**, it announces End of the Round. Other players get one more turn each, then the Round is over.
- **If its deck is not empty**: flip the top 3 cards from its deck onto its discard pile — all 3 unconditionally, not one at a time up to some stopping condition. Then check the color of the *last* (3rd) card flipped, i.e. the new top of the discard pile:
  - If the Dummy Player has **no** crystals of that color in its Inventory, its turn ends immediately (3 cards flipped total).
  - If it has **any** crystals of that color, flip that many additional cards from its deck — one extra card per crystal of that color it holds. The color of these additional cards does not matter and does not trigger any further chaining. Then its turn ends.
- **Not enough cards to flip**: if the deck runs out partway through a flip (during the initial 3, or during the additional flips), flip as many cards as are available. The Dummy Player does *not* announce End of Round in the middle of that turn — it announces it on its *next* turn, per the empty-deck rule above.

## End of Round: round-prep offer interactions

These aren't something the Dummy Player actively "does" at round end — they're how the standard round-prep step (removing the lowest-value card from the Advanced Action offer and from the Spell offer, done every Round in every scenario to refill the offers) is modified when a Dummy Player is in play:

- **Advanced Action offer**: when the lowest card is removed from the Advanced Action offer, it is added to the Dummy Player's deck instead of being discarded as usual. **The Dummy Player's deck is then reshuffled — its entire discard pile is combined with whatever's left in the deck (plus the newly-added card) into one pile, which is shuffled to become the new deck; the discard pile is empty immediately afterward.** This is the Dummy-Player-specific version of the standard "Preparing a New Round" step every real player also does (rulebook p.4: "[Each player] Shuffles all their Deed cards to create a new Deed deck" — a real player's Deed cards means their deck + discard pile + hand combined; the Dummy Player has no hand, so its version is just deck + discard pile). Without folding the discard pile back in here, the Dummy Player's deck would be reduced to almost nothing every Round after the first — reshuffling is what keeps it playable for the rest of the game.
- **Spell offer**: when the lowest card is removed from the Spell offer, it still goes to the bottom of the Spell deck as usual — but in addition, the Dummy Player's Inventory gains one crystal of that Spell card's color. Unlike real players, the Dummy Player is not capped at 3 crystals of the same color.

## Skills in a solo game

In a Solo game specifically (not required for multiplayer cooperative games), you also use a set of Skills belonging to the Dummy Player's Hero:

- Either remove the Dummy Player Hero's Competitive Interactive Skill (the one with the competitive-interactive symbol) before the game starts, or — if it's drawn during the game — discard it and replace it with another token from the stack.
- Randomize the remaining Dummy Player Skill tokens and place them in a face-down pile.
- Every time your own Hero gains a Skill token, also reveal one Skill from the Dummy Player's pile and put it into the Common Skill offer — it becomes available to you the next time you gain a Skill token. Taking a Skill from the Common Skill offer this way still costs the lowest Advanced Action card, per normal Level Up rules.

## Notes from the rulebook (guidance, not mechanics)

- The random flips can occasionally burn through the Dummy Player's deck much faster than a real player would — watch its deck size to gauge how much you should hurry.
- Players have partial, indirect influence over the Dummy Player's deck and crystal composition (via what's left in the Advanced Action/Spell offers each Round) — the rulebook suggests it can be advantageous not to let the Dummy Player accumulate cards and crystals of the same color.
- The Dummy Player's deck is the reason cooperative scenarios are capped at 3 players by default — a 4-player cooperative game needs a second Dummy Player deck built from another unused Hero.

## Not extracted here

Standard Achievements Scoring and the general "no Titles in solo play" rule are already covered in `solo-scoring-overview.md` — not repeated here. Team rules for two-team cooperative scenarios (also on the same rulebook spread) are out of scope for this doc since v1 is Solo Conquest only; extract them separately if a team-cooperative scenario is ever implemented.
