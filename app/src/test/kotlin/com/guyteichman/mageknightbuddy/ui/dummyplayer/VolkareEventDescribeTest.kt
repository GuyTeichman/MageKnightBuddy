package com.guyteichman.mageknightbuddy.ui.dummyplayer

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.ManaColor
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.VolkareCard
import com.guyteichman.mageknightbuddy.domain.VolkareEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [VolkareEvent.describe]'s full branch matrix (Return vs. Quest, Action vs. Spell, Wound
 * vs. non-Wound, City Revealed on/off, red vs. every other color) against the exact rulebook text
 * in `docs/rules/volkares-return.md`/`volkares-quest.md`'s "Course of the Game" sections - this is
 * the app's most branch-heavy hand-written prose logic, so it's worth pinning down directly rather
 * than only exercising it indirectly through the emulator.
 */
class VolkareEventDescribeTest {

    @Test
    fun `RoundStarted describes the deck as drawn once, never reshuffled`() {
        val text = VolkareEvent.RoundStarted(round = 3).describe(Scenario.VolkaresReturn)

        assertEquals("◆", text.icon)
        assertEquals("Round 3", text.meta)
        assertEquals(
            listOf(VolkareDescriptionSpan.Words("Volkare's deck is set - drawn once, never reshuffled.")),
            text.description,
        )
    }

    @Test
    fun `Wound in Volkares Return says he recruits the rolled mana color's unit`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.Wound, cityRevealed = false, manaRoll = ManaColor.GOLD)

        val text = event.describe(Scenario.VolkaresReturn)

        assertEquals("✚", text.icon)
        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Volkare rests and recruits the "),
                VolkareDescriptionSpan.ManaDot(ManaColor.GOLD),
                VolkareDescriptionSpan.Words(" Gold unit."),
            ),
            text.description,
        )
    }

    @Test
    fun `Wound in Volkares Quest says he scares off the rolled mana color's unit, not recruits`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.Wound, cityRevealed = false, manaRoll = ManaColor.BLACK)

        val text = event.describe(Scenario.VolkaresQuest)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Volkare rests and scares off the "),
                VolkareDescriptionSpan.ManaDot(ManaColor.BLACK),
                VolkareDescriptionSpan.Words(" Black unit."),
            ),
            text.description,
        )
    }

    @Test
    fun `non-red Basic Action in Volkares Return, city not revealed, moves once and rerolls`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.GREEN), cityRevealed = false)

        val text = event.describe(Scenario.VolkaresReturn)

        assertEquals("Action revealed", text.title)
        assertEquals("▶", text.icon)
        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.GREEN),
                VolkareDescriptionSpan.Words(" Green - "),
                VolkareDescriptionSpan.Words("Volkare moves one space in this direction and rerolls a matching Source die (gold, if none)."),
            ),
            text.description,
        )
    }

    @Test
    fun `non-red Competitive Spell in Volkares Return, city not revealed, moves twice`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.CompetitiveSpell(CardColor.BLUE), cityRevealed = false)

        val text = event.describe(Scenario.VolkaresReturn)

        assertEquals("Spell revealed", text.title)
        assertEquals("✦", text.icon)
        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.BLUE),
                VolkareDescriptionSpan.Words(" Blue - "),
                VolkareDescriptionSpan.Words("Volkare moves twice in this direction (one Source die reroll)."),
            ),
            text.description,
        )
    }

    @Test
    fun `red Basic Action in Volkares Return, city not revealed, attacks the nearest Mage Knight only`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.RED), cityRevealed = false)

        val text = event.describe(Scenario.VolkaresReturn)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.RED),
                VolkareDescriptionSpan.Words(" Red - "),
                VolkareDescriptionSpan.Words("Volkare doesn't move - he attacks the highest-Fame adjacent Mage Knight."),
            ),
            text.description,
        )
    }

    @Test
    fun `red Competitive Spell in Volkares Return, city not revealed, gets the two-spaces-away nuance`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.CompetitiveSpell(CardColor.RED), cityRevealed = false)

        val text = event.describe(Scenario.VolkaresReturn)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.RED),
                VolkareDescriptionSpan.Words(" Red - "),
                VolkareDescriptionSpan.Words(
                    "Volkare doesn't move - he attacks the highest-Fame Mage Knight within two spaces, then returns.",
                ),
            ),
            text.description,
        )
    }

    @Test
    fun `red Basic Action in Volkares Quest attacks too, but never gets the two-spaces-away nuance`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.RED), cityRevealed = false)

        val text = event.describe(Scenario.VolkaresQuest)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.RED),
                VolkareDescriptionSpan.Words(" Red - "),
                VolkareDescriptionSpan.Words("Volkare doesn't move - he attacks the highest-Fame adjacent Mage Knight."),
            ),
            text.description,
        )
    }

    @Test
    fun `red Competitive Spell in Volkares Quest also lacks the Return-only two-spaces-away nuance`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.CompetitiveSpell(CardColor.RED), cityRevealed = false)

        val text = event.describe(Scenario.VolkaresQuest)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.RED),
                VolkareDescriptionSpan.Words(" Red - "),
                VolkareDescriptionSpan.Words("Volkare doesn't move - he attacks the highest-Fame adjacent Mage Knight."),
            ),
            text.description,
        )
    }

    @Test
    fun `non-red Basic Action in Volkares Quest moves toward the portal, with the adjacency exception noted`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.WHITE), cityRevealed = false)

        val text = event.describe(Scenario.VolkaresQuest)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.WHITE),
                VolkareDescriptionSpan.Words(" White - "),
                VolkareDescriptionSpan.Words(
                    "Volkare moves one space toward the portal in this direction (or closer, if that leaves the map) - " +
                        "enters the portal instead if already adjacent.",
                ),
            ),
            text.description,
        )
    }

    @Test
    fun `non-red Competitive Spell in Volkares Quest moves twice toward the portal`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.CompetitiveSpell(CardColor.GREEN), cityRevealed = false)

        val text = event.describe(Scenario.VolkaresQuest)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.GREEN),
                VolkareDescriptionSpan.Words(" Green - "),
                VolkareDescriptionSpan.Words(
                    "Volkare moves twice toward the portal (one Source die reroll) - enters the portal instead if already adjacent.",
                ),
            ),
            text.description,
        )
    }

    @Test
    fun `once City Revealed, a non-red Basic Action in Volkares Return advances toward the city instead of exploring`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.BLUE), cityRevealed = true)

        val text = event.describe(Scenario.VolkaresReturn)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.BLUE),
                VolkareDescriptionSpan.Words(" Blue - "),
                VolkareDescriptionSpan.Words(
                    "Volkare advances one space toward the city and rerolls a matching Source die. " +
                        "If this brings him into the city, resolve the Battle for the City.",
                ),
            ),
            text.description,
        )
    }

    @Test
    fun `once City Revealed, red also advances toward the city in Volkares Return - it no longer triggers an attack`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.RED), cityRevealed = true)

        val text = event.describe(Scenario.VolkaresReturn)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.RED),
                VolkareDescriptionSpan.Words(" Red - "),
                VolkareDescriptionSpan.Words(
                    "Volkare advances one space toward the city and rerolls a matching Source die. " +
                        "If this brings him into the city, resolve the Battle for the City.",
                ),
            ),
            text.description,
        )
    }

    @Test
    fun `City Revealed on a Competitive Spell in Volkares Return advances him twice toward the city`() {
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.CompetitiveSpell(CardColor.WHITE), cityRevealed = true)

        val text = event.describe(Scenario.VolkaresReturn)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.WHITE),
                VolkareDescriptionSpan.Words(" White - "),
                VolkareDescriptionSpan.Words(
                    "Volkare advances twice toward the city (one Source die reroll). " +
                        "If this brings him into the city, resolve the Battle for the City.",
                ),
            ),
            text.description,
        )
    }

    @Test
    fun `Volkares Quest ignores cityRevealed entirely - a card with it set true still reads as Exploring-phase Quest text`() {
        // cityRevealed is always false in practice for Quest sessions (see CONTEXT.md's "City
        // Revealed" entry), but describe() itself should still key off scenario, not the flag, for
        // Quest cards - this pins that down even if the flag were ever true by mistake.
        val event = VolkareEvent.CardRevealed(round = 1, card = VolkareCard.BasicAction(CardColor.GREEN), cityRevealed = true)

        val text = event.describe(Scenario.VolkaresQuest)

        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words("Revealed "),
                VolkareDescriptionSpan.ColorDot(CardColor.GREEN),
                VolkareDescriptionSpan.Words(" Green - "),
                VolkareDescriptionSpan.Words(
                    "Volkare moves one space toward the portal in this direction (or closer, if that leaves the map) - " +
                        "enters the portal instead if already adjacent.",
                ),
            ),
            text.description,
        )
    }

    @Test
    fun `Frenzy describes a double move-attack as blue, with no Source die reroll`() {
        val text = VolkareEvent.Frenzy(round = 4).describe(Scenario.VolkaresReturn)

        assertEquals("⚡", text.icon)
        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words(
                    "Volkare's deck is empty. He moves/attacks twice as if a blue Spell were revealed, with no Source die reroll.",
                ),
            ),
            text.description,
        )
    }

    @Test
    fun `RoundEnded describes itself as a tracking convenience only`() {
        val text = VolkareEvent.RoundEnded(round = 2).describe(Scenario.VolkaresReturn)

        assertEquals("⚑", text.icon)
        assertEquals(
            listOf(VolkareDescriptionSpan.Words("Tracking convenience only - nothing changes for Volkare.")),
            text.description,
        )
    }

    @Test
    fun `QuestLost describes the last movement card as his final move into the portal`() {
        val text = VolkareEvent.QuestLost(round = 5).describe(Scenario.VolkaresQuest)

        assertEquals("☠", text.icon)
        assertEquals("Volkare reached the portal", text.title)
        assertEquals(
            listOf(
                VolkareDescriptionSpan.Words(
                    "That was the last card that could still move him toward the portal - his final move takes him into it. " +
                        "You lost this scenario.",
                ),
            ),
            text.description,
        )
    }
}
