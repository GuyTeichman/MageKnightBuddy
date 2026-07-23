package com.guyteichman.mageknightbuddy.domain

/**
 * The four colors that Mage Knight action cards come in. Used to categorize
 * cards elsewhere in the domain (e.g. which basic/advanced deck a card belongs to).
 */
enum class CardColor { RED, GREEN, BLUE, WHITE }

/**
 * What a Proxy Player objective card of this color actually tells the player to do -
 * `docs/rules/proxy-player.md`'s "Objective (movement target, by objective card color)", verbatim
 * where the rule names one specific action. [CardColor.BLUE] doesn't have its own action - its
 * rule just says "whichever of the other three applies, on a site further from the portal" - so
 * its label states that condition directly instead of guessing which action it'll turn out to be
 * (this app doesn't track board state, so it never actually knows).
 */
val CardColor.objectiveLabel: String
    get() = when (this) {
        CardColor.GREEN -> "Conquer Adventure Site"
        CardColor.RED -> "Conquer Fortified Site"
        CardColor.WHITE -> "Interact"
        CardColor.BLUE -> "Advance (closest site further from portal)"
    }
