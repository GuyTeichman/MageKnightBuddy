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
One completed solo play-through of a Scenario by a given Knight, entered into the Score Calculator after the game ends. Holds the raw tallies the player enters (Fame, Spells in deck, Artifacts, etc.) and the computed total.
_Avoid_: Game, run, playthrough

**Standard Achievements Scoring**:
The six scoring categories shared across most scenarios: Greatest Knowledge, Greatest Leader, Greatest Adventurer, Greatest Loot, Greatest Conqueror, Greatest Beating. Each has its own point formula based on deck/inventory/unit contents. See `docs/rules/solo-scoring-overview.md`.

**Title**:
The bonus (typically +3 Fame) Standard Achievements Scoring awards to whichever player scored highest in a category, in multiplayer games. **Not awarded in solo play** — with only one player there's nothing to compare against, so each category is just summed directly.
_Avoid_: Bonus, achievement (Title is the comparison bonus; "Achievement" is the category itself)

**Dummy Player**:
The automated non-player character used to pace solo and cooperative games — it takes a simplified turn each round by flipping cards from its own deck. Some scenarios' scoring (e.g. Solo Conquest) counts cards remaining in the Dummy Player's deck.
_Avoid_: AI player, bot, opponent

**Proxy Player**:
A more elaborate solo-simulation mechanic introduced in the Apocalypse Dragon expansion. Distinct from Dummy Player — not yet modeled; deferred to a later phase.
_Avoid_: Dummy player (they are different mechanics from different rulebooks)
