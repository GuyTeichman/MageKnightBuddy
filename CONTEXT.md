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
The automated non-player character used to pace solo and cooperative games — it takes a simplified turn each round by flipping cards from its own deck. Some scenarios' scoring (e.g. Solo Conquest) counts cards remaining in the Dummy Player's deck. The default mode of the Dummy Player tab (below); most scenarios use this mode.
_Avoid_: AI player, bot, opponent

**Dummy Player tab** (stub):
The tab (not yet built — see architecture.md's tab roadmap) that runs whichever player-simulation mode the current scenario needs: Dummy Player, Volkare, or Proxy Player. Exactly one mode is active per scenario, chosen via a mode selector on this one tab — not three separate tabs.

**Volkare**:
The antagonist from The Lost Legion expansion who replaces the Dummy Player in **Volkare's Return** and **Volkare's Quest**. A distinct mode of the **Dummy Player tab**, modeled by **Volkare Session** rather than **Dummy Player Session** — the two share a tab but not an implementation, since Volkare's deck and turn rules diverge too far to reuse.
_Avoid_: Dummy Player (Volkare replaces the Dummy Player in his scenarios but has distinct, more elaborate turn logic — don't conflate the two)

**Volkare Session**:
One playthrough's tracked state for Volkare: his deck (16 generic Basic Action cards + the 4 Competitive Spells + a Race Level-sized batch of Wounds, drawn exactly once, never reshuffled), which Scenario he's driving (Volkare's Return or Volkare's Quest — their Wound-reveal and deck-exhaustion rules diverge, see `docs/rules/volkares-return.md`/`volkares-quest.md`), and his event log. Each turn reveals one card and logs a one-sentence, rules-derived description of its implication (move direction, a Source die reroll, a combat trigger, **Frenzy**) — the app narrates what the card means without simulating the map, combat, or Volkare's actual board position; the player resolves the real consequence at the table. See [ADR-0004](docs/adr/0004-volkare-narrates-cards-not-simulates-board.md).
_Avoid_: Dummy Player Session (a separate domain type, despite living under the same tab)

**Frenzy**:
Volkare's Return-only behavior once his deck is exhausted: every subsequent turn is narrated as if a Spell had been revealed (double move, treated as blue for direction), with no Source die reroll, forever. The rulebook defines no equivalent for Volkare's Quest — there, revealing his *last non-Wound card* instead means that reveal was already his final move into the portal (Wounds never move him, so under correct play he should already be there by then), so the app treats that reveal as an immediate loss rather than inventing an undefined continuation. Any Wounds still trailing it in the deck are simply never drawn.
_Avoid_: Applying Frenzy to Volkare's Quest (Return-only rule — see **Volkare Session**)

**City Revealed**:
A manual, one-way toggle on the Volkare's Return play screen (Volkare's Return only) marking that the city tile has been revealed on the physical board. Before it's set, card reveals are narrated with Exploring-phase wording (a fixed compass direction per card color); after, with Race/Battle-for-the-City wording (move toward the city). The rulebook's separate Race-for-the-City and Battle-for-the-City phases are deliberately collapsed into this one on/off signal, since telling them apart requires knowing Volkare's exact adjacency to the city — board state this app doesn't track.
_Avoid_: Movement Phase (there's no three-way phase concept in the model, just this one toggle)

**Proxy Player**:
A more elaborate, interactive drop-in replacement for the Dummy Player, introduced in the Apocalypse Dragon expansion. Not limited to Apocalypse Dragon scenarios — like Volkare, it's usable as the Dummy Player substitute in any solo/coop scenario that calls for one, not just scenarios the expansion added (e.g. Against the Dragon, Apocalypse is Here). A mode of the Dummy Player tab, not a separate tab. Not yet modeled; deferred to a later phase.
_Avoid_: Dummy player (they are different mechanics from different rulebooks, even though both now live under the same tab)
