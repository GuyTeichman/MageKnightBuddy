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
}

/**
 * Sum of [crystals] held for every color this card matches - a [CardIdentity.DualColor] card
 * sums both of its colors' crystal counts, per this app's dual-color deck-flip ruling (not
 * stated in the rulebook - see `docs/rules/proxy-player.md`'s "The Proxy Player's turn").
 */
fun CardIdentity.matchingCrystalCount(crystals: Map<CardColor, Int>): Int = when (this) {
    is CardIdentity.SingleColor -> crystals.getValue(color)
    is CardIdentity.DualColor -> crystals.getValue(colorA) + crystals.getValue(colorB)
}
