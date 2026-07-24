package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.domain.Knight

/**
 * What "Restore Game" (`DummyPlayerSetupScreen`) would resume right now, so the setup screen can
 * show the player what they're about to resume before they tap the button - issue #125's 2nd item
 * (the 1st item added [round]/[turn] themselves - see `DummyPlayerSession.turnInRound`'s doc
 * comment). [knight] is `null` for [DummyPlayerMode.VOLKARE] - Volkare has no Knight (see
 * CONTEXT.md's "Volkare" entry). Keeping the actual [Knight] enum (rather than a pre-formatted
 * display string) lets the UI render its shield icon, not just its name.
 */
internal data class RestoreGamePreview(
    val mode: DummyPlayerMode,
    val knight: Knight?,
    val round: Int,
    val turn: Int,
)

/**
 * Loads the [RestoreGamePreview] for whichever of the 3 saved Dummy Player tab sessions was most
 * recently updated - the same `repository.updatedAt()` 3-way comparison
 * `DummyPlayerSetupScreen`'s own "Restore Game" button uses to decide which AI screen to navigate
 * to, but this also loads that one session's actual state (via [DummyPlayerSessionRepository.restore]/
 * [VolkareSessionRepository.restore]/[ProxyPlayerSessionRepository.restore]) rather than only its
 * timestamp, since a preview needs real content to show, not just "when". Returns `null` if none
 * of the 3 repositories has ever saved a session (mirrors "Restore Game"'s own disabled state).
 */
internal suspend fun loadRestoreGamePreview(
    repository: DummyPlayerSessionRepository,
    volkareRepository: VolkareSessionRepository,
    proxyPlayerRepository: ProxyPlayerSessionRepository,
): RestoreGamePreview? {
    val timestamps = listOf(
        DummyPlayerMode.STANDARD to (repository.updatedAt() ?: -1),
        DummyPlayerMode.VOLKARE to (volkareRepository.updatedAt() ?: -1),
        DummyPlayerMode.PROXY_PLAYER to (proxyPlayerRepository.updatedAt() ?: -1),
    )
    // maxByOrNull picks the first entry with the highest timestamp, ties broken by list order -
    // the same tie-break DummyPlayerSetupScreen's own comparison already relies on implicitly.
    val mostRecent = timestamps.maxByOrNull { it.second } ?: return null
    if (mostRecent.second < 0) return null // No repository has ever saved a session yet.

    // `when` dispatches on which mode won the comparison above, loading that mode's own full
    // session via its own repository's restore() - each branch reads a different domain type's
    // Knight/round/turnInRound, so this can't be collapsed into one shared code path.
    return when (mostRecent.first) {
        DummyPlayerMode.STANDARD -> repository.restore()?.let { session ->
            RestoreGamePreview(DummyPlayerMode.STANDARD, session.knight, session.round, session.turnInRound)
        }
        DummyPlayerMode.VOLKARE -> volkareRepository.restore()?.let { session ->
            RestoreGamePreview(DummyPlayerMode.VOLKARE, knight = null, session.round, session.turnInRound)
        }
        DummyPlayerMode.PROXY_PLAYER -> proxyPlayerRepository.restore()?.let { session ->
            RestoreGamePreview(DummyPlayerMode.PROXY_PLAYER, session.knight, session.round, session.turnInRound)
        }
    }
}
