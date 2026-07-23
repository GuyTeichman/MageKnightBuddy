package com.guyteichman.mageknightbuddy.domain

/**
 * Which color(s) a card counts as. Every Dummy Player / Proxy Player deck card is either a
 * single fixed color, or one of the 4 Dual-Color Advanced Action cards (see
 * `docs/rules/proxy-player.md`) that count as two colors at once. Used wherever "does this
 * crystal color match this card" needs to work identically for both cases - see
 * [DummyPlayerSession.playTurn]'s crystal-chain check.
 */
sealed interface CardIdentity {
    /** True if [color] matches this card - for [DualColor], either of its two colors counts. */
    fun matches(color: CardColor): Boolean

    /** An ordinary card with one fixed color - every Basic Action and single-color Advanced Action. */
    data class SingleColor(val color: CardColor) : CardIdentity {
        override fun matches(color: CardColor): Boolean = this.color == color
    }

    /**
     * One of the 4 Dual-Color Advanced Action cards (Power of Crystals, Chilling Stare,
     * Explosive Bolt, Rush of Adrenaline - see `docs/rules/proxy-player.md`), which counts as
     * both [colorA] and [colorB] for crystal-chain matching.
     */
    data class DualColor(val colorA: CardColor, val colorB: CardColor) : CardIdentity {
        // `init` blocks run as part of construction, right after the primary constructor's
        // parameters are assigned - this makes colorA == colorB an impossible state for any
        // DualColor instance to exist in, rather than a bug callers could accidentally trigger.
        // Without this, matchingCrystalCount would silently double-count that one color's crystals.
        init {
            require(colorA != colorB) { "DualColor's two colors must be different, got $colorA twice" }
        }

        override fun matches(color: CardColor): Boolean = color == colorA || color == colorB
    }

    companion object {
        /**
         * The complete, closed set of 4 Dual-Color Advanced Action cards that exist for this game
         * (docs/rules/proxy-player.md's table) - a separate small-run product, not part of the
         * base Apocalypse Dragon expansion, so no more of these will ever be printed. Lets the End
         * Round dialog offer them as 4 directly-selectable options instead of a generic
         * "pick 2 colors" control, since there's no need to support combinations that don't exist.
         */
        val DUAL_COLOR_CARDS: List<DualColor> = listOf(
            DualColor(CardColor.GREEN, CardColor.BLUE), // Power of Crystals
            DualColor(CardColor.BLUE, CardColor.WHITE), // Chilling Stare
            DualColor(CardColor.RED, CardColor.WHITE), // Explosive Bolt
            DualColor(CardColor.RED, CardColor.GREEN), // Rush of Adrenaline
        )
    }
}

/**
 * [crystals] held for the color this card matches best - for a [CardIdentity.DualColor] card,
 * the *higher* of its two colors' crystal counts (not their sum - correcting an earlier, unstated
 * app-specific guess; see `docs/rules/proxy-player.md`'s "The Proxy Player's turn"). A dual-color
 * card still only represents one physical card being flipped, so it shouldn't count as if the
 * Proxy Player held crystals of both colors at once - it counts as whichever single color would
 * chain the most reveals.
 */
fun CardIdentity.matchingCrystalCount(crystals: Map<CardColor, Int>): Int = when (this) {
    is CardIdentity.SingleColor -> crystals.getValue(color)
    is CardIdentity.DualColor -> maxOf(crystals.getValue(colorA), crystals.getValue(colorB))
}

/**
 * The Proxy Player objective label for a card of this identity - see [CardColor.objectiveLabel].
 * A [CardIdentity.DualColor] card counts as both colors for movement targeting
 * (docs/rules/proxy-player.md's "Objective"), so in principle both colors' labels apply - except
 * when one of them is [CardColor.BLUE]: Blue's own rule already covers "whichever of the other
 * three applies, on a site further from the portal", making a joined label redundant - Blue's
 * label alone is shown instead of "Blue's label or the other color's label".
 */
val CardIdentity.objectiveLabel: String
    get() = when (this) {
        is CardIdentity.SingleColor -> color.objectiveLabel
        is CardIdentity.DualColor -> if (colorA == CardColor.BLUE || colorB == CardColor.BLUE) {
            CardColor.BLUE.objectiveLabel
        } else {
            listOf(colorA, colorB).sortedBy { it.ordinal }.joinToString(" or ") { it.objectiveLabel }
        }
    }
