# Enemy token art

Round token-face art for the Enemy Picker (issue #178), one JPEG per token named after its
`EnemyToken.id` in the catalogue (`domain/src/main/resources/enemy-tokens.json`). The UI loads
`enemy-tokens/<id>.jpg` from assets and clips it to a circle; a token with no file here falls back
to a text disc (`EnemyTokenFace` in `app/.../ui/enemypicker/EnemyTokenArt.kt`). Assets, not
`res/drawable`, so the set can grow token-by-token keyed by the catalogue's string id — see
[ADR-0007](../../../../../../docs/adr/0007-token-catalogue-as-json-in-domain-resources.md).

## Present so far

All 6 base-game enemy piles (issue #178's green slice plus issue #187's grey/violet/brown/red/white):
`green_*` (Prowlers, Diggers, Cursed Hags, Wolf Riders, Ironclads, Orc Summoners), `grey_*`
(Crossbowmen, Guardsmen, Swordsmen, Golems), `violet_*` (Monks, Illusionists, Ice Mages, Ice Golems,
Fire Mages, Fire Golems), `red_*` (Swamp Dragon, Fire Dragon, Ice Dragon, High Dragon), `brown_*`
(Minotaur, Gargoyle, Medusa, Crypt Worm, Werewolf), `white_*` (Freezers, Gunners, Altem Guardsmen,
Altem Mages) — ids match `enemy-tokens.json` exactly. Ruin tokens (`RuinToken`/`ruin-tokens.json`)
are hexagonal and have no art here yet - not wired into the Enemy Picker's draw flow yet either
(tracked as issue #201). The Lost Legion/Shades of Tezla/Apocalypse Dragon expansions' tokens
follow as those are transcribed.

## Source & provenance

Cropped from the Tabletop Simulator Workshop mod "Mage Knight Plus (Highly Scripted)" (Workshop ID
`1721301081`) — the same mod the Knight card/shield art came from (see
`app/src/main/assets/knight-cards/README.md`), specifically the individual token faces in each
pile's own bag (e.g. "Keep Garrisons" for grey, "Mage Tower Garrisons" for violet, "Draconum" for
red, "Dungeon Monsters" for brown, "City Garrisons" for white). Each token's identity was
cross-checked against the mod's "Enemy Tokens List" reference sheet before cropping, the same
two-source method issue #178 used for green. Each image was re-encoded to 512×512 JPEG (quality 85)
to keep the repo small. Apocalypse Dragon tokens are **not** in that mod (it predates the
expansion) and will be sourced from the AD rulebook / physical components, as Coral's art was.

## Licensing

All art is official WizKids / Vlaada Chvátil art, here via a fan-made Tabletop Simulator mod
(community reproduction, not the publisher's own files). **Re-flag and re-evaluate before any public
release or redistribution** — note the app already attaches a debug APK to GitHub Releases, so this
is not strictly "non-distributed"; a proper credits screen and licensing review are tracked as
follow-ups to issue #178.
