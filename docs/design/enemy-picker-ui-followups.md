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

