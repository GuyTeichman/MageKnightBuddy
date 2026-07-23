# Proxy Player Persistence Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist `ProxyPlayerSession` to Room, mirroring `VolkareSessionDao`/`VolkareSessionEntity`/`VolkareSessionMapper`/`VolkareSessionRepository` exactly, so the setup/AI screens (UI-layer plan) can autosave/restore a Proxy Player session the same way the other two modes already do.

**Architecture:** One new table (`proxy_player_sessions`), single-slot autosave (fixed `SINGLETON_ID` primary key, same as `DummyPlayerSessionEntity`/`VolkareSessionEntity`). Deck/discard/objective/log are JSON string columns via new `ProxyPlayerCardDto`/`ProxyPlayerEventDto` serializable mirrors in `data/` (keeping `domain/` free of serialization annotations, per ADR-0001); crystals flatten into 4 named columns, matching `DummyPlayerSessionEntity`'s convention.

**Tech Stack:** Room, kotlinx.serialization, `BundledSQLiteDriver` for JVM-only DAO tests (ADR-0003).

**Depends on:** `docs/superpowers/plans/2026-07-23-proxy-player-1-domain-layer.md` (needs `ProxyPlayerSession`/`ProxyPlayerCard`/`ProxyPlayerEvent`/`ProxyPlayerObjectiveResolution`) and `...-0-shared-card-identity.md` (needs `CardIdentity`/`CardIdentityDto`).

## Global Constraints

- `domain/` stays plain Kotlin, zero serialization dependencies — DTOs live in `data/` (see [ADR-0001](../../adr/0001-domain-logic-as-plain-kotlin-module.md)).
- No hand-written Room migration — bump `MageKnightBuddyDatabase`'s `version` and rely on `fallbackToDestructiveMigration()` (already configured — see that file's `createDatabase()`), consistent with every prior schema bump (this app has never been published).
- DAO tests use `Room.databaseBuilder<MageKnightBuddyDatabase>(...)` + `BundledSQLiteDriver()` against a temp file, exactly like `VolkareSessionDaoTest`/`VolkareSessionRepositoryTest` — no emulator required (ADR-0003).
- Every public class/function gets a short KDoc summary per this project's commenting standard (root `CLAUDE.md`).
- Run `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test` before each commit.

---

### Task 1: `ProxyPlayerCardDto` and `ProxyPlayerEventDto`

**Files:**
- Create: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerCardDto.kt`
- Create: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerEventDto.kt`

**Interfaces:**
- Consumes: `CardIdentityDto` (shared-card-identity plan, Task 3).
- Produces: `ProxyPlayerCardDto` sealed interface (`BasicAction(color: String)`, `UniqueAction(color: String)`, `AdvancedAction(identity: CardIdentityDto)`); `ProxyPlayerEventDto` sealed interface mirroring `ProxyPlayerEvent`'s 6 cases. Consumed by Task 3 (`ProxyPlayerSessionMapper`).

No test file for this task — plain `@Serializable` data mirrors with no behavior; exercised through `ProxyPlayerSessionMapperTest` in Task 3.

- [ ] **Step 1: Implement `ProxyPlayerCardDto.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard], kept in
 * `data/` for the same reason [DummyPlayerEventDto] is (domain/ must stay free of serialization
 * annotations - see docs/adr/0001-domain-logic-as-plain-kotlin-module.md). [ProxyPlayerSessionMapper]
 * converts between the two.
 */
@Serializable
sealed interface ProxyPlayerCardDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard.BasicAction]. */
    @Serializable
    @SerialName("basic_action")
    data class BasicAction(val color: String) : ProxyPlayerCardDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard.UniqueAction]. */
    @Serializable
    @SerialName("unique_action")
    data class UniqueAction(val color: String) : ProxyPlayerCardDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard.AdvancedAction]. */
    @Serializable
    @SerialName("advanced_action")
    data class AdvancedAction(val identity: CardIdentityDto) : ProxyPlayerCardDto
}
```

- [ ] **Step 2: Implement `ProxyPlayerEventDto.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent], the
 * Proxy Player counterpart to [VolkareEventDto]/[DummyPlayerEventDto] - see either's doc comment
 * for why this near-duplicate hierarchy lives in `data/` instead of on the domain type directly.
 */
@Serializable
sealed interface ProxyPlayerEventDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.RoundStarted]. */
    @Serializable
    @SerialName("round_started")
    data class RoundStarted(val round: Int) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.NewObjectiveDrawn]. */
    @Serializable
    @SerialName("new_objective_drawn")
    data class NewObjectiveDrawn(val round: Int, val objectiveCard: ProxyPlayerCardDto, val discarded: List<ProxyPlayerCardDto>) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.TurnContinued]. */
    @Serializable
    @SerialName("turn_continued")
    data class TurnContinued(val round: Int, val objectiveCard: ProxyPlayerCardDto, val shieldsNow: Int, val revealed: List<ProxyPlayerCardDto>) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.EndOfRoundAnnounced]. */
    @Serializable
    @SerialName("end_of_round_announced")
    data class EndOfRoundAnnounced(val round: Int) : ProxyPlayerEventDto

    /**
     * Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.ObjectiveResolved].
     * [resolution] stores [com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution]'s
     * enum name as a plain `String`, same convention as [VolkareEventDto.CardRevealed.manaRoll].
     */
    @Serializable
    @SerialName("objective_resolved")
    data class ObjectiveResolved(val round: Int, val objectiveCard: ProxyPlayerCardDto, val resolution: String) : ProxyPlayerEventDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent.RoundEnded]. */
    @Serializable
    @SerialName("round_ended")
    data class RoundEnded(
        val round: Int,
        val advancedActionOfferColor: CardIdentityDto,
        val spellOfferColor: String,
        val discardedObjective: ProxyPlayerCardDto?,
    ) : ProxyPlayerEventDto
}
```

- [ ] **Step 3: Confirm the data module still compiles**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:compileKotlin`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerCardDto.kt data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerEventDto.kt
git commit -m "Add ProxyPlayerCardDto and ProxyPlayerEventDto"
```

---

### Task 2: `ProxyPlayerSessionEntity` and `@Database` registration

**Files:**
- Create: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionEntity.kt`
- Modify: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/MageKnightBuddyDatabase.kt`

**Interfaces:**
- Produces: `ProxyPlayerSessionEntity` (`@Entity(tableName = "proxy_player_sessions")`) with fixed `SINGLETON_ID`. Registers `proxyPlayerSessionDao(): ProxyPlayerSessionDao` (implemented in Task 4) on `MageKnightBuddyDatabase`.

- [ ] **Step 1: Create `ProxyPlayerSessionEntity.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table row representing a persisted [com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession].
 * Mirrors [VolkareSessionEntity]/[DummyPlayerSessionEntity]'s design exactly: single-slot autosave
 * via the fixed [SINGLETON_ID] primary key (see GitHub issue #27). [ProxyPlayerSessionMapper]
 * converts to/from the domain session, and [ProxyPlayerSessionDao] does the actual upsert/read.
 *
 * Deck/discard pile/objective card/event log are `...Json` `String` columns, same reason as
 * [VolkareSessionEntity]'s equivalents (Room can't map `List`/sealed-class/nullable-sealed-class
 * columns directly). [objectiveCardJson] is nullable - `null` means no current Objective Card
 * (see `CONTEXT.md`'s **Objective Card** entry). Crystal counts flatten into 4 named columns,
 * matching [DummyPlayerSessionEntity]'s convention (Room has no direct `Map` column support).
 */
@Entity(tableName = "proxy_player_sessions")
data class ProxyPlayerSessionEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val knight: String,
    val wasRandom: Boolean,
    val deckOrderJson: String,
    val discardPileJson: String,
    val crystalsRed: Int,
    val crystalsGreen: Int,
    val crystalsBlue: Int,
    val crystalsWhite: Int,
    val round: Int,
    val roundEnded: Boolean,
    val objectiveCardJson: String?,
    val objectiveShields: Int,
    val logJson: String,
    val updatedAt: Long,
) {
    companion object {
        /** The fixed primary key every saved row uses, enforcing the single-slot autosave design above. */
        const val SINGLETON_ID = 0
    }
}
```

- [ ] **Step 2: Register the entity and DAO on `MageKnightBuddyDatabase`**

In `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/MageKnightBuddyDatabase.kt`, add `ProxyPlayerSessionEntity::class` to `entities`, bump `version` by 1 from its current value with a one-line addition to the running comment explaining the bump (`Bumped N -> N+1: added the new ProxyPlayerSessionEntity table (proxy_player_sessions) - see docs/rules/proxy-player.md.`), and add the DAO accessor:

```kotlin
@Database(
    entities = [ScoringSessionEntity::class, DummyPlayerSessionEntity::class, VolkareSessionEntity::class, ProxyPlayerSessionEntity::class],
    // ... existing comment history ...
    // Bumped N -> N+1: added the new ProxyPlayerSessionEntity table (proxy_player_sessions) - see
    // docs/rules/proxy-player.md. No hand-written migration - fallbackToDestructiveMigration
    // (see createDatabase()) is fine pre-release, same as every prior bump.
    version = N + 1,
    exportSchema = false,
)
abstract class MageKnightBuddyDatabase : RoomDatabase() {
    abstract fun scoringSessionDao(): ScoringSessionDao
    abstract fun dummyPlayerSessionDao(): DummyPlayerSessionDao
    abstract fun volkareSessionDao(): VolkareSessionDao
    abstract fun proxyPlayerSessionDao(): ProxyPlayerSessionDao
}
```

(Read the file first to find its actual current `version = N` — it may already be one higher than `6` if the shared-card-identity plan's Task 3 landed first; increment from whatever is actually there, and preserve the full existing comment history rather than replacing it.)

- [ ] **Step 3: Confirm the data module compiles**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:compileKotlin`
Expected: FAIL — `ProxyPlayerSessionDao` doesn't exist yet (created in Task 4). This is expected; proceed to Task 3 (mapper) before Task 4 makes the module compile again, or reorder locally if your toolchain requires a compiling intermediate state — in that case, do Task 4's Step 1 (a minimal empty-bodied `ProxyPlayerSessionDao` interface with just the 3 method signatures) before continuing here, then fill in Task 3.

- [ ] **Step 4: Commit** (after Task 4 restores compilation — see note above; commit Tasks 2-4 together if your workflow needs a green build per commit)

```bash
git add data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionEntity.kt data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/MageKnightBuddyDatabase.kt
git commit -m "Add ProxyPlayerSessionEntity and register it on MageKnightBuddyDatabase"
```

---

### Task 3: `ProxyPlayerSessionMapper`

**Files:**
- Create: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionMapper.kt`
- Test: `data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionMapperTest.kt`

**Interfaces:**
- Consumes: `ProxyPlayerSession`/`ProxyPlayerCard`/`ProxyPlayerEvent`/`ProxyPlayerObjectiveResolution` (domain-layer plan), `ProxyPlayerCardDto`/`ProxyPlayerEventDto` (Task 1), `ProxyPlayerSessionEntity` (Task 2).
- Produces: `ProxyPlayerSession.toEntity(updatedAt: Long = System.currentTimeMillis()): ProxyPlayerSessionEntity`, `ProxyPlayerSessionEntity.toDomain(): ProxyPlayerSession`. Consumed by Task 5 (`ProxyPlayerSessionRepository`).

- [ ] **Step 1: Write the failing round-trip tests**

```kotlin
package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyPlayerSessionMapperTest {

    @Test
    fun `toEntity and toDomain round-trip a freshly started session`() {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }

    @Test
    fun `toEntity and toDomain round-trip a session with a current Unique Basic Action objective card`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())
            .let {
                // Force a specific objective via restore, since playTurn() on an empty deck just ends the round.
                ProxyPlayerSession.restore(
                    knight = it.knight,
                    wasRandom = it.wasRandom,
                    deckOrder = listOf(ProxyPlayerCard.BasicAction(CardColor.RED)),
                    discardPile = listOf(ProxyPlayerCard.UniqueAction(CardColor.WHITE)),
                    crystals = it.crystals,
                    round = 1,
                    roundEnded = false,
                    objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
                    objectiveShields = 2,
                    log = it.log,
                )
            }

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }

    @Test
    fun `toEntity and toDomain round-trip a session with no current objective card (null)`() {
        val session = ProxyPlayerSession.start(Knight.GOLDYX)

        val entity = session.toEntity(updatedAt = 0L)
        assertEquals(null, entity.objectiveCardJson)

        val restored = entity.toDomain()
        assertEquals(null, restored.objectiveCard)
        assertEquals(session, restored)
    }

    @Test
    fun `toEntity and toDomain round-trip a deck containing a Dual-Color Advanced Action card`() {
        val session = ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList())
            .endRound(
                advancedActionOfferColor = CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
                spellOfferColor = CardColor.WHITE,
            )

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }

    @Test
    fun `toEntity and toDomain round-trip a log containing every event type`() {
        val session = ProxyPlayerSession.start(
            Knight.ARYTHEA,
            deckOrder = listOf(ProxyPlayerCard.BasicAction(CardColor.GREEN)),
        )
            .playTurn() // NewObjectiveDrawn (or EndOfRoundAnnounced if deck too short - 1 card here yields NewObjectiveDrawn)
            .resolveObjective(ProxyPlayerObjectiveResolution.COMPLETED) // ObjectiveResolved
            .endRound(CardIdentity.SingleColor(CardColor.RED), CardColor.BLUE) // RoundEnded

        val restored = session.toEntity(updatedAt = 0L).toDomain()

        assertEquals(session, restored)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test --tests "com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionMapperTest"`
Expected: FAIL — `toEntity`/`toDomain` are unresolved.

- [ ] **Step 3: Implement `ProxyPlayerSessionMapper.kt`**

```kotlin
// Converts between the domain's ProxyPlayerSession/ProxyPlayerEvent/ProxyPlayerCard (plain
// Kotlin, no serialization annotations - see docs/adr/0001-domain-logic-as-plain-kotlin-module.md)
// and the data/ types Room and kotlinx.serialization actually persist: ProxyPlayerSessionEntity
// for the flat/JSON-column Room row, and ProxyPlayerCardDto/ProxyPlayerEventDto for the
// serializable mirrors. Mirrors VolkareSessionMapper.kt's structure, plus DummyPlayerSessionMapper's
// crystal-flattening convention.
package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Maps each domain ProxyPlayerCard variant to its DTO counterpart. The AdvancedAction case reuses
// CardIdentity's own toDto()/toDomain() (private to CardIdentity's own mapper file in the
// shared-card-identity plan) - re-declared here as a local helper since Kotlin `private` top-level
// functions aren't visible across files.
private fun CardIdentity.toDto(): CardIdentityDto = when (this) {
    is CardIdentity.SingleColor -> CardIdentityDto.SingleColor(color.name)
    is CardIdentity.DualColor -> CardIdentityDto.DualColor(colorA.name, colorB.name)
}

private fun CardIdentityDto.toDomain(): CardIdentity = when (this) {
    is CardIdentityDto.SingleColor -> CardIdentity.SingleColor(CardColor.valueOf(color))
    is CardIdentityDto.DualColor -> CardIdentity.DualColor(CardColor.valueOf(colorA), CardColor.valueOf(colorB))
}

private fun ProxyPlayerCard.toDto(): ProxyPlayerCardDto = when (this) {
    is ProxyPlayerCard.BasicAction -> ProxyPlayerCardDto.BasicAction(color.name)
    is ProxyPlayerCard.UniqueAction -> ProxyPlayerCardDto.UniqueAction(color.name)
    is ProxyPlayerCard.AdvancedAction -> ProxyPlayerCardDto.AdvancedAction(identity.toDto())
}

private fun ProxyPlayerCardDto.toDomain(): ProxyPlayerCard = when (this) {
    is ProxyPlayerCardDto.BasicAction -> ProxyPlayerCard.BasicAction(CardColor.valueOf(color))
    is ProxyPlayerCardDto.UniqueAction -> ProxyPlayerCard.UniqueAction(CardColor.valueOf(color))
    is ProxyPlayerCardDto.AdvancedAction -> ProxyPlayerCard.AdvancedAction(identity.toDomain())
}

private fun List<ProxyPlayerCard>.toJson(): String = Json.encodeToString(map { it.toDto() })
private fun String.toProxyPlayerCardList(): List<ProxyPlayerCard> = Json.decodeFromString<List<ProxyPlayerCardDto>>(this).map { it.toDomain() }

// Nullable Objective Card: null <-> null, otherwise a single-element JSON round-trip via the same
// DTO conversion as the deck/discard lists.
private fun ProxyPlayerCard?.toJsonOrNull(): String? = this?.let { Json.encodeToString(it.toDto()) }
private fun String?.toProxyPlayerCardOrNull(): ProxyPlayerCard? = this?.let { Json.decodeFromString<ProxyPlayerCardDto>(it).toDomain() }

private fun ProxyPlayerEvent.toDto(): ProxyPlayerEventDto = when (this) {
    is ProxyPlayerEvent.RoundStarted -> ProxyPlayerEventDto.RoundStarted(round)
    is ProxyPlayerEvent.NewObjectiveDrawn -> ProxyPlayerEventDto.NewObjectiveDrawn(round, objectiveCard.toDto(), discarded.map { it.toDto() })
    is ProxyPlayerEvent.TurnContinued -> ProxyPlayerEventDto.TurnContinued(round, objectiveCard.toDto(), shieldsNow, revealed.map { it.toDto() })
    is ProxyPlayerEvent.EndOfRoundAnnounced -> ProxyPlayerEventDto.EndOfRoundAnnounced(round)
    is ProxyPlayerEvent.ObjectiveResolved -> ProxyPlayerEventDto.ObjectiveResolved(round, objectiveCard.toDto(), resolution.name)
    is ProxyPlayerEvent.RoundEnded -> ProxyPlayerEventDto.RoundEnded(round, advancedActionOfferColor.toDto(), spellOfferColor.name, discardedObjective?.toDto())
}

private fun ProxyPlayerEventDto.toDomain(): ProxyPlayerEvent = when (this) {
    is ProxyPlayerEventDto.RoundStarted -> ProxyPlayerEvent.RoundStarted(round)
    is ProxyPlayerEventDto.NewObjectiveDrawn -> ProxyPlayerEvent.NewObjectiveDrawn(round, objectiveCard.toDomain(), discarded.map { it.toDomain() })
    is ProxyPlayerEventDto.TurnContinued -> ProxyPlayerEvent.TurnContinued(round, objectiveCard.toDomain(), shieldsNow, revealed.map { it.toDomain() })
    is ProxyPlayerEventDto.EndOfRoundAnnounced -> ProxyPlayerEvent.EndOfRoundAnnounced(round)
    is ProxyPlayerEventDto.ObjectiveResolved -> ProxyPlayerEvent.ObjectiveResolved(round, objectiveCard.toDomain(), ProxyPlayerObjectiveResolution.valueOf(resolution))
    is ProxyPlayerEventDto.RoundEnded -> ProxyPlayerEvent.RoundEnded(round, advancedActionOfferColor.toDomain(), CardColor.valueOf(spellOfferColor), discardedObjective?.toDomain())
}

/**
 * Converts a domain session into the Room row that persists it, ready for
 * [ProxyPlayerSessionDao.upsert]. Mirrors [DummyPlayerSession.toEntity]'s crystal-flattening and
 * [VolkareSession.toEntity]'s JSON-column conventions.
 */
fun ProxyPlayerSession.toEntity(updatedAt: Long = System.currentTimeMillis()): ProxyPlayerSessionEntity = ProxyPlayerSessionEntity(
    knight = knight.name,
    wasRandom = wasRandom,
    deckOrderJson = deckOrder.toJson(),
    discardPileJson = discardPile.toJson(),
    crystalsRed = crystals.getValue(CardColor.RED),
    crystalsGreen = crystals.getValue(CardColor.GREEN),
    crystalsBlue = crystals.getValue(CardColor.BLUE),
    crystalsWhite = crystals.getValue(CardColor.WHITE),
    round = round,
    roundEnded = roundEnded,
    objectiveCardJson = objectiveCard.toJsonOrNull(),
    objectiveShields = objectiveShields,
    logJson = Json.encodeToString(log.map { it.toDto() }),
    updatedAt = updatedAt,
)

/** Converts a persisted Room row back into a domain session, via [ProxyPlayerSession.restore] (the reverse of [toEntity] above). */
fun ProxyPlayerSessionEntity.toDomain(): ProxyPlayerSession = ProxyPlayerSession.restore(
    knight = Knight.valueOf(knight),
    wasRandom = wasRandom,
    deckOrder = deckOrderJson.toProxyPlayerCardList(),
    discardPile = discardPileJson.toProxyPlayerCardList(),
    crystals = mapOf(
        CardColor.RED to crystalsRed,
        CardColor.GREEN to crystalsGreen,
        CardColor.BLUE to crystalsBlue,
        CardColor.WHITE to crystalsWhite,
    ),
    round = round,
    roundEnded = roundEnded,
    objectiveCard = objectiveCardJson.toProxyPlayerCardOrNull(),
    objectiveShields = objectiveShields,
    log = Json.decodeFromString<List<ProxyPlayerEventDto>>(logJson).map { it.toDomain() },
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test --tests "com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionMapperTest"`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionMapper.kt data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionMapperTest.kt
git commit -m "Add ProxyPlayerSessionMapper"
```

---

### Task 4: `ProxyPlayerSessionDao`

**Files:**
- Create: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionDao.kt`
- Test: `data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionDaoTest.kt`

**Interfaces:**
- Consumes: `ProxyPlayerSessionEntity` (Task 2).
- Produces: `ProxyPlayerSessionDao` (`upsert`, `get`, `getUpdatedAt`). Consumed by Task 5 (`ProxyPlayerSessionRepository`) and `MageKnightBuddyDatabase.proxyPlayerSessionDao()` (Task 2).

- [ ] **Step 1: Implement `ProxyPlayerSessionDao.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room data-access object for the single autosaved Proxy Player session. Mirrors
 * [VolkareSessionDao]/[DummyPlayerSessionDao] exactly - see either's doc comment for why there
 * are no per-session lookups here.
 */
@Dao
interface ProxyPlayerSessionDao {
    /** Saves [entity] as the current session, overwriting whatever was saved before - see [ProxyPlayerSessionEntity.SINGLETON_ID]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProxyPlayerSessionEntity)

    /** Loads the autosaved session, or `null` if nothing has been saved yet. */
    @Query("SELECT * FROM proxy_player_sessions WHERE id = ${ProxyPlayerSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): ProxyPlayerSessionEntity?

    /** Reads just the autosaved session's [ProxyPlayerSessionEntity.updatedAt], or `null` if nothing has been saved yet - a cheap recency check for the setup screen's "Restore Game" flow. */
    @Query("SELECT updatedAt FROM proxy_player_sessions WHERE id = ${ProxyPlayerSessionEntity.SINGLETON_ID} LIMIT 1")
    suspend fun getUpdatedAt(): Long?
}
```

- [ ] **Step 2: Write the DAO tests**

```kotlin
package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ProxyPlayerSessionDaoTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-proxy-player-session", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `get returns null when nothing has been saved yet`() = runTest {
        val dao = database.proxyPlayerSessionDao()

        assertNull(dao.get())
    }

    @Test
    fun `upsert then get round-trips the saved session`() = runTest {
        val dao = database.proxyPlayerSessionDao()
        val entity = testEntity(round = 1)

        dao.upsert(entity)

        assertEquals(entity, dao.get())
    }

    @Test
    fun `upsert replaces the single saved slot instead of adding a second row`() = runTest {
        val dao = database.proxyPlayerSessionDao()

        dao.upsert(testEntity(round = 1))
        dao.upsert(testEntity(round = 2))

        assertEquals(2, dao.get()?.round)
    }

    @Test
    fun `getUpdatedAt returns null when nothing has been saved yet`() = runTest {
        val dao = database.proxyPlayerSessionDao()

        assertNull(dao.getUpdatedAt())
    }

    @Test
    fun `getUpdatedAt returns the saved row's updatedAt without needing the full session`() = runTest {
        val dao = database.proxyPlayerSessionDao()

        dao.upsert(testEntity(round = 1, updatedAt = 12345L))

        assertEquals(12345L, dao.getUpdatedAt())
    }

    private fun testEntity(round: Int, updatedAt: Long = 0L) = ProxyPlayerSessionEntity(
        knight = "CORAL",
        wasRandom = false,
        deckOrderJson = "[]",
        discardPileJson = "[]",
        crystalsRed = 0,
        crystalsGreen = 0,
        crystalsBlue = 0,
        crystalsWhite = 0,
        round = round,
        roundEnded = false,
        objectiveCardJson = null,
        objectiveShields = 0,
        logJson = "[]",
        updatedAt = updatedAt,
    )
}
```

- [ ] **Step 3: Run the DAO tests**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test --tests "com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionDaoTest"`
Expected: PASS (5 tests)

- [ ] **Step 4: Commit**

```bash
git add data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionDao.kt data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionDaoTest.kt
git commit -m "Add ProxyPlayerSessionDao"
```

---

### Task 5: `ProxyPlayerSessionRepository` and `FakeProxyPlayerSessionDao`

**Files:**
- Create: `data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionRepository.kt`
- Test: `data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionRepositoryTest.kt`
- Create: `app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/FakeProxyPlayerSessionDao.kt` (a test double for the UI-layer plan's ViewModel tests — created here since it belongs next to `FakeDummyPlayerSessionDao`/`FakeVolkareSessionDao`, but has no UI-layer dependency itself)

**Interfaces:**
- Consumes: `ProxyPlayerSession` (domain-layer plan), `ProxyPlayerSessionDao` (Task 4), `ProxyPlayerSessionEntity`/`.toEntity()`/`.toDomain()` (Tasks 2-3).
- Produces: `ProxyPlayerSessionRepository(dao: ProxyPlayerSessionDao)` with `save`/`restore`/`updatedAt`; `FakeProxyPlayerSessionDao` implementing `ProxyPlayerSessionDao`. Consumed by the UI-layer plan's `ProxyPlayerSetupViewModel`/`ProxyPlayerAiViewModel` and their tests.

- [ ] **Step 1: Write the failing repository tests**

```kotlin
package com.guyteichman.mageknightbuddy.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ProxyPlayerSessionRepositoryTest {
    private lateinit var database: MageKnightBuddyDatabase
    private lateinit var dbFile: File
    private lateinit var repository: ProxyPlayerSessionRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("test-proxy-player-repository", ".db")
        database = Room.databaseBuilder<MageKnightBuddyDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = ProxyPlayerSessionRepository(database.proxyPlayerSessionDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `restore returns null when nothing has been saved yet`() = runTest {
        assertNull(repository.restore())
    }

    @Test
    fun `save then restore round-trips a ProxyPlayerSession through Room`() = runTest {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        repository.save(session)

        assertEquals(session, repository.restore())
    }

    @Test
    fun `save silently overwrites the previously saved session`() = runTest {
        val first = ProxyPlayerSession.start(Knight.CORAL)
        val second = ProxyPlayerSession.start(Knight.GOLDYX)

        repository.save(first)
        repository.save(second)

        assertEquals(second, repository.restore())
    }

    @Test
    fun `updatedAt returns the timestamp save was called with, without needing the full session`() = runTest {
        val session = ProxyPlayerSession.start(Knight.CORAL)

        repository.save(session, updatedAt = 42L)

        assertEquals(42L, repository.updatedAt())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test --tests "com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepositoryTest"`
Expected: FAIL — `ProxyPlayerSessionRepository` is unresolved.

- [ ] **Step 3: Implement `ProxyPlayerSessionRepository.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession

/**
 * Persistence entry point for the Dummy Player tab's Proxy Player mode: the rest of the app
 * (`app/`) calls [save]/[restore] with plain domain [ProxyPlayerSession] objects and never has to
 * know about Room, [ProxyPlayerSessionEntity], or the DTO/JSON conversion in
 * [ProxyPlayerSessionMapper]. Mirrors [VolkareSessionRepository]/[DummyPlayerSessionRepository] exactly.
 */
class ProxyPlayerSessionRepository(private val dao: ProxyPlayerSessionDao) {
    /** Autosaves [session], overwriting whatever was previously saved. [updatedAt] defaults to "now". */
    suspend fun save(session: ProxyPlayerSession, updatedAt: Long = System.currentTimeMillis()) {
        dao.upsert(session.toEntity(updatedAt))
    }

    /** Loads the autosaved session, or `null` if nothing has been saved yet. */
    suspend fun restore(): ProxyPlayerSession? = dao.get()?.toDomain()

    /** Reads just the last save's timestamp, or `null` if nothing has been saved yet - lets the setup screen's "Restore Game" flow compare recency across all 3 Dummy Player tab modes. */
    suspend fun updatedAt(): Long? = dao.getUpdatedAt()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test --tests "com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepositoryTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Create `FakeProxyPlayerSessionDao` for the UI-layer plan's tests**

```kotlin
package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionDao
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionEntity
import kotlinx.coroutines.CompletableDeferred

/** The Proxy Player-mode counterpart to [FakeDummyPlayerSessionDao]/[FakeVolkareSessionDao] - see either's doc comment. */
class FakeProxyPlayerSessionDao : ProxyPlayerSessionDao {
    private var stored: ProxyPlayerSessionEntity? = null

    var upsertGate: CompletableDeferred<Unit>? = null

    override suspend fun upsert(entity: ProxyPlayerSessionEntity) {
        upsertGate?.await()
        stored = entity
    }

    override suspend fun get(): ProxyPlayerSessionEntity? = stored

    override suspend fun getUpdatedAt(): Long? = stored?.updatedAt
}
```

- [ ] **Step 6: Run the full data module test suite and confirm `app/` still compiles**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :data:test :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add data/src/main/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionRepository.kt data/src/test/kotlin/com/guyteichman/mageknightbuddy/data/ProxyPlayerSessionRepositoryTest.kt app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/FakeProxyPlayerSessionDao.kt
git commit -m "Add ProxyPlayerSessionRepository and FakeProxyPlayerSessionDao"
```
