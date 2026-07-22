# Volkare mode narrates card implications instead of simulating the board

Volkare's actual turn logic (see `docs/rules/volkares-return.md` and `volkares-quest.md`'s "Course of the Game" sections) is driven by his literal position on the map, distance to the city/portal, which Heroes are adjacent, Source dice in play, and Unit-offer contents — none of which this app has ever modeled. Modeling it would mean mirroring the entire board state, a scope `docs/design/architecture.md` already rules out for the Score Calculator ("a deliberately separate, harder problem... isn't in scope") and that reasoning applies just as much here.

We chose to have Volkare mode reveal one card per turn and log a rules-derived, one-sentence description of what that card *means procedurally* (a move direction plus a Source die reroll instruction, a combat trigger, Frenzy, etc.) rather than attempting to resolve an actual outcome. The player reads the description, then resolves the real consequence — movement, combat, city assault — at the physical table themselves, the same hands-off relationship the standard Dummy Player screen already has with its own flipped cards.

## Consequences

- A reader expecting Volkare mode to "play" Volkare (move his token, resolve fights, know when the scenario ends) will be surprised — it only narrates card implications. Volkare's Quest's deck-exhaustion-as-automatic-loss (see `CONTEXT.md`'s Frenzy entry) is the one exception, since that specific case is fully determinable from deck state alone.
- If a future Proxy Player-style feature ever wants real board simulation, it needs its own design — this decision doesn't preclude that, it just keeps Volkare mode's scope to what card-driven narration alone can support.
