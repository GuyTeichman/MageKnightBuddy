# Faction reward token art

Face art for the Enemy Picker's four **faction reward-token** piles (issue #252), one JPEG per token
named after its `FactionRewardToken.id` in the catalogue
(`domain/src/main/resources/faction-reward-tokens.json`). The UI loads
`faction-reward-tokens/<id>.jpg` from assets and draws it as a **rounded-rectangle tile** — *not*
circle-clipped like enemy faces — because a reward token's face is a square icon tile whose art spans
the full width (e.g. Healing Herbs shows a hand / pencil / map across the whole tile), so a circle
clip would cut the edge icons off. A token with no file here falls back to a text tile
(`FactionRewardTokenFace` in `app/.../ui/enemypicker/FactionRewardTokenArt.kt`). Assets, not
`res/drawable`, so the set is keyed by the catalogue's string id — see
[ADR-0007](../../../../../../docs/adr/0007-token-catalogue-as-json-in-domain-resources.md).

## Pile back art

The four **pile backs** are round faction emblems (a blue leaf = Elementalist, a horned skull = Dark
Crusader, etc.), so they live with the other round pile backs in `../enemy-tokens/backs/` keyed by the
lowercase `TokenPileId` name (`elementalist_rewards.jpg`, `dark_crusader_rewards.jpg`,
`apocalypse_cult_rewards.jpg`, `council_of_void_rewards.jpg`) and are loaded + circle-clipped by the
existing `PileBackFace` — no faction-specific back code needed.

## Present

All 24 tokens (6 types × 2 copies, but art is per *type* so 6 files per pile): the two Shades of Tezla
factions (`reward_elementalist_*`, `reward_dark_crusader_*`) and the two Apocalypse Dragon factions
(`reward_apocalypse_cult_*`, `reward_council_of_void_*`) — ids match `faction-reward-tokens.json`
exactly.

## Source & provenance

Cropped from the Tabletop Simulator Workshop mod "Mage Knight Plus (Highly Scripted)" (Workshop ID
`1721301081`) — the same mod the enemy/ruin token art came from (see `../enemy-tokens/README.md`).
Each faction's `… Reward Tokens` bag holds the token tiles as flat `Custom_Tile`s: the face is the
tile's `CustomImage.ImageURL`, the shared per-faction back its `ImageSecondaryURL`. The English
effect text was read straight from each tile's `Description` field (`{en}…` segment), so the data and
art come from one machine-readable source. Each image re-encoded to 512×512 JPEG (quality 85).

**Provenance:** an earlier project assumption that Apocalypse Dragon wasn't in this mod was wrong — the
mod is actively maintained and its Apocalypse Cult / Council of the Void content is the **official**
expansion's, not homebrew. As with every token pile in this project, the data and art are still
**pending author verification against physical components** before being treated as final; the Shades
of Tezla tokens additionally sit in the same bag as the already-checked SoT enemy tokens (#188).

## Licensing

Same status as the enemy token art (`../enemy-tokens/README.md`): official WizKids / Vlaada Chvátil
art via a fan-made TTS mod. **Re-flag and re-evaluate before any public release or redistribution** —
credits screen + licensing review tracked as follow-ups to issue #178 (#193).
