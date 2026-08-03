package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerCard
import com.guyteichman.mageknightbuddy.domain.ProxyPlayerEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins [ProxyPlayerEvent.describe]'s wording for the two reveal-heavy branches issue #271 trimmed to
 * match the Dummy Player style: [ProxyPlayerEvent.NewObjectiveDrawn] and
 * [ProxyPlayerEvent.TurnContinued]. Both now open with "Objective: …" and list the other flipped
 * cards as bare color dots under "Revealed: …" (no per-card text labels), the way the Dummy Player's
 * top "Revealed:" list does.
 */
class ProxyPlayerEventDescribeTest {

    @Test
    fun `NewObjectiveDrawn opens with Objective and lists the flipped cards as bare dots`() {
        val event = ProxyPlayerEvent.NewObjectiveDrawn(
            round = 1,
            objectiveCard = ProxyPlayerCard.BasicAction(CardColor.BLUE),
            discarded = listOf(ProxyPlayerCard.BasicAction(CardColor.RED), ProxyPlayerCard.BasicAction(CardColor.GREEN)),
        )

        assertEquals(
            LogEntryText(
                icon = "▶",
                title = "Objective drawn",
                meta = "Round 1",
                description = listOf(
                    DescriptionSpan.Words("Objective: "),
                    DescriptionSpan.ColorDot(CardColor.BLUE),
                    DescriptionSpan.Words(" Blue (Basic Action)."),
                    DescriptionSpan.Words(" Revealed: "),
                    DescriptionSpan.ColorDot(CardColor.RED),
                    DescriptionSpan.Words(" "),
                    DescriptionSpan.ColorDot(CardColor.GREEN),
                    DescriptionSpan.Words("."),
                ),
            ),
            event.describe(),
        )
    }

    @Test
    fun `NewObjectiveDrawn with nothing else flipped omits the Revealed clause`() {
        val event = ProxyPlayerEvent.NewObjectiveDrawn(
            round = 3,
            objectiveCard = ProxyPlayerCard.UniqueAction(CardColor.WHITE),
            discarded = emptyList(),
        )

        assertEquals(
            LogEntryText(
                icon = "▶",
                title = "Objective drawn",
                meta = "Round 3",
                description = listOf(
                    DescriptionSpan.Words("Objective: "),
                    DescriptionSpan.ColorDot(CardColor.WHITE),
                    DescriptionSpan.Words(" White (Unique)."),
                ),
            ),
            event.describe(),
        )
    }

    @Test
    fun `TurnContinued keeps the shield count inline and lists revealed cards as bare dots`() {
        val event = ProxyPlayerEvent.TurnContinued(
            round = 2,
            objectiveCard = ProxyPlayerCard.BasicAction(CardColor.RED),
            shieldsNow = 3,
            revealed = listOf(ProxyPlayerCard.BasicAction(CardColor.BLUE), ProxyPlayerCard.BasicAction(CardColor.WHITE)),
        )

        assertEquals(
            LogEntryText(
                icon = "▶",
                title = "Turn continued",
                meta = "Round 2",
                description = listOf(
                    DescriptionSpan.Words("Objective: "),
                    DescriptionSpan.ColorDot(CardColor.RED),
                    DescriptionSpan.Words(" Red (Basic Action), now 3 Shield(s)."),
                    DescriptionSpan.Words(" Revealed: "),
                    DescriptionSpan.ColorDot(CardColor.BLUE),
                    DescriptionSpan.Words(" "),
                    DescriptionSpan.ColorDot(CardColor.WHITE),
                    DescriptionSpan.Words("."),
                ),
            ),
            event.describe(),
        )
    }

    @Test
    fun `TurnContinued with a dual-color objective and no reveals still states the shield count`() {
        val event = ProxyPlayerEvent.TurnContinued(
            round = 4,
            objectiveCard = ProxyPlayerCard.AdvancedAction(CardIdentity.DualColor(CardColor.GREEN, CardColor.BLUE)),
            shieldsNow = 1,
            revealed = emptyList(),
        )

        assertEquals(
            LogEntryText(
                icon = "▶",
                title = "Turn continued",
                meta = "Round 4",
                description = listOf(
                    DescriptionSpan.Words("Objective: "),
                    DescriptionSpan.ColorDot(CardColor.GREEN),
                    DescriptionSpan.Words("/"),
                    DescriptionSpan.ColorDot(CardColor.BLUE),
                    DescriptionSpan.Words(" Green/Blue (Advanced Action), now 1 Shield(s)."),
                ),
            ),
            event.describe(),
        )
    }
}
