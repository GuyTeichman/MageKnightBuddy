package com.guyteichman.mageknightbuddy.domain

/**
 * Whether [round] is a day round, given whether the session started at night. Mage Knight
 * alternates day/night every Round; most scenarios start at day (Round 1), but a session can be
 * set up to start at night instead (the setup screen's "Starts at night?" checkbox) - flipping
 * which parity (odd/even) counts as day. Shared by [DummyPlayerSession], [ProxyPlayerSession], and
 * [VolkareSession] so all three modes derive day/night the same way from just their own `round`
 * and `startsAtNight`, with no per-round state to keep in sync.
 *
 * Consumed as a rule by [ProxyPlayerSession] only (docs/rules/proxy-player.md's "Movement points" -
 * a Gold mana die only counts by day); all three sessions additionally expose it as `isDay` for the
 * AI screens' round chip, which states day/night alongside the round number (issue #180).
 */
fun isDayRound(round: Int, startsAtNight: Boolean): Boolean {
    val isOddRound = round % 2 == 1
    return isOddRound != startsAtNight
}
