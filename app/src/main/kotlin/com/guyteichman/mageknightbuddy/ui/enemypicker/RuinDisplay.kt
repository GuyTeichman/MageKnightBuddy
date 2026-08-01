package com.guyteichman.mageknightbuddy.ui.enemypicker

import com.guyteichman.mageknightbuddy.domain.ManaColor
import com.guyteichman.mageknightbuddy.domain.RuinToken
import com.guyteichman.mageknightbuddy.domain.TokenPileId

/**
 * Player-facing text for a drawn [RuinToken], kept out of the Compose file so the wording is easy to
 * find and adjust. All of it derives from the flat [RuinToken] fields (ADR-0006: the picker never
 * scores anything - even the altar Fame here is derived for display, not tracked).
 */

/** Fame an altar grants, derived from its colour count: the 4-colour Lost Legion altar gives 10, the
 * single-colour base altars give 7 (see [RuinToken]'s size convention). */
internal fun RuinToken.altarFame(): Int = if (altarColors!!.size == 4) 10 else 7

/**
 * The altar's mana-payment prompt. A single-colour altar wants three of that colour; the four-colour
 * altar wants one of *each* listed colour (the size convention spelled out in [RuinToken]).
 */
internal fun RuinToken.altarPrompt(): String {
    val colors = altarColors!!
    return if (colors.size == 4) {
        "Pay 1 mana of each: " + colors.joinToString(", ") { it.displayName() }
    } else {
        "Pay 3 ${colors.single().displayName()} mana"
    }
}

/**
 * The Enemies-With-Treasure draw instruction, e.g. "Draw & fight: 1 Red, 1 Brown" or, when a pile
 * repeats, "Draw & fight: 2 Green". Piles are grouped by colour in first-appearance order so a
 * repeated pile reads as a single count rather than two separate lines.
 */
internal fun RuinToken.enemyDrawPrompt(): String {
    // groupingBy + eachCount collapses [GREEN, GREEN] into {GREEN: 2}; LinkedHashMap keeps the
    // colours in the order they first appear so the text order matches the token's printed order.
    val counts = enemyPiles!!.groupingBy { it }.eachCountTo(LinkedHashMap())
    return "Draw & fight: " + counts.entries.joinToString(", ") { (pile, n) -> "$n ${pile.colorName()}" }
}

/** Short display name of a mana colour, e.g. "Red". */
internal fun ManaColor.displayName(): String = when (this) {
    ManaColor.RED -> "Red"
    ManaColor.GREEN -> "Green"
    ManaColor.BLUE -> "Blue"
    ManaColor.WHITE -> "White"
    ManaColor.GOLD -> "Gold"
    ManaColor.BLACK -> "Black"
}

/** One-letter tag for a mana colour, for the cramped altar art fallback (R/G/B/W). */
internal fun ManaColor.shortLabel(): String = when (this) {
    ManaColor.RED -> "R"
    ManaColor.GREEN -> "G"
    ManaColor.BLUE -> "B"
    ManaColor.WHITE -> "W"
    ManaColor.GOLD -> "Gd"
    ManaColor.BLACK -> "K"
}

/** Just the colour word for a pile ("Red"), shorter than [displayName]'s "Red enemies" - used in the
 * Enemies-With-Treasure draw line where "enemies" is already implied. */
internal fun TokenPileId.colorName(): String = when (this) {
    TokenPileId.GREEN -> "Green"
    TokenPileId.GREY -> "Grey"
    TokenPileId.VIOLET -> "Violet"
    TokenPileId.BROWN -> "Brown"
    TokenPileId.RED -> "Red"
    TokenPileId.WHITE -> "White"
    TokenPileId.RUIN -> "Ruin"
    // Neither the possessed pile nor the faction reward piles ever appear in a ruin's
    // Enemies-With-Treasure draw line (ruins draw only from the enemy colour piles), but this when is
    // exhaustive, so they're only here to satisfy it.
    TokenPileId.POSSESSED -> "Possessed"
    TokenPileId.ELEMENTALIST_REWARDS -> "Elementalist"
    TokenPileId.DARK_CRUSADER_REWARDS -> "Dark Crusader"
    TokenPileId.APOCALYPSE_CULT_REWARDS -> "Apocalypse Cult"
    TokenPileId.COUNCIL_OF_VOID_REWARDS -> "Council of the Void"
}
