# MageKnightBuddy

An Android companion app for the *Mage Knight* board game. Helps a player run solo games: calculating end-of-game scores, and (later) operating the automated Dummy Player and its more elaborate Proxy Player variant.

## Domain glossary

Split into three files by domain area — check the one matching whatever you're touching before introducing a new domain term, and update it the moment a term gets resolved or sharpened. Don't let any of them drift out of sync with the code.

- `docs/context-scoring.md` — Scoreboard tab, Score Calculator wizard, Scoring Session (Scenario, Knight, Player, Outcome, Achievements Scoring, Reputation Track Space, Quest Point, Title, Settings).
- `docs/context-dummy-player.md` — Dummy Player tab and its three modes (Dummy Player, Volkare, Proxy Player), the shared deck/card model (CardIdentity, Reshuffle, Turn/Round, Day/Night).
- `docs/context-enemy-picker.md` — Enemy Picker tab (Token Pile, Draw Log, Ruin Token, Possessed Enemy, Faction Token, Summon Draw, Token Set).
