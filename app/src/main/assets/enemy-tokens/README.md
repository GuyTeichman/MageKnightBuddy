# Enemy token art

Round token-face art for the Enemy Picker (issue #178), one JPEG per token named after its
`EnemyToken.id` in the catalogue (`domain/src/main/resources/enemy-tokens.json`). The UI loads
`enemy-tokens/<id>.jpg` from assets and clips it to a circle; a token with no file here falls back
to a text disc (`EnemyTokenFace` in `app/.../ui/enemypicker/EnemyTokenArt.kt`). Assets, not
`res/drawable`, so the set can grow token-by-token keyed by the catalogue's string id — see
[ADR-0007](../../../../../docs/adr/0007-token-catalogue-as-json-in-domain-resources.md).

## Pile back art (`backs/`)

Face-down **pile back** art (issue #198), one JPEG per `TokenPileId` named after its lowercase enum
name (`backs/<pileid>.jpg`, e.g. `backs/green.jpg`) — not the catalogue's per-token id, since a pile
back is one-per-color rather than one-per-token. `PileBackFace` in `EnemyTokenArt.kt` loads it the
same way `EnemyTokenFace` loads a token face (clipped to a circle, text-disc fallback if missing) and
`EnemyPickerScreen.kt`'s `PileCard` shows it as a tap-to-draw-1 shortcut. All 6 base-game piles
(green/grey/violet/brown/red/white) plus Ruin (`ruin.png`, its hexagon alpha baked in the same way
as the individual ruin faces below) are present. The **Possessed** pile (Apocalypse Dragon) now has its
own back too (`possessed.png`, issue #277) — a transparent-PNG **starfield D-tile** whose non-rectangular
silhouette is the alpha, drawn with **no** circle clip (like `ruin.png`); unlike the square ruin hexagon,
`PileBackFace` renders the possessed D at the pile's full height and proportionally wider, so it reads the
same height as the round discs. The four **faction reward** piles' round emblem backs also live here
(`elementalist_rewards.jpg`, `dark_crusader_rewards.jpg`, `apocalypse_cult_rewards.jpg`,
`council_of_void_rewards.jpg`, issue #252) since they're per-`TokenPileId` round backs like the enemy
piles' — their *face* tiles are the exception, kept in `../faction-reward-tokens/` (see that README).

## Present so far

All 6 base-game enemy piles (issue #178's green slice plus issue #187's grey/violet/brown/red/white):
`green_*` (Prowlers, Diggers, Cursed Hags, Wolf Riders, Ironclads, Orc Summoners), `grey_*`
(Crossbowmen, Guardsmen, Swordsmen, Golems), `violet_*` (Monks, Illusionists, Ice Mages, Ice Golems,
Fire Mages, Fire Golems), `red_*` (Swamp Dragon, Fire Dragon, Ice Dragon, High Dragon), `brown_*`
(Minotaur, Gargoyle, Medusa, Crypt Worm, Werewolf), `white_*` (Freezers, Gunners, Altem Guardsmen,
Altem Mages) — ids match `enemy-tokens.json` exactly. Ruin tokens (`RuinToken`/`ruin-tokens.json`)
are hexagonal; their individual faces (`ruin_*.png`, transparent-PNG hexagon silhouettes) and their
pile back (`backs/ruin.png`) are both present, and the pile is fully wired into the Enemy Picker's
draw flow — altar payment / enemies-with-treasure draw (`RuinZoomDialog`, `RuinInfoDialog`, issue
#201, closed).

All 22 **Lost Legion** enemy tokens (issue #188) are also present, mixed into the same six colour
piles (they share the base tokens' backs): green `orc_skirmishers`/`orc_trackers`/`orc_war_beasts`/
`orc_stonethrower`; grey `thugs`/`shocktroops`/`heroes_fortified`/`heroes_swift`/`heroes_fire`/
`heroes_ice`; violet `sorcerers`/`magic_familiars`; brown `manticore`/`hydra`/`shadow`; red
`lava_dragon`/`dragon_summoner`/`storm_dragon`; white `fire_catapult`/`ice_catapult`/
`delphana_masters`/`grim_legionnaires`. The **Shades of Tezla** enemy tokens are present too — all 22
(11 Elementalist + 11 Dark Crusader), mixed into the same colour piles — as are the 3 **Lost Legion
ruin** tokens and the 9 Apocalypse Dragon **possessed** token faces (`possessed_0*.png`, issue #189) -
now **crescent**-shaped (issue #289): each is masked to the possessed token's own 3D mesh silhouette
(from the "Possessed Tokens" bag), so the circular enemy nests into the bite in `PossessedEnemyFace`
instead of overlapping a square, with the delta icons kept on the left.
The enemy-token catalogue is therefore complete (base + Lost Legion + Shades of Tezla, 73 faces), and
with the Possessed pile *back* (`backs/possessed.png`, issue #277) now added, every pile in the Enemy
Picker has its back art (see `docs/rules/enemy-tokens.md`).

## Source & provenance

Cropped from the Tabletop Simulator Workshop mod "Mage Knight Plus (Highly Scripted)" (Workshop ID
`1721301081`) — the same mod the Knight card/shield art came from (see
`app/src/main/assets/knight-cards/README.md`), specifically the individual token faces in each
pile's own bag (e.g. "Keep Garrisons" for grey, "Mage Tower Garrisons" for violet, "Draconum" for
red, "Dungeon Monsters" for brown, "City Garrisons" for white). Each token's identity was
cross-checked against the mod's "Enemy Tokens List" reference sheet before cropping, the same
two-source method issue #178 used for green. Each image was re-encoded to 512×512 JPEG (quality 85)
to keep the repo small. Apocalypse Dragon **possessed** token faces are **not** in that mod (it
predates the expansion); they were sourced separately (rulebook p.7 superimposition art, issue #189).

The possessed pile-*back* (`backs/possessed.png`, issue #277) also came from outside the base mod: from a
fan-made Apocalypse Dragon add-on — the **"Possessed Tokens"** `Custom_Model_Bag` present in the project
author's own Tabletop Simulator saved game (`Saves/TS_AutoSave.json`). Unlike the round enemy backs (a
flat `CustomImage`), each possessed token is a 3D `Custom_Model`, so its art lives in `CustomMesh.DiffuseURL`
(the starfield texture) shaped by `CustomMesh.MeshURL` (the token's outline). The bag's *own* mesh + diffuse
are the face-down pile's appearance — a starfield **D-tile** — which was downloaded from Steam's CDN, its
white background keyed out to transparent, a **black edge border** baked on to match the round backs' ring,
and re-encoded to a compact transparent PNG (the D's own aspect) whose alpha *is* the tile silhouette.
`PileBackFace` then draws it at the pile's full height and proportionally wider, so it sits the same height
as the round backs.

The `backs/` pile-back art (issue #198) came from the same mod, one level up: every token object in
a pile's bag (e.g. "Marauding Orcs" for green) carries a `CustomImage.ImageSecondaryURL` for its
back face, and every token within one bag shares the exact same value — confirmed by parsing the
mod's own JSON (`...\Mods\Workshop\1721301081.json`) rather than assumed, since a mismatch would
mean the physical tokens don't actually share one uniform back per color. Same crop/re-encode
pipeline as the faces (512×512 JPEG, quality 85); no separate cross-check needed since the mod JSON
already gives a single unambiguous URL per pile.

## Licensing

All art is official WizKids / Vlaada Chvátil art, here via a fan-made Tabletop Simulator mod
(community reproduction, not the publisher's own files). The licensing posture — keep bundling,
mitigated by in-app attribution and a non-affiliation disclaimer — is decided in
[ADR-0010](../../../../../docs/adr/0010-bundled-official-art-licensing-stance.md) (issue #193);
revisit only if the project goes commercial or to an official app store.
