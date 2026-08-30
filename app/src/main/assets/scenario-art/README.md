# Scenario background art

Background art for each scenario (`domain/.../Scenario.kt`), shown by `ScenarioArt`
(`app/.../ui/scenarioart/ScenarioArt.kt`) behind the Score Calculator's scenario picker and the
Scoreboard cards (issue #171).

**Empty at the moment by design.** Issue #285 shipped the code foundation only - `ScenarioArt`
renders a bronze placeholder naming the scenario for anything without art, which is every scenario
until the images land. Issue #288 fills this folder in image-by-image.

## Adding an image (issue #288)

1. Drop `<slug>.jpg` here (a landscape image that vibes with the scenario's theme).
2. Add a matching row to `ScenarioArtCatalogue.entries` (`app/.../ui/scenarioart/`): the
   `scenarioId` (a `Scenario.id`, e.g. `solo_conquest`), the `filename`, and the attribution
   (`author`, `sourceUrl`, `license`).
3. `ScenarioArtAssetsTest` then enforces that every catalogue file is bundled here and no bundled
   `.jpg` is left unreferenced, and `ScenarioArtCatalogueTest` that every row maps to a real scenario.

## Licensing

Only **CC0 / public-domain** or **CC-BY** images are allowed here (decided in the issue #171
planning grill). CC-BY *requires* visible attribution, so issue #288 surfaces the CC-BY credits in
Settings > Credits; CC0 credits are a courtesy. Verify each image's licence at its `sourceUrl`
before adding it. This is separate from the bundled-official-art stance (ADR-0010) that covers the
Knight/token art - scenario art is sourced free-to-use precisely to avoid that debt.
