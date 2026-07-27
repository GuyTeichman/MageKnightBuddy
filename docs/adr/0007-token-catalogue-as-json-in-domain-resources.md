# Token catalogue as JSON in `domain/` resources

The Enemy Picker needs a full stat block for roughly 130 tokens — the 6 enemy colors, ruin tokens, 12 possessed enemies, and 24 faction tokens — each with name, expansion, pile, armor, Fame, attack value(s) and type, resistances, and an ability list. That is a large body of hand-transcribed reference data, and it has to live somewhere.

Three homes were considered:

- **Kotlin `val`s in `domain/`.** Zero new dependencies, compile-time checked (a misspelled ability becomes a build error), IDE-refactorable. Costs one very long, very boring source file.
- **JSON in `app/src/main/assets/`.** Mirrors the existing `field_help.json` exactly. But it makes the catalogue an Android asset, so no `domain` test can validate it, and the domain types would need a mapping layer.
- **JSON in `domain/src/main/resources/`, parsed with kotlinx-serialization.** Data separated from code, still loadable and testable on a plain JVM.

We chose the third. `field_help.json` is the obvious precedent but a poor analogy — it is *prose*, edited often, and genuinely benefits from living outside code — whereas the catalogue is a fixed transcription of physical components that never changes at runtime. What tipped it was keeping 130 mechanical records out of the source tree, where they would dominate the module by line count and make real domain logic harder to find.

## Consequences

- **`domain/` gains its first-ever dependency** (kotlinx-serialization). A reader who knows [ADR-0001](0001-domain-logic-as-plain-kotlin-module.md) will notice and should: the rule that matters there is "zero *Android* dependencies," which this respects — kotlinx-serialization is pure Kotlin and would move to a Kotlin Multiplatform `commonMain` source set unchanged. It is not a licence to add further dependencies casually.
- The trade-off accepted is a **runtime parse-failure mode for data the compiler could have checked for free** — a typo in an ability name yields a blank info window on the phone rather than a build error.
- The mitigation is mandatory, not optional: a `domain` test loads and validates the *entire* catalogue on every `make test`, asserting it parses, that every ability name resolves, that ids and image filenames are unique, and that each pile's token count matches the physical component count. This converts the runtime failure into a CI failure, which is the only reason this option is acceptable.
- Token art is *not* stored alongside the catalogue: images are Android assets in `app/src/main/assets/enemy-tokens/`, referenced by the catalogue's id. `domain` stays free of anything it cannot test.
