# MageKnightBuddy

An Android companion app for the *Mage Knight* board game. Helps a player run solo games: calculating end-of-game scores, and (later) operating the automated Dummy Player and its more elaborate Proxy Player variant.

## Language

**Scenario**:
A named game setup, with its own map/tile configuration, end condition, and scoring rules, defined in the rulebook's Scenario Book (e.g. Solo Conquest, First Reconnaissance).
_Avoid_: Mission, mode

**Knight**:
The Hero character a player plays as (e.g. Tovak, Arythea, Wolfhawk). Recorded on every Scoring Session for history/statistics, and consumed directly by some scenarios' scoring rules (e.g. a knight-specific variant of Solo Conquest introduced in the Apocalypse Dragon expansion).
_Avoid_: Hero, character, player

**Scoring Session**:
One completed solo play-through of a Scenario by a given Knight, entered into the Score Calculator after the game ends. Holds the raw tallies the player enters (Fame, Spells in deck, Artifacts, etc.), an optional Player name, the computed total, and the computed Outcome. Persisted so it can be shown later on the Scoreboard.
_Avoid_: Game, run, playthrough

**Player**:
The optional free-text name of whoever physically played a Scoring Session, entered once per session on the Setup step. Exists so multiple people's histories can eventually be told apart and compared on the Scoreboard. Distinct from Knight: Knight is the in-game Hero character, Player is the real person — the same Player may play different Knights across sessions, and different Players may play the same Knight.
_Avoid_: User, name (ambiguous with Knight's display name)

**Scoreboard**:
The tab and screen listing every Scoring Session saved on this device, as a table (Knight / Score / Outcome), most recent first. Tapping a row opens that session's full category breakdown. Distinct from **Global Scoreboard** (stub, below).
_Avoid_: History, leaderboard (leaderboard is reserved for the future Global Scoreboard)

**Global Scoreboard** (stub):
A hypothetical future online leaderboard comparing scores across players/devices, distinct from the (device-local) Scoreboard above. Not designed in any concrete way — a "maybe far future" idea, deferred indefinitely.

**Outcome**:
Whether a Scoring Session was Won or Lost. Always **derived** from the same raw tallies already entered for scoring, per the Scenario's own victory condition (e.g. Solo Conquest: all cities conquered; Volkare's Return: Volkare defeated) — never a separate manual input. Computed once and stored on the Scoring Session so history/stats can filter by it directly.
_Avoid_: Result, victory (Outcome is the stored Won/Lost value; "victory condition" is the rule that computes it)

**Standard Achievements Scoring**:
The fixed six scoring categories used by every scenario: Greatest Knowledge, Greatest Leader, Greatest Adventurer, Greatest Loot, Greatest Conqueror, Greatest Beating. Each has its own point formula based on deck/inventory/unit contents. Matches the rulebook's own "STANDARD ACHIEVEMENTS SCORING" heading (p.15) exactly — always in play, no expansions or Settings required. See `docs/rules/solo-scoring-overview.md`.

**Achievements Scoring**:
The umbrella for a Scoring Session's full set of scoring categories: Standard Achievements Scoring plus whichever optional variant categories are currently enabled (e.g. Greatest Quester). Which variants are enabled will eventually be driven by Settings; until Settings exists, Greatest Quester is scored unconditionally, on the assumption that Settings' eventual default will be "every expansion enabled."
_Avoid_: Standard Achievements Scoring (that term is reserved for the fixed six; use this term when variants may also be included)

**Reputation Track Space**:
One of the 13 spaces a Knight's Shield token can occupy on the Reputation track, modeled as `ReputationTrackSpace`. Each space bundles two numbers that are easy to conflate: its **position** (how many steps from center, e.g. "+2 Reputation" - what the rulebook's Outcome thresholds check) and its **modifier** (the different, usually smaller value actually printed there, e.g. position +2 prints a +1 modifier - what gets added to/subtracted from a score). The two end spaces are marked "X" instead of a modifier. Currently only consumed by **For the Council**'s scoring (`ForTheCouncilScoringInput.reputationTrackSpace`) - the player picks the space their token is on, and both numbers are derived from that one choice rather than entered separately. See `docs/rules/for-the-council.md`'s "Reputation vs. Reputation modifier" section for the full track table.
_Avoid_: Fame (a separate track entirely, despite sharing a physical board with Reputation); "Reputation" alone (ambiguous between the space's position and its modifier - say which)

**Quest Point**:
A unit of progress gained by completing a step on a Quest Card (Apocalypse Dragon expansion). Feeds the optional Greatest Quester category of Achievements Scoring (1 Fame per Quest Point). Distinct from Fame itself.
_Avoid_: Quest score, quest fame

**Title**:
The bonus (typically +3 Fame) an Achievements Scoring category awards to whichever player scored highest in it, in multiplayer games. **Not awarded in solo play** — with only one player there's nothing to compare against, so each category is just summed directly.
_Avoid_: Bonus, achievement (Title is the comparison bonus; "Achievement" is the category itself)

**Settings** (stub):
The eventual place a player chooses which expansions and optional scoring variants (e.g. Greatest Quester) are in effect — answering "which expansions do I *own*", globally and stably. Deliberately **not** the same question as the **Enemy Picker**'s **Token Set** (which expansions' tokens are in *this game's* piles, a per-game setup choice a scenario can dictate); the two share the word "expansion" and nothing else, so don't collapse them. Not designed yet — deferred to a later phase. Until it exists, the app behaves as if its default will be "every expansion enabled" (see Achievements Scoring) — so Apocalypse Dragon content like Greatest Quester is scored unconditionally rather than gated behind a toggle that doesn't exist yet.

**Dummy Player**:
The automated non-player character used to pace solo and cooperative games — it takes a simplified turn each round by flipping cards from its own deck. Some scenarios' scoring (e.g. Solo Conquest) counts cards remaining in the Dummy Player's deck. The default mode of the Dummy Player tab (below); most scenarios use this mode. Modeled by `DummyPlayerSession`, whose deck/discard pile are lists of **CardIdentity** — the type shared with the future **Proxy Player Session** so a Dual-Color Advanced Action card can enter either mode's deck.
_Avoid_: AI player, bot, opponent

**Reshuffle** (Dummy/Proxy Player deck):
What happens to the Dummy Player's or Proxy Player's deck at End of Round when the Advanced Action offer's lowest card joins it (`docs/rules/dummy-player.md`'s "End of Round"): the deck's **entire discard pile** is combined with whatever cards are still undrawn, plus the newly-added card, into one pile that is then shuffled to become the new deck — the discard pile is empty right after. This mirrors the general "Preparing a New Round" step every real player also does (rulebook p.4: "Shuffles all their Deed cards to create a new Deed deck" — deck + discard pile + hand, combined). Called out explicitly here because "the deck is reshuffled" reads ambiguously in isolation — cross-check any code touching it against this entry, not just the rule doc's one-line phrasing, since a real historical bug (`DummyPlayerSession.endRound`, issue #148) shuffled only the remaining deck and left the discard pile stranded forever, and neither the implementation nor its tests caught it because both were written from the same narrow reading.
_Avoid_: Assuming "shuffle the deck" means only the undrawn cards — always means deck + discard pile combined, for both Dummy Player and Proxy Player

**Dummy Player tab**:
The tab (see architecture.md's tab roadmap) that runs whichever player-simulation mode the current scenario needs: Dummy Player, Volkare, or Proxy Player. Exactly one mode is active per scenario — not three separate tabs. Setup asks in two steps, not a single 3-way choice: first Volkare vs. a Knight (Volkare has no Knight at all), then, only if a Knight was chosen, Standard vs. Proxy Player (two depths of the same Knight-backed opponent).

**Volkare**:
The antagonist from The Lost Legion expansion who replaces the Dummy Player in **Volkare's Return** and **Volkare's Quest**. A distinct mode of the **Dummy Player tab**, modeled by **Volkare Session** rather than **Dummy Player Session** — the two share a tab but not an implementation, since Volkare's deck and turn rules diverge too far to reuse.
_Avoid_: Dummy Player (Volkare replaces the Dummy Player in his scenarios but has distinct, more elaborate turn logic — don't conflate the two)

**Volkare Session**:
One playthrough's tracked state for Volkare: his deck (16 generic Basic Action cards + the 4 Competitive Spells + a Race Level-sized batch of Wounds, drawn exactly once, never reshuffled), which Scenario he's driving (Volkare's Return or Volkare's Quest — their Wound-reveal and deck-exhaustion rules diverge, see `docs/rules/volkares-return.md`/`volkares-quest.md`), and his event log. Each turn reveals one card and logs a one-sentence, rules-derived description of its implication (move direction, a Source die reroll, a combat trigger, **Frenzy**) — the app narrates what the card means without simulating the map, combat, or Volkare's actual board position; the player resolves the real consequence at the table. See [ADR-0004](docs/adr/0004-volkare-narrates-cards-not-simulates-board.md).
_Avoid_: Dummy Player Session (a separate domain type, despite living under the same tab)

**Frenzy**:
Volkare's Return-only behavior once his deck is exhausted: every subsequent turn is narrated as if a Spell had been revealed (double move, treated as blue for direction), with no Source die reroll, forever. The rulebook defines no equivalent for Volkare's Quest — there, revealing the *last card that could still move him toward the portal* (a green/blue/white Basic Action or Competitive Spell — Wounds never move him, and red cards only ever trigger an attack in Quest, never a move) instead means that reveal was already his final move into the portal, so the app treats that reveal as an immediate loss rather than inventing an undefined continuation. Any Wounds or red cards still trailing it in the deck are simply never drawn.
_Avoid_: Applying Frenzy to Volkare's Quest (Return-only rule — see **Volkare Session**)

**City Revealed**:
A manual, one-way toggle on the Volkare's Return play screen (Volkare's Return only) marking that the city tile has been revealed on the physical board. Before it's set, card reveals are narrated with Exploring-phase wording (a fixed compass direction per card color); after, with Race/Battle-for-the-City wording (move toward the city). The rulebook's separate Race-for-the-City and Battle-for-the-City phases are deliberately collapsed into this one on/off signal, since telling them apart requires knowing Volkare's exact adjacency to the city — board state this app doesn't track.
_Avoid_: Movement Phase (there's no three-way phase concept in the model, just this one toggle)

**Proxy Player**:
A more elaborate, interactive drop-in replacement for the Dummy Player, introduced in the Apocalypse Dragon expansion. Not limited to Apocalypse Dragon scenarios — like Volkare, it's usable as the Dummy Player substitute in any solo/coop scenario that calls for one, not just scenarios the expansion added (e.g. Against the Dragon, Apocalypse is Here). A mode of the Dummy Player tab, modeled by **Proxy Player Session**. Like Volkare mode, it narrates rather than simulates (see [ADR-0004](docs/adr/0004-volkare-narrates-cards-not-simulates-board.md)): the app tracks its deck, crystals, and current **Objective Card**, and computes the movement-point formula from them, but never decides which map site is targeted or how movement/conquering actually resolves — the player reads the physical Proxy Player reference card and resolves that at the table. See `docs/rules/proxy-player.md`.
_Avoid_: Dummy player (different mechanics, different rulebook, even though both now live under the same tab)

**Proxy Player Session**:
One playthrough's tracked state for a Proxy Player: its Knight, deck/discard pile (a list of **CardIdentity** — generic Basic Actions, that Knight's 2 **Unique Basic Action Cards**, and any Advanced Actions, including **Dual-Color Advanced Action Cards**, added at round end), crystals, current round, current **Objective Card** (if any), and its event log. Each turn either continues an existing objective (adding a Shield token to it, flipping 3 cards) or draws a new one (flipping 1 card as the objective, then 2 more) — mirroring **Dummy Player**'s flip-and-chain-on-matching-crystals procedure — then the player reports back one of two outcomes: still traveling (the objective persists), or resolved (Explored or Completed at the table — the app doesn't ask which, since both discard the objective card and its Shield tokens with no other tracked-state effect, even Completed via learning an Advanced Action or Spell, which is pure offer bookkeeping — see `docs/rules/proxy-player.md`). Structured independently of **Dummy Player Session**/**Volkare Session** despite sharing the same tab and a similar deck-flip shape.
_Avoid_: Dummy Player Session, Volkare Session (separate domain types, despite conceptual similarity)

**Objective Card**:
The Proxy Player's current target, drawn from their own deck whenever they don't have one. Its color determines which kind of site the Proxy Player will move toward (green: adventure site, red: fortified site/monastery, white: recruit a Unit or learn an Advanced Action/Spell, blue: whichever of those is furthest from the portal) — see `docs/rules/proxy-player.md`'s "Objective". Gains a Shield token every turn it persists, which feeds the movement-point formula. Discarded, with its Shield tokens, the instant the turn resolves (whether Explored or Completed — the app tracks only that it resolved, not which).
_Avoid_: Objective (ambiguous — the physical rulebook also uses "objective" for Quest cards, an unrelated base-game concept)

**Turn (within Round)**:
How many turns a Dummy Player/Volkare/Proxy Player session has played so far in its current Round - `turnInRound` on `DummyPlayerSession`/`VolkareSession`/`ProxyPlayerSession`, shown in each mode's AI-screen header next to the Round chip (e.g. "ROUND 2 · TURN 4") so a player picking the app back up mid-game can see where they left off. Derived by counting each session's own "a turn was actually played" log-entry kind(s) whose logged Round matches the session's current Round - **Turn Played** for Dummy Player, **Card Revealed**/**Frenzy** for Volkare, **New Objective Drawn**/**Turn Continued** for Proxy Player - never an "announced End of Round" entry, since the deck emptying isn't itself a played turn. Resets to 0 the moment `endRound()` advances to a new Round, since that Round has no matching log entries yet.
_Avoid_: Round (Round is the outer game-state counter this increments within; Turn is scoped to "since the current Round began")

**Day/Night**:
Whether the current Round is a day or night Round, per the base rulebook's every-other-Round alternation. Tracked app-side (all 3 Dummy Player tab modes: **Dummy Player**, **Volkare**, **Proxy Player**) via one setup-time flag — "Starts at night?", default unchecked since most scenarios start at day — plus the session's current round number; there's no separate per-round state to update, since `isDayRound(round, startsAtNight)` derives it fresh each time. Only Proxy Player's movement-point Gold-die bonus reads it today (`docs/rules/proxy-player.md`'s "Movement points"); the other two modes track it for parity ahead of a planned day/night visual indicator.

**Unique Basic Action Card**:
One of 2 cards, per Knight, that replace a generic card in that Knight's starting 16-card Basic Action deck (the deck stays 16 cards total) — shown with the Hero's portrait on the physical card. Counts as an Advanced Action for the Proxy Player's movement-point bonus (+2, not +1) despite being a Basic Action. See `docs/rules/proxy-player.md`'s per-Knight table.
_Avoid_: Unique card (Mage Knight base rules also call one-off Artifacts "unique" — say "Unique Basic Action Card" for this specific meaning)

**CardIdentity**:
The domain type modeling which color(s) a **Dummy Player**/**Proxy Player** deck card counts as: `CardIdentity.SingleColor(color)` for an ordinary card, or `CardIdentity.DualColor(colorA, colorB)` for one of the 4 **Dual-Color Advanced Action Cards** (rejecting `colorA == colorB` — a card can't count as the same color twice). Shared by **Dummy Player Session**'s and the future **Proxy Player Session**'s deck/discard-pile representation, since a Dual-Color card can enter either mode's deck only via the same round-end Advanced Action offer step — see [ADR-0005](docs/adr/0005-shared-advanced-action-card-type-for-dual-color-cards.md).
_Avoid_: CardColor alone (the plain 4-value color enum `CardIdentity` wraps — say "CardIdentity" when a card's dual-color-ness matters, "CardColor" for a bare color)

**Enemy Picker**:
The tab and screen that replaces the physical face-down token piles: tapping a **Token Pile** draws a token, shows it zoomed with its title, and records it in the **Draw Log**. Authoritative — the cardboard piles stay in the box and the app is the source of both the randomness and what's left in each pile, so its state persists across app restarts for the whole length of a game. Like the Dummy Player tab's modes it *narrates rather than simulates* ([ADR-0004](docs/adr/0004-volkare-narrates-cards-not-simulates-board.md)): it models no map, no site, and no combat — a drawn token is discarded immediately, and remembering which enemy is still standing on which space is the player's job (the Draw Log is there to help).
_Avoid_: Enemy browser, token reference (it owns real pile state; browsing abilities is a secondary affordance)

**Token Pile**:
One face-down stack of same-backed tokens the **Enemy Picker** draws from — the 6 enemy colors (green/grey/violet/brown/red/white), ruin tokens, possessed enemies, and one per faction. Each has its own Discard Pile, matching the rulebook's "sort the enemy (round) and ruin (hexagonal) tokens by the reverse side, and stack them in seven face down piles. Next to each pile, there is a space for discarded tokens" (p.3). Which piles exist at all depends on the active expansion selection.
_Avoid_: Deck (reserved for the Dummy Player's/Volkare's/Proxy Player's card decks — a Token Pile holds cardboard tokens and has entirely different draw rules); Enemy Pile (3 of the piles hold non-enemies)

**Replenish**:
What happens when a **Token Pile** is drawn empty: its Discard Pile is shuffled and becomes the new pile (rulebook p.3, "If you run out of tokens, reshuffle the discarded ones and create a new face down pile"; the Apocalypse Dragon rulebook repeats it verbatim for possessed enemies and faction tokens). Mandatory and automatic — never a setting.
_Avoid_: Reshuffle (that term is taken, and means the Dummy/Proxy Player deck+discard merge — see its own entry; keeping the two words apart is deliberate)

**Draw Log**:
The **Enemy Picker**'s newest-first record of every token drawn since the last Reset, persisted alongside the pile state. Load-bearing rather than decorative: because the picker discards a token the moment it's drawn and models no map, the log is the *only* record of which enemy is still standing on which space. Tapping an entry re-opens that token zoomed with its info window; any entry can be flagged **still in play** with a short free-text note ("keep, NE tile"), which pins it to its own section at the top until cleared. Flagging is purely a memory aid — it has no effect on any **Token Pile**, so the draw odds are identical whether or not anything is flagged.
_Avoid_: History (reserved for the Scoreboard's saved sessions); treating a flag as pile state

**Possessed Enemy**:
An Apocalypse Dragon enemy formed from *two* tokens: a possessed enemy token plus a normal circular enemy token of a color the triggering text names (Apocalypse Dragon rulebook p.7). The possessed token carries **deltas, not stats** — it modifies the circular token's Armor, topmost Attack, and Fame, and may add a Psychic Attack of value 1-4. The **Enemy Picker** renders the pair the way the cardboard looks (circular token superimposed on the possessed token's circular slot) and shows only the **summed** numbers, never the deltas alongside them, so the player is never left wondering whether they're expected to do the arithmetic themselves. Defeating one also awards a faction token from the faction the triggering text names.
_Avoid_: Treating the possessed token as an enemy in its own right (it has no standalone stat line)

**Faction Token**:
An Apocalypse Dragon reward token drawn from one of two 12-token **Token Pile**s (The Apocalypse Cult, The Council of the Void), typically on defeating a **Possessed Enemy**. The one kind of token the **Enemy Picker** does *not* discard on draw: per the Apocalypse Dragon rulebook (p.6), a drawn faction token is **held** face up in the player's play area until spent, and whatever is still held at game end is worth 1 Fame each. So the picker keeps a small held-inventory list with a Spend action that moves a token to its discard — in-play state it deliberately refuses to keep for enemies ([ADR-0006](docs/adr/0006-enemy-picker-owns-pile-state-but-models-no-map.md)), justified because this is flat inventory rather than board position, and because the rules make it player-visible anyway.
_Avoid_: Treating a faction token like an enemy draw (its lifecycle is draw → hold → spend, not draw → discard)

**Summon Draw**:
The extra **Token Pile** draw an enemy with the Summon ability forces: a token drawn *at the start of the Block phase* (the brown pile for every base-game summoner, but the summoned pile is recorded per-token in the catalogue rather than assumed, since possessed/expansion summoners can draw other colours), which replaces the summoner for the Block and Damage Assigning phases and is always discarded afterward, never yielding Fame or a faction token (rulebook p.9; Apocalypse Dragon p.10). In the **Enemy Picker** it's an explicit action on the drawn enemy rather than an automatic part of the reveal, because the player commits to the fight before legally knowing what gets summoned — auto-drawing would leak that. Logged as a child of the summoner's **Draw Log** entry. When a **Possessed Enemy**'s topmost attack is itself a Summon, the possessed token's Attack delta applies to *this* token instead of the summoner, and is applied at the moment it's drawn.
_Avoid_: Drawing the summoned token at reveal time

**Token Set**:
The **Enemy Picker**'s per-game choice of which expansions' tokens make up its **Token Pile**s — a setup decision (a scenario may dictate it), not a statement about what the player owns. Changing it necessarily rebuilds every pile and clears the **Draw Log**, so edits are staged in the screen's config section and committed by one "Apply & Reset" action rather than taking effect per checkbox. Distinct from **Settings**' eventual global expansion toggles — see that entry.
_Avoid_: Expansion settings, owned expansions (that's **Settings**' question, deliberately kept separate)

**Draw with Replacement**:
The **Enemy Picker**'s off-by-default config toggle: when on, a drawn token returns to its **Token Pile** immediately and the pile is shuffled, so piles never deplete and no Discard Pile accumulates. For pulling a one-off random enemy without perturbing a real game's pile state. When off (the rules-correct default), a draw is without replacement and the pile **Replenish**es only once emptied.
_Avoid_: "Shuffle back in" (ambiguous with Replenish — say which)

**Dual-Color Advanced Action Card**:
An Advanced Action card, from a separate dual-color-cards product (not the base Apocalypse Dragon expansion), that counts as two colors instead of one. Only 4 exist (Power of Crystals, Chilling Stare, Explosive Bolt, Rush of Adrenaline). Enters a **Dummy Player**'s or **Proxy Player**'s deck only via the standard round-end Advanced Action offer step, same as any other Advanced Action — see `docs/rules/proxy-player.md`. Modeled by `CardIdentity.DualColor`, the two-color case of **CardIdentity**.
_Avoid_: Advanced Action (ambiguous — say "Dual-Color" specifically when a card counts as two colors)
