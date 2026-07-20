# Knight identity-card art

Full Hero identity-card art for all 8 Knights this app models (`domain/.../Knight.kt`). Not wired into any screen yet - staged here per issue #70 ahead of a follow-up issue that will decide where/how they're used.

## Source and provenance

- **Tovak, Goldyx, Norowas, Wolfhawk, Arythea, Krang, Braevalar**: cropped from the Tabletop Simulator Workshop mod "Mage Knight Plus (Highly Scripted)" (Workshop ID `1721301081`), specifically a shared 4000x2800px sprite sheet the mod uses for these 7 Knights' Hero cards. See issue #66's closing comment for the exact source file and crop coordinates.
- **Coral**: cropped from `Mage-Knight-The-Apocalypse-Dragon---Rulebook-WEB.pdf` (already in the repo root), p.5 "Coral - New Playable Hero". She isn't in the TTS mod above (it predates the Apocalypse Dragon expansion). Native PDF resolution is low (~90x120px for the card art) - this is the rulebook's own resolution ceiling, not a cropping artifact; see issue #70's closing comment.
- All 8 re-encoded to JPEG (quality 90) from the original crops to keep repo size reasonable - originals were uncompressed PNGs several MB each.

## Licensing

All art is official WizKids/Vlaada Chvátil art. The 7 non-Coral cards come from a fan-made Tabletop Simulator mod (community-hosted reproduction, not the publisher's own files); Coral's comes from a rulebook PDF this project already owns. Fine for this project's current personal, non-distributed status (see `CONTEXT.md` / project memory) - re-flag and re-evaluate before any public release.
