package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.DummyPlayerEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins [DummyPlayerEvent.describe]'s wording, focused on [DummyPlayerEvent.TurnPlayed] - the branch
 * issue #271 reworked so a chained (additional) reveal is listed once (in the "+N revealed" clause)
 * instead of twice (there and again in the top "Revealed:" list). The top list now shows only the
 * mandatory initial 3, and the crystal-match clause reads "... crystal matched → +N revealed: ...".
 */
class DummyPlayerEventDescribeTest {

    private fun single(color: CardColor) = CardIdentity.SingleColor(color)

    @Test
    fun `TurnPlayed with a crystal match lists the additional card once, in the +N clause`() {
        val event = DummyPlayerEvent.TurnPlayed(
            round = 2,
            initialReveal = listOf(single(CardColor.RED), single(CardColor.BLUE), single(CardColor.GREEN)),
            additionalReveal = listOf(single(CardColor.BLUE)),
        )

        assertEquals(
            LogEntryText(
                icon = "▶",
                title = "Turn played",
                // A TurnPlayed row is a turn-start, so its meta names the turn (issue #270).
                meta = "Round 2 · Turn 3",
                description = listOf(
                    DescriptionSpan.Words("Revealed: "),
                    DescriptionSpan.ColorDot(CardColor.RED),
                    DescriptionSpan.Words(" "),
                    DescriptionSpan.ColorDot(CardColor.BLUE),
                    DescriptionSpan.Words(" "),
                    DescriptionSpan.ColorDot(CardColor.GREEN),
                    DescriptionSpan.Words(". "),
                    // The 3rd initial card (Green) is what matched; the crystal dot stands for the
                    // held crystal, not the card, so it's a CrystalDot.
                    DescriptionSpan.CrystalDot(CardColor.GREEN),
                    DescriptionSpan.Words(" Green crystal matched → +1 revealed: "),
                    DescriptionSpan.ColorDot(CardColor.BLUE),
                    DescriptionSpan.Words(" Blue"),
                    DescriptionSpan.Words("."),
                ),
            ),
            event.describe(turnInRound = 3),
        )
    }

    @Test
    fun `TurnPlayed with no match shows only the initial three and says the turn ended`() {
        val event = DummyPlayerEvent.TurnPlayed(
            round = 1,
            initialReveal = listOf(single(CardColor.RED), single(CardColor.BLUE), single(CardColor.GREEN)),
            additionalReveal = emptyList(),
        )

        assertEquals(
            LogEntryText(
                icon = "▶",
                title = "Turn played",
                meta = "Round 1 · Turn 1",
                description = listOf(
                    DescriptionSpan.Words("Revealed: "),
                    DescriptionSpan.ColorDot(CardColor.RED),
                    DescriptionSpan.Words(" "),
                    DescriptionSpan.ColorDot(CardColor.BLUE),
                    DescriptionSpan.Words(" "),
                    DescriptionSpan.ColorDot(CardColor.GREEN),
                    DescriptionSpan.Words(". "),
                    DescriptionSpan.Words("No crystal match, turn ended."),
                ),
            ),
            event.describe(turnInRound = 1),
        )
    }

    @Test
    fun `TurnPlayed with a dual-color match shows both crystals and every chained card once`() {
        val event = DummyPlayerEvent.TurnPlayed(
            round = 5,
            // 3rd initial card is the dual-color Power of Crystals (Green/Blue) that triggers the chain.
            initialReveal = listOf(
                single(CardColor.RED),
                single(CardColor.WHITE),
                CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE),
            ),
            additionalReveal = listOf(single(CardColor.GREEN), single(CardColor.BLUE)),
        )

        assertEquals(
            LogEntryText(
                icon = "▶",
                title = "Turn played",
                meta = "Round 5 · Turn 4",
                description = listOf(
                    DescriptionSpan.Words("Revealed: "),
                    DescriptionSpan.ColorDot(CardColor.RED),
                    DescriptionSpan.Words(" "),
                    DescriptionSpan.ColorDot(CardColor.WHITE),
                    DescriptionSpan.Words(" "),
                    // Dual-color card renders both dots joined by "/" (see addCardDots).
                    DescriptionSpan.ColorDot(CardColor.GREEN),
                    DescriptionSpan.Words("/"),
                    DescriptionSpan.ColorDot(CardColor.BLUE),
                    DescriptionSpan.Words(". "),
                    DescriptionSpan.CrystalDot(CardColor.GREEN),
                    DescriptionSpan.CrystalDot(CardColor.BLUE),
                    DescriptionSpan.Words(" Green/Blue crystal matched → +2 revealed: "),
                    DescriptionSpan.ColorDot(CardColor.GREEN),
                    DescriptionSpan.Words(" Green"),
                    DescriptionSpan.Words(", "),
                    DescriptionSpan.ColorDot(CardColor.BLUE),
                    DescriptionSpan.Words(" Blue"),
                    DescriptionSpan.Words("."),
                ),
            ),
            event.describe(turnInRound = 4),
        )
    }
}
