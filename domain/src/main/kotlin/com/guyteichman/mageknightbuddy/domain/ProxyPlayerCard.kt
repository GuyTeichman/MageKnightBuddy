package com.guyteichman.mageknightbuddy.domain

/**
 * One card in a Proxy Player's deck (docs/rules/proxy-player.md's "The Proxy Player's turn" and
 * "Unique Basic Action cards"/"Dual-color Advanced Action cards"). Distinguishes 3 kinds because
 * the movement-point formula treats them differently: a generic [BasicAction] gives a smaller
 * bonus than a Hero's own [UniqueAction] card or any [AdvancedAction] - see [movementBonus].
 */
sealed interface ProxyPlayerCard {
    /** The card's contribution to the Proxy Player's movement-point total - see docs/rules/proxy-player.md's "Movement points". */
    val movementBonus: Int

    /** True if [color] matches this card - see each variant's own rule. */
    fun matches(color: CardColor): Boolean

    /** One of the 12 generic cards left in a Knight's 16-card starting deck after its 2 [UniqueAction]s replace 2 others. */
    data class BasicAction(val color: CardColor) : ProxyPlayerCard {
        override val movementBonus: Int get() = 1
        override fun matches(color: CardColor): Boolean = this.color == color
    }

    /**
     * One of a Knight's 2 portrait cards that replace 2 generic cards in their starting deck
     * (docs/rules/proxy-player.md's per-Knight table). Counts as an Advanced Action for the
     * movement bonus (+2) despite being a Basic Action.
     */
    data class UniqueAction(val color: CardColor) : ProxyPlayerCard {
        override val movementBonus: Int get() = 2
        override fun matches(color: CardColor): Boolean = this.color == color
    }

    /**
     * An Advanced Action added to the deck at round end (docs/rules/proxy-player.md's "When
     * preparing a new Round") - single- or dual-color, see [CardIdentity]. This is the only path
     * a [CardIdentity.DualColor] card can ever enter a Proxy Player's deck.
     */
    data class AdvancedAction(val identity: CardIdentity) : ProxyPlayerCard {
        override val movementBonus: Int get() = 2
        override fun matches(color: CardColor): Boolean = identity.matches(color)
    }
}

/**
 * Sum of [crystals] held for every color this card matches - see [CardIdentity.matchingCrystalCount],
 * which this delegates to for [ProxyPlayerCard.AdvancedAction]. Used by [ProxyPlayerSession]'s
 * deck-flip crystal-chain check, mirroring [DummyPlayerSession.playTurn]'s equivalent.
 */
fun ProxyPlayerCard.matchingCrystalCount(crystals: Map<CardColor, Int>): Int = when (this) {
    is ProxyPlayerCard.BasicAction -> crystals.getValue(color)
    is ProxyPlayerCard.UniqueAction -> crystals.getValue(color)
    is ProxyPlayerCard.AdvancedAction -> identity.matchingCrystalCount(crystals)
}
