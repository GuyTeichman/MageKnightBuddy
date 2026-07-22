package com.guyteichman.mageknightbuddy.domain

/**
 * One card in Volkare's deck (docs/rules/volkares-return.md / volkares-quest.md's "Setup"):
 * assembled from an unused Hero's 16 Basic Action cards, the 4 removed competitive Spells (one
 * per [CardColor]), and Wounds, all shuffled together. A sealed interface rather than reusing
 * [CardColor] directly, since Wounds don't carry a color of their own - only the two real card
 * kinds do.
 */
sealed interface VolkareCard {
    /** One of the unused Hero's 16 Basic Action cards (4 per [CardColor]). */
    data class BasicAction(val color: CardColor) : VolkareCard

    /** One of the 4 competitive Spells removed from the player's offer for this scenario (one per [CardColor]). */
    data class CompetitiveSpell(val color: CardColor) : VolkareCard

    /**
     * A Wound card. `data object` since every Wound is identical - unlike [BasicAction] and
     * [CompetitiveSpell], Wounds have no color to distinguish one from another.
     */
    data object Wound : VolkareCard
}
