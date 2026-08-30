# Bundle official Mage Knight art, mitigated by in-app attribution and a non-affiliation disclaimer

**Status:** Accepted (issue #193).

MageKnightBuddy reproduces official *Mage Knight* content: Hero identity-card art, enemy- and ruin-token faces, site icons, faction-reward tiles, and rules text (help citations, `docs/rules/*.md`). The art is © WizKids / Vlaada Chvátil, sourced mostly from a community Tabletop Simulator mod and, for a few sets, from WizKids' own published PDFs; per-asset provenance lives in the four `app/src/main/assets/*/README.md` files.

Historically each of those READMEs carried the same caveat — roughly *"fine for this project's personal, non-distributed status; re-flag before any public release."* That framing was **false in practice**: `.github/workflows/publish.yml` builds a debug APK (`assembleDebug`) and attaches `app-debug.apk` to a GitHub Release on every `v*.*.*` tag, and v1.0.0 shipped 2026-07-24. The app is already distributed. The caveat also lived in four places at once, which is exactly how the stale "non-distributed" claim rotted unnoticed (see the retro note in `CLAUDE.md`). Issue #193 forced the choice: resolve the posture instead of perpetually deferring it, and record it in **one** canonical place.

## Decision

**Keep bundling the official art and rules text.** Mitigate — not clear — the licensing exposure with two in-app measures, both in the Settings *Credits* section:

- **Attribution.** Credit the game's designers, the publisher, and the artists (per modeled expansion), so the reproduced work is properly acknowledged rather than passed off.
- **A non-affiliation disclaimer.** State plainly that this is an unofficial, fan-made companion app, not affiliated with or endorsed by WizKids/NECA, and that *Mage Knight* and all related names, art, and rules text are © their respective owners.

Rationale: this is a non-commercial, single-author, **debug-signed** personal project, and the app *is* a visual token/card/site reference — stripping the official art would gut its purpose. Attribution plus a clear non-affiliation notice is the customary posture for fan companion tools of this kind. Removing or replacing the art with original/CC assets was considered and rejected as disproportionate to the actual risk of a personal project at this scale (it is tracked below as the revisit path, not the current action).

This is a **knowing risk-acceptance by the author, not a legal opinion or a grant of permission.**

## Revisit if

Re-open this decision — likely replacing bundled official art with original/CC/licensed assets, or seeking permission — if any of these change:

- The project becomes **commercial or monetized** beyond the existing "Buy me a coffee" tip link (which is a donation, not a sale of the art).
- It is published to an **official app store** (Google Play / App Store) or otherwise distributed as a signed release rather than a debug APK on a GitHub Release.
- The **rights-holder objects** or issues a takedown.

## Consequences

- **One source of truth.** The four `app/src/main/assets/*/README.md` licensing sections now point here instead of each restating (and re-drifting) the caveat. Their per-asset *provenance* notes stay; only the licensing/next-steps wording defers to this ADR.
- **The Settings *Credits* section gains** the attribution list (reordered so game creators and the publisher come first, the CC0 app-icon credits last) and the non-affiliation disclaimer. No assets are removed and no screen's visuals change.
- **The "review" tracked by #193 is now closed**, with an explicit, discoverable outcome and a defined trigger for reopening it — rather than an open-ended "re-evaluate someday" scattered across README files.
