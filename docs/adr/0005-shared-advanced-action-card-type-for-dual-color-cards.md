# Dummy Player and Proxy Player share one Advanced Action card type

`DummyPlayerSession`'s deck is currently `List<CardColor>` — sufficient because every card it can ever hold (16 starting Basic Actions, plus Advanced Actions added at round end) has exactly one color. Dual-color Advanced Action cards (see `docs/rules/proxy-player.md`) break that assumption: a card that counts as two colors can enter *either* a standard Dummy Player's or a Proxy Player's deck, and only ever through the same round-end mechanism both share.

Rather than only supporting dual-color cards in the new `ProxyPlayerSession` and leaving `DummyPlayerSession` unable to represent one (which a table using dual-color cards would hit immediately, since standard Dummy Player mode is far more commonly used), both sessions consume one shared card-identity type distinguishing single-color from dual-color Advanced Actions. This means `DummyPlayerSession`'s deck type, `endRound` signature, and `remainingByColor`-style display all change as part of building Proxy Player mode, not just additive new code — a deliberate, if broader-than-expected, scope call. Since the app has never been published (no real on-device data to preserve — see `docs/adr/0003-room-tests-via-bundled-sqlite-driver.md`'s sibling reasoning), the Room schema change is a destructive migration, not a data-preserving one.

## Consequences

- `DummyPlayerScreen`'s deck-composition display needs a visual treatment for "this card counts as two colors" — not yet decided, to be worked out with the project owner separately from this decision.
- Basic Actions and Unique Basic Action Cards (Proxy Player only) never carry two colors — the shared type's dual-color case is reachable only via Advanced Actions.
