# Proxy Player UI Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Proxy Player mode to the Dummy Player tab's UI: setup (pick a Knight), an AI screen showing the current Objective Card / Shield count / movement points with Play Turn, Explored, and Completed actions, an End Round dialog, and a movement-rules help button — wired through `ProxyPlayerSetupViewModel`/`ProxyPlayerAiViewModel`.

**Architecture:** `ProxyPlayerSetupViewModel` mirrors `DummyPlayerSetupViewModel` (a Knight picker, no Scenario/RaceLevel fields — Proxy Player, unlike Volkare, picks a Knight the same way standard Dummy Player does). `ProxyPlayerAiViewModel` mirrors `DummyPlayerAiViewModel`/`VolkareAiViewModel` (restore-on-create, mutate-and-autosave actions, an `isBusy` double-tap guard). This plan also **refactors the setup screen's mode selection**: today "Volkare" is a hidden extra entry inside the Knight dropdown (`KnightPicker`'s `volkareSelected` flag) — a hack that works because Volkare has no Knight of its own. Proxy Player *does* pick a Knight, so bolting it onto that same dropdown as a third special entry doesn't hold up; this plan replaces it with a proper 3-way segmented selector (Standard / Volkare / Proxy Player) at the top of the setup screen, with the Knight sub-picker shown for both Standard and Proxy Player.

**Tech Stack:** Jetpack Compose (Material 3), Navigation Compose, Kotlin coroutines.

**Depends on:** `docs/superpowers/plans/2026-07-23-proxy-player-1-domain-layer.md` and `...-2-persistence-layer.md`.

## Global Constraints

- Every field tied to a rulebook mechanic gets a "?" `HelpButton` sourced from `app/src/main/assets/field_help.json` — never a raw rulebook filename in UI text (established project convention).
- ViewModels get TDD rigor (they hold real logic); pure Compose screen scaffolding does not, per this project's CLAUDE.md — but every new composable/property still gets its KDoc/inline-comment per the commenting standard.
- **Before finalizing Task 5's mana-die input**, run the `/prototype` skill to compare the two UX options discussed during design (an explicit yes/no toggle per turn vs. a static reminder in the movement-points breakdown) — the project owner asked to feel both before committing. Task 5 below implements the toggle as the default starting point; swap it if the prototype favors the alternative.
- Run `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew test build` before each commit.

---

### Task 1: `ProxyPlayerSetupViewModel`

**Files:**
- Create: `app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/ProxyPlayerSetupViewModel.kt`
- Test: `app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/ProxyPlayerSetupViewModelTest.kt`

**Interfaces:**
- Consumes: `ProxyPlayerSessionRepository` (persistence-layer plan), `ProxyPlayerSession.start` (domain-layer plan), `FakeProxyPlayerSessionDao` (persistence-layer plan, for the test).
- Produces: `ProxyPlayerSetupViewModel(savedStateHandle, repository)` with `knight`, `wasRandom`, `hasSavedSession`, `pickKnight`, `pickRandom`, `start()`, and a `factory(repository)` companion. Consumed by Task 4 (setup screen).

- [ ] **Step 1: Write the failing tests**

Read `app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerSetupViewModelTest.kt` first to copy its exact `SavedStateHandle`/coroutine-test fixture setup, then write the equivalent for Proxy Player:

```kotlin
package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.lifecycle.SavedStateHandle
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class ProxyPlayerSetupViewModelTest {

    @Test
    fun `defaults to the first Knight, not random, and no saved session`() = runTest {
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()))
        advanceUntilIdle()

        assertEquals(Knight.entries.first(), viewModel.knight)
        assertEquals(false, viewModel.wasRandom)
        assertEquals(false, viewModel.hasSavedSession)
    }

    @Test
    fun `pickKnight selects the Knight directly and clears wasRandom`() {
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()))

        viewModel.pickKnight(Knight.CORAL)

        assertEquals(Knight.CORAL, viewModel.knight)
        assertEquals(false, viewModel.wasRandom)
    }

    @Test
    fun `pickRandom rolls a Knight immediately and sets wasRandom`() {
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()))

        viewModel.pickRandom()

        assertTrue(viewModel.wasRandom)
    }

    @Test
    fun `start saves a fresh session and sets hasSavedSession`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        val viewModel = ProxyPlayerSetupViewModel(SavedStateHandle(), repository)
        advanceUntilIdle()

        viewModel.pickKnight(Knight.GOLDYX)
        viewModel.start()

        assertEquals(true, viewModel.hasSavedSession)
        assertEquals(Knight.GOLDYX, repository.restore()?.knight)
    }
}
```

(If the copied fixture from `DummyPlayerSetupViewModelTest.kt` uses a different coroutine-test idiom than shown above — e.g. a custom test rule — match that file's actual style exactly rather than this sketch.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :app:testDebugUnitTest --tests "com.guyteichman.mageknightbuddy.ui.dummyplayer.ProxyPlayerSetupViewModelTest"`
Expected: FAIL — `ProxyPlayerSetupViewModel` is unresolved.

- [ ] **Step 3: Implement `ProxyPlayerSetupViewModel.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlinx.coroutines.launch

/**
 * Backs Proxy Player mode's setup fields: tracks the chosen [Knight] (or "Random") - the same
 * shape as [DummyPlayerSetupViewModel], since Proxy Player picks a Knight the same way standard
 * Dummy Player does (unlike Volkare, which has no Knight field at all - see
 * [VolkareSetupViewModel]). On Start, builds a fresh [ProxyPlayerSession] and autosaves it.
 */
@OptIn(SavedStateHandleSaveableApi::class)
class ProxyPlayerSetupViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProxyPlayerSessionRepository,
) : ViewModel() {

    var knight: Knight by savedStateHandle.saveable("knight") { mutableStateOf(Knight.entries.first()) }
    var wasRandom: Boolean by savedStateHandle.saveable("wasRandom") { mutableStateOf(false) }

    var hasSavedSession: Boolean by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            hasSavedSession = repository.restore() != null
        }
    }

    /** Picks a specific Knight directly, clearing the "(Random)" badge. */
    fun pickKnight(selected: Knight) {
        knight = selected
        wasRandom = false
    }

    /** Resolves "Random" immediately, mirroring [DummyPlayerSetupViewModel.pickRandom]. */
    fun pickRandom() {
        knight = Knight.entries.random()
        wasRandom = true
    }

    /** Builds a new session for the chosen Knight and autosaves it, overwriting any previously saved Proxy Player session. */
    suspend fun start() {
        repository.save(ProxyPlayerSession.start(knight = knight, wasRandom = wasRandom))
        hasSavedSession = true
    }

    companion object {
        fun factory(repository: ProxyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProxyPlayerSetupViewModel(createSavedStateHandle(), repository) }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :app:testDebugUnitTest --tests "com.guyteichman.mageknightbuddy.ui.dummyplayer.ProxyPlayerSetupViewModelTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/ProxyPlayerSetupViewModel.kt app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/ProxyPlayerSetupViewModelTest.kt
git commit -m "Add ProxyPlayerSetupViewModel"
```

---

### Task 2: `ProxyPlayerAiViewModel`

**Files:**
- Create: `app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/ProxyPlayerAiViewModel.kt`
- Test: `app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/ProxyPlayerAiViewModelTest.kt`

**Interfaces:**
- Consumes: `ProxyPlayerSessionRepository`, `ProxyPlayerSession.playTurn/resolveObjective/endRound`, `CardIdentity`.
- Produces: `ProxyPlayerAiViewModel(repository)` with `session`, `isBusy`, `playTurn()`, `resolveObjective(resolution)`, `endRound(advancedActionOfferColor, spellOfferColor)`. Consumed by Task 5 (AI screen).

- [ ] **Step 1: Write the failing tests**

Read `app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerAiViewModelTest.kt` first to copy its exact fixture/coroutine-test style, then write:

```kotlin
package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class ProxyPlayerAiViewModelTest {

    @Test
    fun `restores the saved session on creation`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        repository.save(ProxyPlayerSession.start(Knight.CORAL))
        val viewModel = ProxyPlayerAiViewModel(repository)
        advanceUntilIdle()

        assertEquals(Knight.CORAL, viewModel.session?.knight)
    }

    @Test
    fun `playTurn advances the session and autosaves`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        repository.save(ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()))
        val viewModel = ProxyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.playTurn()

        assertEquals(true, viewModel.session?.roundEnded)
        assertEquals(true, repository.restore()?.roundEnded)
    }

    @Test
    fun `resolveObjective discards the current objective and autosaves`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        repository.save(
            ProxyPlayerSession.restore(
                knight = Knight.CORAL,
                wasRandom = false,
                deckOrder = emptyList(),
                discardPile = emptyList(),
                crystals = ProxyPlayerSession.start(Knight.CORAL).crystals,
                round = 1,
                roundEnded = false,
                objectiveCard = ProxyPlayerCard.BasicAction(CardColor.GREEN),
                objectiveShields = 1,
                log = emptyList(),
            ),
        )
        val viewModel = ProxyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.resolveObjective(ProxyPlayerObjectiveResolution.COMPLETED)

        assertNull(viewModel.session?.objectiveCard)
        assertNull(repository.restore()?.objectiveCard)
    }

    @Test
    fun `endRound applies the round-prep offer interactions and autosaves`() = runTest {
        val repository = ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao())
        repository.save(ProxyPlayerSession.start(Knight.CORAL, deckOrder = emptyList()))
        val viewModel = ProxyPlayerAiViewModel(repository)
        advanceUntilIdle()

        viewModel.endRound(advancedActionOfferColor = CardIdentity.SingleColor(CardColor.WHITE), spellOfferColor = CardColor.BLUE)

        assertEquals(2, viewModel.session?.round)
        assertEquals(2, repository.restore()?.round)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :app:testDebugUnitTest --tests "com.guyteichman.mageknightbuddy.ui.dummyplayer.ProxyPlayerAiViewModelTest"`
Expected: FAIL — `ProxyPlayerAiViewModel` is unresolved.

- [ ] **Step 3: Implement `ProxyPlayerAiViewModel.kt`**

```kotlin
package com.guyteichman.mageknightbuddy.ui.dummyplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerObjectiveResolution
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import kotlinx.coroutines.launch

/**
 * Backs Proxy Player mode's AI (turn/round) screen: restores the saved [ProxyPlayerSession] on
 * creation and mutates it via [playTurn]/[resolveObjective]/[endRound], autosaving through
 * [repository] after every mutation - the Proxy Player counterpart to
 * [DummyPlayerAiViewModel]/[VolkareAiViewModel], including the same [isBusy] double-tap guard.
 */
class ProxyPlayerAiViewModel(private val repository: ProxyPlayerSessionRepository) : ViewModel() {

    var session: ProxyPlayerSession? by mutableStateOf(null)
        private set

    var isBusy: Boolean by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            session = repository.restore()
        }
    }

    /** Plays one Proxy Player turn (docs/rules/proxy-player.md) and autosaves. */
    suspend fun playTurn() {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.playTurn() ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    /** Resolves the current Objective Card as Explored or Completed (docs/rules/proxy-player.md's "Resolution") and autosaves. */
    suspend fun resolveObjective(resolution: ProxyPlayerObjectiveResolution) {
        if (isBusy) return
        isBusy = true
        try {
            val next = session?.resolveObjective(resolution) ?: return
            session = next
            repository.save(next)
        } finally {
            isBusy = false
        }
    }

    /** Applies the round-prep offer interactions and autosaves - see [ProxyPlayerSession.endRound]. */
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

    companion object {
        fun factory(repository: ProxyPlayerSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProxyPlayerAiViewModel(repository) }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :app:testDebugUnitTest --tests "com.guyteichman.mageknightbuddy.ui.dummyplayer.ProxyPlayerAiViewModelTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/ProxyPlayerAiViewModel.kt app/src/test/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/ProxyPlayerAiViewModelTest.kt
git commit -m "Add ProxyPlayerAiViewModel"
```

---

### Task 3: Wire `ProxyPlayerSessionRepository` at the app composition root

**Files:**
- Modify: `app/src/main/kotlin/com/guyteichman/mageknightbuddy/MageKnightBuddyApplication.kt`
- Modify: `app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/MageKnightBuddyApp.kt`

**Interfaces:**
- Consumes: `ProxyPlayerSessionRepository`, `MageKnightBuddyDatabase.proxyPlayerSessionDao()` (persistence-layer plan).
- Produces: `MageKnightBuddyApplication.proxyPlayerSessionRepository`, threaded into `DummyPlayerTab(...)`'s call site as a new `proxyPlayerRepository` parameter (added in Task 4).

No test — this is pure wiring (constructor calls and parameter threading), verified by the build.

- [ ] **Step 1: Add the repository to `MageKnightBuddyApplication.kt`**

Add alongside the existing `volkareSessionRepository`:

```kotlin
    val proxyPlayerSessionRepository by lazy { ProxyPlayerSessionRepository(database.proxyPlayerSessionDao()) }
```

Add the matching import: `import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository`.

- [ ] **Step 2: Thread it through `MageKnightBuddyApp.kt`**

Add a `proxyPlayerRepository: ProxyPlayerSessionRepository` parameter to the composable that currently takes `dummyPlayerRepository`/`volkareRepository`, and pass it to `DummyPlayerTab(...)`'s call site (this call site's exact new shape is `DummyPlayerTab(repository = dummyPlayerRepository, volkareRepository = volkareRepository, proxyPlayerRepository = proxyPlayerRepository, fieldHelp = fieldHelp)` — the `DummyPlayerTab` signature itself is updated in Task 4). Add the matching import and update wherever this composable is called (likely `MainActivity` or similar — find via `grep -rn "MageKnightBuddyApp(" app/src/main`) to pass `application.proxyPlayerSessionRepository`.

- [ ] **Step 3: Confirm the app module compiles**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :app:compileDebugKotlin`
Expected: FAIL until Task 4 updates `DummyPlayerTab`'s signature to accept the new parameter — that's expected; proceed directly to Task 4 before running this check again.

- [ ] **Step 4: Commit** (together with Task 4, once the app module compiles again)

```bash
git add app/src/main/kotlin/com/guyteichman/mageknightbuddy/MageKnightBuddyApplication.kt app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/MageKnightBuddyApp.kt
git commit -m "Wire ProxyPlayerSessionRepository at the app composition root"
```

---

### Task 4: Refactor the setup screen into a 3-way mode selector

**Files:**
- Modify: `app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerScreen.kt`

**Interfaces:**
- Consumes: `ProxyPlayerSetupViewModel` (Task 1), `ProxyPlayerSessionRepository` (Task 3).
- Produces: `DummyPlayerTab(repository, volkareRepository, proxyPlayerRepository, fieldHelp)` (added `proxyPlayerRepository` parameter and a `PROXY_PLAYER_AI_ROUTE` destination). A new `DummyPlayerMode` enum (`STANDARD`, `VOLKARE`, `PROXY_PLAYER`) replacing the current `volkareSelected: Boolean` flag throughout this file.

This task is Compose scaffolding (no new domain/ViewModel logic — it re-routes existing state into a clearer UI shape), so it doesn't need TDD rigor per this project's CLAUDE.md, but every changed/added composable still needs its KDoc updated to match the new shape.

- [ ] **Step 1: Add the `DummyPlayerMode` enum and the `PROXY_PLAYER_AI_ROUTE` constant**

Near the top of the file, alongside the existing route constants:

```kotlin
private const val PROXY_PLAYER_AI_ROUTE = "proxy_player_ai"

/** Which of the Dummy Player tab's 3 modes is currently selected on the setup screen - see `CONTEXT.md`'s "Dummy Player tab" entry. Replaces the old `volkareSelected: Boolean` flag now that Proxy Player also needs its own selected state. */
private enum class DummyPlayerMode { STANDARD, VOLKARE, PROXY_PLAYER }
```

- [ ] **Step 2: Update `DummyPlayerTab`'s signature and nested `NavHost`**

```kotlin
@Composable
fun DummyPlayerTab(
    repository: DummyPlayerSessionRepository,
    volkareRepository: VolkareSessionRepository,
    proxyPlayerRepository: ProxyPlayerSessionRepository,
    fieldHelp: Map<String, FieldHelp>,
) {
    val nestedNavController = rememberNavController()

    NavHost(navController = nestedNavController, startDestination = DUMMY_PLAYER_SETUP_ROUTE) {
        composable(DUMMY_PLAYER_SETUP_ROUTE) {
            DummyPlayerSetupScreen(
                repository = repository,
                volkareRepository = volkareRepository,
                proxyPlayerRepository = proxyPlayerRepository,
                onStart = { nestedNavController.navigate(DUMMY_PLAYER_AI_ROUTE) },
                onRestore = { nestedNavController.navigate(DUMMY_PLAYER_AI_ROUTE) },
                onStartVolkare = { nestedNavController.navigate(VOLKARE_AI_ROUTE) },
                onRestoreVolkare = { nestedNavController.navigate(VOLKARE_AI_ROUTE) },
                onStartProxyPlayer = { nestedNavController.navigate(PROXY_PLAYER_AI_ROUTE) },
                onRestoreProxyPlayer = { nestedNavController.navigate(PROXY_PLAYER_AI_ROUTE) },
            )
        }
        composable(DUMMY_PLAYER_AI_ROUTE) {
            DummyPlayerAiScreen(repository = repository, fieldHelp = fieldHelp, onBack = { nestedNavController.popBackStack() })
        }
        composable(VOLKARE_AI_ROUTE) {
            VolkareAiScreen(repository = volkareRepository, onBack = { nestedNavController.popBackStack() })
        }
        composable(PROXY_PLAYER_AI_ROUTE) {
            ProxyPlayerAiScreen(repository = proxyPlayerRepository, fieldHelp = fieldHelp, onBack = { nestedNavController.popBackStack() })
        }
    }
}
```

Add the matching import: `import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository`.

- [ ] **Step 3: Replace `volkareSelected: Boolean` with `mode: DummyPlayerMode` in `DummyPlayerSetupScreen`**

```kotlin
@Composable
private fun DummyPlayerSetupScreen(
    repository: DummyPlayerSessionRepository,
    volkareRepository: VolkareSessionRepository,
    proxyPlayerRepository: ProxyPlayerSessionRepository,
    onStart: () -> Unit,
    onRestore: () -> Unit,
    onStartVolkare: () -> Unit,
    onRestoreVolkare: () -> Unit,
    onStartProxyPlayer: () -> Unit,
    onRestoreProxyPlayer: () -> Unit,
) {
    val viewModel: DummyPlayerSetupViewModel = viewModel(factory = DummyPlayerSetupViewModel.factory(repository))
    val volkareViewModel: VolkareSetupViewModel = viewModel(factory = VolkareSetupViewModel.factory(volkareRepository))
    val proxyPlayerViewModel: ProxyPlayerSetupViewModel = viewModel(factory = ProxyPlayerSetupViewModel.factory(proxyPlayerRepository))
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(DummyPlayerMode.STANDARD) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Dummy Player")

        // 3-way segmented mode selector - replaces the old "Volkare hidden in the Knight
        // dropdown" hack now that Proxy Player also needs a Knight sub-picker of its own.
        DummyPlayerModeSelector(mode = mode, onModeSelected = { mode = it })

        when (mode) {
            DummyPlayerMode.STANDARD -> KnightOnlyPicker(
                knight = viewModel.knight,
                wasRandom = viewModel.wasRandom,
                onKnightSelected = viewModel::pickKnight,
                onRandomSelected = viewModel::pickRandom,
            )
            DummyPlayerMode.PROXY_PLAYER -> KnightOnlyPicker(
                knight = proxyPlayerViewModel.knight,
                wasRandom = proxyPlayerViewModel.wasRandom,
                onKnightSelected = proxyPlayerViewModel::pickKnight,
                onRandomSelected = proxyPlayerViewModel::pickRandom,
            )
            DummyPlayerMode.VOLKARE -> VolkareSetupFields(
                scenario = volkareViewModel.scenario,
                raceLevel = volkareViewModel.raceLevel,
                woundCount = volkareViewModel.woundCount,
                woundCountIsCustom = volkareViewModel.woundCountIsCustom,
                onScenarioSelected = volkareViewModel::pickScenario,
                onRaceLevelSelected = volkareViewModel::pickRaceLevel,
                onWoundCountChanged = volkareViewModel::changeWoundCount,
            )
        }

        Button(
            onClick = {
                scope.launch {
                    when (mode) {
                        DummyPlayerMode.STANDARD -> { viewModel.start(); onStart() }
                        DummyPlayerMode.VOLKARE -> { volkareViewModel.start(); onStartVolkare() }
                        DummyPlayerMode.PROXY_PLAYER -> { proxyPlayerViewModel.start(); onStartProxyPlayer() }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start")
        }

        OutlinedButton(
            onClick = {
                // Restore Game always resumes whichever of the 3 saved sessions is most recent,
                // regardless of what's currently selected above.
                scope.launch {
                    val timestamps = listOf(
                        DummyPlayerMode.STANDARD to (repository.updatedAt() ?: -1),
                        DummyPlayerMode.VOLKARE to (volkareRepository.updatedAt() ?: -1),
                        DummyPlayerMode.PROXY_PLAYER to (proxyPlayerRepository.updatedAt() ?: -1),
                    )
                    when (timestamps.maxByOrNull { it.second }?.first) {
                        DummyPlayerMode.VOLKARE -> onRestoreVolkare()
                        DummyPlayerMode.PROXY_PLAYER -> onRestoreProxyPlayer()
                        else -> onRestore()
                    }
                }
            },
            enabled = viewModel.hasSavedSession || volkareViewModel.hasSavedSession || proxyPlayerViewModel.hasSavedSession,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Restore Game")
        }
    }
}
```

- [ ] **Step 4: Add `DummyPlayerModeSelector` and rename `KnightPicker` to `KnightOnlyPicker`**

```kotlin
/** The 3-way Standard/Volkare/Proxy Player mode selector at the top of the setup screen - see `CONTEXT.md`'s "Dummy Player tab" entry. */
@Composable
private fun DummyPlayerModeSelector(mode: DummyPlayerMode, onModeSelected: (DummyPlayerMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = mode == DummyPlayerMode.STANDARD, onClick = { onModeSelected(DummyPlayerMode.STANDARD) }, label = { Text("Standard") })
        FilterChip(selected = mode == DummyPlayerMode.VOLKARE, onClick = { onModeSelected(DummyPlayerMode.VOLKARE) }, label = { Text("Volkare") })
        FilterChip(selected = mode == DummyPlayerMode.PROXY_PLAYER, onClick = { onModeSelected(DummyPlayerMode.PROXY_PLAYER) }, label = { Text("Proxy Player") })
    }
}
```

Rename the existing `KnightPicker` composable to `KnightOnlyPicker` and drop its `volkareSelected`/`onVolkareSelected` parameters and the "Volkare" dropdown entry/branch inside it (that responsibility moved to `DummyPlayerModeSelector` above) — keep everything else (the Knight/Random dropdown entries, `KnightShieldIcon`/`RandomShieldIcon` rendering) unchanged. Update its doc comment to drop the now-obsolete "Volkare" paragraph.

- [ ] **Step 5: Update every other call site referencing the old `KnightPicker`/`volkareSelected` name**

Run: `grep -rn "KnightPicker\|volkareSelected" app/src/main app/src/test` and fix each remaining reference to use `KnightOnlyPicker`/`DummyPlayerMode` instead.

- [ ] **Step 6: Build**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew :app:compileDebugKotlin`
Expected: FAIL — `ProxyPlayerAiScreen` doesn't exist yet (Task 5). Expected at this point; continue to Task 5.

- [ ] **Step 7: Commit** (together with Task 5, once the module compiles)

```bash
git add app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerScreen.kt app/src/main/kotlin/com/guyteichman/mageknightbuddy/MageKnightBuddyApplication.kt app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/MageKnightBuddyApp.kt
git commit -m "Replace the hidden Volkare dropdown entry with a 3-way mode selector"
```

---

### Task 5: `ProxyPlayerAiScreen` and movement-rules help content

**Files:**
- Modify: `app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerScreen.kt`
- Modify: `app/src/main/assets/field_help.json`

**Interfaces:**
- Consumes: `ProxyPlayerAiViewModel` (Task 2), `ProxyPlayerObjectiveResolution`/`ProxyPlayerCard`/`CardIdentity`.
- Produces: `ProxyPlayerAiScreen(repository, fieldHelp, onBack)` composable (referenced by Task 4's `NavHost`).

- [ ] **Step 1: Add the `field_help.json` entries**

Read `app/src/main/assets/field_help.json` first to match its existing key/value style and citation format, then add two entries:

```json
"Proxy Player Movement": {
  "text": "Move by the most direct path toward your objective - terrain cost doesn't matter. You can't move into a space with another Hero, a lake, or a mountain. If your path would provoke or pass through a rampaging enemy, or pass through an unconquered fortified site/another player's keep, stop and resolve that instead of continuing.",
  "source": "Mage Knight: The Apocalypse Dragon rulebook, p.17"
},
"Proxy Player Objective": {
  "text": "Green: move to the closest unconquered adventure site. Red: closest unconquered fortified site or monastery. White: closest site to recruit a Unit or learn an Advanced Action/Spell. Blue: whichever of those is furthest from the portal. If none is reachable, explore instead.",
  "source": "Mage Knight: The Apocalypse Dragon rulebook, p.16-17"
}
```

- [ ] **Step 2: Implement `ProxyPlayerAiScreen`**

Add to `DummyPlayerScreen.kt` (or split into a new file `ProxyPlayerScreen.kt` if this file is already large — check its current line count first with `wc -l` and follow this project's "split if a file you're modifying has grown unwieldy" convention from CLAUDE.md):

```kotlin
/**
 * Human-readable label for a [ProxyPlayerCard], distinguishing the 3 kinds your original design
 * notes called out as needing to be visually clear - a generic Basic Action, a Knight's own
 * Unique Basic Action Card, or an Advanced Action (single- or dual-color) - since only the latter
 * two get the +2 movement bonus (see [ProxyPlayerCard.movementBonus]).
 */
private fun ProxyPlayerCard.displayText(): String = when (this) {
    is ProxyPlayerCard.BasicAction -> "${color.name.lowercase().replaceFirstChar { it.uppercase() }} (Basic Action)"
    is ProxyPlayerCard.UniqueAction -> "${color.name.lowercase().replaceFirstChar { it.uppercase() }} (Unique)"
    is ProxyPlayerCard.AdvancedAction -> when (val id = identity) {
        is CardIdentity.SingleColor -> "${id.color.name.lowercase().replaceFirstChar { it.uppercase() }} (Advanced Action)"
        is CardIdentity.DualColor -> "${id.colorA.name.lowercase().replaceFirstChar { it.uppercase() }}/${id.colorB.name.lowercase().replaceFirstChar { it.uppercase() }} (Advanced Action)"
    }
}

/**
 * Proxy Player mode's AI (turn/round) screen: shows the current Objective Card (or "no
 * objective" before the first turn) via [displayText] - distinguishing Basic Action / Unique /
 * Advanced Action per your original design notes - its Shield count, the computed movement-point
 * total (see [ProxyPlayerSession.movementPoints]), and 3 actions - Play Turn, and (only once
 * there's a current objective) Explored/Completed to resolve it - plus an End Round dialog
 * matching [DummyPlayerAiScreen]'s shape. Per docs/rules/proxy-player.md's app-scope note: this
 * screen never decides *where* the Hero moves or *what* gets conquered - only the movement-point
 * number and Objective Card/Shield bookkeeping.
 */
@Composable
private fun ProxyPlayerAiScreen(repository: ProxyPlayerSessionRepository, fieldHelp: Map<String, FieldHelp>, onBack: () -> Unit) {
    val viewModel: ProxyPlayerAiViewModel = viewModel(factory = ProxyPlayerAiViewModel.factory(repository))
    val scope = rememberCoroutineScope()
    val session = viewModel.session
    // The player's per-turn report of whether a matching-color (or gold, by day) mana die
    // currently sits in the Source - see docs/rules/proxy-player.md's "Movement points". This is
    // the toggle-per-turn design from the plan's prototype note; swap this state/UI if the
    // /prototype comparison favored the static-reminder alternative instead.
    var hasMatchingManaDie by remember { mutableStateOf(false) }
    var showEndRoundDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proxy Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (session == null) {
                CircularProgressIndicator()
            } else {
                Text("Round ${session.round}")

                if (session.objectiveCard == null) {
                    Text("No current objective - tap Play Turn to draw one.")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Objective: ${session.objectiveCard.displayText()}")
                        HelpButton(keys = listOf("Proxy Player Objective"), fieldHelp = fieldHelp)
                    }
                    Text("Shields: ${session.objectiveShields}")

                    LabeledCheckbox(
                        checked = hasMatchingManaDie,
                        onCheckedChange = { hasMatchingManaDie = it },
                        label = "Matching mana die in the Source",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Movement points: ${session.movementPoints(hasMatchingManaDie)}")
                        HelpButton(keys = listOf("Proxy Player Movement"), fieldHelp = fieldHelp)
                    }
                }

                Button(
                    onClick = { scope.launch { viewModel.playTurn() } },
                    enabled = !viewModel.isBusy && !session.roundEnded,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Play Turn")
                }

                if (session.objectiveCard != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { scope.launch { viewModel.resolveObjective(ProxyPlayerObjectiveResolution.EXPLORED) } },
                            enabled = !viewModel.isBusy,
                        ) {
                            Text("Explored")
                        }
                        OutlinedButton(
                            onClick = { scope.launch { viewModel.resolveObjective(ProxyPlayerObjectiveResolution.COMPLETED) } },
                            enabled = !viewModel.isBusy,
                        ) {
                            Text("Completed")
                        }
                    }
                }

                OutlinedButton(onClick = { showEndRoundDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("End Round")
                }
            }
        }
    }

    if (showEndRoundDialog) {
        // Same Advanced Action offer color (+ optional dual-color) / Spell offer color shape as
        // the standard Dummy Player's End Round dialog (see the shared-card-identity plan's Task
        // 4) - copy that dialog's structure verbatim, swapping its viewModel.endRound(...) call
        // for viewModel.endRound(advancedActionOfferColor = ..., spellOfferColor = ...) here.
        ProxyPlayerEndRoundDialog(
            fieldHelp = fieldHelp,
            onDismiss = { showEndRoundDialog = false },
            onConfirm = { advancedActionOfferColor, spellOfferColor ->
                scope.launch {
                    viewModel.endRound(advancedActionOfferColor, spellOfferColor)
                    showEndRoundDialog = false
                }
            },
        )
    }
}

/**
 * The End Round dialog: same fields as the standard Dummy Player's (Advanced Action offer color,
 * optionally dual-color, and Spell offer color) - see the shared-card-identity plan's Task 4 for
 * the exact `CardIdentity` assembly logic to copy here (checkbox + optional second color picker).
 */
@Composable
private fun ProxyPlayerEndRoundDialog(
    fieldHelp: Map<String, FieldHelp>,
    onDismiss: () -> Unit,
    onConfirm: (advancedActionOfferColor: CardIdentity, spellOfferColor: CardColor) -> Unit,
) {
    // Implementation mirrors the standard Dummy Player's End Round dialog exactly (color pickers
    // + dual-color checkbox for the Advanced Action offer, a single color picker for the Spell
    // offer, a HelpButton(keys = listOf("Round-Prep Offers"), fieldHelp = fieldHelp)) - copy that
    // dialog's composable body here rather than re-deriving it from scratch.
}
```

- [ ] **Step 3: Build**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew build`
Expected: PASS (full build, all modules)

- [ ] **Step 4: Manually verify in the running app**

Use the `run` skill to launch the app: navigate to the Dummy Player tab, select "Proxy Player" mode, pick a Knight, tap Start, tap Play Turn a few times to see an objective get drawn and continued, tap Explored/Completed to confirm the objective clears, tap End Round, and tap both new HelpButtons to confirm their dialog content reads correctly against `docs/rules/proxy-player.md`.

- [ ] **Step 5: Run the full test suite**

Run: `JAVA_HOME="/d/Guy_Teichman/Android/android-studio/jbr" ./gradlew test`
Expected: PASS (all modules)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/guyteichman/mageknightbuddy/ui/dummyplayer/DummyPlayerScreen.kt app/src/main/assets/field_help.json
git commit -m "Add ProxyPlayerAiScreen and movement-rules help content"
```
