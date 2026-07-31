# Sites & map features

Ground-truth transcription of every **map-feature description** the *Mage Knight* Quick Reference
Sheet reproduces — the physical **Site Description cards** / help cards (name + art + "what you can
do here and how"). This is the source of truth for the Sites tab's catalogue (issue #177 / #233),
the same role `docs/rules/enemy-tokens.md` plays for the Enemy Picker.

## Scope

**In scope** — every map-feature box on the Quick Reference Sheet: the sites, the two rampaging
enemies (Marauding Orcs, Draconum), the Walls terrain feature, and the three Shades of Tezla
location tiles. **21 entries.**

**Out of scope** — the Quick Reference Sheet's **"Enemy Token Abilities"** block (Defensive /
Offensive abilities, Unit Resistances, Multiple Attacks) on page 2. That reference already lives in
the Enemy Picker's ability info window (`docs/rules/enemy-tokens.md` + `EnemyAbilityText.kt`);
duplicating it here would invite drift. **Apocalypse Dragon sites** are deferred to Sub-issue F
(issue #238) — the Quick Reference Sheet predates that expansion and doesn't cover them.

## Source & provenance

Primary source: **`Mage-Knight-Board-Game-Quick-Reference-Sheet.pdf`** (the Ultimate Edition Quick
Reference Sheet, abbreviated **QRS** below — page 1 is the base game's sites, page 2 the expansion
sites plus the excluded abilities block). The QRS reproduces the physical Site Description cards; the
Lost Legion rulebook says so explicitly: *"Please check the Site Description cards. They should
provide the complete rules for each site but they are written and explained in even more detail
here"* (Expansion Rulebook p.5).

Where the QRS is terse or ambiguous, the fuller rulebook wording is used to resolve it and cited
alongside:

- **`Mage-Knight-Board-Game-Ultimate-Edition-Expansion-Rule-Books-September-2018.pdf`** (abbreviated
  **Expansion Rulebook** below) — "New Map Features" (Lost Legion, p.5) and "New Map Location Tiles"
  (Shades of Tezla, p.21).

## Expansion attribution

Every entry's `expansion` is verified against a rulebook, not inferred from the QRS layout:

| Expansion | Source | Entries |
|-----------|--------|---------|
| `BASE` | QRS p.1 | Crystal Mines, Magical Glade, Marauding Orcs, Draconum, Village, Monastery, Mage Tower, Keep, Monster Den, Spawning Grounds, Dungeon, Tomb, Ancient Ruins (13) |
| `LOST_LEGION` | QRS p.2 + Expansion Rulebook p.5 ("New Map Features") | Walls, Deep Mines, Refugee Camp, Maze, Labyrinth (5) |
| `SHADES_OF_TEZLA` | QRS p.2 + Expansion Rulebook p.21 ("New Map Location Tiles") | Hidden Valley Tile, Necropolis Tile, Graveyard Tile (3) |

The **Krang** expansion adds **no** sites — its only new rule is the "Flip Back Ability" (Expansion
Rulebook p.18) — which is why the Sites domain uses its own `SiteExpansion` enum
(`BASE`/`LOST_LEGION`/`SHADES_OF_TEZLA`) rather than reusing the Enemy Picker's `Expansion` enum:
sites never split Shades of Tezla by faction (a token quirk), never come from Krang, and shouldn't
be coupled to the Token Set's meaning. See `docs/context-sites.md`.

## Category taxonomy

`category` is an **app-side grouping** chosen to power the future sort/group/filter controls
(Sub-issue E / issue #237); it is *not* an official rulebook taxonomy, so the assignments below are
judgment calls open to revision. Definitions:

| Category | Meaning | Members |
|----------|---------|---------|
| `RAMPAGING_ENEMY` | An enemy roaming the map, not a location you own | Marauding Orcs, Draconum |
| `FORTIFIED_SITE` | A defended site you assault and then own | Keep, Mage Tower |
| `ADVENTURE_SITE` | A site you spend an action to *enter and fight* for a reward | Monster Den, Spawning Grounds, Dungeon, Tomb, Ancient Ruins, Maze, Labyrinth |
| `SETTLEMENT` | A friendly site you *interact* at (recruit / heal) | Village, Monastery, Refugee Camp |
| `RESOURCE_SITE` | A site giving a passive start/end-of-turn benefit | Crystal Mines, Deep Mines, Magical Glade |
| `SPECIAL_TILE` | A location tile placed *over* a map space | Hidden Valley Tile, Necropolis Tile, Graveyard Tile |
| `TERRAIN_FEATURE` | A map terrain feature, not a site you occupy | Walls |

Note the one deliberate inconsistency: the **Hidden Valley Tile** is functionally a Magical Glade
(same Healing Essence + Imbued With Magic text) but is categorised `SPECIAL_TILE`, not
`RESOURCE_SITE`, because the rulebook introduces it as a "location tile" placed over a space
alongside Necropolis/Graveyard (Expansion Rulebook p.21) — its structural nature wins over its
functional twin.

---

# BASE (Quick Reference Sheet page 1)

### Crystal Mines
`id: crystal_mines` · `category: RESOURCE_SITE` · `expansion: BASE` · QRS p.1

- **Mining:** If you end your turn on a mine, gain one mana crystal of the color that the mine
  produces to your Inventory.

### Magical Glade
`id: magical_glade` · `category: RESOURCE_SITE` · `expansion: BASE` · QRS p.1

- **Healing Essence:** If you end your turn on a magical glade, you can throw away one Wound card
  from your hand or discard pile. This is not the same as Healing and the effect cannot be combined
  with other Healing effects.
- **Imbued With Magic:** If you start your turn on a magical glade during the Day, you gain a gold
  mana token. If you start your turn on a magical glade during the Night, you gain a black mana
  token.

### Marauding Orcs
`id: marauding_orcs` · `category: RAMPAGING_ENEMY` · `expansion: BASE` · QRS p.1

The QRS spells the title "Maurauding Orcs"; the correct spelling "Marauding Orcs" is used here, to
match `docs/rules/enemy-tokens.md`.

- **When Revealed:** Place a green Orc enemy token face up on this space.
- **Effect:** No one can enter a space that is occupied by an Orc enemy token. Orcs can be provoked
  into combat by a player who moves from one space adjacent to them to another space adjacent to
  them.
- **Action:** You can challenge Orcs from an adjacent space as your action.
- **Reward:** If you defeat the Orcs, discard their token and gain Reputation +1.

### Draconum
`id: draconum` · `category: RAMPAGING_ENEMY` · `expansion: BASE` · QRS p.1

- **When Revealed:** Place a red Draconum enemy token face up on this space.
- **Effect:** No one can enter a space that is occupied by a Draconum enemy token. Draconum can be
  provoked into combat by a player who moves from one space adjacent to it to another space adjacent
  to it.
- **Action:** You can challenge a Draconum from an adjacent space as your action.
- **Reward:** If you defeat the Draconum, discard its token and gain Reputation +2.

### Village
`id: village` · `category: SETTLEMENT` · `expansion: BASE` · QRS p.1

- **Recruiting:** Units with the village icon can be recruited here.
- **Healing:** You can buy 1 point of Healing for 3 Influence here.
- **Plundering:** You can plunder a village during another player's turn. You can only plunder a
  village once between each of your turns. If you do, draw two cards and get Reputation -1.

### Monastery
`id: monastery` · `category: SETTLEMENT` · `expansion: BASE` · QRS p.1

- **Recruiting:** Units with the monastery icon can be recruited here.
- **Healing:** You can buy 1 point of Healing for 2 Influence here.
- **Training:** When a monastery is revealed, put the top card of the Advanced Actions deck face up
  in the Units offer. Advanced Actions in the Units offer can be bought at any monastery for 6
  Influence.
- **Burning a Monastery:** You can try to burn a monastery as your action for the turn. If you do,
  you get Reputation -3. Draw a random violet enemy token to fight. Your Units cannot be used in
  this combat. If you defeat the enemy, mark the space with a Shield token and get an Artifact as
  your reward. The monastery is now destroyed.

### Mage Tower
`id: mage_tower` · `category: FORTIFIED_SITE` · `expansion: BASE` · QRS p.1

- **When Revealed:** Place a violet enemy token face down on this space. The token is revealed
  during the Day if a player is adjacent to it.
- **While Unconquered:** Can be assaulted and you get Reputation -1. The defending enemy is
  fortified. If successfully assaulted, mark it with a Shield token and gain a Spell as your reward.
- **While Conquered:** Any player can recruit Units and buy Spells here. Spells can be bought for 7
  Influence plus a mana that is the same color as the Spell being bought.

### Keep
`id: keep` · `category: FORTIFIED_SITE` · `expansion: BASE` · QRS p.1

- **When Revealed:** Place a grey enemy token face down on this space. This token is revealed during
  the Day if a player is adjacent to it.
- **Unconquered Keep:** Can be assaulted and you get Reputation -1. The defending enemies are
  fortified. If successfully assaulted, mark it with a Shield token. You now own that keep.
- **Other Players' Keeps:** Can be assaulted and you get Reputation -1. If the owner is not present,
  draw a random grey enemy token as the defenders. They are fortified. If successful, you get half
  Fame (rounded up) for defeating the defenders and you replace the owner's Shield token with one of
  your own.
- **Your Keep:** You can recruit Units with the keep icon here. If you end your turn on or adjacent
  to a keep you own, your Hand limit is 1 higher for each Keep you own.

### Monster Den
`id: monster_den` · `category: ADVENTURE_SITE` · `expansion: BASE` · QRS p.1

- **While Unconquered:** You may enter a monster den as your action for the turn. If you do, draw a
  brown enemy token to fight. If you fail to defeat it, leave the enemy token face up on the space.
  Next time a player chooses to enter the den, he fights this token.
- **Reward:** If you defeat the enemy, mark the space as conquered with your Shield token and get
  two random mana crystals as your reward. Roll a mana die two times to determine their color. If
  gold is rolled, you choose the color you gain; if black is rolled you get Fame +1 instead of a
  crystal.

### Spawning Grounds
`id: spawning_grounds` · `category: ADVENTURE_SITE` · `expansion: BASE` · QRS p.1

- **While Unconquered:** You may enter the spawning grounds as your action for the turn. If you do,
  draw two brown enemy tokens and fight them. If you fail to defeat them both, any undefeated tokens
  remain face up on the space and any defeated enemies are replaced with a new face down brown enemy
  token — next time a player chooses to enter the grounds, he reveals and fights these tokens.
- **Reward:** If you defeat both enemies, mark the space as conquered with your Shield token and
  gain an Artifact and three random mana crystals as your reward. Roll a mana die three times to
  determine their color. If gold is rolled, you choose the color you gain; if black is rolled you
  get Fame +1 instead of a crystal.

### Dungeon
`id: dungeon` · `category: ADVENTURE_SITE` · `expansion: BASE` · QRS p.1

- **While Unconquered:** You can enter a dungeon as your action for the turn. If you do, reveal a
  brown enemy token and fight it. Night rules apply for this combat and Units cannot be used. If you
  defeat the enemy, mark the space with a Shield token and roll a mana die to determine your reward.
  For gold or black, gain a Spell; otherwise, gain an Artifact.
- **While Conquered:** You can enter a conquered dungeon to fight a random brown enemy token with
  the same limitations (Night and no Units). If you defeat it, you get no reward (other than the
  Fame) and you do not mark the space with a Shield token.

### Tomb
`id: tomb` · `category: ADVENTURE_SITE` · `expansion: BASE` · QRS p.1

- **While Unconquered:** You can enter a tomb as your action for the turn. If you do, draw a red
  Draconum enemy token to fight. Night rules apply for this combat, and Units cannot be used. If you
  defeat the enemy, mark the tomb with a Shield token and gain one Spell and one Artifact as your
  reward. Otherwise, discard the enemy token.
- **While Conquered:** You can enter a conquered tomb to fight a random red enemy token with the
  same limitations (Night and no Units). If you defeat it, you get no reward (other than the Fame)
  and you do not mark the space with a Shield token. *(The QRS prints "conquered dungeon" here — a
  QRS copy-paste slip from the Dungeon entry; it means a conquered tomb.)*

### Ancient Ruins
`id: ancient_ruins` · `category: ADVENTURE_SITE` · `expansion: BASE` · QRS p.1

- **When Revealed:** Place a yellow token face up if it is Day, face down if it is Night. A face
  down token is revealed at the start of the next Day Round, or if someone enters the space.
- **While Unconquered:** You can enter the ancient ruins as your action for the turn. There will
  either be an altar there, or enemies to fight.
- **Altar:** You can pay 3 mana of the color shown on the yellow token as tribute to the altar. If
  you do, mark the space with a Shield token and gain 7 Fame as your reward.
- **Enemies:** Draw the enemies depicted on the yellow token and fight them. Any undefeated enemies
  remain on the space and can be fought later. If you defeat the last enemy here, mark the space with
  a Shield token and get the reward depicted on the yellow token.

---

# LOST LEGION (Quick Reference Sheet page 2 · Expansion Rulebook p.5 "New Map Features")

### Walls
`id: walls` · `category: TERRAIN_FEATURE` · `expansion: LOST_LEGION` · QRS p.2 · Expansion Rulebook p.5

A wall is always between two spaces of the map (Expansion Rulebook p.5), not a space you occupy —
hence `TERRAIN_FEATURE`.

- **Movement:** To cross a wall on the map, you have to pay 1 extra Move point. Terrain discounts
  cannot negate this extra cost. Walls have no effect on movement invoked directly by an effect
  (Flight, etc.).
- **Combat:** When challenging rampaging enemies across a wall, consider the enemies to be fortified
  (walls count as site fortifications). When attacking another player or Volkare across a wall,
  consider the target to be fortified. When assaulting a fortified site across a wall, consider the
  garrison to be double fortified.
- **Provoking:** Your move does not provoke rampaging enemies if the target space of that move is
  separated from the enemy by a wall (i.e. rampaging enemies never attack you across a wall).

### Deep Mines
`id: deep_mines` · `category: RESOURCE_SITE` · `expansion: LOST_LEGION` · QRS p.2 · Expansion Rulebook p.5

- **Mining:** If you end your turn on a mine, choose and gain one mana crystal of one of the colors
  that the mine produces to your Inventory. *(Deep mines work exactly like Crystal Mines except you
  choose among the depicted colors instead of a fixed color — one of two colors in desert and
  forest, one of all four basic colors in the swamp; Expansion Rulebook p.5.)*

### Refugee Camp
`id: refugee_camp` · `category: SETTLEMENT` · `expansion: LOST_LEGION` · QRS p.2 · Expansion Rulebook p.5

- **Recruiting:** You can interact to recruit Units here. For purposes of recruiting, you may
  consider this to be any of the depicted sites. Units that require more advanced sites for
  recruitment have their cost increased. For Units that can be recruited on multiple sites, choose
  the cheapest price. *(Expansion Rulebook p.5 spells it out: recruit any Unit from the offer; a
  Unit that can be recruited in a Village costs its normal price, one recruitable only in a
  keep/mage tower/monastery costs 1 Influence more, and a city-only Unit costs 3 more. All regular
  interaction rules — including Reputation — still apply.)*

### Maze
`id: maze` · `category: ADVENTURE_SITE` · `expansion: LOST_LEGION` · QRS p.2 · Expansion Rulebook p.5

Maze and Labyrinth are "new adventure sites" (Expansion Rulebook p.5). They share the same
enter/choose-path/fight structure; the two differ only in the enemy fought and the reward tier (the
Labyrinth adds an Advanced Action).

- **Entering:** You may enter a maze as your action. If you do so, you may choose one ready
  unwounded Unit to accompany you. You cannot use any Unit other than the chosen one during the
  entire action. Then choose a path and fight.
- **Choosing Path:** Pay 2, 4, or 6 Move. (Move points left from the Move phase cannot be used for
  this.)
- **Combat:** Draw and fight a **brown** (Dungeon Monster) enemy token. Whether you defeat it or
  not, discard it afterwards (next time, a new token will be drawn). *(The QRS art is ambiguous on
  the color; Expansion Rulebook p.5 confirms "a random brown monster enemy token (in a maze)".)*
- **Reward:** If you defeat it, claim your reward according to the amount of Move points you have
  paid: two crystals of your choice, a Spell, or an Artifact. Put your Shield on the corresponding
  spot on the map to mark this path as conquered.
- **Partially Conquered:** Another player may enter the maze, but will have to choose another path.
  No player may successfully conquer the same maze twice, and each path may be conquered only once.

### Labyrinth
`id: labyrinth` · `category: ADVENTURE_SITE` · `expansion: LOST_LEGION` · QRS p.2 · Expansion Rulebook p.5

- **Entering:** You may enter a labyrinth as your action. If you do so, you may choose one ready
  unwounded Unit to accompany you. You cannot use any Unit other than the chosen one during the
  entire action. Then choose a path and fight.
- **Choosing Path:** Pay 2, 4, or 6 Move. (Move points left from the Move phase cannot be used for
  this.)
- **Combat:** Draw and fight a **red Draconum** enemy token. Whether you defeat it or not, discard
  it afterwards (next time, a new token will be drawn). *(Expansion Rulebook p.5: "a random Draconum
  enemy token (in a labyrinth)".)*
- **Reward:** If you defeat it, claim your reward according to the amount of Move points you have
  paid: two crystals of your choice, a Spell, or an Artifact. In all three cases, you also gain an
  Advanced Action. Put your Shield on the map to mark this path as conquered.
- **Partially Conquered:** Another player may enter the labyrinth, but will have to choose another
  path. No player may successfully conquer the same labyrinth twice, and each path may be conquered
  only once.

---

# SHADES OF TEZLA (Quick Reference Sheet page 2 · Expansion Rulebook p.21 "New Map Location Tiles")

### Hidden Valley Tile
`id: hidden_valley_tile` · `category: SPECIAL_TILE` · `expansion: SHADES_OF_TEZLA` · QRS p.2 · Expansion Rulebook p.21

The Hidden Valley location tile "acts like a Magical Glade" (Expansion Rulebook p.21) — same Healing
Essence and Imbued With Magic text — with an added move cost.

- **Healing Essence:** If you end your turn on a Hidden Valley Tile, you can throw away one Wound
  card from your hand or discard pile. This is not the same as Healing and the effect cannot be
  combined with other Healing effects.
- **Imbued With Magic:** If you start your turn on a Hidden Valley Tile during the Day, you gain a
  gold mana token. If you start your turn on a Hidden Valley Tile during the Night, you gain a black
  mana token.
- **Movement:** The move cost of the Hidden Valley space is 2.

### Necropolis Tile
`id: necropolis_tile` · `category: SPECIAL_TILE` · `expansion: SHADES_OF_TEZLA` · QRS p.2 · Expansion Rulebook p.21

- **Eternal Night:** During the Day, Night rules apply in a Necropolis Tile.
- **Imbued With Magic:** At Night, if you start your turn on a Necropolis Tile, then you gain a
  black mana crystal.
- **Movement:** The move cost of the Necropolis space is 2.

### Graveyard Tile
`id: graveyard_tile` · `category: SPECIAL_TILE` · `expansion: SHADES_OF_TEZLA` · QRS p.2 · Expansion Rulebook p.21

- **Eternal Night:** During the Day, Night rules apply in a graveyard.
- **Imbued With Magic:** At Night, if you start your turn in a graveyard, then you gain a black mana
  crystal.
- **Movement:** The move cost of the Graveyard space is determined by the terrain type shown on the
  space it covers (just like a Ruins tile). The fact that Night rules apply while in a graveyard
  does not affect the Move cost of the terrain it is in (so to move into a forest space that has a
  graveyard in it will still cost 3 Move points during the Day).

---

## Status

First-draft transcription from the QRS + Expansion Rulebook, **pending verification by the project
author against the physical Site Description cards** (same sourcing caveat as
`docs/rules/enemy-tokens.md`). The `category` taxonomy in particular is an app-side grouping open to
revision (see above).
