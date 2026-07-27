# Enemy token art

Round token-face art for the Enemy Picker (issue #178), one JPEG per token named after its
`EnemyToken.id` in the catalogue (`domain/src/main/resources/enemy-tokens.json`). The UI loads
`enemy-tokens/<id>.jpg` from assets and clips it to a circle; a token with no file here falls back
to a text disc (`EnemyTokenFace` in `app/.../ui/enemypicker/EnemyTokenArt.kt`). Assets, not
`res/drawable`, so the set can grow token-by-token keyed by the catalogue's string id — see
[ADR-0007](../../../../../../docs/adr/0007-token-catalogue-as-json-in-domain-resources.md).

## Present so far

The 6 base-game **green** Marauding Orcs only (issue #178's first slice): `green_prowlers`,
`green_diggers`, `green_cursed_hags`, `green_wolf_riders`, `green_ironclads`, `green_orc_summoners`.
The other piles' art follows as those tokens are transcribed.

## Source & provenance

Cropped from the Tabletop Simulator Workshop mod "Mage Knight Plus (Highly Scripted)" (Workshop ID
`1721301081`) — the same mod the Knight card/shield art came from (see
`app/src/main/assets/knight-cards/README.md`), specifically the individual token faces in its
"Marauding Orcs" bag. Each was re-encoded to 512×512 JPEG (quality 85) to keep the repo small.
Apocalypse Dragon tokens are **not** in that mod (it predates the expansion) and will be sourced
from the AD rulebook / physical components, as Coral's art was.

## Licensing

All art is official WizKids / Vlaada Chvátil art, here via a fan-made Tabletop Simulator mod
(community reproduction, not the publisher's own files). **Re-flag and re-evaluate before any public
release or redistribution** — note the app already attaches a debug APK to GitHub Releases, so this
is not strictly "non-distributed"; a proper credits screen and licensing review are tracked as
follow-ups to issue #178.
