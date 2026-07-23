package com.guyteichman.mageknightbuddy.domain

/**
 * Whether [round] is a day round, given whether the session started at night. Mage Knight
 * alternates day/night every Round; most scenarios start at day (Round 1), but a session can be
 * set up to start at night instead (the setup screen's "Starts at night?" checkbox) - flipping
 * which parity (odd/even) counts as day. Shared by [DummyPlayerSession], [ProxyPlayerSession], and
 * [VolkareSession] so all three modes derive day/night the same way from just their own `round`
 * and `startsAtNight`, with no per-round state to keep in sync.
 *
 * Currently consumed by [ProxyPlayerSession] (docs/rules/proxy-player.md's "Movement points" -
 * a Gold mana die only counts by day); the other two sessions track `startsAtNight` too, for a
 * planned day/night visual indicator, even though no rule of theirs reads it yet.
 */
fun isDayRound(round: Int, startsAtNight: Boolean): Boolean {
    val isOddRound = round % 2 == 1
    return isOddRound != startsAtNight
}
