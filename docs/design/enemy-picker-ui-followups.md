# Enemy Picker — PR #186 feedback & UI follow-ups

Working design doc capturing the author's feedback on PR #186 (issue #178, the green-pile
vertical slice) and the decisions reached while grilling them into a plan. Raw notes are
recorded verbatim first; the **Decisions** section is filled in as each is resolved, and the
**Scope** section records what lands in #186 vs. new issues.

> Status: **Decisions resolved & confirmed (2026-07-27).** The PR #186 slice (D1, D2 domain +
> log grouping, D5, D6 docs) is **implemented**; the net-new UI (D2 Defeat button, D3 grid, D4
> back-art) is filed as follow-up sub-issues of #178 — see the Scope section.

## Raw feedback (verbatim, 2026-07-27)

1. Special abilities are generally divided into two types: "offensive" and "defensive".
   "offensive" abilities apply to ALL of an enemy's attacks, not just one; so treat them
   similarly to how defensive abilities / resistances are treated.
2. "Vampiric" is an offensive ability.
3. *(For future reference, not a current mistake — needs documenting.)* When a summoner enemy
   summons a token to attack, that summoned token also applies its own offensive abilities to
   its attack(s).
4. In planning we agreed that when drawing more than 1 enemy we show an overview of them in a
   grid; clicking one opens the detail view (with the arrows to switch between enemies). Add
   that overview grid when `n_enemies > 1`.
5. Each enemy type/pile has a unique "back" art representing it. Import that art and display it
   in the pile's selector. Possibly make tapping the art a shortcut for "draw 1 enemy from this
   pile", in addition to the counter + button option.
6. The enemy detail screen should have a "discard/defeat enemy" button in addition to
   "close/minimize" — it automatically flags that enemy as no longer in play. Do something
   similar for the grid view (e.g. checkboxes next to the enemy overviews). Note: in most cases
   an enemy is revealed and then defeated — this should be reflected in the default values.
7. The "still in play" flag currently does nothing other than show a flag icon.
8. The enemy detail (zoom) screen lists the attack in the summary title AND again below in
   detail. Drop it from the title — just list armor and fame there, rest below.

## Decisions

### D1 — Ability model: offensive abilities are token-wide (notes 1, 2)

Offensive special abilities apply to the **whole enemy** (every attack it has), same as defensive
ones — per the Lost Legion "Multiple Attacks" rule. So they stop being per-attack.

- **Delete `AttackModifier`** and the `modifiers` field on `EnemyAttack`. `EnemyAttack` becomes
  `{ value, element, summons }` (element genuinely *is* per-attack — a multi-attack enemy can mix
  elements, so it stays).
- **Two token-level enums**, split by type so an ability can't land in the wrong bucket:
  - `DefensiveAbility { FORTIFIED, ELUSIVE, ARCANE_IMMUNITY }`
  - `OffensiveAbility { SWIFT, BRUTAL, POISON, PARALYZE, CUMBERSOME, ASSASSINATION, VAMPIRIC }`
    (**Vampiric moves here** — note 2 — from the old `EnemyAbility`.)
- `EnemyToken` holds `defensiveAbilities: Set<DefensiveAbility>` and
  `offensiveAbilities: Set<OffensiveAbility>`. **Resistances stay their own
  `resistances: Set<AttackElement>`** — they're defensive but parameterised by element, which an
  enum entry can't carry. The info window groups display as Defensive (resistances + defensive
  abilities) and Offensive.
- JSON schema changes accordingly (modifiers move from each attack up to the token). The 6 green
  tokens are re-encoded: Cursed Hags→offensive POISON, Wolf Riders→SWIFT, Ironclads→BRUTAL
  (+ resist PHYSICAL, unchanged), Diggers→defensive FORTIFIED. Validation test + mapper + DB
  version bump follow.

### D2 — Drawn-enemy lifecycle & Defeat (notes 6, 7)

A drawn enemy is **In play by default** (it was just revealed onto the board). Defeating is an
explicit one-tap action, because reveal→defeat is the common flow.

- Replace `DrawLogEntry.stillInPlay: Boolean = false` with **`defeated: Boolean = false`** (in
  play by default; keep the free-text `note`). Rename ripples through DTO/entity/mapper; DB
  version bump.
- Draw Log grouping flips: **"On the board"** (not defeated) pinned at top, **"Defeated"** dimmed
  below. This gives the flag real meaning (fixes note 7 — today it only shows an icon).
- UI action (follow-up issue): detail screen gets a prominent **Defeat** button beside Close;
  grid cells get a **Defeat checkbox** (note 6).

### D3 — Multi-draw grid overview (note 4)

> **Superseded by #203/D18** (2026-07-28): the "Draw Log keeps one row per token" line below turned
> out not to be the final word - see D18.

- Draw of **N = 1** → opens that enemy's detail (zoom) directly, as today.
- Draw of **N > 1** → opens a **grid** of all N (art + name + Defeat checkbox per cell); tapping a
  cell opens that enemy's detail with prev/next arrows across the batch; back returns to the grid.
- Draw Log keeps **one row per token** (tapping a row opens that single token), unchanged.

### D4 — Pile back-art + tap-to-draw (note 5)

- Import each pile's distinct face-down **back art** (green/grey/violet/brown/red/white + ruins)
  from the same TTS mod (Workshop `1721301081`) used for the faces; show it on the pile card.
- **Tapping the back-art draws exactly 1** from that pile (then opens its detail, since N=1 goes
  straight to zoom). The stepper + "Draw N" button stay for drawing several at once.

### D5 — Zoom detail: drop attack from the summary line (note 8)

The summary line under the art currently reads `Armor X · Fame Y · Attack Z`, duplicating the
per-attack detail listed just below it. Change `statLine()` to **`Armor X · Fame Y`** only; the
attack(s) remain in the detail list below.

### D6 — Summoned tokens re-apply their own offensive abilities (note 3 — documentation only)

When a summoner draws a token to fight in its place, that summoned token applies **its own**
offensive abilities to its attack(s). No code now (the Summon Draw action is issue #191) — record
this in `CONTEXT.md`'s "Summon Draw" entry and in issue #191 so it isn't lost.

## Scope: what lands in PR #186 vs. new issues

**In PR #186 (model / schema / not-yet-shipped, + trivial UI):**
- D1 ability-model refactor (domain enums, JSON catalogue, validation test, mapper, DB bump).
- D2 *domain half* — rename `stillInPlay`→`defeated`, flip default, DTO/entity/mapper, DB bump,
  and the Draw Log's On-the-board / Defeated grouping.
- D5 zoom summary-line trim (one line of UI).
- D6 documentation (CONTEXT.md + issue #191).

*(Both D1 and D2 bump the DB version; since neither has shipped, fold them into a single
v10→v11 bump on this branch rather than two.)*

**New follow-up sub-issue(s) of #178 (net-new UI):**
- **[#197] Defeat UI + multi-draw grid:** D2 *UI half* (Defeat button on detail) + D3 grid
  (with per-cell Defeat checkboxes).
- **[#198] pile back-art + tap-to-draw:** D4.

Note 3 (summoned tokens re-apply their own offensive abilities, D6) is documented on **#191**
(the Summon Draw action) rather than being its own issue.

## Follow-up grilling for #197 implementation (2026-07-28)

The original combined issue (then #192) was split, grilling-session style, into three: **#192**
(multi-pile draw, deferred), **#197** (this doc's D2-UI + D3, now the active focus), and **#203**
(Draw Log visual grouping by `batchId`, deferred — D3 explicitly keeps the log at one row per
token, and grouping the persisted log rows was never actually resolved by D3 despite being in the
original issue text). Decisions below are for #197's implementation specifically.

### D7 — Grid/detail navigation model

New `GridState` (batch's token IDs) wraps the existing `ZoomState`/`TokenZoomDialog` unchanged:
`EnemyPickerTab` holds `gridState: GridState?`, and, nested inside an open grid, an optional
`ZoomState?` for "drilled into a cell." Opening a detail from a grid cell sets the nested
`ZoomState`; back on that detail clears it back to the grid; dismissing the grid clears both.
Draw of **N = 1** skips the grid entirely and sets a top-level `ZoomState` directly, same as
today — including the Draw Log row tap-in case, which always opens detail directly with no grid
regardless of the row's original batch size.

### D8 — Grid checkbox default & write-through

Grid cell Defeat checkboxes default to **unchecked** (on-the-board), matching persisted
`DrawLogEntry.defeated` — this supersedes the *raw* note 6 text ("reflect reveal→defeat as the
default"), which was superseded by D2's actual resolution (`defeated = false` by default, Defeat
is an explicit one-tap action). Toggling a checkbox calls `viewModel.setDefeated(...)`
**immediately** — no staged/pending state, same as the existing `DefeatDialog` flow; the grid is a
thin view over session state, not a second source of truth.

### D9 — Grid layout

`LazyVerticalGrid` with `GridCells.Adaptive(minSize = ...)` (not a fixed column count) — the
common case is a **2-6 token batch** (author's estimate), with counts up to `MAX_BATCH = 20`
being rare; size the adaptive `minSize` so 2-6 renders as a comfortably large grid, and let it
naturally pack more/smaller columns for the rare large batch rather than tuning for the 20-case.

### D10 — Grid dialog sizing & dismissal

Dialog sizes **to content** (not fixed near-fullscreen), with a max-height cap so it scrolls
internally once a batch is large enough to exceed it — keeps the common 2-6 case compact instead
of mostly-empty fullscreen. Dismissal matches whatever convention `TokenZoomDialog` already uses
(system back / tap-outside-scrim) — no separate "Done" button, since D8's immediate write-through
means there's no pending state to confirm on close.

Flagged for **possible future iteration**, contingent on how #203 (Draw Log batch grouping) ends
up shaping the log UI: the dismiss gesture might later grow an explicit "save for later" vs.
"defeat remaining" affordance instead of a bare close. Not building that now — noted so it isn't
forgotten.

> **Resolved by #203** (2026-07-28): #203 reopens this exact dialog from the Draw Log (D18) but
> made no change to its dismiss gesture - the "save for later"/"defeat remaining" idea above is
> still just an idea, not superseded or ruled out, so it stays flagged for whenever it comes up
> again.

### D11 — Detail screen Defeat button: style & post-tap behavior

Decided by author deferring to assistant's recommendation, **alternatives recorded for later
iteration**:

- **Style (chosen):** Defeat as the large filled/primary button; Close as a plain text/outline
  button beside or below it (typical Material primary/secondary pairing).
  - *Alternative not taken:* Defeat as a large primary action with Close demoted to a small
    icon/back-arrow in a top bar instead of a peer button.
- **Post-tap behavior (chosen):** tapping Defeat marks the entry defeated and the user **stays**
  on that detail screen (button/state updates in place, e.g. becomes a "Defeated" indicator) —
  does *not* auto-navigate back to the grid.
  - *Alternative not taken:* auto-return to the grid after Defeat, so the user flows
    reveal→defeat→next-cell without an extra manual back-tap. Rejected for now because it would
    fight the prev/next arrows for a user who wants to keep flipping through details without
    bouncing back to the grid each time — but worth revisiting if in-practice usage shows the
    auto-return flow is what people actually want.

## Follow-up grilling for #192 implementation (2026-07-28)

#192 (multi-pile simultaneous draw, deferred until #197 landed — see that issue's text) is now
the active focus. Decisions below are for #192's implementation specifically; they build directly
on #197's D7-D11 (the grid this issue now feeds multi-pile batches into).

### D12 — Domain: consolidate onto one map-based `draw()`

`EnemyPickerSession.draw(pileId, count, batchId, shuffle)` is deleted outright (confirmed unused
outside the app's own call sites once the UI is unified per D13, and unlikely to be needed again)
and replaced by a single **`draw(draws: Map<TokenPileId, Int>, batchId = System.currentTimeMillis(),
shuffle = { it.shuffled() })`** — there is no separate single-pile entry point; a single-pile draw
is just a one-entry map.

- Internally loops piles in **`TokenPileId.entries` order** (GREEN, GREY, VIOLET, BROWN, RED,
  WHITE, RUIN — already the rulebook's difficulty order, author-confirmed 2026-07-28), not
  whatever order the caller's `Map` happens to iterate in, so `DrawLogEntry` insertion order (and
  therefore Draw Log display order for a batch) is deterministic regardless of UI selection order.
- Reuses the exact same per-pile draw/replenish logic the old `draw()` had, inlined in this
  function's own loop body (no separate private helper needed - there's only the one call site
  now that the old single-pile overload is gone) — one implementation of "how a pile gets drawn
  from," not two.
- Validates `require(draws.isNotEmpty())` and `require(draws.values.all { it >= 1 })`, mirroring
  the old function's own `require(count >= 1)` rather than silently tolerating a malformed call.
- No DB/schema migration: `DrawLogEntry` already carries `pile` and a shared `batchId` per entry
  (its doc comment already anticipated "later - a multi-pile draw"), so persisting a multi-pile
  batch needs no new fields.
- Existing domain/data tests calling the old `draw(pileId, count)` are rewritten to
  `draw(mapOf(pileId to count))`.

### D13 — UI: unify single- and multi-pile draw into one stepper-driven action

Replaces the per-`PileCard` "Draw" button entirely, rather than adding a separate multi-select
mode alongside it (author's idea, 2026-07-28):

- Every pile's quantity stepper now **defaults to 0** (was 1) and its minimum is **0** (was 1) -
  0 means "not part of this draw."
- The per-card "Draw" button is removed. **One global "Draw"/"Draw N" button** (N = sum of every
  pile's nonzero stepper) replaces all of them, calling `draw()` with a map of every pile whose
  stepper is > 0.
- Firing it resets every stepper back to 0.
- This is the *only* draw path now - there is no separate single-pile UI flow. Setting one pile's
  stepper > 0 and leaving the rest at 0 degenerates to a one-entry map (D12), so N=1 still opens
  the token detail directly and N>1 still opens the grid overview, unchanged from #197's D7.
- Independent of #198 (tap a pile's back-art to quick-draw 1) - that issue is a separate shortcut
  interaction, not blocked by or dependent on this redesign.

### D14 — UI: pile card layout - two per row, odd one out goes full-width

Removing the per-card Draw button frees up enough width to fit two `PileCard`s per row (author's
suggestion, 2026-07-28):

- Implemented by chunking `pileIds` into pairs and emitting each pair as one `Row` `item` inside
  the existing single `LazyColumn` (Draw Log and Config section stay on the same scroll) - not a
  nested `LazyVerticalGrid`, to avoid nested-scrollable handling.
- A trailing odd card (today: RUIN, since it's last in `TokenPileId.entries` and the base set has
  7 piles) renders **full-width** rather than half-width-with-a-spacer - deliberately, since Ruin
  isn't a real enemy pile and plays by different rules (see `CONTEXT.md`'s "Ruin Token"), so it
  reading visually distinct is fitting rather than an inconsistency to hide.

### D15 — UI: global "Draw" button placement and disabled state

- Lives in a **`Scaffold` bottom bar**, always visible regardless of scroll position (not a FAB,
  not inline in the list) - the standard Material pattern for one primary action over a set of
  selectable items above it.
- **Always rendered, disabled when the total is 0** (not hidden/shown based on state) - avoids the
  bottom bar popping in and out as steppers change.

### D16 — UI: grid dialog adaptation for multi-pile batches

`TokenGridDialog` (#197) assumed one pile per batch for its title and had no per-cell pile label.
Now that a batch can span piles:

- **Title** becomes the generic **"N tokens drawn"** for any batch of size > 1, single-pile or
  multi-pile alike - replaces the old `"${count} ${pile.displayName()} drawn"`.
- **No per-cell pile label added** - the token art's own color already makes the source pile
  obvious at a glance (author's call, 2026-07-28), so cells stay exactly as #197 built them (art +
  name + Defeat checkbox).

### D17 — No combined cross-pile cap

Each pile's stepper keeps its existing `MAX_BATCH = 20` individual cap; no additional cap on the
*combined* total across every selected pile. A very large combined batch (worst case ~140 tokens
across all 7 piles) is an accepted edge case rather than something to clamp down on, since the
grid is already adaptive and internally scrollable (#197's D9/D10) and won't break - it just
wouldn't be pretty, which is fine for a rare case.

## Follow-up grilling for #203 implementation (2026-07-28)

#203 (Draw Log visual grouping by `batchId`) was filed deliberately deferred - see its own text -
because D3 above had already decided the log stays one-row-per-token and never actually resolved
grouping. Grilling it surfaced that a purely *visual* grouping (divider/band/badge, one row still
per token) wasn't actually what was wanted: the author's answer was to collapse a batch to a
single row entirely, which **overturns D3** rather than refining it. Decisions below supersede
D3's "Draw Log keeps one row per token" line; D3's N=1-vs-N>1 draw-time behavior is untouched.

### D18 — Draw Log row = one batch, not one token (supersedes D3)

- **Batch size 1** → tap opens that token's detail (zoom) directly, exactly as D3/D7 already do at
  draw time - no change to the single-token path.
- **Batch size > 1** → the whole batch collapses into **one row** ([D20](#d20-batch-row-content));
  tapping it (or its "View" button) reopens the **same grid overview** a fresh N>1 draw opens -
  same `GridState`/`TokenGridDialog`, same D7 nested-zoom, D8 checkboxes, D9 adaptive layout, D10
  dismiss. There is **no separate "read-only history" mode**: reopening a batch from the log later
  is fully identical to opening it right after drawing, since there's no meaningful difference
  between "just drew these" and "looking at them again" (the same precedent #191's
  view-without-redrawing work already set for single tokens, commit a393d5e).
- Implemented as a pure grouping function (`groupDrawLog`, `DrawLogGrouping.kt`) rather than logic
  inlined in the Composable, specifically so it's unit-testable per the repo's TDD standard - this
  is real branching logic (which rows merge, which section they land in), not UI scaffolding.

### D19 — Bucketing: a batch stays "on the board" until every member is defeated

A batch of 3 with 1 defeated and 2 still up stays in **"On the board"** - it moves to "Defeated"
only once the *last* member clears. Matches the existing per-token rule's spirit ("on the board" =
still needs attention) and means a batch's row moves **exactly once**, at completion, rather than
flapping between sections as individual members get defeated one at a time. (Rejected: moving at a
>50% defeated threshold - arbitrary, no rules-basis, and the row could flip back and forth.)

### D20 — Batch row content

A collapsed batch row (`DrawLogBatchRow`) shows:

- Up to **4** small token-art thumbnails (`BATCH_ROW_THUMBNAIL_CAP`), then a **"+N"** tail for the
  rest - sized so the common 2-6 batch (D9's estimate) mostly shows every member, and a rare large
  batch just summarizes instead of squeezing thumbnails illegibly small.
- Title **"N tokens drawn"** (reusing the phrasing #197's grid title already established).
- Subtitle: the pile name, **only when every member shares one** (a batch is single-pile today;
  #192 will eventually make multi-pile batches possible, and D16 already decided per-token pile
  labels aren't needed once art color conveys it - so the segment is just omitted once a batch
  spans piles, no new logic needed until #192 actually exists) - followed by
  **"X on board, Y defeated"** counts.
- **No Defeat control of its own** - a size>1 row can't represent "defeat this token" for N
  tokens, so the icon is dropped entirely; defeating a batch member only happens through the
  grid's own checkboxes (D8), which tapping the row reopens.

