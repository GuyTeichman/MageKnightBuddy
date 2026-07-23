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
The eventual place a player chooses which expansions and optional scoring variants (e.g. Greatest Quester) are in effect. Not designed yet — deferred to a later phase. Until it exists, the app behaves as if its default will be "every expansion enabled" (see Achievements Scoring) — so Apocalypse Dragon content like Greatest Quester is scored unconditionally rather than gated behind a toggle that doesn't exist yet.

**Dummy Player**:
The automated non-player character used to pace solo and cooperative games — it takes a simplified turn each round by flipping cards from its own deck. Some scenarios' scoring (e.g. Solo Conquest) counts cards remaining in the Dummy Player's deck. The default mode of the Dummy Player tab (below); most scenarios use this mode. Modeled by `DummyPlayerSession`, whose deck/discard pile are lists of **CardIdentity** — the type shared with the future **Proxy Player Session** so a Dual-Color Advanced Action card can enter either mode's deck.
_Avoid_: AI player, bot, opponent

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
One playthrough's tracked state for a Proxy Player: its Knight, deck/discard pile (a list of **CardIdentity** — generic Basic Actions, that Knight's 2 **Unique Basic Action Cards**, and any Advanced Actions, including **Dual-Color Advanced Action Cards**, added at round end), crystals, current round, current **Objective Card** (if any), and its event log. Each turn either continues an existing objective (adding a Shield token to it, flipping 3 cards) or draws a new one (flipping 1 card as the objective, then 2 more) — mirroring **Dummy Player**'s flip-and-chain-on-matching-crystals procedure — then the player reports back exactly one of three outcomes: still traveling (the objective persists), Explored, or Completed. The latter two both discard the objective card and its Shield tokens, regardless of how the objective was actually resolved at the table — even Completed via learning an Advanced Action or Spell has no further effect, since that's pure offer bookkeeping (see `docs/rules/proxy-player.md`). Structured independently of **Dummy Player Session**/**Volkare Session** despite sharing the same tab and a similar deck-flip shape.
_Avoid_: Dummy Player Session, Volkare Session (separate domain types, despite conceptual similarity)

**Objective Card**:
The Proxy Player's current target, drawn from their own deck whenever they don't have one. Its color determines which kind of site the Proxy Player will move toward (green: adventure site, red: fortified site/monastery, white: recruit a Unit or learn an Advanced Action/Spell, blue: whichever of those is furthest from the portal) — see `docs/rules/proxy-player.md`'s "Objective". Gains a Shield token every turn it persists, which feeds the movement-point formula. Discarded, with its Shield tokens, the instant the turn resolves as Explored or Completed.
_Avoid_: Objective (ambiguous — the physical rulebook also uses "objective" for Quest cards, an unrelated base-game concept)

**Unique Basic Action Card**:
One of 2 cards, per Knight, that replace a generic card in that Knight's starting 16-card Basic Action deck (the deck stays 16 cards total) — shown with the Hero's portrait on the physical card. Counts as an Advanced Action for the Proxy Player's movement-point bonus (+2, not +1) despite being a Basic Action. See `docs/rules/proxy-player.md`'s per-Knight table.
_Avoid_: Unique card (Mage Knight base rules also call one-off Artifacts "unique" — say "Unique Basic Action Card" for this specific meaning)

**CardIdentity**:
The domain type modeling which color(s) a **Dummy Player**/**Proxy Player** deck card counts as: `CardIdentity.SingleColor(color)` for an ordinary card, or `CardIdentity.DualColor(colorA, colorB)` for one of the 4 **Dual-Color Advanced Action Cards** (rejecting `colorA == colorB` — a card can't count as the same color twice). Shared by **Dummy Player Session**'s and the future **Proxy Player Session**'s deck/discard-pile representation, since a Dual-Color card can enter either mode's deck only via the same round-end Advanced Action offer step — see [ADR-0005](docs/adr/0005-shared-advanced-action-card-type-for-dual-color-cards.md).
_Avoid_: CardColor alone (the plain 4-value color enum `CardIdentity` wraps — say "CardIdentity" when a card's dual-color-ness matters, "CardColor" for a bare color)

**Dual-Color Advanced Action Card**:
An Advanced Action card, from a separate dual-color-cards product (not the base Apocalypse Dragon expansion), that counts as two colors instead of one. Only 4 exist (Power of Crystals, Chilling Stare, Explosive Bolt, Rush of Adrenaline). Enters a **Dummy Player**'s or **Proxy Player**'s deck only via the standard round-end Advanced Action offer step, same as any other Advanced Action — see `docs/rules/proxy-player.md`. Modeled by `CardIdentity.DualColor`, the two-color case of **CardIdentity**.
_Avoid_: Advanced Action (ambiguous — say "Dual-Color" specifically when a card counts as two colors)
