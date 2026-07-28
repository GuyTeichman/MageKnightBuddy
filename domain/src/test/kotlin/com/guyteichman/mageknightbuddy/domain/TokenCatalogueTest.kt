package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The mandatory catalogue-validation test ADR-0007 requires: it loads and checks the *entire*
 * shipped `enemy-tokens.json` on every `make test`, converting what would otherwise be a runtime
 * parse/typo failure on the phone into a CI failure. It also spot-checks a few tokens against
 * values reasoned out by hand from `docs/rules/enemy-tokens.md` (not read off the JSON), so a
 * wrong transcription is caught, not just a malformed file.
 */
class TokenCatalogueTest {

    // Expected physical token count per pile (copies summed). Grows as each pile is transcribed;
    // green/grey/violet/brown/red/white are the *base game's* six enemy piles (see the rules doc).
    // None of these totals include the Lost Legion expansion's own additions to each pile (issue
    // #188), which will raise these numbers when they're added.
    private val expectedPileCounts = mapOf(
        TokenPileId.GREEN to 12,
        TokenPileId.GREY to 10,
        TokenPileId.VIOLET to 10,
        TokenPileId.BROWN to 10,
        TokenPileId.RED to 8,
        TokenPileId.WHITE to 10,
    )

    @Test
    fun `the shipped catalogue parses and is non-empty`() {
        assertTrue(TokenCatalogue.tokens.isNotEmpty(), "enemy-tokens.json parsed to an empty list")
    }

    @Test
    fun `every token id is unique`() {
        val ids = TokenCatalogue.tokens.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate token id(s): ${ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}")
    }

    @Test
    fun `every token has at least one copy and non-negative stats`() {
        TokenCatalogue.tokens.forEach { token ->
            assertTrue(token.copies >= 1, "${token.id} has copies < 1")
            assertTrue(token.armor >= 0, "${token.id} has negative armor")
            assertTrue(token.fame >= 0, "${token.id} has negative fame")
            token.attacks.forEach { attack ->
                attack.value?.let { assertTrue(it >= 0, "${token.id} has a negative attack value") }
            }
        }
    }

    @Test
    fun `every attack is either a numeric attack or a summon, never both or neither`() {
        TokenCatalogue.tokens.forEach { token ->
            token.attacks.forEach { attack ->
                assertTrue(
                    (attack.value != null) != (attack.summons != null),
                    "${token.id} has an attack that is neither purely numeric nor purely a summon: $attack",
                )
            }
        }
    }

    @Test
    fun `each pile's total token count matches the physical component count`() {
        expectedPileCounts.forEach { (pile, expected) ->
            val actual = TokenCatalogue.tokens.filter { it.pile == pile }.sumOf { it.copies }
            assertEquals(expected, actual, "pile $pile has $actual tokens, expected $expected")
        }
        // No token belongs to a pile we haven't declared an expected count for yet.
        val pilesPresent = TokenCatalogue.tokens.map { it.pile }.toSet()
        assertEquals(expectedPileCounts.keys, pilesPresent, "catalogue has tokens in undeclared pile(s)")
    }

    @Test
    fun `green pile has the six Marauding Orc types`() {
        val greenNames = TokenCatalogue.tokens.filter { it.pile == TokenPileId.GREEN }.map { it.name }.toSet()
        assertEquals(
            setOf("Prowlers", "Diggers", "Cursed Hags", "Wolf Riders", "Ironclads", "Orc Summoners"),
            greenNames,
        )
    }

    @Test
    fun `grey pile has the four Keep Guardian types`() {
        val greyNames = TokenCatalogue.tokens.filter { it.pile == TokenPileId.GREY }.map { it.name }.toSet()
        assertEquals(setOf("Crossbowmen", "Guardsmen", "Swordsmen", "Golems"), greyNames)
    }

    @Test
    fun `violet pile has the six Mage Tower Guardian types`() {
        val violetNames = TokenCatalogue.tokens.filter { it.pile == TokenPileId.VIOLET }.map { it.name }.toSet()
        assertEquals(
            setOf("Monks", "Illusionists", "Ice Mages", "Ice Golems", "Fire Mages", "Fire Golems"),
            violetNames,
        )
    }

    @Test
    fun `red pile has the four Draconum types`() {
        val redNames = TokenCatalogue.tokens.filter { it.pile == TokenPileId.RED }.map { it.name }.toSet()
        assertEquals(setOf("Swamp Dragon", "Fire Dragon", "Ice Dragon", "High Dragon"), redNames)
    }

    @Test
    fun `brown pile has the five Dungeon Monster types`() {
        val brownNames = TokenCatalogue.tokens.filter { it.pile == TokenPileId.BROWN }.map { it.name }.toSet()
        assertEquals(setOf("Minotaur", "Gargoyle", "Medusa", "Crypt Worm", "Werewolf"), brownNames)
    }

    @Test
    fun `white pile has the four City Garrison types`() {
        val whiteNames = TokenCatalogue.tokens.filter { it.pile == TokenPileId.WHITE }.map { it.name }.toSet()
        assertEquals(setOf("Freezers", "Gunners", "Altem Guardsmen", "Altem Mages"), whiteNames)
    }

    // The spot-checks below use values derived by hand from docs/rules/enemy-tokens.md's table.

    @Test
    fun `Ironclads is a physically-resistant brutal attacker`() {
        val ironclads = assertNotNull(TokenCatalogue.byId("green_ironclads"))
        assertEquals(3, ironclads.armor)
        assertEquals(4, ironclads.fame)
        assertEquals(setOf(AttackElement.PHYSICAL), ironclads.resistances)
        // Brutal is a whole-token offensive ability, not a per-attack modifier (Multiple Attacks).
        assertEquals(setOf(OffensiveAbility.BRUTAL), ironclads.offensiveAbilities)
        val attack = ironclads.attacks.single()
        assertEquals(4, attack.value)
        assertEquals(AttackElement.PHYSICAL, attack.element)
    }

    @Test
    fun `Cursed Hags has a poison attack`() {
        val hags = assertNotNull(TokenCatalogue.byId("green_cursed_hags"))
        assertEquals(5, hags.armor)
        assertEquals(3, hags.fame)
        assertEquals(setOf(OffensiveAbility.POISON), hags.offensiveAbilities)
        assertEquals(3, hags.attacks.single().value)
    }

    @Test
    fun `Diggers is fortified`() {
        val diggers = assertNotNull(TokenCatalogue.byId("green_diggers"))
        assertTrue(DefensiveAbility.FORTIFIED in diggers.defensiveAbilities)
    }

    @Test
    fun `Orc Summoners summons a brown token instead of attacking`() {
        val summoners = assertNotNull(TokenCatalogue.byId("green_orc_summoners"))
        val attack = summoners.attacks.single()
        assertTrue(attack.isSummon)
        assertEquals(TokenPileId.BROWN, attack.summons)
        assertEquals(null, attack.value)
    }

    @Test
    fun `Guardsmen is fortified`() {
        val guardsmen = assertNotNull(TokenCatalogue.byId("grey_guardsmen"))
        assertEquals(7, guardsmen.armor)
        assertTrue(DefensiveAbility.FORTIFIED in guardsmen.defensiveAbilities)
    }

    @Test
    fun `Crossbowmen is a swift attacker`() {
        val crossbowmen = assertNotNull(TokenCatalogue.byId("grey_crossbowmen"))
        assertEquals(setOf(OffensiveAbility.SWIFT), crossbowmen.offensiveAbilities)
        assertEquals(4, crossbowmen.attacks.single().value)
    }

    @Test
    fun `Illusionists summons a brown token instead of attacking`() {
        val illusionists = assertNotNull(TokenCatalogue.byId("violet_illusionists"))
        assertEquals(setOf(AttackElement.PHYSICAL), illusionists.resistances)
        val attack = illusionists.attacks.single()
        assertTrue(attack.isSummon)
        assertEquals(TokenPileId.BROWN, attack.summons)
        assertEquals(null, attack.value)
    }

    @Test
    fun `Ice Golems resist both ice and physical damage and are Paralyzing`() {
        val iceGolems = assertNotNull(TokenCatalogue.byId("violet_ice_golems"))
        assertEquals(1, iceGolems.copies)
        assertEquals(setOf(AttackElement.ICE, AttackElement.PHYSICAL), iceGolems.resistances)
        assertEquals(setOf(OffensiveAbility.PARALYZE), iceGolems.offensiveAbilities)
        assertEquals(AttackElement.ICE, iceGolems.attacks.single().element)
    }

    @Test
    fun `Fire Golems resist both fire and physical damage and are Brutal`() {
        val fireGolems = assertNotNull(TokenCatalogue.byId("violet_fire_golems"))
        assertEquals(1, fireGolems.copies)
        assertEquals(setOf(AttackElement.FIRE, AttackElement.PHYSICAL), fireGolems.resistances)
        assertEquals(setOf(OffensiveAbility.BRUTAL), fireGolems.offensiveAbilities)
        assertEquals(AttackElement.FIRE, fireGolems.attacks.single().element)
    }

    @Test
    fun `High Dragon has a Cold Fire attack and is Brutal`() {
        val highDragon = assertNotNull(TokenCatalogue.byId("red_high_dragon"))
        assertEquals(9, highDragon.armor)
        assertEquals(9, highDragon.fame)
        assertEquals(setOf(AttackElement.FIRE, AttackElement.ICE), highDragon.resistances)
        assertEquals(setOf(OffensiveAbility.BRUTAL), highDragon.offensiveAbilities)
        assertEquals(AttackElement.COLD_FIRE, highDragon.attacks.single().element)
    }

    @Test
    fun `Ice Dragon resists ice and physical damage and is Paralyzing`() {
        val iceDragon = assertNotNull(TokenCatalogue.byId("red_ice_dragon"))
        assertEquals(7, iceDragon.armor)
        assertEquals(setOf(AttackElement.ICE, AttackElement.PHYSICAL), iceDragon.resistances)
        assertEquals(setOf(OffensiveAbility.PARALYZE), iceDragon.offensiveAbilities)
        assertEquals(AttackElement.ICE, iceDragon.attacks.single().element)
    }

    @Test
    fun `Minotaur is a brutal attacker`() {
        val minotaur = assertNotNull(TokenCatalogue.byId("brown_minotaur"))
        assertEquals(setOf(OffensiveAbility.BRUTAL), minotaur.offensiveAbilities)
        assertEquals(5, minotaur.attacks.single().value)
    }

    @Test
    fun `Medusa is a paralyzing attacker`() {
        val medusa = assertNotNull(TokenCatalogue.byId("brown_medusa"))
        assertEquals(setOf(OffensiveAbility.PARALYZE), medusa.offensiveAbilities)
        assertEquals(6, medusa.attacks.single().value)
    }

    @Test
    fun `Werewolf is a swift attacker`() {
        val werewolf = assertNotNull(TokenCatalogue.byId("brown_werewolf"))
        assertEquals(setOf(OffensiveAbility.SWIFT), werewolf.offensiveAbilities)
        assertEquals(7, werewolf.attacks.single().value)
    }

    @Test
    fun `Gunners is a brutal attacker`() {
        val gunners = assertNotNull(TokenCatalogue.byId("white_gunners"))
        assertEquals(setOf(OffensiveAbility.BRUTAL), gunners.offensiveAbilities)
        assertEquals(setOf(AttackElement.ICE), gunners.resistances)
        assertEquals(AttackElement.FIRE, gunners.attacks.single().element)
    }

    @Test
    fun `Altem Guardsmen is fortified and resists all three elements plus physical`() {
        val altemGuardsmen = assertNotNull(TokenCatalogue.byId("white_altem_guardsmen"))
        assertTrue(DefensiveAbility.FORTIFIED in altemGuardsmen.defensiveAbilities)
        assertEquals(
            setOf(AttackElement.FIRE, AttackElement.ICE, AttackElement.PHYSICAL),
            altemGuardsmen.resistances,
        )
    }
}
