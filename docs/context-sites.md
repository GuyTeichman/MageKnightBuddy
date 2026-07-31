# Domain glossary — Sites

Terms for the Sites tab: an in-app, browsable reference of every map-feature the physical *Mage
Knight* **Site Description cards** / help cards describe (name + art + "what you can do here and
how"). See `CONTEXT.md` for the other glossary slices (Scoring, Dummy Player, Enemy Picker).

**Sites tab**:
The 5th bottom-nav tab (issue #177): a flat, alphabetical, searchable list of **Site**s, each opening
a full-screen detail with its art and rules. Purely a **reference** — unlike the Enemy Picker it owns
no real game state, models no map, and simulates nothing; its catalogue is static and the only thing
it will ever persist is the player's **Site** favorites (Sub-issue D / issue #236). The tab itself is
Sub-issue B (issue #234); this doc and the domain catalogue (Sub-issue A / issue #233) are its data
foundation.
_Avoid_: Site tracker, map tracker (it tracks nothing about a live game); "cards" as a synonym for
the tab (a **Site** models a card's *content*, not a stack you draw from)

**Site**:
One transcribed map-feature — a static record modeling a physical **Site Description card** (`Site`
in `domain/`, loaded from `sites.json` via the **Site Catalogue**). Carries an `id`, `name`,
**Site Category**, **Site Expansion**, an ordered list of **Site Section**s, and an optional `art`
filename. Covers three loosely-different kinds of thing the Quick Reference Sheet groups together:
true sites (Keep, Village, Dungeon, …), the two rampaging enemies (Marauding Orcs, Draconum), the
Walls terrain feature, and the Shades of Tezla location tiles — see `docs/rules/sites.md` for the
full transcription with rulebook citations. The **Enemy Token Abilities** reference on the Quick
Reference Sheet is deliberately *not* a Site: that lives in the Enemy Picker
(`docs/rules/enemy-tokens.md`), and duplicating it would invite drift.
_Avoid_: Location (too broad — a rampaging enemy or a wall isn't a location you occupy); confusing a
`Site` with the Enemy Picker's `EnemyToken` (different domain, different tab)

**Site Section**:
One titled rule paragraph of a **Site** (`SiteSection` = `heading` + `body`) — e.g. a Keep's "When
Revealed", "Unconquered Keep", "Other Players' Keeps", "Your Keep". Modeled as an *ordered list*
rather than one text blob so the printed order is preserved and the tab's search matches across every
heading and body (so searching "conquered" surfaces every **Site** with a *While Conquered* section).
_Avoid_: Field, entry (reserved for the Score Calculator's inputs and the Draw Log's rows)

**Site Category**:
An **app-side grouping** of **Site**s (`SiteCategory`: `RAMPAGING_ENEMY`, `FORTIFIED_SITE`,
`ADVENTURE_SITE`, `SETTLEMENT`, `RESOURCE_SITE`, `SPECIAL_TILE`, `TERRAIN_FEATURE`) — *not* an
official rulebook taxonomy, but a set of buckets chosen to power the future sort/group/filter controls
(Sub-issue E / issue #237). Carried on every **Site** from the start (even though v1 renders a flat
alphabetical list) so adding those controls needs no data migration. The definitions and each
**Site**'s assignment — which are judgment calls open to revision — live in `docs/rules/sites.md`'s
"Category taxonomy" table.
_Avoid_: treating a category as rules-authoritative (it's an app grouping); "type" (ambiguous)

**Site Expansion**:
Which *Mage Knight* product a **Site** first appeared in (`SiteExpansion`: `BASE`, `LOST_LEGION`,
`SHADES_OF_TEZLA`). Deliberately a **separate enum from the Enemy Picker's `Expansion`**: sites never
split Shades of Tezla by faction (`Expansion` does, because *tokens* come in two factions), no site
comes from Krang (its only addition is the Flip Back ability), and coupling the Sites tab to the
**Token Set**'s meaning would be wrong. Apocalypse Dragon sites — and the matching enum value — are
deferred to Sub-issue F (issue #238). Verified per-entry against the rulebooks in `docs/rules/sites.md`.
_Avoid_: reusing `Expansion` (`docs/context-enemy-picker.md`) for sites — different axis, different
values

**Site Catalogue**:
The full set of transcribed **Site**s, loaded once from `sites.json` in `domain/` resources
(`SiteCatalogue`, mirroring the Enemy Picker's `TokenCatalogue`) — the catalogue-as-JSON-in-domain
pattern of [ADR-0007](adr/0007-token-catalogue-as-json-in-domain-resources.md), including its
mandatory `SiteCatalogueTest` that validates the whole file on every `make test`. Loading lives in
`domain` (not `data`) precisely so that plain-JVM test can run without an emulator. Art is *not* stored
here: images are app assets in `app/src/main/assets/site-art/`, referenced by each **Site**'s `id`
(Sub-issue C / issue #235), so `domain` stays free of anything it cannot test.
_Avoid_: Token Catalogue (that's the Enemy Picker's, a sibling but separate catalogue)
