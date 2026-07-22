# Volkare mode narrates deck reveals, not a full board simulation

The Lost Legion's Volkare's Return and Volkare's Quest replace the standard Dummy Player with Volkare: his own deck (an unused Hero's Basic Actions + the 4 removed competitive Spells + Wounds), and a "Course of the Game" procedure (`docs/rules/volkares-return.md`/`volkares-quest.md`, both p.13/15-16, deliberately left un-extracted from the rulebook) where each card revealed from his deck tells the player how he explores, paces toward his target, attacks, and retreats on the physical board.

That procedure is a full movement/combat AI: it reads Volkare's current map position, the state of the target city or portal, his army composition, and combat results, then decides his next move. Modeling all of that in `domain/` would mean building a second, Volkare-shaped copy of the board/combat state the app has otherwise deliberately never modeled (this app scores completed games from tallies the player enters after the fact — see `architecture.md`'s "post-game wizard only" note on the Score Calculator; it doesn't track the board during play).

We chose the narrower role instead, matching what the standard Dummy Player already does: `VolkareSession` deterministically reveals the next card from Volkare's deck, tracks the discard pile, round count, and (Volkare's Return only) a manually-toggled `cityRevealed` flag, and logs each reveal as a `VolkareEvent`. The player reads each revealed card off the log, consults the physical rulebook's Course of the Game procedure themselves, and moves the Volkare figure/resolves combat by hand — the same division of labor as the standard Dummy Player, whose card flips the player also interprets by hand rather than the app tracking Source dice or combat outcomes.

`cityRevealed` is captured on each `VolkareEvent.CardRevealed` at the moment the card was revealed, not read live off the session when the log is displayed later — a card's guidance can read differently once the city's been revealed, so freezing the flag's value at reveal time keeps old log entries' narration stable even if the player toggles the setting afterward.

## Consequences

- `VolkareSession` never knows Volkare's board position, his army's composition/HP, or combat outcomes — only his deck/discard/round/log. If a future feature wants that (e.g. auto-resolving combat), it's a different, larger piece of work, not an extension of this session shape.
- The full Course of the Game movement/combat procedure stays undocumented in `docs/rules/` (as already noted in both scenario docs' "Not extracted here" sections) — this ADR is what makes that omission a deliberate, permanent scope boundary rather than a TODO.
- The player still needs the physical rulebook (or a future help dialog quoting it) open to interpret what a revealed card means for Volkare's move — this mode narrates, it doesn't referee.
