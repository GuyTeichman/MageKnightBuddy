# Site art

Icon art for the **Sites** tab (issue #235), one JPEG per `Site.id` in the catalogue
(`domain/src/main/resources/sites.json`). The UI loads `site-art/<Site.art>` from assets and shows it
in the list-row thumbnail and, at a modest framed size, in the detail header (`SiteArt.kt` in
`app/.../ui/sites/`). A site whose `art` is `null` falls back to a category-hinting placeholder icon;
every site currently has art, so the placeholder is only a safety net now. Assets, not `res/drawable`,
so the set is keyed by the catalogue's `art` filename and grows by adding a file, not code — see
[ADR-0007](../../../../../../docs/adr/0007-token-catalogue-as-json-in-domain-resources.md).

Two tests keep this folder honest: the domain `SiteCatalogueTest` checks the `art` filenames are
unique/non-blank, and `SiteArtAssetsTest` (Robolectric, so it can read the merged assets) checks
every referenced file is actually bundled and that no bundled image is unreferenced.

## Present so far

**All 21** in-scope sites, including `graveyard_tile` (which had no distinct art before). Apocalypse
Dragon sites are out of scope here (issue #238).

## Source & provenance

Each image is the **site's reference icon, extracted from the official Mage Knight _Quick Reference
Sheet_** (`Mage-Knight-Board-Game-Quick-Reference-Sheet.pdf` at the repo root, © 2015 WizKids/NECA —
the publisher's own player aid, the same document `docs/rules/sites.md`'s wording tracks). That PDF is
an Adobe InDesign export in which every placed graphic is an embedded 300 ppi JPEG, so each site's
icon comes out as its own image — no page-rasterizing or hand-cropping. Pipeline (reproducible from
the tracked PDF):

1. `pdfimages -png` pulls every embedded image (with proper CMYK→RGB conversion). The site icons are
   the ~100–151 px vignettes, one beside each site heading across the sheet's two pages.
2. Each icon is centred on a square canvas filled with its own cream background colour (so every tile
   is a uniform, seamless cream square) and re-encoded to a 300×300 JPEG (quality 90).

This replaces an earlier approach that cropped the site illustrations out of a Tabletop Simulator
mod's Site Description cards: those crops carried the card's parchment texture and stray rules-icon
bleed, framed each site inconsistently, and left `graveyard_tile` without distinct art. The
quick-reference icons are purpose-made, uniform in style and framing, and cover all 21 sites in one
pass. They are small (~150 px source), which is why the detail header shows each at a modest framed
size rather than full-bleed.

## Licensing

All art is official WizKids / Vlaada Chvátil art. Unlike the earlier fan-mod crops, this comes from
WizKids' **own** published PDF (a community-distributed player aid, not a third-party reproduction) —
arguably cleaner provenance, but the same caveat applies: **re-flag and re-evaluate before any public
release or redistribution.** The app already attaches a debug APK to GitHub Releases, so this is not
strictly "non-distributed"; a proper credits screen and licensing review are tracked as follow-ups to
issue #178.
