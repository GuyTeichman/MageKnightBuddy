# Domain glossary — Enemy Picker

Terms for the Enemy Picker tab: the app-side replacement for the physical face-down enemy/ruin/faction token piles. See `CONTEXT.md` for the other two glossary slices (Scoring, Dummy Player tab).

**Enemy Picker**:
The tab and screen that replaces the physical face-down token piles: tapping a **Token Pile** draws a token, shows it zoomed with its title, and records it in the **Draw Log**. Authoritative — the cardboard piles stay in the box and the app is the source of both the randomness and what's left in each pile, so its state persists across app restarts for the whole length of a game. Like the Dummy Player tab's modes it *narrates rather than simulates* ([ADR-0004](adr/0004-volkare-narrates-cards-not-simulates-board.md)): it models no map, no site, and no combat — a drawn token is discarded immediately, and remembering which enemy is still standing on which space is the player's job (the Draw Log is there to help).
_Avoid_: Enemy browser, token reference (it owns real pile state; browsing abilities is a secondary affordance)

**Token Pile**:
One face-down stack of same-backed tokens the **Enemy Picker** draws from — the 6 enemy colors (green/grey/violet/brown/red/white), ruin tokens, possessed enemies, and one per faction. Each has its own Discard Pile, matching the rulebook's "sort the enemy (round) and ruin (hexagonal) tokens by the reverse side, and stack them in seven face down piles. Next to each pile, there is a space for discarded tokens" (p.3). Since #251 the Discard Pile holds only tokens that have been **discarded** (a defeated enemy, a spent faction reward, an ephemeral summon child) — *not* every drawn token: a drawn-but-undefeated token is held **on the board**, out of both the draw pile and the discard, recorded only by its **Draw Log** entry (so `draw pile + discard` is not the pile's full contents while tokens stand). Which piles exist at all depends on the active expansion selection. Its face-down draw pile can be surveyed (issue #231) as an **unordered composition** — the remaining tokens grouped by identity with copy counts, sorted alphabetically, opened from the pile's tappable count line — so a player can estimate combat odds. It deliberately never exposes draw *order*: showing what's left is legitimate public information (you know each pile's full contents at setup and the discard is face-up), but showing the next token would break the picker's face-down secrecy.
_Avoid_: Deck (reserved for `docs/context-dummy-player.md`'s Dummy Player's/Volkare's/Proxy Player's card decks — a Token Pile holds cardboard tokens and has entirely different draw rules); Enemy Pile (3 of the piles hold non-enemies)

**Ruin Token**:
One of the RUIN **Token Pile**'s hexagonal tokens (12 base game + 3 Lost Legion; rulebook
"Revealing Ruins" / Ultimate Edition Walkthrough), modeled as `RuinToken` - a sibling type to
**Enemy Token**, not a variant of it, since a Ruin token prints no Armor/Attack/Fame block. Gated by
the **Token Set** like enemy tokens (via `RuinToken.expansion`). Two kinds:
- an **Ancient Altar** (`altarColors`): pay mana for Fame, no combat. A single-colour altar means
  pay 3 of that colour for 7 Fame; the Lost Legion four-colour altar means pay one of *each* colour
  for 10 Fame. The list's size (1 or 4) is the whole distinction - the Fame is derived from it for
  the prompt, never stored.
- an **Enemies With Treasure** draw (`enemyPiles`): draw one token from each pile in the list, in
  order, and fight them all; a pile repeated in the list means that many draws from it (base
  `ruin_green_green` = two from Green), and Lost Legion adds a three-enemy token. The drawn enemies
  are attached under the ruin via the same **Summon Draw** machinery, but - unlike summoned tokens -
  they render in full (defensive abilities matter) and can be **partially defeated** (each is its own
  Draw Log flag; a ruin group is drawn once, never re-drawn).

The printed `reward` is **displayed as reference text but never tracked or scored**
([ADR-0006](adr/0006-enemy-picker-owns-pile-state-but-models-no-map.md), amended by issue #201):
`RuinToken` still tracks only which pile(s) to draw from, plus this flavour string. All ruin *data*
and *art* (base + Lost Legion) is bundled: each face is a **hexagon-shaped transparent PNG**
(`enemy-tokens/<id>.png`), cropped from the TTS mod's hex-token diffuse textures and cut to the true
hexagon silhouette (so ruins read as hexes, visibly distinct from the round enemy tokens); the RUIN
pile back is the same treatment. The text tile remains only as the not-yet-sourced fallback.
_Avoid_: Enemy Token (different shape entirely - no armor/attack/fame, and an Ancient Altar isn't
combat at all)

**Replenish**:
What happens when a **Token Pile** is drawn empty: its Discard Pile is shuffled and becomes the new pile (rulebook p.3, "If you run out of tokens, reshuffle the discarded ones and create a new face down pile"; the Apocalypse Dragon rulebook repeats it verbatim for possessed enemies and faction tokens). Only the **discard** is reshuffled — tokens held **on the board** (drawn but undefeated, since #251) are still in play and are never reshuffled back in. Mandatory and automatic — never a setting. Happens **eagerly**: the reshuffle fires the instant a draw empties the draw pile *and there is a non-empty discard to reshuffle*, so a pile with discardable tokens never rests at 0 remaining (issue #231). Distribution-identical to reshuffling lazily on the next draw. Since #251 a pile can be **genuinely empty**: if the draw pile *and* the discard are both empty because every token drawn is still on the board, there is nothing to reshuffle — the pile can't be drawn until a token is defeated back into its discard, and the UI shows "Nothing to draw" rather than assuming a non-empty discard. An empty draw pile with a non-empty discard can also surface from a restored session, which the draw code replenishes on entry.
_Avoid_: Reshuffle (that term is taken by `docs/context-dummy-player.md`'s entry, and means the Dummy/Proxy Player deck+discard merge — see its own entry; keeping the two words apart is deliberate)

**Draw Log**:
The **Enemy Picker**'s newest-first record of every token drawn since the last Reset, persisted alongside the pile state. Load-bearing rather than decorative: because the picker holds an undefeated token *on the board* (out of both piles) and models no map, the log is the *only* record of which enemy is still standing on which space — and, since issue #251, the derived source of which tokens are on the board at all. Since issue #203, one row is one **draw batch** (every token sharing a `batchId`), not one token: a batch of 1 renders and behaves exactly as before (tapping re-opens that token zoomed with its info window), while a batch of more than 1 collapses into a single row - tapping it (re)opens the same grid overview a fresh multi-token draw opens, whether that draw just happened or is being revisited later. Each entry carries a **defeated** flag: a freshly drawn enemy starts **on the board** (not defeated) because it was just revealed, and is marked defeated once the player beats it — the common reveal→defeat flow. The log groups the on-the-board batches at the top and the defeated ones (dimmed) below; a batch stays on-the-board until *every* member is defeated, moving to Defeated only once the last one clears. A single-token row may carry a short free-text note ("keep, NE tile") and its own Defeat toggle; a collapsed batch row has no Defeat control of its own — defeating a member only happens through the grid's checkboxes. Since #251 the flag is **no longer a pure memory aid**: marking an entry defeated moves its token from the board into its **Token Pile**'s Discard Pile (and un-marking pulls it back), so a **Replenish** reshuffles exactly the defeated tokens — the two exceptions being a **Draw with Replacement** game and an ephemeral **Summon Draw** child, where it stays a memory-aid flag only.
_Avoid_: History (reserved for the Scoreboard's saved sessions); "still in play" (the flag's sense is inverted — it tracks *defeated*, defaulting to on-the-board); assuming a row is always one token (true only for a batch of size 1, since #203); assuming the defeated flag never touches pile state (it does since #251 — except under replacement / for ephemeral summon children)

**Possessed Enemy**:
An Apocalypse Dragon enemy formed from *two* tokens: a possessed enemy token plus a normal circular enemy token of a color the triggering text names (Apocalypse Dragon rulebook p.7). The possessed token carries **deltas, not stats** — it modifies the circular token's Armor, topmost Attack, and Fame, and may add a Psychic Attack of value 1-4. The **Enemy Picker** renders the pair the way the cardboard looks (circular token superimposed on the possessed token's circular slot) and shows only the **summed** numbers, never the deltas alongside them, so the player is never left wondering whether they're expected to do the arithmetic themselves. Defeating one also awards a faction token from the faction the triggering text names.
_Avoid_: Treating the possessed token as an enemy in its own right (it has no standalone stat line)

**Faction Reward Token** (modelled as `FactionRewardToken`; formerly just "Faction Token"):
A held reward token drawn from one of **four** 12-token (6 types × 2) faction **Token Pile**s — the
Shades of Tezla **Elementalist** and **Dark Crusader** factions, and the Apocalypse Dragon **Apocalypse
Cult** and **Council of the Void** factions — won by defeating that faction's enemies (SoT faction
enemies; AD **Possessed Enemies**). It prints no Armor/Attack/Fame, only a one-off effect, plus the
line printed on *every* reward token: *"may be discarded during interactions for 1 Fame, or 3
Influence."* Its lifecycle is **draw → hold → spend**, not draw → discard (see
`docs/rules/faction-reward-tokens.md`). Each pile is surfaced by its faction's existing **Token Set**
`Expansion` toggle (the single Apocalypse Dragon toggle surfaces both AD piles).

In the UI (issue #252) the held tokens collapse into a **single pinned Draw Log entry** at the top;
tapping it opens a grid where each held token shows its effect and a **Spend** checkbox, and spending
one moves it to the dimmed "done" history — reusing the Draw Log's checked-off flag rather than a
bespoke inventory panel. **Deliberately not scored** (the rescope of closed issue #190): the app shows
the discard-for-Fame/Influence option as reference text but tracks no Fame or reward state (ADR-0006);
the player takes it themselves.
Its held/spent state is now **pile-correct** (issue #251, done): a drawn reward is held **on the
board** (out of both piles) until spent, and "spend" (the checked-off flag) moves it into the pile's
discard, exactly the enemy defeat lifecycle — so a **Replenish** reshuffles only spent rewards, never
held ones.
_Avoid_: Treating a faction reward token like an enemy draw (its lifecycle is draw → hold → spend);
assuming it is scored (it isn't — #190 was closed); the bare name "Faction Token" (prefer "Faction
Reward Token" to distinguish it from a faction's *enemy* tokens, which mix into the colour piles)

**Summon Draw**:
The extra **Token Pile** draw an enemy with the Summon ability forces: a token drawn *at the start of the Block phase* (the brown pile for every base-game summoner, but the summoned pile is recorded per-token in the catalogue rather than assumed, since possessed/expansion summoners can draw other colours), which replaces the summoner for the Block and Damage Assigning phases and is always discarded afterward, never yielding Fame or a faction token (rulebook p.9; Apocalypse Dragon p.10). In the **Enemy Picker** it's an explicit action on the drawn enemy rather than an automatic part of the reveal, because the player commits to the fight before legally knowing what gets summoned — auto-drawing would leak that. The summoned token fights with **its own** stats *and its own offensive abilities* (a summoned Brutal token attacks Brutally) — the summoner lends only the fight slot, not its abilities. When a **Possessed Enemy**'s topmost attack is itself a Summon, the possessed token's Attack delta applies to *this* token instead of the summoner, and is applied at the moment it's drawn (not yet implemented - possessed enemies are #189).

A summoned child is **ephemeral**, unlike every other **Draw Log** entry: it isn't independently resolved. The summoner's *own* Defeat flag marks the whole encounter (summon or no summon) over, and re-engaging a summoner that wasn't defeated draws a **fresh** child - so Summon is a repeatable action (relabelled "Re-summon" once used), not one-shot. `DrawLogEntry.parentIndex` records which log entry summoned a child; because the log is append-only, a re-summon *appends* a new child rather than replacing the old one, so several entries can share one `parentIndex` - `EnemyPickerSession.currentChildrenOf` resolves which are *current* (the most recent shared `batchId`, since a token with several Summon attacks draws and discards all of them together in one action). A token with two Summon attacks is a real rules case (not hypothetical) but has no current catalogue token, so it's untested against real data - see #191's implementation notes.
_Avoid_: Drawing the summoned token at reveal time; treating a child like a normal Draw Log entry (it has no independent Defeat state)

**Token Set**:
The **Enemy Picker**'s per-game choice of which expansions' tokens make up its **Token Pile**s — a setup decision (a scenario may dictate it), not a statement about what the player owns. Changing it necessarily rebuilds every pile and clears the **Draw Log**, so edits are staged in the screen's config section and committed by one "Apply & Reset" action rather than taking effect per checkbox. Each entry is one `Expansion` value; note **Shades of Tezla contributes two** — its Elementalist and Dark Crusader factions are separately-tickable sets that mix into the green/brown/red piles (the only Shades *enemy* mode the single-axis Token Set can express; the faction-only/separate-enemy-pile scenarios remain deferred — see `docs/rules/enemy-tokens.md`'s Shades section). Each faction toggle *also* now surfaces that faction's **Faction Reward Token** pile (issue #252), and the single Apocalypse Dragon toggle surfaces both AD reward piles — so ticking a faction brings in both its mixed-in enemies and its reward pile. Distinct from `docs/context-scoring.md`'s **Settings**' eventual global expansion toggles — see that entry.
_Avoid_: Expansion settings, owned expansions (that's **Settings**' question, deliberately kept separate)

**Draw with Replacement**:
The **Enemy Picker**'s off-by-default config toggle: when on, a drawn token returns to its **Token Pile** immediately and the pile is shuffled, so piles never deplete and no Discard Pile accumulates. For pulling a one-off random enemy without perturbing a real game's pile state. When off (the rules-correct default), a draw is without replacement and the pile **Replenish**es only once emptied.
_Avoid_: "Shuffle back in" (ambiguous with Replenish — say which)

**Offensive / Defensive Ability**:
The two kinds of enemy-token special ability (Quick Reference Sheet, "Enemy Token Abilities"). **Defensive** abilities govern how the enemy is *attacked* — Fortified, Elusive, Arcane Immunity, Unfortified (the Lost Legion's opposite of Fortified: ignores all site fortifications), plus the element **Resistances** (Physical/Fire/Ice), which are a defensive trait parameterised by element. **Offensive** abilities modify the enemy's *own* attack — Swift, Brutal, Poison, Paralyze, Cumbersome, Assassination, Vampiric. Both kinds are **whole-token**: they apply to *every* one of the enemy's attacks, not a single one (Lost Legion, "Multiple Attacks"), which is why the domain models them as `Set`s on `EnemyToken` (`defensiveAbilities`, `offensiveAbilities`, `resistances`) rather than on individual attacks — only an attack's value and element are per-attack. **Elusive** is the one defensive ability that carries a number: its second, higher Armor value lives in `EnemyToken.elusiveArmor` (non-null exactly when Elusive is present), kept off the enum for the same reason Resistances are — an enum entry can't hold the value. A **Summon**ed token applies its own offensive abilities (see **Summon Draw**). *Vampiric is offensive* (it grows the enemy's Armor from *its* attacks landing), a classification worth stating because its effect is on Armor and could be mistaken for defensive.
_Avoid_: per-attack modifier (abilities are token-wide, never attached to one attack)
