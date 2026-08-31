# Scenario background art

Background art for each scenario (`domain/.../Scenario.kt`), shown by `ScenarioArt`
(`app/.../ui/scenarioart/ScenarioArt.kt`) behind the Score Calculator's scenario picker and the
Scoreboard cards (issue #171).

Filled by issue #288: all 15 scenarios are illustrated. Each file is `<scenarioId>.jpg`, matched 1:1
to a row in `ScenarioArtCatalogue.entries`. `ScenarioArt` still degrades to a bronze placeholder for
any *future* scenario added without art, so a new scenario renders before its image lands.

## Adding / replacing an image

1. Drop `<scenarioId>.jpg` here — a landscape image (portrait sources are cropped to landscape). At
   render the app center-crops (`ContentScale.Crop`) and lays a ~35% black scrim over it, so pick art
   whose subject sits near the middle and stays legible when darkened. Downscale to ~1400px wide.
2. Add / edit the matching row in `ScenarioArtCatalogue.entries` (`app/.../ui/scenarioart/`): the
   `scenarioId` (a `Scenario.id`, e.g. `solo_conquest`), the `filename`, the `workTitle`, and the
   attribution (`author`, `sourceUrl`, `license`).
3. `ScenarioArtAssetsTest` enforces that every catalogue file is bundled here and no bundled `.jpg`
   is left unreferenced; `ScenarioArtCatalogueTest` checks that every row maps to a real scenario and
   has no blank fields.

## Licensing

Scenario art is sourced **free-to-use** precisely to avoid adding to the bundled-official-art debt
(ADR-0010) — separate from the Knight/token/site art that ADR covers. Only these licences are used:

- **Public domain** / **CC0** — the whole shipped slate. Attribution isn't legally required, so the
  credits (Settings → Credits → *Scenario backgrounds*) are a courtesy.
- **CC-BY** — allowed; would *require* the same visible attribution, already wired.

Non-Commercial (NC) licences and paid royalty-free stock are **excluded**: the app is open-source, so
downstream redistribution can't be controlled, and both of those forbid exactly that. CC-BY-SA is
avoided by default (cropping/scrim makes an adaptation that must carry ShareAlike).

**Two knowing exceptions** reuse official © WizKids *Mage Knight* art — *Against the Dragon*
(Apocalypse Dragon) and *Volkare's Return* (Lost Legion) — where no clean image reads as
unambiguously as the real antagonist. These are marked `license = OFFICIAL` and fall under ADR-0010's
risk-accepted stance (attribution + non-affiliation disclaimer in Settings → Credits), not the clean
scope above. Verify each image's licence at its `sourceUrl` before adding it.

## Per-image attribution

Public-domain / CC0 unless noted. Sources are Wikimedia Commons (which states each licence) except
where noted; the two `OFFICIAL` rows cite the WizKids landing page rather than the fan-wiki/CDN the
files were grabbed from.

| Scenario | Artwork | Artist | Licence |
| --- | --- | --- | --- |
| Conquest | Ruins of the Trakai Island Castle at Sunset (1866) | Józef Marszewski | Public domain |
| First Reconnaissance | Wanderer above the Sea of Fog (1818) | Caspar David Friedrich | Public domain |
| For the Council | The Magic Circle (1886) | John William Waterhouse | Public domain |
| The Hidden Valley | Autumn — On the Hudson River (1860) | Jasper Francis Cropsey | Public domain |
| The Realm of the Dead | Crusaders Attacking the Castle of Punia | Wincenty Dmochowski | Public domain |
| Against the Dragon | Apocalypse Dragon expansion art | WizKids (art by Gong Studios) | **© WizKids** (ADR-0010) |
| Against the Horsemen | The Spirit of War (1851) | Jasper Francis Cropsey | CC0 (NGA) |
| Apocalypse is Here | The Great Day of His Wrath (1851–1853) | John Martin | Public domain |
| The Fractured Lands | Aurora Borealis (1865) | Frederic Edwin Church | Public domain |
| Life and Death | Cotopaxi (1862) | Frederic Edwin Church | Public domain |
| The Lost Relic | The Course of Empire — Desolation (1836) | Thomas Cole | Public domain |
| Against the Apocalypse | A Capriccio with the Pyramid of Cestius (c. 1800) | Hubert Robert | Public domain (via Artvee) |
| Solo Conquest Challenge | Dolbadern Castle (1800) | J. M. W. Turner | Public domain |
| Volkare's Quest | Illustration for *The Boy's King Arthur* (1917) | N. C. Wyeth | Public domain (via Artvee) |
| Volkare's Return | Mage Knight: Lost Legion expansion art | WizKids | **© WizKids** (ADR-0010) |

The exact `sourceUrl` for each is the machine-readable record in `ScenarioArtCatalogue.entries`.
