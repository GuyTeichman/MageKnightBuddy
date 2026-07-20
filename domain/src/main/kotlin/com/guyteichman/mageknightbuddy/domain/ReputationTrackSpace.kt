package com.guyteichman.mageknightbuddy.domain

/**
 * One space on the Reputation track a Shield token can occupy (base rulebook's Fame-and-
 * Reputation board, p.2, p.7). The physical board prints exactly one number per space -
 * [modifier] - and that's the only value any rule ever means by "Reputation", whether it's a
 * win condition ("Reputation +2 or higher") or a scoring bonus ("Reputation modifier"). There is
 * no separate raw step-count printed anywhere on the board, and no rule ever asks a player to
 * count how many spaces they've moved - so this type doesn't invent one. `null` marks the single
 * "X" space at the very negative end, which has no modifier at all (see `ForTheCouncilScoring`
 * for how that space is scored instead); the positive end has no X space, it simply tops out at
 * [PLUS_5]. See `docs/rules/for-the-council.md`'s "Reputation vs. Reputation modifier" section
 * for the full track table this enum encodes.
 */
enum class ReputationTrackSpace(val modifier: Int?) {
    NEGATIVE_X(modifier = null),
    MINUS_5(modifier = -5),
    MINUS_3(modifier = -3),
    MINUS_2(modifier = -2),
    MINUS_1(modifier = -1),
    CENTER(modifier = 0),
    PLUS_1(modifier = 1),
    PLUS_2(modifier = 2),
    PLUS_3(modifier = 3),
    PLUS_5(modifier = 5),
    ;

    /** Whether this is the track's one "X" space (no modifier - see rulebook p.7). */
    val isXSpace: Boolean get() = modifier == null
}
