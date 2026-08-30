package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent
import com.guyteichman.mageknightbuddy.domain.VolkareEvent

/*
 * Per-round turn numbering for the AI-tab event logs (issue #270). A *turn* starts when the AI
 * draws from its deck; the draw entry gets a number, and everything else in the log (round
 * boundaries, setup, mid-turn follow-ups) stays unnumbered. Each mode's function returns a list the
 * same length as its log, where each turn-start entry holds its 1-based turn index *within its own
 * round* and every other entry is null - the UI then renders "Round N · Turn M" only where the value
 * is non-null (see roundTurnMeta). One function per mode (dummyTurnNumbers/proxyTurnNumbers/
 * volkareTurnNumbers) rather than one overloaded name, since the three `List<...Event>` types erase
 * to the same JVM signature.
 *
 * The set of events treated as a turn-start deliberately mirrors each session's own `turnInRound`
 * property, so a log entry's turn number always agrees with the header chip's "Turn M".
 */

/**
 * The shared counting engine behind every mode's turn-numbering function. Walks [log] once, keeping a
 * running count of turn-starts *per round* ([roundOf] reads each entry's own round), and assigns
 * each turn-start ([isTurnStart]) the next number in its round. Non-turn entries map to null.
 *
 * Filtering by each entry's own round (rather than resetting at some boundary event) matches how
 * `DummyPlayerSession.turnInRound` counts, and stays correct even though `endRound()` never logs a
 * fresh `RoundStarted` for the round it advances into.
 */
private fun <E> logTurnNumbers(log: List<E>, roundOf: (E) -> Int, isTurnStart: (E) -> Boolean): List<Int?> {
    // Running per-round tally: round number -> how many turn-starts seen in it so far.
    val countByRound = HashMap<Int, Int>()
    return log.map { event ->
        if (isTurnStart(event)) {
            val round = roundOf(event)
            // getOrDefault + 1: this entry is the next turn in its round.
            val next = countByRound.getOrDefault(round, 0) + 1
            countByRound[round] = next
            next
        } else {
            null
        }
    }
}

/**
 * The meta line for one log row: "Round N" normally, "Round N · Turn M" when the row is a
 * turn-start. [turnInRound] is this entry's value from the mode's turn-numbering function above
 * (non-null only on a turn-start), so this formatter stays dumb - the classification lives in one
 * place. The "·" separator matches the header chip's "Round N · ☀ DAY · Turn M" (issue #270).
 */
internal fun roundTurnMeta(round: Int, turnInRound: Int?): String =
    if (turnInRound != null) "Round $round · Turn $turnInRound" else "Round $round"

/** Each [DummyPlayerEvent]'s own round - a plain `when` since the sealed interface has no shared `round` property. */
private fun DummyPlayerEvent.round(): Int = when (this) {
    is DummyPlayerEvent.RoundStarted -> round
    is DummyPlayerEvent.TurnPlayed -> round
    is DummyPlayerEvent.EndOfRoundAnnounced -> round
    is DummyPlayerEvent.RoundEnded -> round
    is DummyPlayerEvent.TacticPicked -> round
}

/**
 * Turn numbers for a Standard Dummy Player log. Only [DummyPlayerEvent.TurnPlayed] is a turn-start,
 * matching `DummyPlayerSession.turnInRound` (which counts exactly those).
 */
internal fun dummyTurnNumbers(log: List<DummyPlayerEvent>): List<Int?> =
    logTurnNumbers(log, roundOf = { it.round() }, isTurnStart = { it is DummyPlayerEvent.TurnPlayed })

/** Each [ProxyPlayerEvent]'s own round - a plain `when` since the sealed interface has no shared `round` property. */
private fun ProxyPlayerEvent.round(): Int = when (this) {
    is ProxyPlayerEvent.RoundStarted -> round
    is ProxyPlayerEvent.NewObjectiveDrawn -> round
    is ProxyPlayerEvent.TurnContinued -> round
    is ProxyPlayerEvent.EndOfRoundAnnounced -> round
    is ProxyPlayerEvent.ObjectiveResolved -> round
    is ProxyPlayerEvent.RoundEnded -> round
    is ProxyPlayerEvent.TacticPicked -> round
}

/**
 * Turn numbers for a Proxy Player log. A turn-start is either draw branch of `playTurn` -
 * [ProxyPlayerEvent.NewObjectiveDrawn] or [ProxyPlayerEvent.TurnContinued] - matching
 * `ProxyPlayerSession.turnInRound` (which counts exactly those two). [ProxyPlayerEvent.ObjectiveResolved]
 * happens mid-turn, not on a draw, so it stays unnumbered.
 */
internal fun proxyTurnNumbers(log: List<ProxyPlayerEvent>): List<Int?> =
    logTurnNumbers(
        log,
        roundOf = { it.round() },
        isTurnStart = { it is ProxyPlayerEvent.NewObjectiveDrawn || it is ProxyPlayerEvent.TurnContinued },
    )

/** Each [VolkareEvent]'s own round - a plain `when` since the sealed interface has no shared `round` property. */
private fun VolkareEvent.round(): Int = when (this) {
    is VolkareEvent.RoundStarted -> round
    is VolkareEvent.CardRevealed -> round
    is VolkareEvent.Frenzy -> round
    is VolkareEvent.RoundEnded -> round
    is VolkareEvent.QuestLost -> round
    is VolkareEvent.TacticPicked -> round
}

/**
 * Turn numbers for a Volkare log. A turn-start is a [VolkareEvent.CardRevealed] (a normal reveal) or
 * a [VolkareEvent.Frenzy] (Volkare's Return's repeatable empty-deck turn), matching
 * `VolkareSession.turnInRound`. [VolkareEvent.QuestLost] is deliberately excluded: it's a marker
 * logged alongside the [VolkareEvent.CardRevealed] for the *same* turn, so counting it would
 * double-count that turn.
 */
internal fun volkareTurnNumbers(log: List<VolkareEvent>): List<Int?> =
    logTurnNumbers(
        log,
        roundOf = { it.round() },
        isTurnStart = { it is VolkareEvent.CardRevealed || it is VolkareEvent.Frenzy },
    )
