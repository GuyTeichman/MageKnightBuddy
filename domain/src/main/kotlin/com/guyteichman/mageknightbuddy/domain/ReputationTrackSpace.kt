package com.guyteichman.mageknightbuddy.domain

/**
 * One space on the Reputation track a Shield token can occupy (base rulebook's Fame-and-
 * Reputation board, p.2). [position] is how many steps the space sits from the track's center
 * ("0 Reputation") - negative toward the penalty end, positive toward the bonus end - and is
 * what the rulebook means by e.g. "+2 Reputation" (p.7): a *raw* count of steps, not a value
 * printed on the board. [modifier] is the different, usually smaller value actually printed at
 * that space, added to Influence/scoring - `null` for the two end spaces marked "X" instead of a
 * number, which have no modifier at all (see `ForTheCouncilScoring` for how those two are scored
 * instead). See `docs/rules/for-the-council.md`'s "Reputation vs. Reputation modifier" section
 * for the full track table this enum encodes.
 */
enum class ReputationTrackSpace(val position: Int, val modifier: Int?) {
    NEGATIVE_X(position = -6, modifier = null),
    MINUS_5(position = -5, modifier = -5),
    MINUS_4(position = -4, modifier = -3),
    MINUS_3(position = -3, modifier = -2),
    MINUS_2(position = -2, modifier = -1),
    MINUS_1(position = -1, modifier = -1),
    CENTER(position = 0, modifier = 0),
    PLUS_1(position = 1, modifier = 1),
    PLUS_2(position = 2, modifier = 1),
    PLUS_3(position = 3, modifier = 2),
    PLUS_4(position = 4, modifier = 3),
    PLUS_5(position = 5, modifier = 5),
    POSITIVE_X(position = 6, modifier = null),
    ;

    /** Whether this is one of the two "X" end spaces (no modifier - see rulebook p.7). */
    val isXSpace: Boolean get() = modifier == null

    companion object {
        /** Looks up a [ReputationTrackSpace] by its [position] (e.g. when reading back from persistence). */
        fun fromPosition(position: Int): ReputationTrackSpace = entries.first { it.position == position }
    }
}
