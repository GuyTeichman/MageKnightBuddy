package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerSession
import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerSession
import com.guyteichman.mageknightbuddy.domain.RaceLevel
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareCard
import com.guyteichman.mageknightbuddy.domain.VolkareSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class RestoreGamePreviewTest {

    private fun repositories(): Triple<DummyPlayerSessionRepository, VolkareSessionRepository, ProxyPlayerSessionRepository> =
        Triple(
            DummyPlayerSessionRepository(FakeDummyPlayerSessionDao()),
            VolkareSessionRepository(FakeVolkareSessionDao()),
            ProxyPlayerSessionRepository(FakeProxyPlayerSessionDao()),
        )

    @Test
    fun `loadRestoreGamePreview returns null when nothing has been saved in any of the 3 repositories`() = runTest {
        val (repository, volkareRepository, proxyPlayerRepository) = repositories()

        val preview = loadRestoreGamePreview(repository, volkareRepository, proxyPlayerRepository)

        assertNull(preview)
    }

    @Test
    fun `loadRestoreGamePreview returns the Standard session's Knight, round, and turn when it's the only one saved`() = runTest {
        val (repository, volkareRepository, proxyPlayerRepository) = repositories()
        // Play 2 turns via the session's own playTurn(), per CLAUDE.md's guidance on building test
        // state through the class's own prior methods rather than a hand-picked shortcut.
        val session = DummyPlayerSession.start(
            Knight.ARYTHEA,
            deckOrder = List(6) { CardIdentity.SingleColor(CardColor.RED) },
        ).playTurn().playTurn()
        repository.save(session, updatedAt = 1000)

        val preview = loadRestoreGamePreview(repository, volkareRepository, proxyPlayerRepository)

        assertEquals(RestoreGamePreview(DummyPlayerMode.STANDARD, Knight.ARYTHEA, round = 1, turn = 2), preview)
    }

    @Test
    fun `loadRestoreGamePreview returns a null knight for Volkare, since Volkare has no Knight`() = runTest {
        val (repository, volkareRepository, proxyPlayerRepository) = repositories()
        val session = VolkareSession.start(
            Scenario.VolkaresReturn,
            RaceLevel.FAIR,
            woundCount = 0,
            deckOrder = List(3) { VolkareCard.BasicAction(CardColor.RED) },
        ).playTurn()
        volkareRepository.save(session, updatedAt = 1000)

        val preview = loadRestoreGamePreview(repository, volkareRepository, proxyPlayerRepository)

        assertEquals(RestoreGamePreview(DummyPlayerMode.VOLKARE, knight = null, round = 1, turn = 1), preview)
    }

    @Test
    fun `loadRestoreGamePreview returns the Proxy Player session's Knight, round, and turn`() = runTest {
        val (repository, volkareRepository, proxyPlayerRepository) = repositories()
        val session = ProxyPlayerSession.start(
            Knight.GOLDYX,
            deckOrder = List(4) { ProxyPlayerCard.BasicAction(CardColor.RED) },
        ).playTurn()
        proxyPlayerRepository.save(session, updatedAt = 1000)

        val preview = loadRestoreGamePreview(repository, volkareRepository, proxyPlayerRepository)

        assertEquals(RestoreGamePreview(DummyPlayerMode.PROXY_PLAYER, Knight.GOLDYX, round = 1, turn = 1), preview)
    }

    @Test
    fun `loadRestoreGamePreview picks whichever of the 3 saved sessions was updated most recently`() = runTest {
        val (repository, volkareRepository, proxyPlayerRepository) = repositories()
        repository.save(DummyPlayerSession.start(Knight.TOVAK), updatedAt = 1000)
        volkareRepository.save(VolkareSession.start(Scenario.VolkaresReturn, RaceLevel.FAIR), updatedAt = 3000)
        proxyPlayerRepository.save(ProxyPlayerSession.start(Knight.KRANG), updatedAt = 2000)

        val preview = loadRestoreGamePreview(repository, volkareRepository, proxyPlayerRepository)

        assertEquals(DummyPlayerMode.VOLKARE, preview?.mode)
    }
}
