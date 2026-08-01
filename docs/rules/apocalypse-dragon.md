# Apocalypse Dragon — possessed enemies

Ground-truth rules for the Apocalypse Dragon expansion's **possessed enemies** (issue #189), for the
Enemy Picker (issue #178). Scope here is deliberately narrow: **possessed enemies only**. The
expansion's **faction tokens** (the held reward inventory) are tracked separately and get their own
rules coverage — this doc references them only as "the reward a possessed enemy grants," which is a
defining trait of a possessed enemy, not the faction-token machinery itself.

Everything below cites the **Apocalypse Dragon rulebook**
(`Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf`), pages 5–8, plus the per-token deltas
transcribed from the TTS mod (see Source & provenance).

## Source & provenance

Per-token modifier values were transcribed from the Tabletop Simulator Workshop mod "Mage Knight Plus
(Highly Scripted)" (Workshop ID `1721301081`) — the same mod all other token art/stats came from —
**cross-checked two independent ways that agree**:

- the mod's big `LuaScript` block encodes every possessed token as a Lua table keyed by GUID, e.g.
  `["302a34"]={name="Possesed 01", pugType="possessed", fame=1, attack={M={4}}, reward=1, faction="Apoc"}`,
  and
- the **individual token face images** in the mod's "Possessed Tokens" bag (each a `Custom_Model`
  whose `CustomMesh.DiffuseURL` is the printed face, exactly like the ruin hexes), read icon-by-icon.

The Lua fields map to the printed icons as verified on the faces:

- `armour` = **Armor** delta (dark breastplate badge)
- `boost` = **topmost Attack** delta (the split stone-fist / fire-ice attack badge)
- `attack={M={N}}` = a **Psychic Attack** of value `N` (the pink brain badge; `M` = mental/psychic)
- `fame` = **Fame** delta (red banner badge)
- `reward=1` (present on all 12) = the reminder that defeating a possessed enemy grants a Faction
  token. `faction="Apoc"` is hardcoded by the mod's scripted scenarios and is **not** a property of
  the token — the rulebook determines the faction from the triggering text (see below), so it is not
  modelled.

**Correction to an earlier assumption:** issue #189 and the `tts_mod_asset_extraction` memory note
both state "Apocalypse Dragon is NOT in this mod." That is wrong — the mod contains a "Possessed
Tokens" bag (+ discards) and both faction reward-token sets ("Apocalypse Cult" / "Council of the
Void", which the memory note mislabels as homebrew). AD content extracts by the established method.

**Pending author verification against the physical components**, same caveat as the base/Lost
Legion/Shades piles.

## What a possessed enemy is

*"There is a new type of enemy, possessed enemies, that modify the enemy they possess."* (p.5)

A possessed enemy is built from **two** tokens (p.7):

- a **possessed enemy token** (one of 12, drawn face-down from the possessed pile), which prints
  **deltas, not stats**, and
- a normal **circular enemy token** placed *on* the possessed token.

Crucially, **neither the circular enemy's colour nor the awarded faction is printed on the possessed
token** — both are named by *the text that told you to draw it* (p.7):

> The type of circular enemy token will be indicated by the text telling you to draw a possessed
> enemy token. […] The faction that the Faction token is taken from will also be indicated by the
> text telling you to draw a possessed enemy token.

Worked example (p.7): *"If you are told to draw an Apocalypse possessed brown enemy token, draw a
possessed enemy token and a brown enemy token and place the brown enemy token on the possessed enemy
token. If you defeat this enemy, gain an Apocalypse Faction token in addition to any other rewards."*

So the app must let the player pick the **circular colour** (which colour pile the circular enemy is
drawn from). The faction is *not* modelled here (it is the faction-token feature's concern, and the
picker only needs "this is possessed / it rewards a faction token").

## The four possessed modifiers (p.7)

A possessed token modifies the circular enemy by any combination of:

| Modifier | Icon | Rule |
|----------|------|------|
| **Armor** | dark breastplate badge, `−1 / +1 / +2` | The enemy's Armor value is modified by the amount shown. |
| **Attack (topmost)** | split fist / fire-ice badge, `−1 / +1` | The enemy's Attack value **of its topmost attack** (if it has multiple attacks) is modified by the amount shown. **If the topmost attack is a Summon, the *summoned* enemy's topmost Attack is modified instead** (applied when the token is summoned). |
| **Psychic Attack** | pink brain badge, `1 / 2 / 3 / 4` | The enemy **additionally** has a Psychic Attack of the value shown. This is a *new* attack, not a modification of an existing one. It does **not** benefit from any offensive abilities the enemy has (p.7–8). |
| **Fame** | red banner badge, `−1 / +1` | The enemy's Fame value is modified by the amount shown. |

Any subset can be present on one token (e.g. Possessed 04 modifies Armor **and** adds a Psychic
Attack). A missing icon means that modifier is `0` (or, for Psychic Attack, absent).

### The Enemy Picker shows sums, never deltas

The picker combines the possessed token with the circular enemy and displays the **summed** Armor /
topmost Attack / Fame (plus the Psychic Attack, if any) — never the deltas alongside the sums, so the
player is never left doing arithmetic. The deltas remain visible only as the possessed token's own
printed art (superimposed behind the circular token), exactly as the cardboard looks. See
`docs/context-enemy-picker.md`'s **Possessed Enemy** entry.

### Summon interaction (worked)

If the circular enemy's topmost attack is a **Summon** (e.g. an Orc Summoner, whose only attack is
`Summon → Brown`), the Attack delta has no numeric attack on the summoner to modify, so per p.7 it
applies to the **summoned** token's topmost attack, at the moment that token is drawn. The Enemy
Picker implements this: the summoned child looks up its summoner's possessed Attack delta and shows
`base + delta` for its topmost attack. Armor / Fame / Psychic modifiers still belong to the summoner
(the possessed enemy you fight), not the summoned token.

## Psychic Attack and Block (p.8)

The picker **narrates, it does not simulate combat** ([ADR-0004]/[ADR-0006]), so it only *displays*
the Psychic Attack value; the block rules below are reference context (surfaced in the ability info
window), not logic the app computes:

- A Psychic Attack is **not** a physical attack and has **no element** (never Physical/Ice/Fire/Cold
  Fire).
- It never benefits from the enemy's offensive abilities.
- Only Psychic Block (or Influence spent as Psychic Block) blocks it efficiently; all other Block is
  halved. Damage from an unblocked Psychic Attack is assigned using a Unit's *level* (not its Armor)
  as Armor, or the Hero's Armor for the Hero.

## Faction-token reward (reference only)

Every possessed enemy, when defeated, rewards **one Faction token** from the faction the triggering
text names (p.7). The Enemy Picker surfaces this as a faction-agnostic reminder on the composite
("Reward: a Faction token") — it does **not** own the held faction-token inventory, its Spend action,
or the end-of-game 1-Fame-each scoring; those are the faction-token feature's concern (rulebook p.6;
[ADR-0006](../adr/0006-enemy-picker-owns-pile-state-but-models-no-map.md)'s documented exception).

## Pile mechanics

*"Just as with other enemy tokens, at the start of the game, shuffle the possessed enemy tokens face
down and stack them in a pile. During the game, if you run out of tokens, reshuffle the discarded
ones and create a new face down pile. If all possessed enemy tokens are in use, do not place a
possessed enemy token with a possessed enemy."* (p.7)

The possessed pile therefore behaves like every other **Token Pile**: 12 tokens, drawn face-down,
**Replenished** from its discard when emptied. The one physical-only clause — *"if all possessed
enemy tokens are in use, do not place a possessed enemy token"* — is about the cardboard scarcity of
having several possessed enemies standing on the board at once in a multiplayer game. It is **moot in
the Enemy Picker**, which models no board and discards a drawn token immediately (eager Replenish,
issue #231): the pile is never "all in use", so a possessed draw always yields a possessed token.

## The 12 possessed tokens

9 distinct types across 12 tokens. `Armor` / `Attack` / `Fame` columns are the deltas the token
applies; `Psychic` is the added Psychic Attack (— = none). Ids are `possessed_01`…`possessed_09` (the
tokens carry no official individual names — the rulebook and mod both number them).

| id | Copies | Armor Δ | Attack Δ (topmost) | Fame Δ | Psychic |
|----|:------:|:-------:|:------------------:|:------:|:-------:|
| possessed_01 | 1 | — | — | +1 | 4 |
| possessed_02 | 1 | — | +1 | +1 | 3 |
| possessed_03 | 3 | — | — | 0 | 1 |
| possessed_04 | 1 | +2 | — | +1 | 2 |
| possessed_05 | 1 | +1 | — | +1 | 3 |
| possessed_06 | 1 | +1 | — | 0 | — |
| possessed_07 | 2 | −1 | — | −1 | — |
| possessed_08 | 1 | — | −1 | −1 | — |
| possessed_09 | 1 | — | +1 | 0 | — |

(Copies sum to 12. Every token additionally rewards a Faction token on defeat — not a column since
it is universal.)

## Modelling notes

- A possessed token is **not** an `EnemyToken` (it prints no standalone Armor/Attack/Fame). It is a
  new `PossessedToken(id, armorDelta, attackDelta, fameDelta, psychicAttack: Int?, copies)` in its
  own `possessed-tokens.json` catalogue — the same sibling-type-with-its-own-JSON pattern
  `RuinToken` established ([ADR-0007](../adr/0007-token-catalogue-as-json-in-domain-resources.md)).
- The composite is recorded as **one** `DrawLogEntry`: its `tokenId` is the circular enemy and a new
  nullable `possessedTokenId` names the possessed token. One row = one enemy, defeated with one tap.
- A combine helper produces the summed display values; the deltas are never shown beside the sums.
- `Expansion.APOCALYPSE_DRAGON` gates the possessed pile into the **Token Set**, and a new
  `TokenPileId.POSSESSED` names it.

## Out of scope here

Faction-token inventory/Spend/scoring (separate work; rulebook p.6), the AD scenarios that *use*
possessed enemies (all deferred — see `docs/rules/against-the-apocalypse.md`,
`against-the-dragon.md`, `apocalypse-is-here.md`), Coral the new Hero, Quest cards, and the new map
features (Oasis/Ziggurat/Pyramid). Only the possessed-enemy token mechanics needed by the Enemy
Picker are captured above.
