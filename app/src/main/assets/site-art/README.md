# Site art

Illustration art for the **Sites** tab (issue #235), one JPEG per `Site.id` in the catalogue
(`domain/src/main/resources/sites.json`). The UI loads `site-art/<Site.art>` from assets and shows it
in the list row thumbnail and the detail header; a site whose `art` is still `null` falls back to a
category-hinting placeholder icon (`SiteArt.kt` in `app/.../ui/sites/`). Assets, not `res/drawable`,
so the set grows site-by-site keyed by the catalogue's `art` filename — see
[ADR-0007](../../../../../../docs/adr/0007-token-catalogue-as-json-in-domain-resources.md).

Two tests keep this folder honest: the domain `SiteCatalogueTest` checks the `art` filenames are
unique/non-blank, and `SiteArtAssetsTest` (Robolectric, so it can read the merged assets) checks
every referenced file is actually bundled and that no bundled image is unreferenced.

## Present so far

**20 of the 21** sites. Only `graveyard_tile` has no image: it shares the single physical
"Necropolis / Graveyard" **Site Description card** (one illustration) with `necropolis_tile`, so
there is no distinct Graveyard illustration to crop — it keeps the placeholder until one is sourced
(e.g. from the map tiles). Apocalypse Dragon sites are out of scope here (issue #238).

## Source & provenance

Each image is the **illustration cropped from that site's Site Description card** in the Tabletop
Simulator Workshop mod "Mage Knight Plus (Highly Scripted)" (Workshop ID `1721301081`) — the same mod
the enemy-token and Knight-card art came from (see `app/src/main/assets/enemy-tokens/README.md`). The
mod holds all the site-description cards as one deck rendered from a single 4×7 sprite-sheet; each
site's CardID indexes one cell, and the illustration was cropped out of that cell (the card's rules
text is rendered separately from `sites.json` as `SiteSection`s, so only the picture is kept).
Combined cards ("Crystal Mines & Magical Glade", "Deep Mines & Refugee Camp") were split into their
two per-site illustrations. Each crop was re-encoded to JPEG (quality 88) to keep the repo small.

## Licensing

All art is official WizKids / Vlaada Chvátil art, here via a fan-made Tabletop Simulator mod
(community reproduction, not the publisher's own files). **Re-flag and re-evaluate before any public
release or redistribution** — note the app already attaches a debug APK to GitHub Releases, so this
is not strictly "non-distributed"; a proper credits screen and licensing review are tracked as
follow-ups to issue #178.
