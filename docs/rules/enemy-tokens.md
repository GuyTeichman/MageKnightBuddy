# Enemy & ruin tokens

Ground-truth transcription of the physical enemy/ruin token stat blocks, for the Enemy Picker
(issue #178). Each token's Armor / Attack / Fame / abilities are printed only on the cardboard, not
in any rulebook PDF — so unlike the other `docs/rules/*.md` files (which cite rulebook pages), this
one cites the **token faces themselves** as reproduced in a community reference.

## Token pile lifecycle (draw → board → discard)

How a token moves through its pile once play starts — the rule the Enemy Picker's
`EnemyPickerSession` models (issue #251), spelled out here because it leans on a convention the
rulebook states once at setup and then assumes throughout:

- **Setup.** Sort the round enemy and hexagonal ruin tokens by their reverse side and stack them in
  face-down piles, with a space beside each for its **discarded** tokens (rulebook p.3, "seven face
  down piles").
- **Draw / reveal.** A token's identity is undetermined until it is revealed, at which point it is
  taken from the top of its face-down pile and placed **on the board** (its space). It is now out of
  the draw pile but **not** in the discard.
- **Defeated → discard.** Only when an enemy is **defeated** is its token put on that pile's
  **discard** pile. An **undefeated** enemy stays on its space — a rampaging enemy standing from tile
  reveal until it is killed, a garrison sitting on a keep for several Rounds, or a monster that
  survives a failed fight at a den / spawning grounds / ruins (rulebook p.11, 3c) — so it is in
  *neither* pile. This defeated-→-discard step is the piece the rulebook never states as its own line
  (it follows from the general combat rules); it is **pending author verification** against the
  physical rulebook for an exact page, the same caveat every pile in this file carries.
- **Replenish.** When a pile's face-down stack runs out, reshuffle **its discard** into a new
  face-down pile (rulebook p.3, "If you run out of tokens, reshuffle the discarded ones and create a
  new face down pile"). On-board tokens are still in play and are **not** reshuffled — so a pile can
  be genuinely empty (nothing left to draw) while its tokens stand on the board, until one is
  defeated back into the discard.
- **Summoned tokens** are the exception: a token drawn for a **Summon** attack fills the summoner's
  fight slot and is **discarded when the combat ends**, never independently defeated (rulebook p.9) —
  so the app treats it as discarded the instant it is drawn.
- **Faction reward tokens** follow the same shape with "spend" in place of "defeat" (held on the
  board until spent, then discarded) — see `faction-reward-tokens.md`.

## Source & provenance

Transcribed from the Tabletop Simulator Workshop mod "Mage Knight Plus (Highly Scripted)"
(Workshop ID `1721301081`) — the same mod the Knight shield/card art came from (see
`app/src/main/assets/knight-cards/README.md`). Two independent views inside that mod were
cross-checked against each other and agree:

- its **"Enemy Tokens List"** reference sheet (a single printed board of every base-game enemy
  token, grouped by pile colour, each labelled with its name and copy count), and
- the **individual token face images** inside the mod's "Marauding Orcs" bag.

Both were read directly off the images. **Pending verification by the project author against the
physical components** before being treated as final (the numbers below are a first draft from the
mod, per the agreed sourcing plan). The same method covers grey/violet/brown/red/white (issue #187):
the reference sheet's per-pile rows cross-checked against each pile's own bag ("Keep Garrisons",
"Mage Tower Garrisons", "Draconum", "Dungeon Monsters", "City Garrisons").

## Token face layout

Each round enemy token prints three numbers plus icons:

- **Armor** — top, in a dark shield badge. Fortification/resistance icons sit beside it (a stone
  tower = **Fortified**; a pentagon containing a fist/flame/snowflake = resistance to that element).
- **Attack** — left, in a stone-fist badge for a physical attack (a coloured element badge for a
  magical one). Offensive-ability icons print beside it (winged arrow = **Swift**; flaming skull =
  **Brutal**; green skull-droplet = **Poison**). Although these icons sit next to the attack, they
  are **whole-token** abilities that apply to *every* attack the enemy makes (Lost Legion, "Multiple
  Attacks") — not per-attack modifiers. A **Summon** token shows a brown-token icon here instead of
  an attack number — it draws a brown token to fight in its place, so it has no printed attack value.
- **Fame** — bottom, in a red banner.

## Green pile — Marauding Orcs (base game)

12 tokens: 6 types, 2 copies each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Prowlers      | 2 | 3 | 4 | Physical | — | 2 | — |
| Diggers       | 2 | 3 | 3 | Physical | — | 2 | Fortified |
| Cursed Hags   | 2 | 5 | 3 | Physical | Poison | 3 | — |
| Wolf Riders   | 2 | 4 | 3 | Physical | Swift | 3 | — |
| Ironclads     | 2 | 3 | 4 | Physical | Brutal | 4 | Physical Resistance |
| Orc Summoners | 2 | 4 | — | — | Summon → Brown | 4 | — |

Ability meanings are on the Quick Reference Sheet ("Enemy Token Abilities"), split into **offensive**
(Swift/Brutal/Poison/Paralyze/Cumbersome/Assassination/Vampiric — modify the enemy's own attacks) and
**defensive** (Fortified/Elusive/Arcane Immunity, plus element Resistances — govern how it's
attacked). Both are whole-token; the domain models them as `EnemyToken.offensiveAbilities` /
`defensiveAbilities` / `resistances` (see `docs/context-enemy-picker.md`'s "Offensive / Defensive Ability"), and
they're mirrored in the app's info window (`EnemyAbilityText.kt`). **Orc Summoners has no printed
attack value** — at
the start of the Block phase it draws a **brown** token to fight in its place (see `docs/context-enemy-picker.md`'s
"Summon Draw"). The summoned pile is recorded per-token in the catalogue rather than assumed to be
brown, since possessed/expansion summoners can draw from other colours.

## Grey pile — Keep Guardians (base game)

10 tokens: 4 types (issue #187, source: "Enemy Tokens List" reference sheet, cross-checked against
the "Keep Garrisons" bag's individual token faces).

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Crossbowmen | 3 | 4 | 4 | Physical | Swift | 3 | — |
| Guardsmen   | 3 | 7 | 3 | Physical | — | 3 | Fortified |
| Swordsmen   | 2 | 5 | 6 | Physical | — | 4 | — |
| Golems      | 2 | 5 | 2 | Physical | — | 4 | Physical Resistance |

## Violet pile — Mage Tower Guardians (base game)

10 tokens: 6 types (issue #187, same sourcing as Grey; individual faces cross-checked against the
"Mage Tower Garrisons" bag).

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Monks         | 2 | 5 | 5 | Physical | Poison | 4 | — |
| Illusionists  | 2 | 3 | — | — | Summon → Brown | 4 | Physical Resistance |
| Ice Mages     | 2 | 6 | 5 | Ice | — | 5 | Ice Resistance |
| Ice Golems    | 1 | 4 | 2 | Ice | Paralyze | 5 | Ice + Physical Resistance |
| Fire Mages    | 2 | 5 | 6 | Fire | — | 5 | Fire Resistance |
| Fire Golems   | 1 | 4 | 3 | Fire | Brutal | 5 | Fire + Physical Resistance |

**Illusionists has no printed attack value** - like Orc Summoners, it summons a token instead of
attacking. Its summon icon's colour matches the **Brown** pile's swatch on the reference sheet.

## Red pile — Draconum (base game)

8 tokens: 4 types, the game's dragons (issue #187, cross-checked against the "Draconum" bag - despite
the internal bag name, this is the pile the rulebook calls **Red**).

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Swamp Dragon | 2 | 9 | 5 | Physical | Swift, Poison | 7 | — |
| Fire Dragon  | 2 | 7 | 9 | Fire | — | 8 | Fire + Physical Resistance |
| Ice Dragon   | 2 | 7 | 6 | Ice | Paralyze | 8 | Ice + Physical Resistance |
| High Dragon  | 2 | 9 | 6 | Cold Fire | Brutal | 9 | Fire + Ice Resistance |

High Dragon resists **both** Fire and Ice, which per the Quick Reference Sheet means it also
resists Cold Fire attacks - see `AttackElement`'s doc comment.

## Brown pile — Dungeon Monsters (base game)

10 tokens: 5 types (issue #187, cross-checked against the "Dungeon Monsters" bag - despite the
internal bag name, this is the pile the rulebook calls **Brown**) - used at Dungeons and (per the
Walkthrough's "Revealing Ruins") for some Ruin combats; it is *not* the Ruin pile itself.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Minotaur    | 2 | 5 | 5 | Physical | Brutal | 4 | — |
| Gargoyle    | 2 | 4 | 5 | Physical | — | 4 | Physical Resistance |
| Medusa      | 2 | 4 | 6 | Physical | Paralyze | 5 | — |
| Crypt Worm  | 2 | 6 | 6 | Physical | — | 5 | Fortified |
| Werewolf    | 2 | 5 | 7 | Physical | Swift | 5 | — |

## White pile — City Garrisons (base game)

10 tokens: 4 types (issue #187, cross-checked against the "City Garrisons" bag) - the "Altem"
tokens are the game's toughest base-game enemies short of the dragons.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Freezers        | 3 | 7 | 3 | Ice | Swift, Paralyze | 7 | Fire Resistance |
| Gunners         | 3 | 6 | 6 | Fire | Brutal | 7 | Ice Resistance |
| Altem Guardsmen | 2 | 7 | 5 | Physical | — | 8 | Fortified; Fire + Ice + Physical Resistance |
| Altem Mages     | 2 | 8 | 4 | Cold Fire | Brutal, Poison | 8 | Physical Resistance |

## Lost Legion expansion tokens (issue #188)

The Lost Legion adds enemy tokens to all six base piles. They share the base tokens' **backs**, so
they shuffle straight into the same colour piles (which is why the pile totals rise: GREEN 12→20,
GREY 10→18, VIOLET 10→14, BROWN 10→16, RED 8→14, WHITE 10→16). Tagged `Expansion.LOST_LEGION` in the
catalogue so the Enemy Picker's Token Set can include or exclude them per game.

**Source & provenance**: transcribed the same two-source way as the base piles — the individual
token faces in each pile's "Lost Legion …" bag in the TTS mod (Workshop `1721301081`), read
icon-by-icon, cross-checked against the mod's **"Enemy Tokens List"** reference sheet (its second
page, "The Lost Legion Enemy Tokens", which prints every token's name and copy count). Ability
*meanings* for the new keywords are quoted from the official **The Lost Legion rulebook** (2013),
"New Enemy Token Abilities" — not just inferred from the icons. Still **pending author verification
against the physical components**, same caveat as the base piles.

**New abilities introduced here** (all defined in the Lost Legion rulebook, p.5):

- **Elusive** (`DefensiveAbility.ELUSIVE`): the enemy has *two* Armor values. The higher one is
  always used in the Ranged & Siege phase, and stays in use for the rest of combat unless **all** of
  its attacks are blocked, in which case the lower value applies in the Attack phase. Modelled as
  `EnemyToken.armor` (lower, printed large) plus `EnemyToken.elusiveArmor` (higher); the tables below
  write it as `3 (6)` = lower (higher).
- **Unfortified** (`DefensiveAbility.UNFORTIFIED`): the logical opposite of Fortified — the enemy
  ignores all site fortifications, so a keep/city garrison or wall never makes it count as Fortified.
- **Multiple Attacks**: some tokens print *several* separate attack values, each blocked and assigned
  as if from a different enemy (rulebook, "Enemies With Multiple Attacks"). Modelled as several
  entries in `EnemyToken.attacks`; per-attack `element` can differ (a "Heroes" token below mixes a
  physical and an elemental attack), while offensive abilities remain whole-token. Note the rulebook
  clarifies Multiple Attacks and Summon are *types of attack*, not offensive abilities.
- **Reputation as a reward** (`EnemyToken.reputation`): five Lost Legion grey tokens print a
  Reputation change beside their Fame — **+1 for Thugs**, **−1 for each of the four Heroes** —
  gained/lost when you defeat them (rulebook p.5, "Reputation as a Reward"). Every other token prints
  none (`reputation` defaults to `0`). It's a printed token attribute, shown in the zoom stat line
  like Fame ("Armor 5 · Fame 2 · Reputation +1"); the Enemy Picker still keeps no Fame or rewards as
  *scoring* state (ADR-0006) — this just mirrors what's on the cardboard.

Icon legend used while transcribing: a stone tower = Fortified; a tower with a red **X** =
Unfortified; two stacked Armor shields (e.g. `3`/faded `6`) = Elusive; element pentagons = the
matching resistance; a fist pentagon = Physical resistance; a purple pentagram = Arcane Immunity;
winged arrow = Swift; flaming skull = Brutal; green skull-droplet = Poison; bloody dagger =
Assassination; grey statue/bust = Paralyze; a tan rubble token = a Summon drawing from the Brown pile.

### Green pile — Marauding Orcs (Lost Legion)

8 tokens: 4 types, 2 copies each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Orc Skirmishers  | 2 | 4     | 1 + 1 | Physical | Multiple Attacks | 2 | — |
| Orc Trackers     | 2 | 3 (6) | 4     | Physical | Assassination    | 3 | Elusive |
| Orc War Beasts   | 2 | 5     | 3     | Physical | Brutal           | 3 | Unfortified; Fire + Ice Resistance |
| Orc Stonethrower | 2 | 2     | 7     | Physical | —                | 4 | Fortified; Physical Resistance |

### Grey pile — Keep Guardians (Lost Legion)

8 tokens: 6 types. Four are all named **Heroes** on the cardboard (one copy each) — the catalogue
keeps them apart by `id` (`grey_heroes_fortified` / `_swift` / `_fire` / `_ice`) while sharing the
name. Thugs and Shocktroops have two copies each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Thugs             | 2 | 5     | 3           | Physical | Brutal                     | 2 (Rep +1) | — |
| Shocktroops       | 2 | 3 (6) | 5           | Physical | —                          | 3 | Unfortified; Elusive |
| Heroes (`_fortified`) | 1 | 4 | 5 + 3       | Physical | Multiple Attacks           | 5 (Rep −1) | Fortified |
| Heroes (`_swift`)     | 1 | 5 | 3 + 2       | Physical | Swift, Multiple Attacks    | 5 (Rep −1) | — |
| Heroes (`_fire`)      | 1 | 4 | 4 phys + 2  | Phys + Fire | Multiple Attacks        | 5 (Rep −1) | Fire Resistance |
| Heroes (`_ice`)       | 1 | 4 | 3 phys + 3  | Phys + Ice  | Multiple Attacks        | 5 (Rep −1) | Ice Resistance |

### Violet pile — Mage Tower Guardians (Lost Legion)

4 tokens: 2 types, 2 copies each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Sorcerers       | 2 | 6 | 6     | Physical | Assassination, Poison    | 5 | Arcane Immunity |
| Magic Familiars | 2 | 7 | 3 + 3 | Physical | Brutal, Multiple Attacks | 5 | Unfortified |

### Brown pile — Dungeon Monsters (Lost Legion)

6 tokens: 3 types, 2 copies each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Manticore | 2 | 6     | 4         | Physical  | Swift, Assassination, Poison | 5 | Fire Resistance |
| Hydra     | 2 | 6     | 2 + 2 + 2 | Physical  | Multiple Attacks             | 5 | Ice Resistance |
| Shadow    | 2 | 4 (8) | 4         | Cold Fire | —                            | 4 | Elusive; Arcane Immunity |

### Red pile — Draconum (Lost Legion)

6 tokens: 3 types, 2 copies each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Lava Dragon     | 2 | 8      | 6 | Fire     | Brutal          | 8 | Fortified; Fire Resistance |
| Dragon Summoner | 2 | 8      | — | —        | Summon → Brown ×2 | 9 | Arcane Immunity; Physical Resistance |
| Storm Dragon    | 2 | 7 (14) | 4 | Ice      | Swift           | 7 | Elusive; Ice Resistance |

**Dragon Summoner** is the rulebook's "Summoning Dragon": it has a *double* Summon attack — it draws
**two** Brown tokens, blocked separately (rulebook p.5, "Enemies With Multiple Attacks"). It's the
first catalogue token with more than one Summon attack, exercising the multi-summon path #191 built.

### White pile — City Garrisons (Lost Legion)

6 tokens: 4 types.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Fire Catapult     | 1 | 7  | 8  | Fire      | —                        | 7 | Fortified |
| Ice Catapult      | 1 | 6  | 9  | Ice       | —                        | 7 | Fortified |
| Delphana Masters  | 2 | 8  | 5  | Cold Fire | Assassination, Paralyze  | 9 | Fire + Ice Resistance |
| Grim Legionnaires | 2 | 10 | 11 | Physical  | —                        | 8 | Unfortified; Arcane Immunity |

**Lost Legion ruin tokens: transcribed and wired (issue #201).** The Lost Legion adds 3 hexagonal
ruin tokens - a four-colour "pay one of each → 10 Fame" Altar and two Enemies-With-Treasure tokens,
one of which draws **three** enemies (which is exactly why `enemyPiles` is a variable-length list,
not a fixed pair). Their exact data is in the "Ruin tokens" section's table below, transcribed from
the rulebook p.20 token strip and the TTS "Enemy Tokens List" reference sheet. Their face art lives
in a *separate* `/Lost Legion/Lost Legion Ruins` bag in the mod (not the base "Ruins" bag), so all
three are now bundled too. The Shades of Tezla enemy tokens are transcribed in their own
section below.

## Shades of Tezla expansion tokens (issue #188)

Shades of Tezla adds **32 enemy tokens split into two factions** — the **Elementalist** and the
**Dark Crusader**. Each faction has the same shape: 8 green ("Marauding …"), 4 brown ("… Dungeon
Monsters") and 4 red ("… Draconum") enemies — no grey/violet/white additions. The green/brown/red
token backs match the base piles, so a faction's tokens can shuffle straight into those three colour
piles (which is why those totals rise: GREEN 20→36, BROWN 16→24, RED 14→22 with both factions in).

**Two selectable token sets, not one.** Unlike the Lost Legion, these enemies belong to two factions
that scenarios use differently: some use one faction, some use both (split by map geography), some
use neither. The full "faction-only, separate pile per faction" setup needs per-faction piles the
Enemy Picker's Token Set can't express yet (deferred to the possessed-enemy/faction work, #189/#190).
What the app *does* model is the rulebook's explicitly-sanctioned looser mode — *"faction enemy
tokens can be mixed in with the regular enemy tokens … In some scenarios only one faction is used. In
this case you can mix the other faction's enemy tokens in with the regular enemy tokens if you wish"*
(Shades of Tezla rules, "Variants for Other Scenarios"). So each faction is its own
`Expansion.SHADES_OF_TEZLA_ELEMENTALIST` / `Expansion.SHADES_OF_TEZLA_DARK_CRUSADER` — a separately
tickable Token Set entry that mixes into the piles, exactly like Lost Legion.

**Source & provenance**: transcribed from the individual token faces in each faction's "Marauding …",
"… Dungeon Monsters" and "… Draconum" bags in the TTS mod (Workshop `1721301081`), **cross-checked
against the mod's own per-token combat-script stat table** (the "Highly Scripted" mod encodes every
token's armour/attack/element/abilities in Lua for its combat automation, keyed by token GUID — a
machine-readable second source, unlike the base/Lost Legion piles which relied on eye-read reference
sheets). Ability *meanings* for the new keywords are quoted from the official **Shades of Tezla
rulebook** ("New Enemy Token Abilities"). Still **pending author verification against the physical
components**, same caveat as the other piles.

**New ability introduced here**:

- **Defend** (`EnemyToken.defend`, a nullable number — the reserved "valued defensive ability" from
  `DefensiveAbility`'s doc): *"The first enemy that you attack in combat (either in the Ranged phase
  or the normal attack phase) has its Armor value increased by the value of the Defend ability, until
  the end of that combat"* (rulebook p.2). Printed as a small numbered shield beside the Armor. Like
  `elusiveArmor`, it's a number on the token rather than a `DefensiveAbility` enum entry, since the
  enum can't carry the value. Written in the tables below as "Defend N".

Other keywords are all ones the base/Lost Legion piles already use: **Vampiric** (Dark Crusaders lean
on it — +1 Armor for the rest of combat per Unit wounded / Wound dealt), Multiple Attacks, Elusive
(the higher Armor value, shown as `4 (8)` = lower (higher); always exactly **double** the printed
value on these tokens), Unfortified, Fortified, Arcane Immunity, Swift, Brutal, Poison, Paralyze,
Cumbersome, Assassination, and the element Resistances. **Summon** here draws from the *same faction's*
pile (Shrouded Necromancers summons a green faction enemy) — the app models the pile it draws from
(`GREEN`), not the faction. A faction symbol (a blue leaf for Elementalists, a horned skull for Dark
Crusaders) prints to the right of the Fame banner; it marks the faction and the faction-reward the
token grants on defeat, neither of which the Enemy Picker tracks (ADR-0006), so it isn't modelled.

### Elementalist faction

#### Green pile — Marauding Elementalist

8 tokens: 5 types.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Elemental Priestesses | 1 | 4     | 3 + 3 | Ice + Fire | Multiple Attacks    | 3 | Fire + Ice Resistance |
| Elven Protectors      | 2 | 4     | 3     | Physical   | Defend 2            | 2 | Fire Resistance |
| Crystal Sprites       | 2 | 1 (2) | 1 + 1 | Ice        | Multiple Attacks, Defend 1 | 1 | Elusive; Ice Resistance |
| Centaur Outriders     | 2 | 5     | 3     | Physical   | Swift               | 2 | — |
| Cloud Griffons        | 1 | 4 (8) | 4     | Physical   | Swift               | 3 | Unfortified; Elusive |

#### Brown pile — Elementalist Dungeon Monsters

4 tokens: 4 types, 1 copy each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Air Elemental   | 1 | 4 (8) | 3 | Cold Fire | Swift            | 4 | Elusive; Fire + Ice Resistance |
| Fire Elemental  | 1 | 6     | 7 | Fire      | —                | 4 | Fire Resistance |
| Water Elemental | 1 | 7     | 6 | Ice       | —                | 4 | Ice Resistance |
| Earth Elemental | 1 | 5     | 4 | Physical  | Brutal, Cumbersome | 4 | Fortified; Physical Resistance |

#### Red pile — Elementalist Draconum

4 tokens: 2 types, 2 copies each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:------:|:-----:|---------|-----------|:----:|-------------------------|
| Savage Dragon    | 2 | 7      | 5 | Physical  | Brutal | 6 | Physical Resistance |
| Lightning Dragon | 2 | 7 (14) | 6 | Cold Fire | —      | 7 | Elusive; Fire + Ice Resistance |

### Dark Crusader faction

#### Green pile — Marauding Dark Crusader

8 tokens: 5 types.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:-----:|:------:|---------|-----------|:----:|-------------------------|
| Corrupted Priests     | 1 | 5 | 4         | Cold Fire | Vampiric, Defend 1         | 3 | — |
| Zombie Horde          | 2 | 5 | 1 + 1 + 1 | Physical  | Multiple Attacks, Cumbersome | 2 | Ice Resistance |
| Gibbering Ghouls      | 2 | 4 | 4         | Physical  | Vampiric                   | 2 | — |
| Shrouded Necromancers | 1 | 5 | —         | —         | Summon → Green             | 3 | Fortified |
| Skeletal Warriors     | 2 | 4 | 3         | Physical  | —                          | 1 | Fire Resistance |

#### Brown pile — Dark Crusader Dungeon Monsters

4 tokens: 4 types, 1 copy each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:------:|:-----:|---------|-----------|:----:|-------------------------|
| Blood Demon | 1 | 6      | 6 | Physical | Brutal, Assassination | 5 | Arcane Immunity; Fire Resistance |
| Pain Wraith | 1 | 4 (8)  | 4 | Physical | Paralyze              | 3 | Elusive |
| Vampire     | 1 | 5 (10) | 5 | Physical | Vampiric              | 4 | Elusive |
| Mummy       | 1 | 5      | 4 | Physical | Poison                | 4 | Ice + Physical Resistance |

#### Red pile — Dark Crusader Draconum

4 tokens: 2 types, 2 copies each.

| Token | Copies | Armor | Attack | Element | Modifiers | Fame | Resistances / abilities |
|-------|:------:|:------:|:-----:|---------|-----------|:----:|-------------------------|
| Vampire Dragon | 2 | 8 (16) | 8 | Physical | Vampiric               | 7 | Elusive |
| Death Dragon   | 2 | 9      | 7 | Physical | Paralyze, Assassination | 6 | — |

**Shades of Tezla ruins & other non-enemy tokens are out of scope here.** The expansion also adds new
location tokens (Necropolis, Hidden Valley, Cemetery), faction leaders and a faction die — none of
which are round-enemy tokens, so they're not part of the Enemy Picker's pile model. Its **faction
reward tokens** *are* now modelled, but as their own concept in `docs/rules/faction-reward-tokens.md`
(`FactionRewardToken`, issue #252) rather than here — they're held rewards, not enemies.

## Ruin tokens (base game)

The RUIN pile's 12 base-game tokens are hexagonal and print no armor/attack/fame - they're modelled
as `RuinToken`, a sibling type to `EnemyToken` in its own catalogue (`ruin-tokens.json`), not an
`EnemyToken` variant (see `RuinToken`'s doc comment for why: a flat nullable-field record mirroring
how `EnemyAttack` already tells a numeric attack from a Summon apart, rather than introducing a
sealed/polymorphic JSON shape for one type). Per the rulebook's "Revealing Ruins" section (also see
the Ultimate Edition Walkthrough), a Ruin token is one of two kinds:

- **Ancient Altar** (4 base tokens): shows three mana crystals of one colour. Pay three mana of that
  colour to immediately gain 7 Fame; no combat. Base game colours: Green, Blue, White, Red. Modelled
  as `RuinToken.altarColors`, a `List<ManaColor>` whose **size is the whole distinction** (spelled
  out here so nothing leans on an unstated convention): **size 1** = pay 3 of that colour → 7 Fame;
  **size 4** = the Lost Legion pay-one-of-each altar → 10 Fame (see the Lost Legion section above).
  The Fame is derived from the size for the on-screen prompt, never stored.
- **Enemies With Treasure** (8 base tokens): shows enemy-pile badges. Draw one token from each named
  pile and fight them all; if you defeat them, claim the reward printed on the token. Modelled as
  `RuinToken.enemyPiles`, a `List<TokenPileId>` drawn **in order**, where **a pile repeated in the
  list means that many draws from it** (base game's Green+Green = `[GREEN, GREEN]`; Lost Legion adds
  a three-enemy token) - this generalises the old "two badges" reading. The reward is `RuinToken.reward`,
  **displayed as reference text but never tracked or scored** (ADR-0006, amended by #201).

  The full base + Lost Legion ruin data below was **re-transcribed against ground truth** during #201
  - the TTS mod's "Enemy Tokens List" reference sheet (which reproduces every token face) cross-checked
  against the Lost Legion rulebook p.20 token strip. This **corrected two pile-pair errors and several
  rewards** from the original #187 transcription: the tokens read then as "Grey + Red" and "Red +
  Violet" are actually **Grey + Brown** and **Brown + Violet** (a tan sword badge, not the red-dragon
  badge), and "Grey + White" gives a **Spell** (violet card), not an Advanced Action. Reward-icon key:
  gold goblet = Artifact, violet card = Spell, deed card = Advanced Action, figure = Unit, four gems =
  set of 4 crystals.

| Piles drawn (badge order) | Reward | Expansion |
|---------------------------|--------|-----------|
| Brown + Red | 2 Artifacts | Base |
| Green + Red | Artifact + Advanced Action | Base |
| Grey + White | Artifact + Spell | Base |
| Grey + Brown | Artifact | Base |
| Grey + Violet | Unit | Base |
| Green + Brown | Artifact | Base |
| Green + Green | Set of 4 crystals | Base |
| Brown + Violet | Spell + set of 4 crystals | Base |
| Green + Green + Green | Unit | Lost Legion |
| Violet + Violet | Spell + Advanced Action | Lost Legion |

Lost Legion altar: one crystal each of **Green, Blue, White, Red → 10 Fame** (`altarColors` size 4).

**Wired into the Enemy Picker (issue #201).** `EnemyPickerSession` builds the single RUIN pile from
`RuinTokenCatalogue` (one copy per ruin, gated by the Token Set), the pile is tap-to-draw-1 in the
selector, and a drawn ruin gets its own dialog: an Ancient Altar shows its mana prompt + derived
Fame, an Enemies-With-Treasure ruin shows its draw instruction + reward + a one-shot "Draw its
enemies" button (which draws the prescribed enemies via the shared Summon Draw machinery and attaches
them under the ruin, rendered in full so their defensive abilities show, each independently
defeatable).

**Art (all 15 ruins done, #201):** every ruin - 12 base + 3 Lost Legion - is bundled as face art.
Although the mod holds ruins as 3-D `Custom_Model`s, each model's `DiffuseURL` is a flat two-hex
texture (left = the token face, right = the shared rubble back). The base 12 live in the mod's "Ruins"
bag; the 3 Lost Legion ruins live in a *separate* `/Lost Legion/Lost Legion Ruins` bag (found by
scanning every object sharing the ruin hex `MeshURL`, not just the base bag - an earlier pass looked
only in "Ruins" and wrongly concluded both that ruins were mesh-only *and* that the LL faces were
absent). Each face is cut to the hexagon by a **single geometric mask**, not by colour: because every ruin
shares the same mesh/UV, its hexagon sits at the exact same place in the half-texture, so one polygon
- a regular pointy-top hexagon, vertices `(219,0) (438,125) (438,375) (219,500) (0,375) (0,125)` in
the 438x500 half - fits all 15 faces and the back. (A first attempt colour-keyed the yellow surround
via flood-fill; it bled through the same-yellow altar interiors and nibbled some tokens, so it was
dropped in favour of the coordinate mask.) The masked result is a transparent square PNG -
`app/src/main/assets/enemy-tokens/ruin_*.png`, plus `backs/ruin.png` for the shared rubble back. They
render with **no** Compose clip (the PNG alpha is the hexagon), which is what makes ruins read as hexes
next to the round enemy discs. The data itself (base
+ Lost Legion) is fully transcribed above.

## Follow-up work (issue #178)

Tracked as GitHub sub-issues of #178:

- Wire the RUIN pile into the Enemy Picker's draw flow, UI, and art (#201)
- Lost Legion and Shades of Tezla enemy tokens — done (#188); Lost Legion ruin tokens done with the
  RUIN-pile work (#201); Shades of Tezla's faction-only/separate-enemy-pile scenarios still deferred
- Faction reward tokens (all four factions across Shades of Tezla + Apocalypse Dragon) — done (#252),
  see `docs/rules/faction-reward-tokens.md`; the discard-pile-correctness follow-up (#251) is done too
  — see "Token pile lifecycle" above
- Apocalypse Dragon — possessed enemies & AD enemy tokens (#189, narrowed: its faction-token half
  moved to #252); wiring faction tokens into the Score Calculator (#190) was **closed** — faction
  reward tokens are drawn in the Enemy Picker, not scored
- Explicit Summon Draw action (#191) — incl. summoned tokens applying their own offensive abilities
- Multi-pile simultaneous draw & large-batch display (#192)
- Defeat action (detail button) + multi-draw grid overview (#197)
- Pile back-art on the selector + tap-to-draw-1 (#198)
- Credits screen + art licensing review (#193)
- Bug: destructive DB migration crash-loop on upgrade (#194)
- Chore: refresh CLAUDE.md's stale scope line (#195)

Design context lives in `docs/context-enemy-picker.md`, [ADR-0006](../adr/0006-enemy-picker-owns-pile-state-but-models-no-map.md),
[ADR-0007](../adr/0007-token-catalogue-as-json-in-domain-resources.md), and
`docs/design/architecture.md`. The art/data extraction method is in project memory
(`tts_mod_asset_extraction.md`).
