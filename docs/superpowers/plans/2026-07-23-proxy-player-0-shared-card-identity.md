# Shared Advanced Action Card Identity (Dual-Color Cards) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce a `CardIdentity` type (single-color vs. dual-color) shared by `DummyPlayerSession` and the future `ProxyPlayerSession`, and migrate `DummyPlayerSession`'s existing deck/discard/event/persistence/UI code to use it, so a Dual-Color Advanced Action card (see `docs/rules/proxy-player.md`) can enter either mode's deck via the existing round-end mechanism.

**Architecture:** `CardIdentity` is a plain-Kotlin sealed interface in `domain/` (`SingleColor(color)` / `DualColor(colorA, colorB)`) with a `matches(color)` predicate. `DummyPlayerSession`'s `deckOrder`/`discardPile` change from `List<CardColor>` to `List<CardIdentity>`; its crystal-chain check in `playTurn()` sums matching crystals across both colors for a `DualColor` card. Persistence gets a matching `CardIdentityDto` and a destructive Room migration (no real on-device data — see `docs/adr/0003-room-tests-via-bundled-sqlite-driver.md`'s sibling reasoning). The UI's End Round dialog gains a way to say "this was a dual-color card" and pick its second color.

**Tech Stack:** Kotlin, kotlinx.serialization, Room, Jetpack Compose (Material 3).

## Global Constraints

- `domain/` stays plain Kotlin, zero Android/serialization dependencies — see [ADR-0001](../../adr/0001-domain-logic-as-plain-kotlin-module.md). `CardIdentity` lives there; its serializable DTO mirror lives in `data/`.
- No backwards-compatible Room migration — bump the DB version and let old rows fail to parse (this app has never been published; see [ADR-0005](../../adr/0005-shared-advanced-action-card-type-for-dual-color-cards.md)).
- Every public class/function gets a short KDoc summary per this project's commenting standard (see root `CLAUDE.md`).
- Build with `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew test` (domain/data unit tests) and `./gradlew build` (full build) before each commit.

---

### Task 1: `CardIdentity` domain type

**Files:**
- Create: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/CardIdentity.kt`
- Test: `domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/CardIdentityTest.kt`

**Interfaces:**
- Produces: `CardIdentity` sealed interface with `SingleColor(color: CardColor)` and `DualColor(colorA: CardColor, colorB: CardColor)`, a `matches(color: CardColor): Boolean` member, and a `matchingCrystalCount(crystals: Map<CardColor, Int>): Int` extension function. Consumed by Task 2 onward.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardIdentityTest {

    @Test
    fun `SingleColor matches only its own color`() {
        val card = CardIdentity.SingleColor(CardColor.RED)

        assertTrue(card.matches(CardColor.RED))
        assertFalse(card.matches(CardColor.GREEN))
    }

    @Test
    fun `DualColor matches either of its two colors`() {
        val card = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)

        assertTrue(card.matches(CardColor.GREEN))
        assertTrue(card.matches(CardColor.BLUE))
        assertFalse(card.matches(CardColor.RED))
    }

    @Test
    fun `matchingCrystalCount for SingleColor is that color's crystal count`() {
        val card = CardIdentity.SingleColor(CardColor.RED)
        val crystals = mapOf(CardColor.RED to 2, CardColor.GREEN to 0, CardColor.BLUE to 1, CardColor.WHITE to 0)

        assertEquals(2, card.matchingCrystalCount(crystals))
    }

    @Test
    fun `matchingCrystalCount for DualColor sums both colors' crystal counts`() {
        val card = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)
        val crystals = mapOf(CardColor.RED to 0, CardColor.GREEN to 2, CardColor.BLUE to 1, CardColor.WHITE to 0)

        assertEquals(3, card.matchingCrystalCount(crystals))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.CardIdentityTest"`
Expected: FAIL — `CardIdentity` is unresolved.

- [ ] **Step 3: Implement `CardIdentity`**

```kotlin
package com.guyteichman.mageknightbuddy.domain

/**
 * Which color(s) a card counts as. Every Dummy Player / Proxy Player deck card is either a
 * single fixed color, or one of the 4 Dual-Color Advanced Action cards (see
 * `docs/rules/proxy-player.md`) that count as two colors at once. Used wherever "does this
 * crystal color match this card" needs to work identically for both cases - see
 * [DummyPlayerSession.playTurn]'s crystal-chain check.
 */
sealed interface CardIdentity {
    /** True if [color] matches this card - for [DualColor], either of its two colors counts. */
    fun matches(color: CardColor): Boolean

    /** An ordinary card with one fixed color - every Basic Action and single-color Advanced Action. */
    data class SingleColor(val color: CardColor) : CardIdentity {
        override fun matches(color: CardColor): Boolean = this.color == color
    }

    /**
     * One of the 4 Dual-Color Advanced Action cards (Power of Crystals, Chilling Stare,
     * Explosive Bolt, Rush of Adrenaline - see `docs/rules/proxy-player.md`), which counts as
     * both [colorA] and [colorB] for crystal-chain matching.
     */
    data class DualColor(val colorA: CardColor, val colorB: CardColor) : CardIdentity {
        override fun matches(color: CardColor): Boolean = color == colorA || color == colorB
    }
}

/**
 * Sum of [crystals] held for every color this card matches - a [CardIdentity.DualColor] card
 * sums both of its colors' crystal counts, per this app's dual-color deck-flip ruling (not
 * stated in the rulebook - see `docs/rules/proxy-player.md`'s "The Proxy Player's turn").
 */
fun CardIdentity.matchingCrystalCount(crystals: Map<CardColor, Int>): Int = when (this) {
    is CardIdentity.SingleColor -> crystals.getValue(color)
    is CardIdentity.DualColor -> crystals.getValue(colorA) + crystals.getValue(colorB)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.CardIdentityTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/CardIdentity.kt domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/CardIdentityTest.kt
git commit -m "Add CardIdentity for single- and dual-color Advanced Action cards"
```

---

### Task 2: Migrate `DummyPlayerSession`/`DummyPlayerEvent` to `CardIdentity`

**Files:**
- Modify: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSession.kt`
- Modify: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerEvent.kt`
- Modify: `domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSessionTest.kt`

**Interfaces:**
- Consumes: `CardIdentity` (Task 1).
- Produces: `DummyPlayerSession.deckOrder`/`discardPile: List<CardIdentity>`; `DummyPlayerSession.endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor)`; `DummyPlayerEvent.TurnPlayed(round, initialReveal: List<CardIdentity>, additionalReveal: List<CardIdentity>)`; `DummyPlayerEvent.RoundEnded(round, advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor)`. `remainingByColor: Map<CardColor, Int>` keeps its existing signature. Consumed by the persistence-layer plan (mapper/entity/dto) and the UI-layer plan (`DummyPlayerAiViewModel`/`DummyPlayerScreen`).

- [ ] **Step 1: Update `DummyPlayerEvent.kt`'s two affected cases**

Replace the `TurnPlayed` and `RoundEnded` cases in `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerEvent.kt`:

```kotlin
    /**
     * Recorded after [DummyPlayerSession.playTurn] resolves a turn: the flip-3-cards-then-chain-
     * on-crystal-match procedure from docs/rules/dummy-player.md ("Turn procedure"). [initialReveal]
     * is the mandatory first 3 cards flipped; [additionalReveal] is any extra cards flipped because
     * the 3rd card's color(s) matched a crystal the Dummy Player holds (empty if there was no match).
     */
    data class TurnPlayed(
        val round: Int,
        val initialReveal: List<CardIdentity>,
        val additionalReveal: List<CardIdentity>,
    ) : DummyPlayerEvent
```

```kotlin
    /**
     * Recorded after [DummyPlayerSession.endRound] resolves the round-prep offer interactions that
     * feed the Dummy Player's deck and crystals - docs/rules/dummy-player.md ("End of Round").
     * [advancedActionOfferColor] is the card added to its deck (single- or dual-color, see
     * [CardIdentity]); [spellOfferColor] is the color of the crystal added to its Inventory (Spells
     * are never dual-color).
     */
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardIdentity,
        val spellOfferColor: CardColor,
    ) : DummyPlayerEvent
```

- [ ] **Step 2: Update `DummyPlayerSession.kt`'s type signatures and logic**

In `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSession.kt`, change the class properties:

```kotlin
data class DummyPlayerSession private constructor(
    val knight: Knight,
    val wasRandom: Boolean,
    val deckOrder: List<CardIdentity>,
    val discardPile: List<CardIdentity>,
    val crystals: Map<CardColor, Int>,
    val round: Int,
    val roundEnded: Boolean,
    val log: List<DummyPlayerEvent>,
) {
    val remainingByColor: Map<CardColor, Int>
        get() = CardColor.entries.associateWith { color -> deckOrder.count { it.matches(color) } }
```

Change `playTurn()`'s crystal-chain lookup:

```kotlin
    fun playTurn(): DummyPlayerSession {
        if (roundEnded) return this

        if (deckOrder.isEmpty()) {
            return copy(roundEnded = true, log = log + DummyPlayerEvent.EndOfRoundAnnounced(round))
        }

        val initialReveal = deckOrder.take(3)
        val afterInitial = deckOrder.drop(3)
        val lastCard = initialReveal.last()
        val matchingCrystals = lastCard.matchingCrystalCount(crystals)
        val additionalCount = minOf(matchingCrystals, afterInitial.size)
        val additionalReveal = afterInitial.take(additionalCount)
        val remainingDeck = afterInitial.drop(additionalCount)

        return copy(
            deckOrder = remainingDeck,
            discardPile = discardPile + initialReveal + additionalReveal,
            log = log + DummyPlayerEvent.TurnPlayed(round, initialReveal, additionalReveal),
        )
    }
```

Change `endRound()`'s signature:

```kotlin
    fun endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor): DummyPlayerSession = copy(
        deckOrder = (deckOrder + advancedActionOfferColor).shuffled(),
        crystals = crystals + (spellOfferColor to crystals.getValue(spellOfferColor) + 1),
        round = round + 1,
        roundEnded = false,
        log = log + DummyPlayerEvent.RoundEnded(round, advancedActionOfferColor, spellOfferColor),
    )
```

Change the companion object's default deck builder and `restore`'s parameter types:

```kotlin
        fun start(
            knight: Knight,
            wasRandom: Boolean = false,
            deckOrder: List<CardIdentity> = CardColor.entries.flatMap { color -> List(4) { CardIdentity.SingleColor(color) } }.shuffled(),
        ): DummyPlayerSession = DummyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = emptyList(),
            crystals = startingCrystals(knight),
            round = 1,
            roundEnded = false,
            log = listOf(DummyPlayerEvent.RoundStarted(round = 1)),
        )
```

```kotlin
        fun restore(
            knight: Knight,
            wasRandom: Boolean,
            deckOrder: List<CardIdentity>,
            discardPile: List<CardIdentity>,
            crystals: Map<CardColor, Int>,
            round: Int,
            roundEnded: Boolean,
            log: List<DummyPlayerEvent>,
        ): DummyPlayerSession = DummyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = discardPile,
            crystals = crystals,
            round = round,
            roundEnded = roundEnded,
            log = log,
        )
```

- [ ] **Step 3: Run the build to see the compile errors in the test file**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:compileTestKotlin`
Expected: FAIL — type mismatches in `DummyPlayerSessionTest.kt` (`List<CardColor>` where `List<CardIdentity>` is now expected, and `CardColor` where `CardIdentity` is now expected).

- [ ] **Step 4: Migrate the existing test file, and add 2 new dual-color tests**

Replace `domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSessionTest.kt` in full:

```kotlin
package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class DummyPlayerSessionTest {

    @Test
    fun `start creates a 16-card deck of 4 Red, 4 Green, 4 Blue, 4 White`() {
        val session = DummyPlayerSession.start(Knight.GOLDYX)

        assertEquals(
            mapOf(
                CardColor.RED to 4,
                CardColor.GREEN to 4,
                CardColor.BLUE to 4,
                CardColor.WHITE to 4,
            ),
            session.remainingByColor,
        )
    }

    @Test
    fun `start gives Goldyx 2 Green and 1 Blue crystal, per the rulebook's own example`() {
        val session = DummyPlayerSession.start(Knight.GOLDYX)

        assertEquals(
            mapOf(
                CardColor.RED to 0,
                CardColor.GREEN to 2,
                CardColor.BLUE to 1,
                CardColor.WHITE to 0,
            ),
            session.crystals,
        )
    }

    @Test
    fun `start gives Arythea 2 Red and 1 White crystal`() {
        val session = DummyPlayerSession.start(Knight.ARYTHEA)

        assertEquals(
            mapOf(
                CardColor.RED to 2,
                CardColor.GREEN to 0,
                CardColor.BLUE to 0,
                CardColor.WHITE to 1,
            ),
            session.crystals,
        )
    }

    @Test
    fun `startRandom sets wasRandom and picks a knight whose crystals match the starting table`() {
        val session = DummyPlayerSession.startRandom(random = Random(0))

        assertEquals(true, session.wasRandom)
        assertEquals(DummyPlayerSession.start(session.knight).crystals, session.crystals)
    }

    @Test
    fun `start sets wasRandom to false for an explicitly chosen knight`() {
        val session = DummyPlayerSession.start(Knight.CORAL)

        assertEquals(false, session.wasRandom)
        assertEquals(Knight.CORAL, session.knight)
    }

    @Test
    fun `playTurn flips 3 cards and ends the turn when the 3rd card's color has no matching crystal`() {
        // Coral's starting crystals are White, White, Red (no Green) - see docs/rules/dummy-player.md's example.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.WHITE, CardColor.RED, CardColor.GREEN, CardColor.BLUE, CardColor.WHITE)
                .map { CardIdentity.SingleColor(it) },
        )

        val next = session.playTurn()

        assertEquals(
            listOf(CardColor.WHITE, CardColor.RED, CardColor.GREEN).map { CardIdentity.SingleColor(it) },
            next.discardPile,
        )
        assertEquals(listOf(CardColor.BLUE, CardColor.WHITE).map { CardIdentity.SingleColor(it) }, next.deckOrder)
    }

    @Test
    fun `playTurn chains one additional reveal per matching crystal of the 3rd card's color`() {
        // Coral holds 2 White crystals - the 3rd card (White) should chain 2 additional reveals.
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                CardColor.WHITE, CardColor.RED, CardColor.WHITE,
                CardColor.GREEN, CardColor.WHITE, CardColor.RED,
            ).map { CardIdentity.SingleColor(it) },
        )

        val next = session.playTurn()

        assertEquals(
            listOf(CardColor.WHITE, CardColor.RED, CardColor.WHITE, CardColor.GREEN, CardColor.WHITE)
                .map { CardIdentity.SingleColor(it) },
            next.discardPile,
        )
        assertEquals(listOf(CardIdentity.SingleColor(CardColor.RED)), next.deckOrder)
    }

    @Test
    fun `playTurn's crystal-chain match counts crystals of either of a Dual-Color card's two colors`() {
        // Coral holds 2 White, 1 Red crystals, 0 Green/Blue. A Green+Blue dual-color 3rd card
        // matches neither color directly, but Power of Crystals-style cards would; here we use a
        // dual-color card matching Coral's owned colors (White+Red) to assert the chain triggers
        // for the SUM of both matched colors' crystals (2 White + 1 Red = 3 additional reveals).
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                CardIdentity.SingleColor(CardColor.GREEN),
                CardIdentity.SingleColor(CardColor.BLUE),
                CardIdentity.DualColor(CardColor.WHITE, CardColor.RED),
                CardIdentity.SingleColor(CardColor.GREEN),
                CardIdentity.SingleColor(CardColor.BLUE),
                CardIdentity.SingleColor(CardColor.GREEN),
            ),
        )

        val next = session.playTurn()

        assertEquals(
            listOf(
                CardIdentity.SingleColor(CardColor.GREEN),
                CardIdentity.SingleColor(CardColor.BLUE),
                CardIdentity.DualColor(CardColor.WHITE, CardColor.RED),
                CardIdentity.SingleColor(CardColor.GREEN),
                CardIdentity.SingleColor(CardColor.BLUE),
                CardIdentity.SingleColor(CardColor.GREEN),
            ),
            next.discardPile,
        )
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn on a near-empty deck flips only what's available, same logic as a full flip`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.BLUE, CardColor.WHITE).map { CardIdentity.SingleColor(it) },
        )

        val next = session.playTurn()

        assertEquals(listOf(CardColor.BLUE, CardColor.WHITE).map { CardIdentity.SingleColor(it) }, next.discardPile)
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn on a near-empty deck still chains if there's nothing left to chain into`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.GREEN, CardColor.WHITE).map { CardIdentity.SingleColor(it) },
        )

        val next = session.playTurn()

        assertEquals(listOf(CardColor.GREEN, CardColor.WHITE).map { CardIdentity.SingleColor(it) }, next.discardPile)
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn on an empty deck announces End of Round instead of flipping`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.playTurn()

        assertEquals(true, next.roundEnded)
        assertEquals(emptyList(), next.discardPile)
        assertEquals(DummyPlayerEvent.EndOfRoundAnnounced(round = 1), next.log.last())
    }

    @Test
    fun `playTurn is a no-op once roundEnded is already true`() {
        val ended = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = ended.playTurn()

        assertEquals(ended.roundEnded, next.roundEnded)
        assertEquals(ended.log, next.log)
        assertEquals(ended.deckOrder, next.deckOrder)
    }

    @Test
    fun `endRound appends the Advanced Action offer color to the deck, reshuffled`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.RED, CardColor.GREEN).map { CardIdentity.SingleColor(it) },
        )

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(
            mapOf(CardColor.RED to 1, CardColor.GREEN to 1, CardColor.BLUE to 0, CardColor.WHITE to 1),
            next.remainingByColor,
        )
    }

    @Test
    fun `endRound can append a Dual-Color Advanced Action card, counted toward both colors' remainingByColor`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(1, next.remainingByColor.getValue(CardColor.GREEN))
        assertEquals(1, next.remainingByColor.getValue(CardColor.BLUE))
        assertEquals(listOf(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)), next.deckOrder)
    }

    @Test
    fun `endRound grants +1 crystal of the Spell offer color, uncapped`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(3, next.crystals.getValue(CardColor.WHITE))
    }

    @Test
    fun `endRound increments the round, resets roundEnded, and logs the round-ended event`() {
        val session = DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, next.round)
        assertEquals(false, next.roundEnded)
        assertEquals(
            DummyPlayerEvent.RoundEnded(
                round = 1,
                advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
                spellOfferColor = CardColor.BLUE,
            ),
            next.log.last(),
        )
    }

    @Test
    fun `endRound is always callable, even mid-round with cards still in the deck`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardIdentity.SingleColor(CardColor.RED)),
        )

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, next.round)
    }

    @Test
    fun `restore reconstructs a session with the exact same state it was given`() {
        val original = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(CardColor.RED, CardColor.GREEN).map { CardIdentity.SingleColor(it) },
        ).playTurn().endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        val restored = DummyPlayerSession.restore(
            knight = original.knight,
            wasRandom = original.wasRandom,
            deckOrder = original.deckOrder,
            discardPile = original.discardPile,
            crystals = original.crystals,
            round = original.round,
            roundEnded = original.roundEnded,
            log = original.log,
        )

        assertEquals(original, restored)
    }
}
```

- [ ] **Step 5: Run the domain tests**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.DummyPlayerSessionTest"`
Expected: PASS (18 tests)

- [ ] **Step 6: Commit**

```bash
git add domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSession.kt domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerEvent.kt domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSessionTest.kt
git commit -m "Migrate DummyPlayerSession's deck to CardIdentity for dual-color card support"
```

---

### Task 3: Persistence — `CardIdentityDto`, mapper, entity, DB version bump

**Files:**
- Create: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/CardIdentityDto.kt`
- Modify: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/DummyPlayerEventDto.kt`
- Modify: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/DummyPlayerSessionMapper.kt`
- Test: `data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/DummyPlayerSessionMapperTest.kt`
- Modify (DB version + migration note): wherever the app's `RoomDatabase` version is declared — locate via `grep -rn "@Database" app/ data/`

**Interfaces:**
- Consumes: `CardIdentity` (Task 1), `DummyPlayerSession`/`DummyPlayerEvent` (Task 2).
- Produces: `CardIdentityDto` sealed interface (`SingleColor(color: String)` / `DualColor(colorA: String, colorB: String)`) plus `CardIdentity.toDto()`/`CardIdentityDto.toDomain()` extension functions in the mapper file, consumed by the UI-layer plan's mapper usage indirectly (no direct UI dependency).

- [ ] **Step 1: Locate the Room database version**

Run: `grep -rn "@Database" "D:/Guy_Teichman/PyCharmProjects/MageKnightBuddy/.claude/worktrees/shield-assets-refresh/app" "D:/Guy_Teichman/PyCharmProjects/MageKnightBuddy/.claude/worktrees/shield-assets-refresh/data"`

Note the file and current `version = N`. You will bump it to `N + 1` in Step 5.

- [ ] **Step 2: Write the failing mapper test additions**

Add to `data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/DummyPlayerSessionMapperTest.kt` (append inside the existing test class — read the file first to match its existing style/imports before appending):

```kotlin
    @Test
    fun `toEntity and toDomain round-trip a deck containing a Dual-Color Advanced Action card`() {
        val session = DummyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                CardIdentity.SingleColor(CardColor.RED),
                CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            ),
        ).endRound(
            advancedActionOfferColor = CardIdentity.DualColor(CardColor.WHITE, CardColor.RED),
            spellOfferColor = CardColor.BLUE,
        )

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }
```

- [ ] **Step 3: Run the test to verify it fails to compile/pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test --tests "com.guyteichman.mageknightbuddy.data.DummyPlayerSessionMapperTest"`
Expected: FAIL — `toEntity`/`toDomain` still expect `List<CardColor>` (compile error) until Steps 4-5 land.

- [ ] **Step 4: Create `CardIdentityDto.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.CardIdentity], kept in
 * `data/` for the same reason [DummyPlayerEventDto] is: `domain/` must stay free of
 * kotlinx.serialization annotations/dependencies (see
 * docs/adr/0001-domain-logic-as-plain-kotlin-module.md). [DummyPlayerSessionMapper] converts
 * between the two.
 */
@Serializable
sealed interface CardIdentityDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.CardIdentity.SingleColor]. */
    @Serializable
    @SerialName("single_color")
    data class SingleColor(val color: String) : CardIdentityDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.CardIdentity.DualColor]. */
    @Serializable
    @SerialName("dual_color")
    data class DualColor(val colorA: String, val colorB: String) : CardIdentityDto
}
```

- [ ] **Step 5: Update `DummyPlayerEventDto.kt`'s two affected cases**

Replace the `TurnPlayed` and `RoundEnded` cases in `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/DummyPlayerEventDto.kt`:

```kotlin
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent.TurnPlayed]. */
    @Serializable
    @SerialName("turn_played")
    data class TurnPlayed(
        val round: Int,
        val initialReveal: List<CardIdentityDto>,
        val additionalReveal: List<CardIdentityDto>,
    ) : DummyPlayerEventDto
```

```kotlin
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent.RoundEnded]. */
    @Serializable
    @SerialName("round_ended")
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardIdentityDto,
        val spellOfferColor: String,
    ) : DummyPlayerEventDto
```

Also bump the schema: since the `logJson` column's JSON *shape* changed for `TurnPlayed`/`RoundEnded` even though the column type didn't, a stale saved row will fail `kotlinx.serialization` decoding on launch — same class of issue already documented in the `ReputationTrackSpace` migration (see git log commit "Collapse For the Council's Reputation modifier..."). Note this as a comment above the `@Database` version bump in Step 8.

- [ ] **Step 6: Update `DummyPlayerSessionMapper.kt`**

Add the `CardIdentity <-> CardIdentityDto` conversion functions and update the deck/discard/event conversions:

```kotlin
// Maps a domain CardIdentity to its DTO mirror.
private fun CardIdentity.toDto(): CardIdentityDto = when (this) {
    is CardIdentity.SingleColor -> CardIdentityDto.SingleColor(color.name)
    is CardIdentity.DualColor -> CardIdentityDto.DualColor(colorA.name, colorB.name)
}

// The reverse of toDto() above.
private fun CardIdentityDto.toDomain(): CardIdentity = when (this) {
    is CardIdentityDto.SingleColor -> CardIdentity.SingleColor(CardColor.valueOf(color))
    is CardIdentityDto.DualColor -> CardIdentity.DualColor(CardColor.valueOf(colorA), CardColor.valueOf(colorB))
}

// Replaces the old `List<CardColor>.toJson()`/`String.toCardColorList()` pair.
private fun List<CardIdentity>.toJson(): String = Json.encodeToString(map { it.toDto() })
private fun String.toCardIdentityList(): List<CardIdentity> = Json.decodeFromString<List<CardIdentityDto>>(this).map { it.toDomain() }
```

Update the `DummyPlayerEvent.toDto()`/`DummyPlayerEventDto.toDomain()` `TurnPlayed`/`RoundEnded` branches:

```kotlin
    is DummyPlayerEvent.TurnPlayed -> DummyPlayerEventDto.TurnPlayed(
        round = round,
        initialReveal = initialReveal.map { it.toDto() },
        additionalReveal = additionalReveal.map { it.toDto() },
    )
    is DummyPlayerEvent.RoundEnded -> DummyPlayerEventDto.RoundEnded(
        round = round,
        advancedActionOfferColor = advancedActionOfferColor.toDto(),
        spellOfferColor = spellOfferColor.name,
    )
```

```kotlin
    is DummyPlayerEventDto.TurnPlayed -> DummyPlayerEvent.TurnPlayed(
        round = round,
        initialReveal = initialReveal.map { it.toDomain() },
        additionalReveal = additionalReveal.map { it.toDomain() },
    )
    is DummyPlayerEventDto.RoundEnded -> DummyPlayerEvent.RoundEnded(
        round = round,
        advancedActionOfferColor = advancedActionOfferColor.toDomain(),
        spellOfferColor = CardColor.valueOf(spellOfferColor),
    )
```

Update `DummyPlayerSession.toEntity()`/`DummyPlayerSessionEntity.toDomain()` to call `.toJson()`/`.toCardIdentityList()` instead of the old `CardColor`-only helpers (`deckOrder.toJson()`, `discardPileJson.toCardIdentityList()` etc. — same call sites, new helper names).

- [ ] **Step 7: Run the mapper test**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test --tests "com.guyteichman.mageknightbuddy.data.DummyPlayerSessionMapperTest"`
Expected: PASS

- [ ] **Step 8: Bump the Room database version**

At the `@Database` declaration found in Step 1, increment `version` by 1 and add a one-line comment above it: `// vN -> vN+1: DummyPlayerSessionEntity's logJson/deckOrderJson shape changed (CardIdentity for dual-color cards) - destructive migration, no real user data (see docs/adr/0005-shared-advanced-action-card-type-for-dual-color-cards.md).` Confirm (via the same `grep -rn "@Database"` output) that `fallbackToDestructiveMigration()` (or equivalent) is already configured — if not, this is out of scope for this task; flag it rather than silently adding new migration machinery.

- [ ] **Step 9: Run the full data module test suite**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test`
Expected: PASS (all existing DAO/mapper/repository tests, plus the new one)

- [ ] **Step 10: Commit**

```bash
git add data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/CardIdentityDto.kt data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/DummyPlayerEventDto.kt data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/DummyPlayerSessionMapper.kt data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/DummyPlayerSessionMapperTest.kt
git commit -m "Persist CardIdentity for Dummy Player's deck; bump Room schema version"
```

---

### Task 4: UI — End Round dialog supports a dual-color Advanced Action

**Files:**
- Modify: `app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerAiViewModel.kt`
- Modify: `app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerScreen.kt` (the End Round dialog, around the `HelpButton(keys = listOf("Round-Prep Offers"), ...)` call site)
- Test: `app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerAiViewModelTest.kt`

**Interfaces:**
- Consumes: `CardIdentity` (Task 1), `DummyPlayerSession.endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor)` (Task 2).
- Produces: `DummyPlayerAiViewModel.endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor)`.

**Note on visuals:** the exact visual treatment for "this card counts as two colors" (badge, split-color chip, separate row in the deck-composition display) is explicitly **not decided yet** — see ADR-0005's Consequences. This task wires the End Round dialog's *input* (letting the player report a dual-color card) with a minimal, functional UI (a checkbox + second color dropdown), not a final visual design. Confirm the deck-composition display's dual-color treatment with the project owner before polishing it further.

- [ ] **Step 1: Write the failing ViewModel test**

Read `app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerAiViewModelTest.kt` first to match its existing fixture/setup style, then add:

```kotlin
    @Test
    fun `endRound accepts a Dual-Color Advanced Action offer card`() = runTest {
        val repository = DummyPlayerSessionRepository(FakeDummyPlayerSessionDao())
        repository.save(DummyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()))
        val viewModel = DummyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.endRound(
            advancedActionOfferColor = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(
            CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            viewModel.session?.deckOrder?.single(),
        )
    }
```

(Match the exact `runTest`/`advanceUntilIdle` idiom already used by the surrounding tests in that file — copy it verbatim rather than guessing at the coroutine test setup.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :app:testDebugUnitTest --tests "com.guyteichman.mageknightbuddy.ui.dummyplayer.DummyPlayerAiViewModelTest"`
Expected: FAIL — `endRound`'s parameter type is still `CardColor`.

- [ ] **Step 3: Update `DummyPlayerAiViewModel.endRound`'s signature**

```kotlin
    /**
     * Applies the round-prep offer interactions (see docs/rules/dummy-player.md) and autosaves.
     * [advancedActionOfferColor] is the card removed from the Advanced Action offer this
     * round-prep - single- or dual-color, see [CardIdentity] - and [spellOfferColor] is the color
     * of the card removed from the Spell offer (Spells are never dual-color).
     */
    suspend fun endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor) {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.endRound(advancedActionOfferColor, spellOfferColor) ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }
```

- [ ] **Step 4: Run the ViewModel test**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :app:testDebugUnitTest --tests "com.guyteichman.mageknightbuddy.ui.dummyplayer.DummyPlayerAiViewModelTest"`
Expected: PASS

- [ ] **Step 5: Update the End Round dialog's Advanced Action color picker**

Read the End Round dialog composable in `app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerScreen.kt` (around the `HelpButton(keys = listOf("Round-Prep Offers"), ...)` call site) to find the existing single-color picker state (`var advancedActionOfferColor by remember { mutableStateOf(CardColor.entries.first()) }` or equivalent). Add a second piece of local state, `var isDualColor by remember { mutableStateOf(false) }` and `var secondColor by remember { mutableStateOf(CardColor.entries.first()) }`, a `Checkbox`/`LabeledCheckbox` labeled "Dual-color card", and — when checked — a second color dropdown reusing whatever color-picker composable the existing single picker already uses. Change the dialog's confirm action to build `if (isDualColor) CardIdentity.DualColor(advancedActionOfferColor, secondColor) else CardIdentity.SingleColor(advancedActionOfferColor)` and pass that to `viewModel.endRound(...)`.

This step is UI scaffolding with no new domain logic (the dialog just assembles a `CardIdentity` from two dropdown selections) — per this project's CLAUDE.md, it doesn't need TDD rigor, but every new composable/property still needs its KDoc/inline-comment treatment per the commenting standard.

- [ ] **Step 6: Manually verify in the running app**

Use the `run` skill to launch the app, start a Dummy Player session, tap "End Round", check "Dual-color card", pick two colors, confirm, and verify the deck-composition display doesn't crash (its exact dual-color visual is out of scope here — just confirm no crash and the counts are sane per Task 2's `remainingByColor` semantics).

- [ ] **Step 7: Run the full app test suite and build**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew test build`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerAiViewModel.kt app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerScreen.kt app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerAiViewModelTest.kt
git commit -m "Let the End Round dialog report a Dual-Color Advanced Action card"
```
