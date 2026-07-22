package com.guyteichman.mageknightbuddy.domain

/**
 * The six faces of Volkare's mana die (docs/rules/volkares-return.md / volkares-quest.md's Wound
 * card rules): rolled whenever a Wound is revealed from his deck, to pick which Unit-offer slot
 * color is affected. Distinct from [CardColor] - the die has two extra faces, Gold and Black, that
 * no card color carries.
 */
enum class ManaColor { RED, GREEN, BLUE, WHITE, GOLD, BLACK }
