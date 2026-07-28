# Enemy & ruin tokens

Ground-truth transcription of the physical enemy/ruin token stat blocks, for the Enemy Picker
(issue #178). Each token's Armor / Attack / Fame / abilities are printed only on the cardboard, not
in any rulebook PDF — so unlike the other `docs/rules/*.md` files (which cite rulebook pages), this
one cites the **token faces themselves** as reproduced in a community reference.

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
`defensiveAbilities` / `resistances` (see `CONTEXT.md`'s "Offensive / Defensive Ability"), and
they're mirrored in the app's info window (`EnemyAbilityText.kt`). **Orc Summoners has no printed
attack value** — at
the start of the Block phase it draws a **brown** token to fight in its place (see `CONTEXT.md`'s
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

**Lost Legion ruin tokens are out of scope here.** The Lost Legion also adds 3 hexagonal ruin tokens
(a four-colour "pay 3 mana → 10 Fame" Altar and two Enemies-With-Treasure tokens). They're deferred
to the RUIN-pile work (issue #201) along with the base ruins, since the RUIN pile isn't wired into
the picker yet, the four-colour altar needs a `RuinToken` model change, and the mod ships those
tokens as 3-D meshes with no croppable face art. Shades of Tezla tokens are also deferred: unlike the
Lost Legion, they're the Elementalist / Dark Crusader **faction** enemies (two mutually-exclusive
factions), which the current single-axis Token Set can't represent — that's the possessed-enemy /
faction-token work tracked separately (#189/#190).

## Ruin tokens (base game)

The RUIN pile's 12 base-game tokens are hexagonal and print no armor/attack/fame - they're modelled
as `RuinToken`, a sibling type to `EnemyToken` in its own catalogue (`ruin-tokens.json`), not an
`EnemyToken` variant (see `RuinToken`'s doc comment for why: a flat nullable-field record mirroring
how `EnemyAttack` already tells a numeric attack from a Summon apart, rather than introducing a
sealed/polymorphic JSON shape for one type). Per the rulebook's "Revealing Ruins" section (also see
the Ultimate Edition Walkthrough), a Ruin token is one of two kinds:

- **Ancient Altar** (4 tokens): shows three mana crystals of one colour. Pay three mana of that
  colour to immediately gain 7 Fame; no combat. Base game colours: Green, Blue, White, Red (the
  Lost Legion expansion adds a 4-colour/10-Fame variant, deferred with the rest of the RUIN pile -
  issue #201; see the Lost Legion section above).
- **Enemies With Treasure** (8 tokens): shows two enemy-pile badges (the same pile can appear
  twice - one base-game token draws both its enemies from Green). Draw one token from each named
  pile and fight both; if you defeat both, claim the reward printed on the token. The reward isn't
  modelled in `RuinToken` - the Enemy Picker only tracks pile/draw state, never rewards or Fame
  (ADR-0006), so reward text is flavour only, listed here for completeness:

| Piles drawn | Reward |
|-------------|--------|
| Red + Brown | 2 Artifacts |
| Green + Brown | Artifact + Spell |
| Grey + White | Artifact + Advanced Action |
| Grey + Red | Artifact |
| Grey + Violet | Unit |
| Green + Red | Artifact |
| Green + Green | Set of 4 crystals |
| Red + Violet | Advanced Action + set of 4 crystals |

**Not yet wired into the Enemy Picker's draw flow** - `RuinTokenCatalogue` is transcribed and
validated (mirroring `TokenCatalogue`'s mandatory catalogue-validation test, ADR-0007) but
`EnemyPickerSession`/the UI don't build or render the RUIN pile yet, since a drawn Ruin token needs
different UI treatment than a round enemy token (a mana-payment prompt vs. a two-pile draw
instruction) rather than an armor/attack/fame display, and no art exists for any `ruin_*` id yet
either. Tracked as issue #201.

## Follow-up work (issue #178)

Tracked as GitHub sub-issues of #178:

- Wire the RUIN pile into the Enemy Picker's draw flow, UI, and art (#201)
- Lost Legion enemy tokens — done (#188); Shades of Tezla enemy tokens still pending (#188), plus
  Lost Legion ruin tokens deferred to the RUIN-pile work (#201)
- Apocalypse Dragon — possessed enemies & faction tokens (#189); faction tokens → Score Calculator (#190)
- Explicit Summon Draw action (#191) — incl. summoned tokens applying their own offensive abilities
- Multi-pile simultaneous draw & large-batch display (#192)
- Defeat action (detail button) + multi-draw grid overview (#197)
- Pile back-art on the selector + tap-to-draw-1 (#198)
- Credits screen + art licensing review (#193)
- Bug: destructive DB migration crash-loop on upgrade (#194)
- Chore: refresh CLAUDE.md's stale scope line (#195)

Design context lives in `CONTEXT.md` (Enemy Picker glossary), [ADR-0006](../adr/0006-enemy-picker-owns-pile-state-but-models-no-map.md),
[ADR-0007](../adr/0007-token-catalogue-as-json-in-domain-resources.md), and
`docs/design/architecture.md`. The art/data extraction method is in project memory
(`tts_mod_asset_extraction.md`).
