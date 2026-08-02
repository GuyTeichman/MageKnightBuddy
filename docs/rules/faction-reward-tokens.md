# Faction reward tokens

Ground-truth reference for the four **faction reward token** piles the Enemy Picker draws (issue
#252): the Shades of Tezla **Elementalist** and **Dark Crusader** factions, and the Apocalypse Dragon
**Apocalypse Cult** and **Council of the Void** factions. These are the held reward tokens a player
wins from that faction's enemies — a sibling concept to enemy/ruin tokens, modelled as
`FactionRewardToken` in its own `faction-reward-tokens.json` catalogue.

## What they are

Each faction has a face-down pile of **12 reward tokens: 6 types × 2 copies**. A reward token prints
no Armor/Attack/Fame block — only a **one-off effect**, plus a line printed identically on **every**
token in all four factions:

> *This token may be discarded during interactions for 1 Fame, or 3 Influence.*

So a held reward token has two mutually exclusive uses: play it for its printed effect, **or** cash it
in during an interaction for **1 Fame or 3 Influence**. Because that footer is universal, it is stored
once (here and as a fixed UI footer), never repeated in each token's `effectText`.

## Lifecycle (how the Enemy Picker models them)

Unlike an enemy (draw → fight → discard), a faction reward token's lifecycle is **draw → hold →
spend**:

1. **Earn & draw** — you earn a reward by defeating that faction's enemies (Shades of Tezla faction
   enemies; Apocalypse Dragon possessed enemies of that faction), then draw one **at random** from the
   face-down reward pile. In the app this is a manual pile tap, like a ruin — the app owns the
   randomness but does **not** auto-draw a reward when an enemy is defeated (it models no board;
   ADR-0006).
2. **Hold** — the drawn token is kept face up in your play area until used. The app collects held
   tokens into one pinned Draw Log entry.
3. **Spend** — using the effect (or cashing it in for Fame/Influence) discards the token. In the app,
   "spend" is a checkbox, reusing the Draw Log's defeated/checked-off flag.

**Not scored.** The app deliberately does not wire any of this into the Score Calculator (this is the
rescope of the old issue #190): the Fame/Influence discard option is shown as reference text so the
player can act on it themselves, but the app tracks no Fame or reward state (ADR-0006).

> **Interim pile behaviour (issue #251).** For now a drawn reward token uses the same discard-on-draw
> machinery as enemies, so "spend" is a pure memory aid with no pile effect. Making it pile-correct
> (a held token is out of both piles until spent, at which point it enters the discard and can be
> reshuffled on a Replenish) is tracked in #251, which fixes enemies, ruins, and reward tokens at
> once.

## Piles and gating

Each pile is gated by the faction's existing `Expansion` Token Set toggle: `SHADES_OF_TEZLA_ELEMENTALIST`
and `SHADES_OF_TEZLA_DARK_CRUSADER` each surface their reward pile; the single `APOCALYPSE_DRAGON`
toggle surfaces **both** AD reward piles. Rationale: you only ever earn a faction's rewards by
fighting that faction's enemies, so "faction is in play" is exactly when its reward pile is relevant.

## Token effects (transcribed)

The exact effect wording below is what the app displays; it drives no logic (reference text only).

### Elementalist (Shades of Tezla)

| Token | Effect |
|-------|--------|
| Healing Herbs | Heal 1, or Throw away a Wound card from your discard pile. |
| Tome of Relearning | On your turn, when not in combat, you may swap one of your skills for another in the common skills area. |
| Fire Gem | Add the Fire element to an attack or block from a single action card. |
| Ice Shard | Add the Ice element to an attack or block from a single action card. |
| Cloak of Shielding | Once, when your Hero is about to be wounded, you may ignore one Wound and any additional effects from it. |
| Mystical Map | During your movement, reveal a new tile at a distance of up to 3 spaces. This costs no movement. |

### Dark Crusader (Shades of Tezla)

| Token | Effect |
|-------|--------|
| Vampiric Chalice | After combat this turn, for each enemy you defeated you may discard a Wound (to a maximum of 4). |
| Amulet of Reawakening | Pick a card at random from your discard pile and put it on the bottom of your Deed Deck. At the end of your turn, draw as if your Hand Limit is 1 higher. |
| Vial of Toxin | One source of physical attack gains +3 (+2 in ranged, +1 in siege). This bonus is lost if the attack is no longer physical. |
| Ghostly Elixir | One attack on a single action card becomes ranged attack. |
| Orb of Twilight | You may change one Black mana die in the source to Gold, or one Gold mana die in the source to Black. |
| Staff of Concealment | Move 2. Your movement does not provoke rampaging enemies or enemies with ambush this turn. Also, any enemies pursuing you are not moved and do not attack you this turn. |

### Apocalypse Cult (Apocalypse Dragon)

| Token | Effect |
|-------|--------|
| Cloak of the Martyr | Choose one Unit. It may be assigned damage during combat this turn even if it is Wounded or has already been assigned damage once during this combat. After assigning damage to it, destroy it. |
| Mask of Fear | Reduce one enemy attack by 2. You may not use regular (Silver) units for the remainder of this combat. |
| Blade of Dominance | Spend an amount of Attack. One enemy with total attack(s) equal to or less than this amount does not attack this turn. |
| Blood Fury Dagger | For each Wound you take from one enemy's attack(s), get Attack 2 during your Attack phase, to a maximum of 6 Attack. |
| Ring of Shadows | During the Assign Damage Phase, ignore all wounds (and any additional effects from them) from one enemy's attack(s). Ignore all Fame gained and Reputation gained & lost for the remainder of this combat. |
| Boot of Charging | Move 2. You must use this to move into a space adjacent to a rampaging enemy and you must fight that enemy this turn. Get Attack 2 in the attack phase which you may only use against that enemy. |

### Council of the Void (Apocalypse Dragon)

| Token | Effect |
|-------|--------|
| Binding Contract | Get a discount of 2 towards the cost of recruiting one Unit. You may discard one card or mana crystal to increase the discount to 4. |
| Helm of Mind Shielding | Psychic Block 2. This turn, your Block (but not your Units') is Psychic Block in addition to its other type(s). |
| Mask of Impersonation | When you are about to lose Reputation, reduce the Reputation loss by 1 and you may give another player Reputation -1. |
| Scroll of Seeking | If you are interacting at an inhabited site, you may recruit one Unit as if at any site. If you are not at an inhabited site, as your action this turn you may interact to recruit one Unit as if at a village. |
| Tome of Trading | On your turn, when not in combat, you may swap an Advanced Action or Spell in your hand with one of the same color in the corresponding offer. |
| Aura Gauntlet | Choose a basic mana color shown on a die in the Source and gain the benefit: Blue — Block 2; Green — Heal 1; Red — Attack 2; White — Influence 2. |

## Source & provenance

Transcribed from the Tabletop Simulator Workshop mod "Mage Knight Plus (Highly Scripted)" (Workshop
ID `1721301081`) — the same mod the enemy/ruin token art and stats came from (see
`enemy-tokens.md`). Each faction's `… Reward Tokens` bag holds the tiles as flat `Custom_Tile`s; the
English effect text was read directly from each tile's `Description` field (`{en}…` segment — a
machine-readable source), and the face/back art from its image URLs. Obvious spelling typos in the
mod's transcription were corrected (e.g. "adition" → "addition", "loose" → "lose"); rules wording was
left intact.

**Pending author verification against physical components**, the same caveat every token pile in this
project carries — no more, no less. (An earlier project assumption that Apocalypse Dragon wasn't in
this mod at all was wrong: the mod is actively maintained, and its Apocalypse Cult / Council of the
Void content is the **official** expansion's, not homebrew.) The "how you earn a reward" mechanic
above (defeat a faction/possessed enemy → draw from that pile) is the standard rule but is likewise
worth confirming against the Shades of Tezla and Apocalypse Dragon rulebooks; the app does not depend
on it (drawing is a manual tap).
