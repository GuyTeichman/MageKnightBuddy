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
mod, per the agreed sourcing plan).

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

## Other piles

The grey / violet / brown / red / white enemy piles and the ruin pile, plus the Lost Legion,
Shades of Tezla, and Apocalypse Dragon tokens, are not transcribed yet — the green pile is the
first vertical slice (issue #178). The same "Enemy Tokens List" reference sheet covers every base
and Lost Legion token and will be the source for the rest.

## Follow-up work (issue #178)

Tracked as GitHub sub-issues of #178:

- Remaining base-game piles — grey/violet/brown/red/white + ruins (#187)
- Lost Legion & Shades of Tezla tokens (#188)
- Apocalypse Dragon — possessed enemies & faction tokens (#189); faction tokens → Score Calculator (#190)
- Explicit Summon Draw action (#191)
- Multi-pile simultaneous draw & large-batch display (#192)
- Credits screen + art licensing review (#193)
- Bug: destructive DB migration crash-loop on upgrade (#194)
- Chore: refresh CLAUDE.md's stale scope line (#195)

Design context lives in `CONTEXT.md` (Enemy Picker glossary), [ADR-0006](../adr/0006-enemy-picker-owns-pile-state-but-models-no-map.md),
[ADR-0007](../adr/0007-token-catalogue-as-json-in-domain-resources.md), and
`docs/design/architecture.md`. The art/data extraction method is in project memory
(`tts_mod_asset_extraction.md`).
