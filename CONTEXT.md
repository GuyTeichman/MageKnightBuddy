# MageKnightBuddy

An Android companion app for the *Mage Knight* board game. Helps a player run solo games: calculating end-of-game scores, and (later) operating the automated Dummy Player and Apocalypse Dragon's Proxy Player.

## Language

**Scenario**:
A named game setup, with its own map/tile configuration, end condition, and scoring rules, defined in the rulebook's Scenario Book (e.g. Solo Conquest, First Reconnaissance).
_Avoid_: Mission, mode

**Knight**:
The Hero character a player plays as (e.g. Tovak, Arythea, Wolfhawk). Recorded on every Scoring Session for history/statistics, and consumed directly by some scenarios' scoring rules (e.g. a knight-specific variant of Solo Conquest introduced in the Apocalypse Dragon expansion).
_Avoid_: Hero, character, player

**Scoring Session**:
One completed solo play-through of a Scenario by a given Knight, entered into the Score Calculator after the game ends. Holds the raw tallies the player enters (Fame, Spells in deck, Artifacts, etc.), the computed total, and the computed Outcome.
_Avoid_: Game, run, playthrough

**Outcome**:
Whether a Scoring Session was Won or Lost. Always **derived** from the same raw tallies already entered for scoring, per the Scenario's own victory condition (e.g. Solo Conquest: all cities conquered; Volkare's Return: Volkare defeated) — never a separate manual input. Computed once and stored on the Scoring Session so history/stats can filter by it directly.
_Avoid_: Result, victory (Outcome is the stored Won/Lost value; "victory condition" is the rule that computes it)

**Standard Achievements Scoring**:
The fixed six scoring categories used by every scenario: Greatest Knowledge, Greatest Leader, Greatest Adventurer, Greatest Loot, Greatest Conqueror, Greatest Beating. Each has its own point formula based on deck/inventory/unit contents. Matches the rulebook's own "STANDARD ACHIEVEMENTS SCORING" heading (p.15) exactly — always in play, no expansions or Settings required. See `docs/rules/solo-scoring-overview.md`.

**Achievements Scoring**:
The umbrella for a Scoring Session's full set of scoring categories: Standard Achievements Scoring plus whichever optional variant categories are currently enabled (e.g. Greatest Quester). Which variants are enabled will eventually be driven by Settings.
_Avoid_: Standard Achievements Scoring (that term is reserved for the fixed six; use this term when variants may also be included)

**Quest Point**:
A unit of progress gained by completing a step on a Quest Card (Apocalypse Dragon expansion). Feeds the optional Greatest Quester category of Achievements Scoring (1 Fame per Quest Point). Distinct from Fame itself.
_Avoid_: Quest score, quest fame

**Title**:
The bonus (typically +3 Fame) an Achievements Scoring category awards to whichever player scored highest in it, in multiplayer games. **Not awarded in solo play** — with only one player there's nothing to compare against, so each category is just summed directly.
_Avoid_: Bonus, achievement (Title is the comparison bonus; "Achievement" is the category itself)

**Settings** (stub):
The eventual place a player chooses which expansions and optional scoring variants (e.g. Greatest Quester) are in effect. Not designed yet — deferred to a later phase.

**Dummy Player**:
The automated non-player character used to pace solo and cooperative games — it takes a simplified turn each round by flipping cards from its own deck. Some scenarios' scoring (e.g. Solo Conquest) counts cards remaining in the Dummy Player's deck.
_Avoid_: AI player, bot, opponent

**Proxy Player**:
A more elaborate solo-simulation mechanic introduced in the Apocalypse Dragon expansion. Distinct from Dummy Player — not yet modeled; deferred to a later phase.
_Avoid_: Dummy player (they are different mechanics from different rulebooks)
