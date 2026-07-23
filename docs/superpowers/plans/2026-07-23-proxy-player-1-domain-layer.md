# Proxy Player Domain Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `ProxyPlayerSession` (and its supporting `ProxyPlayerCard`/`ProxyPlayerEvent` types) in `domain/`: the pure-Kotlin state machine for Proxy Player mode, per `docs/rules/proxy-player.md` and `CONTEXT.md`'s **Proxy Player Session**/**Objective Card** entries.

**Architecture:** Mirrors `VolkareSession`'s shape exactly (private constructor + companion factory functions `start`/`startRandom`/`restore`, immutable `copy()`-based transitions, an append-only `log`). `ProxyPlayerCard` distinguishes generic Basic Actions, Unique Basic Action Cards, and Advanced Actions (reusing `CardIdentity` from the shared-card-identity plan for the latter's single/dual-color split). The session narrates rather than simulates (ADR-0004): it tracks deck, discard, crystals, current Objective Card, and its Shield count, and computes the movement-point formula, but never decides map targets.

**Tech Stack:** Kotlin (plain, no Android dependencies — domain/ module).

**Depends on:** `docs/superpowers/plans/2026-07-23-proxy-player-0-shared-card-identity.md` must land first — this plan's `ProxyPlayerCard.AdvancedAction` case wraps `CardIdentity` from that plan.

## Global Constraints

- `domain/` stays plain Kotlin, zero Android/serialization dependencies — see [ADR-0001](../../adr/0001-domain-logic-as-plain-kotlin-module.md).
- Every public class/function gets a short KDoc summary per this project's commenting standard (root `CLAUDE.md`).
- Cross-check every rule against `docs/rules/proxy-player.md` before considering a step done — do not trust memory of the rulebook.
- Run `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test` before each commit.

---

### Task 1: Extract `startingCrystals` as a shared, non-private function

**Files:**
- Modify: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSession.kt`

**Interfaces:**
- Produces: top-level `internal fun startingCrystals(knight: Knight): Map<CardColor, Int>` and `internal val STARTING_CRYSTAL_DOTS: Map<Knight, List<CardColor>>`, moved out of `DummyPlayerSession`'s companion object (where they're currently `private`) to file scope so `ProxyPlayerSession` (Task 4) can reuse them — both modes' Setup step draws crystals from the same per-Knight dots table (`docs/rules/dummy-player.md`'s Setup, reused verbatim by `docs/rules/proxy-player.md`'s "At the start of the game").

- [ ] **Step 1: Run the existing tests to record the current passing baseline**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.DummyPlayerSessionTest"`
Expected: PASS (18 tests, per the shared-card-identity plan's Task 2) — this is the refactor's safety net, not a new failing test, since moving a private function to file scope changes no behavior.

- [ ] **Step 2: Move `STARTING_CRYSTAL_DOTS` and `startingCrystals` out of the companion object**

In `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSession.kt`, cut the `STARTING_CRYSTAL_DOTS` property and `startingCrystals` function out of `DummyPlayerSession`'s `companion object` block and paste them at file scope (outside the class entirely, same file), changing their visibility from `private` to `internal`:

```kotlin
/**
 * Starting crystal dots per Knight, from docs/rules/dummy-player.md ("Setup"): the Dummy
 * Player (and, per docs/rules/proxy-player.md's "At the start of the game", the Proxy Player
 * too) starts with one crystal per colored dot on the bottom of its Hero card (e.g. Goldyx's
 * dots are green, green, blue, so it starts with 2 green + 1 blue crystal). The base rulebook
 * only shows Goldyx as its worked example - the other 7 Knights' values were sourced by
 * visually reading their actual Hero cards (see the full per-Knight table and source citations
 * in docs/rules/dummy-player.md, or research tickets #31/#66/#70). Feeds [startingCrystals]
 * below. `internal` (not `private`) so [ProxyPlayerSession] can reuse it too.
 */
internal val STARTING_CRYSTAL_DOTS: Map<Knight, List<CardColor>> = mapOf(
    Knight.TOVAK to listOf(CardColor.RED, CardColor.BLUE, CardColor.BLUE),
    Knight.GOLDYX to listOf(CardColor.GREEN, CardColor.GREEN, CardColor.BLUE),
    Knight.NOROWAS to listOf(CardColor.GREEN, CardColor.WHITE, CardColor.WHITE),
    Knight.WOLFHAWK to listOf(CardColor.WHITE, CardColor.WHITE, CardColor.BLUE),
    Knight.ARYTHEA to listOf(CardColor.RED, CardColor.RED, CardColor.WHITE),
    Knight.KRANG to listOf(CardColor.RED, CardColor.RED, CardColor.GREEN),
    Knight.BRAEVALAR to listOf(CardColor.GREEN, CardColor.BLUE, CardColor.BLUE),
    Knight.CORAL to listOf(CardColor.WHITE, CardColor.WHITE, CardColor.RED),
)

/** Converts a Knight's starting dots (see [STARTING_CRYSTAL_DOTS]) into a color-to-count map for the initial crystal Inventory. Shared by [DummyPlayerSession] and [ProxyPlayerSession]. */
internal fun startingCrystals(knight: Knight): Map<CardColor, Int> {
    val dots = STARTING_CRYSTAL_DOTS.getValue(knight)
    return CardColor.entries.associateWith { color -> dots.count { it == color } }
}
```

Update the two call sites inside `DummyPlayerSession`'s companion object (`start`'s `crystals = startingCrystals(knight)`) — unchanged, since the function name/signature didn't change, only its location and visibility.

- [ ] **Step 3: Run the tests again to confirm no behavior changed**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.DummyPlayerSessionTest"`
Expected: PASS (same 18 tests, unchanged)

- [ ] **Step 4: Commit**

```bash
git add domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/DummyPlayerSession.kt
git commit -m "Share startingCrystals between DummyPlayerSession and ProxyPlayerSession"
```

---

### Task 2: `ProxyPlayerCard` and `ProxyPlayerObjectiveResolution`

**Files:**
- Create: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerCard.kt`
- Test: `domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerCardTest.kt`

**Interfaces:**
- Consumes: `CardIdentity`/`CardIdentity.matchingCrystalCount` (shared-card-identity plan, Task 1).
- Produces: `ProxyPlayerCard` sealed interface (`BasicAction(color)`, `UniqueAction(color)`, `AdvancedAction(identity: CardIdentity)`) with `movementBonus: Int` and `matches(color: CardColor): Boolean`; `ProxyPlayerCard.matchingCrystalCount(crystals): Int` extension; `ProxyPlayerObjectiveResolution` enum (`EXPLORED`, `COMPLETED`). Consumed by Task 3 (`ProxyPlayerEvent`) and Tasks 4-6 (`ProxyPlayerSession`).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProxyPlayerCardTest {

    @Test
    fun `BasicAction has a movement bonus of 1 and matches only its own color`() {
        val card = ProxyPlayerCard.BasicAction(CardColor.RED)

        assertEquals(1, card.movementBonus)
        assertTrue(card.matches(CardColor.RED))
        assertFalse(card.matches(CardColor.GREEN))
    }

    @Test
    fun `UniqueAction has a movement bonus of 2 despite being a Basic Action`() {
        val card = ProxyPlayerCard.UniqueAction(CardColor.BLUE)

        assertEquals(2, card.movementBonus)
        assertTrue(card.matches(CardColor.BLUE))
    }

    @Test
    fun `AdvancedAction has a movement bonus of 2 and defers matching to its CardIdentity`() {
        val singleColor = ProxyPlayerCard.AdvancedAction(CardIdentity.SingleColor(CardColor.WHITE))
        val dualColor = ProxyPlayerCard.AdvancedAction(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE))

        assertEquals(2, singleColor.movementBonus)
        assertTrue(singleColor.matches(CardColor.WHITE))
        assertEquals(2, dualColor.movementBonus)
        assertTrue(dualColor.matches(CardColor.GREEN))
        assertTrue(dualColor.matches(CardColor.BLUE))
        assertFalse(dualColor.matches(CardColor.RED))
    }

    @Test
    fun `matchingCrystalCount delegates to each card kind's own color(s)`() {
        val crystals = mapOf(CardColor.RED to 2, CardColor.GREEN to 1, CardColor.BLUE to 1, CardColor.WHITE to 0)

        assertEquals(2, ProxyPlayerCard.BasicAction(CardColor.RED).matchingCrystalCount(crystals))
        assertEquals(0, ProxyPlayerCard.UniqueAction(CardColor.WHITE).matchingCrystalCount(crystals))
        assertEquals(
            2,
            ProxyPlayerCard.AdvancedAction(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)).matchingCrystalCount(crystals),
        )
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.ProxyPlayerCardTest"`
Expected: FAIL — `ProxyPlayerCard` is unresolved.

- [ ] **Step 3: Implement `ProxyPlayerCard.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.domain

/**
 * One card in a Proxy Player's deck (docs/rules/proxy-player.md's "The Proxy Player's turn" and
 * "Unique Basic Action cards"/"Dual-color Advanced Action cards"). Distinguishes 3 kinds because
 * the movement-point formula treats them differently: a generic [BasicAction] gives a smaller
 * bonus than a Hero's own [UniqueAction] card or any [AdvancedAction] - see [movementBonus].
 */
sealed interface ProxyPlayerCard {
    /** The card's contribution to the Proxy Player's movement-point total - see docs/rules/proxy-player.md's "Movement points". */
    val movementBonus: Int

    /** True if [color] matches this card - see each variant's own rule. */
    fun matches(color: CardColor): Boolean

    /** One of the 12 generic cards left in a Knight's 16-card starting deck after its 2 [UniqueAction]s replace 2 others. */
    data class BasicAction(val color: CardColor) : ProxyPlayerCard {
        override val movementBonus: Int get() = 1
        override fun matches(color: CardColor): Boolean = this.color == color
    }

    /**
     * One of a Knight's 2 portrait cards that replace 2 generic cards in their starting deck
     * (docs/rules/proxy-player.md's per-Knight table). Counts as an Advanced Action for the
     * movement bonus (+2) despite being a Basic Action.
     */
    data class UniqueAction(val color: CardColor) : ProxyPlayerCard {
        override val movementBonus: Int get() = 2
        override fun matches(color: CardColor): Boolean = this.color == color
    }

    /**
     * An Advanced Action added to the deck at round end (docs/rules/proxy-player.md's "When
     * preparing a new Round") - single- or dual-color, see [CardIdentity]. This is the only path
     * a [CardIdentity.DualColor] card can ever enter a Proxy Player's deck.
     */
    data class AdvancedAction(val identity: CardIdentity) : ProxyPlayerCard {
        override val movementBonus: Int get() = 2
        override fun matches(color: CardColor): Boolean = identity.matches(color)
    }
}

/**
 * Sum of [crystals] held for every color this card matches - see [CardIdentity.matchingCrystalCount],
 * which this delegates to for [ProxyPlayerCard.AdvancedAction]. Used by [ProxyPlayerSession]'s
 * deck-flip crystal-chain check, mirroring [DummyPlayerSession.playTurn]'s equivalent.
 */
fun ProxyPlayerCard.matchingCrystalCount(crystals: Map<CardColor, Int>): Int = when (this) {
    is ProxyPlayerCard.BasicAction -> crystals.getValue(color)
    is ProxyPlayerCard.UniqueAction -> crystals.getValue(color)
    is ProxyPlayerCard.AdvancedAction -> identity.matchingCrystalCount(crystals)
}

/**
 * How a Proxy Player's turn ended when they had a current Objective Card - the 3 top-level
 * outcomes from docs/rules/proxy-player.md's "Resolution". "Nothing" (still traveling) isn't a
 * case here - it's simply not calling [ProxyPlayerSession.resolveObjective] this turn, since the
 * Objective Card and its Shields already persist automatically via [ProxyPlayerSession.playTurn].
 */
enum class ProxyPlayerObjectiveResolution { EXPLORED, COMPLETED }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.ProxyPlayerCardTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerCard.kt domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerCardTest.kt
git commit -m "Add ProxyPlayerCard and ProxyPlayerObjectiveResolution"
```

---

### Task 3: `ProxyPlayerEvent`

**Files:**
- Create: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerEvent.kt`

**Interfaces:**
- Consumes: `ProxyPlayerCard`, `ProxyPlayerObjectiveResolution` (Task 2), `CardIdentity` (shared-card-identity plan).
- Produces: `ProxyPlayerEvent` sealed interface: `RoundStarted(round)`, `NewObjectiveDrawn(round, objectiveCard, discarded)`, `TurnContinued(round, objectiveCard, shieldsNow, revealed)`, `EndOfRoundAnnounced(round)`, `ObjectiveResolved(round, objectiveCard, resolution)`, `RoundEnded(round, advancedActionOfferColor, spellOfferColor, discardedObjective)`. Consumed by Tasks 4-6 (`ProxyPlayerSession`).

No test file for this task — a sealed interface of plain data classes has no behavior of its own to test; its correctness is exercised through `ProxyPlayerSessionTest` in Tasks 4-6.

- [ ] **Step 1: Implement `ProxyPlayerEvent.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.domain

/**
 * One entry in a [ProxyPlayerSession]'s history log, in the order it happened - the Proxy Player
 * counterpart to [DummyPlayerEvent]/[VolkareEvent], with the same "sealed interface as a closed
 * set of cases" shape (see [DummyPlayerEvent]'s doc comment for why `sealed` matters here).
 */
sealed interface ProxyPlayerEvent {
    /** Recorded when a new Round begins - see [ProxyPlayerSession.start] and `endRound()`. */
    data class RoundStarted(val round: Int) : ProxyPlayerEvent

    /**
     * Recorded when [ProxyPlayerSession.playTurn] draws a fresh Objective Card because there
     * wasn't one already (docs/rules/proxy-player.md's "The Proxy Player's turn", the "don't have
     * an objective card" branch). [objectiveCard] is the newly drawn objective; [discarded] is
     * whatever else was flipped in the same mandatory-3 batch (2 cards, or fewer if the deck ran
     * low, or the mandatory batch's crystal-chain extension - see [ProxyPlayerSession.playTurn]).
     */
    data class NewObjectiveDrawn(val round: Int, val objectiveCard: ProxyPlayerCard, val discarded: List<ProxyPlayerCard>) : ProxyPlayerEvent

    /**
     * Recorded when [ProxyPlayerSession.playTurn] continues an existing Objective Card
     * (docs/rules/proxy-player.md's "have an objective card" branch): a Shield token is added to
     * it and 3 cards (plus any crystal-chain extension) are flipped to the discard pile.
     * [shieldsNow] is the Objective Card's new Shield count, including this turn's addition.
     */
    data class TurnContinued(val round: Int, val objectiveCard: ProxyPlayerCard, val shieldsNow: Int, val revealed: List<ProxyPlayerCard>) : ProxyPlayerEvent

    /** Recorded when [ProxyPlayerSession.playTurn] finds an empty deck - announces End of Round instead of playing. */
    data class EndOfRoundAnnounced(val round: Int) : ProxyPlayerEvent

    /**
     * Recorded after [ProxyPlayerSession.resolveObjective] discards the current Objective Card
     * and its Shields - docs/rules/proxy-player.md's "Resolution". [resolution] distinguishes
     * Explored from Completed for the log's narration only; both have the identical state effect
     * (see [ProxyPlayerObjectiveResolution]'s doc comment).
     */
    data class ObjectiveResolved(val round: Int, val objectiveCard: ProxyPlayerCard, val resolution: ProxyPlayerObjectiveResolution) : ProxyPlayerEvent

    /**
     * Recorded after [ProxyPlayerSession.endRound] resolves the round-prep offer interactions,
     * shared verbatim with [DummyPlayerEvent.RoundEnded] (docs/rules/proxy-player.md's "When
     * preparing a new Round"). [discardedObjective] is non-null if a lingering Objective Card
     * (one that was still being pursued when the Round ended) was discarded as part of this step.
     */
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardIdentity,
        val spellOfferColor: CardColor,
        val discardedObjective: ProxyPlayerCard?,
    ) : ProxyPlayerEvent
}
```

- [ ] **Step 2: Confirm the domain module still compiles**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:compileKotlin`
Expected: PASS (no consumers yet, so this is purely a compile check)

- [ ] **Step 3: Commit**

```bash
git add domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerEvent.kt
git commit -m "Add ProxyPlayerEvent"
```

---

### Task 4: `ProxyPlayerSession` — setup, `movementPoints`, `restore`

**Files:**
- Create: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSession.kt`
- Test: `domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSessionTest.kt`

**Interfaces:**
- Consumes: `ProxyPlayerCard`, `ProxyPlayerEvent`, `startingCrystals`/`STARTING_CRYSTAL_DOTS` (Task 1), `CardIdentity`.
- Produces: `ProxyPlayerSession` data class (`knight`, `wasRandom`, `deckOrder: List<ProxyPlayerCard>`, `discardPile: List<ProxyPlayerCard>`, `crystals: Map<CardColor, Int>`, `round: Int`, `roundEnded: Boolean`, `objectiveCard: ProxyPlayerCard?`, `objectiveShields: Int`, `log: List<ProxyPlayerEvent>`) with `movementPoints(hasMatchingManaDie: Boolean): Int`, and companion `start`/`startRandom`/`restore`. Consumed by Tasks 5-6 (`playTurn`/`resolveObjective`/`endRound`) and the persistence/UI plans.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProxyPlayerSessionTest {

    @Test
    fun `start builds a 16-card deck with Tovak's 2 unique cards replacing one Blue and one Red generic`() {
        val session = ProxyPlayerSession.start(Knight.TOVAK)

        assertEquals(16, session.deckOrder.size)
        assertEquals(1, session.deckOrder.count { it == ProxyPlayerCard.UniqueAction(CardColor.BLUE) })
        assertEquals(1, session.deckOrder.count { it == ProxyPlayerCard.UniqueAction(CardColor.RED) })
        assertEquals(3, session.deckOrder.count { it == ProxyPlayerCard.BasicAction(CardColor.BLUE) })
        assertEquals(3, session.deckOrder.count { it == ProxyPlayerCard.BasicAction(CardColor.RED) })
        assertEquals(4, session.deckOrder.count { it == ProxyPlayerCard.BasicAction(CardColor.GREEN) })
        assertEquals(4, session.deckOrder.count { it == ProxyPlayerCard.BasicAction(CardColor.WHITE) })
    }

    @Test
    fun `start gives Goldyx the same 2 Green, 1 Blue starting crystals as a standard Dummy Player`() {
        val session = ProxyPlayerSession.start(Knight.GOLDYX)

        assertEquals(DummyPlayerSession.start(Knight.GOLDYX).crystals, session.crystals)
    }

    @Test
    fun `start has no objective card, 0 rounds ended, and round 1`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        assertNull(session.objectiveCard)
        assertEquals(0, session.objectiveShields)
        assertEquals(1, session.round)
        assertEquals(false, session.roundEnded)
    }

    @Test
    fun `startRandom sets wasRandom and picks a knight whose crystals match the starting table`() {
        val session = ProxyPlayerSession.startRandom(random = Random(0))

        assertEquals(true, session.wasRandom)
        assertEquals(ProxyPlayerSession.start(session.knight).crystals, session.crystals)
    }

    @Test
    fun `movementPoints with no objective card is 0`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        assertEquals(0, session.movementPoints(hasMatchingManaDie = false))
    }

    @Test
    fun `movementPoints sums the objective card's bonus, its Shields, and a matching mana die`() {
        val session = ProxyPlayerSession.restore(
            knight = Knight.ARYTHEA,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.ARYTHEA),
            round = 1,
            roundEnded = false,
            objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.RED),
            objectiveShields = 1,
            log = emptyList(),
        )

        // +2 (unique card) + 1 (shield) + 1 (matching mana die) = 4.
        assertEquals(4, session.movementPoints(hasMatchingManaDie = true))
        // Without the matching die: +2 + 1 = 3.
        assertEquals(3, session.movementPoints(hasMatchingManaDie = false))
    }

    @Test
    fun `restore reconstructs a session with the exact same state it was given`() {
        val original = ProxyPlayerSession.start(Knight.CORAL)

        val restored = ProxyPlayerSession.restore(
            knight = original.knight,
            wasRandom = original.wasRandom,
            deckOrder = original.deckOrder,
            discardPile = original.discardPile,
            crystals = original.crystals,
            round = original.round,
            roundEnded = original.roundEnded,
            objectiveCard = original.objectiveCard,
            objectiveShields = original.objectiveShields,
            log = original.log,
        )

        assertEquals(original, restored)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.ProxyPlayerSessionTest"`
Expected: FAIL — `ProxyPlayerSession` is unresolved.

- [ ] **Step 3: Implement `ProxyPlayerSession.kt` (setup, `movementPoints`, `restore` only — `playTurn`/`resolveObjective`/`endRound` are added in Tasks 5-6)**

```kotlin
package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random

/**
 * Immutable snapshot of one Proxy Player's state at a point in time - its deck, discard pile,
 * crystal Inventory, current Round, current Objective Card and its Shield count, and a log of
 * events so far. Implements the Proxy Player mode described in docs/rules/proxy-player.md: like
 * [VolkareSession], it narrates rather than simulates the board (see ADR-0004) - it tracks
 * everything the movement-point formula needs, but never decides which map site is targeted or
 * how movement/conquering actually resolves.
 *
 * Mirrors [VolkareSession]'s shape: a `private` constructor reached only through the companion
 * object's [start]/[startRandom]/[restore] factory functions, and every method returns a new
 * `ProxyPlayerSession` via `copy()` rather than mutating this one.
 */
data class ProxyPlayerSession private constructor(
    val knight: Knight,
    val wasRandom: Boolean,
    val deckOrder: List<ProxyPlayerCard>,
    val discardPile: List<ProxyPlayerCard>,
    val crystals: Map<CardColor, Int>,
    val round: Int,
    val roundEnded: Boolean,
    val objectiveCard: ProxyPlayerCard?,
    val objectiveShields: Int,
    val log: List<ProxyPlayerEvent>,
) {
    /**
     * The Proxy Player's total movement points this turn (docs/rules/proxy-player.md's "Movement
     * points"): [objectiveCard]'s own [ProxyPlayerCard.movementBonus] plus [objectiveShields],
     * plus +1 if [hasMatchingManaDie] is true - the player reports this each turn, since whether a
     * matching-color (or gold, by day) mana die sits in the Source is shared table state this app
     * doesn't track (unlike [VolkareSession], which rolls its own die - this is the reverse: an
     * observation, not a randomized event). Returns 0 if there's no current [objectiveCard].
     */
    fun movementPoints(hasMatchingManaDie: Boolean): Int {
        val card = objectiveCard ?: return 0
        return card.movementBonus + objectiveShields + (if (hasMatchingManaDie) 1 else 0)
    }

    companion object {
        /**
         * Which 2 of a Knight's 16 starting Basic Action cards are replaced by that Knight's own
         * Unique Basic Action Cards, by color slot (docs/rules/proxy-player.md's per-Knight
         * table). Feeds [buildStartingDeck].
         */
        private val UNIQUE_CARD_COLORS: Map<Knight, List<CardColor>> = mapOf(
            Knight.TOVAK to listOf(CardColor.BLUE, CardColor.RED),
            Knight.GOLDYX to listOf(CardColor.BLUE, CardColor.GREEN),
            Knight.NOROWAS to listOf(CardColor.WHITE, CardColor.GREEN),
            Knight.WOLFHAWK to listOf(CardColor.BLUE, CardColor.WHITE),
            Knight.ARYTHEA to listOf(CardColor.WHITE, CardColor.RED),
            Knight.KRANG to listOf(CardColor.RED, CardColor.GREEN),
            Knight.BRAEVALAR to listOf(CardColor.BLUE, CardColor.GREEN),
            Knight.CORAL to listOf(CardColor.WHITE, CardColor.RED),
        )

        /**
         * Builds a Knight's starting 16-card deck (docs/rules/proxy-player.md's "Unique Basic
         * Action cards"): the same 16-card, 4-per-color shape as [DummyPlayerSession.start]'s
         * default deck, except the 2 colors in [UNIQUE_CARD_COLORS] each have one of their 4
         * generic copies replaced by a [ProxyPlayerCard.UniqueAction] instead.
         */
        private fun buildStartingDeck(knight: Knight): List<ProxyPlayerCard> {
            val uniqueColors = UNIQUE_CARD_COLORS.getValue(knight)
            // Start every color at 4 generic copies, then subtract one per unique-card color.
            val basicCounts = CardColor.entries.associateWith { 4 }.toMutableMap()
            val cards = mutableListOf<ProxyPlayerCard>()
            for (color in uniqueColors) {
                cards += ProxyPlayerCard.UniqueAction(color)
                basicCounts[color] = basicCounts.getValue(color) - 1
            }
            for (color in CardColor.entries) {
                repeat(basicCounts.getValue(color)) { cards += ProxyPlayerCard.BasicAction(color) }
            }
            return cards
        }

        /**
         * Begins a new Proxy Player session for a chosen [Knight] - session setup per
         * docs/rules/proxy-player.md ("At the start of the game"): a shuffled starting deck (see
         * [buildStartingDeck]), that Knight's starting crystal Inventory (shared with
         * [DummyPlayerSession] via [startingCrystals]), no current objective, and Round 1.
         */
        fun start(
            knight: Knight,
            wasRandom: Boolean = false,
            deckOrder: List<ProxyPlayerCard> = buildStartingDeck(knight).shuffled(),
        ): ProxyPlayerSession = ProxyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = emptyList(),
            crystals = startingCrystals(knight),
            round = 1,
            roundEnded = false,
            objectiveCard = null,
            objectiveShields = 0,
            log = listOf(ProxyPlayerEvent.RoundStarted(round = 1)),
        )

        /** Begins a new session with a randomly-chosen [Knight] - mirrors [DummyPlayerSession.startRandom]. */
        fun startRandom(random: Random = Random): ProxyPlayerSession {
            val knight = Knight.entries.toList().random(random)
            return start(knight, wasRandom = true)
        }

        /** Reconstructs a session from its full persisted state - not for general use; [start]/[startRandom] begin a new session. */
        fun restore(
            knight: Knight,
            wasRandom: Boolean,
            deckOrder: List<ProxyPlayerCard>,
            discardPile: List<ProxyPlayerCard>,
            crystals: Map<CardColor, Int>,
            round: Int,
            roundEnded: Boolean,
            objectiveCard: ProxyPlayerCard?,
            objectiveShields: Int,
            log: List<ProxyPlayerEvent>,
        ): ProxyPlayerSession = ProxyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = discardPile,
            crystals = crystals,
            round = round,
            roundEnded = roundEnded,
            objectiveCard = objectiveCard,
            objectiveShields = objectiveShields,
            log = log,
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.ProxyPlayerSessionTest"`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSession.kt domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSessionTest.kt
git commit -m "Add ProxyPlayerSession setup, movementPoints, and restore"
```

---

### Task 5: `ProxyPlayerSession.playTurn`

**Files:**
- Modify: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSession.kt`
- Modify: `domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSessionTest.kt`

**Interfaces:**
- Produces: `ProxyPlayerSession.playTurn(): ProxyPlayerSession`. Consumed by the UI-layer plan's `ProxyPlayerAiViewModel`.

- [ ] **Step 1: Write the failing tests (append to `ProxyPlayerSessionTest.kt`)**

```kotlin
    @Test
    fun `playTurn on an empty deck announces End of Round instead of flipping`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.playTurn()

        assertEquals(true, next.roundEnded)
        assertEquals(ProxyPlayerEvent.EndOfRoundAnnounced(round = 1), next.log.last())
    }

    @Test
    fun `playTurn is a no-op once roundEnded is already true`() {
        val ended = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = ended.playTurn()

        assertEquals(ended, next)
    }

    @Test
    fun `playTurn with no objective card draws the first flipped card as the new objective`() {
        // Coral has no Green crystals, so the flip stops at the mandatory 3 (last card Green, no match).
        val session = ProxyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
            ),
        )

        val next = session.playTurn()

        assertEquals(ProxyPlayerCard.UniqueAction(CardColor.WHITE), next.objectiveCard)
        assertEquals(0, next.objectiveShields)
        assertEquals(
            listOf(ProxyPlayerCard.BasicAction(CardColor.RED), ProxyPlayerCard.BasicAction(CardColor.GREEN)),
            next.discardPile,
        )
        assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.BLUE)), next.deckOrder)
        assertEquals(
            ProxyPlayerEvent.NewObjectiveDrawn(
                round = 1,
                objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                discarded = listOf(ProxyPlayerCard.BasicAction(CardColor.RED), ProxyPlayerCard.BasicAction(CardColor.GREEN)),
            ),
            next.log.last(),
        )
    }

    @Test
    fun `playTurn with no objective card chains extra flips off the 3rd card's matching crystals`() {
        // Coral holds 2 White crystals - a White 3rd card should chain 2 additional reveals.
        val session = ProxyPlayerSession.start(
            Knight.CORAL,
            deckOrder = listOf(
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.WHITE),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
            ),
        )

        val next = session.playTurn()

        assertEquals(ProxyPlayerCard.BasicAction(CardColor.RED), next.objectiveCard)
        assertEquals(
            listOf(
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.WHITE),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
            ),
            next.discardPile,
        )
        assertEquals(emptyList(), next.deckOrder)
    }

    @Test
    fun `playTurn with an existing objective card adds a Shield token and flips 3 cards to discard`() {
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = listOf(
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
            ),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
            objectiveShields = 1,
            log = emptyList(),
        )

        val next = session.playTurn()

        assertEquals(ProxyPlayerCard.UniqueAction(CardColor.WHITE), next.objectiveCard)
        assertEquals(2, next.objectiveShields)
        assertEquals(
            listOf(
                ProxyPlayerCard.BasicAction(CardColor.RED),
                ProxyPlayerCard.BasicAction(CardColor.GREEN),
                ProxyPlayerCard.BasicAction(CardColor.BLUE),
            ),
            next.discardPile,
        )
        assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.GREEN)), next.deckOrder)
        assertEquals(
            ProxyPlayerEvent.TurnContinued(
                round = 1,
                objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                shieldsNow = 2,
                revealed = listOf(
                    ProxyPlayerCard.BasicAction(CardColor.RED),
                    ProxyPlayerCard.BasicAction(CardColor.GREEN),
                    ProxyPlayerCard.BasicAction(CardColor.BLUE),
                ),
            ),
            next.log.last(),
        )
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.ProxyPlayerSessionTest"`
Expected: FAIL — `playTurn` is unresolved.

- [ ] **Step 3: Implement `playTurn` (add to the `ProxyPlayerSession` class body, above `movementPoints`)**

```kotlin
    /**
     * Plays one Proxy Player turn (docs/rules/proxy-player.md's "The Proxy Player's turn"). If
     * the deck is empty, announces End of Round instead (mirrors
     * [DummyPlayerSession.playTurn]'s empty-deck guard). Otherwise branches on whether there's a
     * current [objectiveCard]:
     * - **Has one**: adds a Shield token to it, then flips 3 cards (plus any crystal-chain
     *   extension - see [flipMandatoryAndChain]) onto the discard pile.
     * - **Doesn't have one**: flips the same mandatory-3-plus-chain batch, but the *first* card
     *   becomes the new [objectiveCard] instead of being discarded - the rest go to the discard
     *   pile. If the deck had only 1 card left, that single card becomes the objective and
     *   nothing is discarded (see docs/rules/proxy-player.md's note on this case).
     *
     * Once [roundEnded] is true, further calls are a no-op (mirrors [DummyPlayerSession.playTurn]).
     */
    fun playTurn(): ProxyPlayerSession {
        if (roundEnded) return this

        if (deckOrder.isEmpty()) {
            return copy(roundEnded = true, log = log + ProxyPlayerEvent.EndOfRoundAnnounced(round))
        }

        val (revealed, remainingDeck) = flipMandatoryAndChain(deckOrder, mandatoryCount = 3, crystals)

        return if (objectiveCard != null) {
            val shieldsNow = objectiveShields + 1
            copy(
                deckOrder = remainingDeck,
                discardPile = discardPile + revealed,
                objectiveShields = shieldsNow,
                log = log + ProxyPlayerEvent.TurnContinued(round, objectiveCard, shieldsNow, revealed),
            )
        } else {
            val newObjective = revealed.first()
            val discarded = revealed.drop(1)
            copy(
                deckOrder = remainingDeck,
                discardPile = discardPile + discarded,
                objectiveCard = newObjective,
                objectiveShields = 0,
                log = log + ProxyPlayerEvent.NewObjectiveDrawn(round, newObjective, discarded),
            )
        }
    }
```

Add the shared private helper at file scope (outside the class, near the bottom of the file):

```kotlin
/**
 * Flips up to [mandatoryCount] cards off the front of [deck], then - if there's a last flipped
 * card and it matches any crystals in [crystals] (see [ProxyPlayerCard.matchingCrystalCount]) -
 * flips one additional card per matching crystal, capped by what's left in the deck. Returns the
 * full list of revealed cards (in flip order) and the remaining deck. Shared by both branches of
 * [ProxyPlayerSession.playTurn], since the flip-and-chain mechanics are identical regardless of
 * which flipped card ends up becoming the new objective vs. going straight to discard.
 */
private fun flipMandatoryAndChain(
    deck: List<ProxyPlayerCard>,
    mandatoryCount: Int,
    crystals: Map<CardColor, Int>,
): Pair<List<ProxyPlayerCard>, List<ProxyPlayerCard>> {
    val mandatory = deck.take(mandatoryCount)
    val afterMandatory = deck.drop(mandatoryCount)
    val lastCard = mandatory.lastOrNull() ?: return mandatory to afterMandatory
    val matching = lastCard.matchingCrystalCount(crystals)
    val additionalCount = minOf(matching, afterMandatory.size)
    val additional = afterMandatory.take(additionalCount)
    val remaining = afterMandatory.drop(additionalCount)
    return (mandatory + additional) to remaining
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.ProxyPlayerSessionTest"`
Expected: PASS (12 tests)

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSession.kt domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSessionTest.kt
git commit -m "Add ProxyPlayerSession.playTurn"
```

---

### Task 6: `ProxyPlayerSession.resolveObjective` and `endRound`

**Files:**
- Modify: `domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSession.kt`
- Modify: `domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSessionTest.kt`

**Interfaces:**
- Consumes: `CardIdentity` (shared-card-identity plan).
- Produces: `ProxyPlayerSession.resolveObjective(resolution: ProxyPlayerObjectiveResolution): ProxyPlayerSession`, `ProxyPlayerSession.endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor): ProxyPlayerSession`. Consumed by the UI-layer plan's `ProxyPlayerAiViewModel`.

- [ ] **Step 1: Write the failing tests (append to `ProxyPlayerSessionTest.kt`)**

```kotlin
    @Test
    fun `resolveObjective discards the objective card and clears its Shields, regardless of resolution`() {
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            objectiveCard = ProxyPlayerCard.BasicAction(CardColor.GREEN),
            objectiveShields = 2,
            log = emptyList(),
        )

        val explored = session.resolveObjective(ProxyPlayerObjectiveResolution.EXPLORED)
        val completed = session.resolveObjective(ProxyPlayerObjectiveResolution.COMPLETED)

        for (next in listOf(explored, completed)) {
            assertEquals(null, next.objectiveCard)
            assertEquals(0, next.objectiveShields)
            assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.GREEN)), next.discardPile)
        }
        assertEquals(
            ProxyPlayerEvent.ObjectiveResolved(1, ProxyPlayerCard.BasicAction(CardColor.GREEN), ProxyPlayerObjectiveResolution.EXPLORED),
            explored.log.last(),
        )
        assertEquals(
            ProxyPlayerEvent.ObjectiveResolved(1, ProxyPlayerCard.BasicAction(CardColor.GREEN), ProxyPlayerObjectiveResolution.COMPLETED),
            completed.log.last(),
        )
    }

    @Test
    fun `resolveObjective is a no-op if there's no current objective card`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        val next = session.resolveObjective(ProxyPlayerObjectiveResolution.EXPLORED)

        assertEquals(session, next)
    }

    @Test
    fun `endRound appends the Advanced Action offer card to the deck and grants a Spell-color crystal`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            spellOfferColor = CardColor.WHITE,
        )

        assertEquals(listOf(ProxyPlayerCard.AdvancedAction(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE))), next.deckOrder)
        assertEquals(startingCrystals(Knight.CORAL).getValue(CardColor.WHITE) + 1, next.crystals.getValue(CardColor.WHITE))
    }

    @Test
    fun `endRound discards a lingering objective card and its Shields first`() {
        val session = ProxyPlayerSession.restore(
            knight = Knight.CORAL,
            wasRandom = false,
            deckOrder = emptyList(),
            discardPile = emptyList(),
            crystals = startingCrystals(Knight.CORAL),
            round = 1,
            roundEnded = false,
            objectiveCard = ProxyPlayerCard.BasicAction(CardColor.RED),
            objectiveShields = 3,
            log = emptyList(),
        )

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(null, next.objectiveCard)
        assertEquals(0, next.objectiveShields)
        assertEquals(listOf(ProxyPlayerCard.BasicAction(CardColor.RED)), next.discardPile)
        assertEquals(
            ProxyPlayerEvent.RoundEnded(1, CardIdentity.SingleColor(CardColor.WHITE), CardColor.BLUE, ProxyPlayerCard.BasicAction(CardColor.RED)),
            next.log.last(),
        )
    }

    @Test
    fun `endRound with no lingering objective logs a null discardedObjective`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(
            ProxyPlayerEvent.RoundEnded(1, CardIdentity.SingleColor(CardColor.WHITE), CardColor.BLUE, null),
            next.log.last(),
        )
    }

    @Test
    fun `endRound increments the round and resets roundEnded`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()).playTurn()

        val next = session.endRound(
            advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE),
            spellOfferColor = CardColor.BLUE,
        )

        assertEquals(2, next.round)
        assertEquals(false, next.roundEnded)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test --tests "com.guyteichman.mageknightbuddy.domain.ProxyPlayerSessionTest"`
Expected: FAIL — `resolveObjective`/`endRound` are unresolved.

- [ ] **Step 3: Implement `resolveObjective` and `endRound` (add to the `ProxyPlayerSession` class body, after `playTurn`)**

```kotlin
    /**
     * Discards the current [objectiveCard] and clears [objectiveShields] - docs/rules/proxy-player.md's
     * "Resolution": both Explored and Completed have the identical state effect, so [resolution]
     * only affects the logged event's narration, not what actually changes. A no-op if there's no
     * current objective (defensive - the UI should only offer this action when one exists).
     */
    fun resolveObjective(resolution: ProxyPlayerObjectiveResolution): ProxyPlayerSession {
        val card = objectiveCard ?: return this
        return copy(
            objectiveCard = null,
            objectiveShields = 0,
            discardPile = discardPile + card,
            log = log + ProxyPlayerEvent.ObjectiveResolved(round, card, resolution),
        )
    }

    /**
     * Applies the round-prep offer interactions - identical mechanism to
     * [DummyPlayerSession.endRound] (docs/rules/dummy-player.md's "End of Round", reused verbatim
     * by docs/rules/proxy-player.md's "When preparing a new Round"), plus one Proxy Player-only
     * step first: if there's a lingering [objectiveCard] (still being pursued when the Round
     * ended), it's discarded along with its Shields before the standard offer interactions run.
     */
    fun endRound(advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor): ProxyPlayerSession {
        val discardedObjective = objectiveCard
        val discardAfterObjective = if (discardedObjective != null) discardPile + discardedObjective else discardPile
        return copy(
            objectiveCard = null,
            objectiveShields = 0,
            deckOrder = (deckOrder + ProxyPlayerCard.AdvancedAction(advancedActionOfferColor)).shuffled(),
            discardPile = discardAfterObjective,
            crystals = crystals + (spellOfferColor to crystals.getValue(spellOfferColor) + 1),
            round = round + 1,
            roundEnded = false,
            log = log + ProxyPlayerEvent.RoundEnded(round, advancedActionOfferColor, spellOfferColor, discardedObjective),
        )
    }
```

- [ ] **Step 4: Run the full domain test suite**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :domain:test`
Expected: PASS (all domain tests, including 18 new/updated `ProxyPlayerSessionTest` cases)

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSession.kt domain/src/test/kotlin/com/guyteichman/mageknightbuddy/domain/ProxyPlayerSessionTest.kt
git commit -m "Add ProxyPlayerSession.resolveObjective and endRound"
```
