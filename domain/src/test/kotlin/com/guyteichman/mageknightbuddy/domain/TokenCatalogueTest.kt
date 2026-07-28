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

    // Expected *total* physical token count per pile (copies summed across all expansions). These
    // are base game + Lost Legion (issue #188): Lost Legion tokens share the base tokens' backs and
    // shuffle into the same six colour piles, so e.g. GREEN is 12 base + 8 Lost Legion = 20. The
    // per-expansion breakdown that adds up to these is asserted separately below, so a token added
    // to the wrong pile or tagged the wrong expansion is caught, not just a wrong grand total.
    private val expectedPileCounts = mapOf(
        TokenPileId.GREEN to 20,
        TokenPileId.GREY to 18,
        TokenPileId.VIOLET to 14,
        TokenPileId.BROWN to 16,
        TokenPileId.RED to 14,
        TokenPileId.WHITE to 16,
    )

    // Base game's own contribution to each pile (the pre-#188 counts).
    private val expectedBasePileCounts = mapOf(
        TokenPileId.GREEN to 12,
        TokenPileId.GREY to 10,
        TokenPileId.VIOLET to 10,
        TokenPileId.BROWN to 10,
        TokenPileId.RED to 8,
        TokenPileId.WHITE to 10,
    )

    // Lost Legion's own additions to each pile (issue #188), sourced by counting the individual
    // token faces in each pile's Lost Legion bag in the TTS mod, cross-checked against the mod's
    // "Enemy Tokens List" reference sheet (see docs/rules/enemy-tokens.md's Lost Legion section).
    private val expectedLostLegionPileCounts = mapOf(
        TokenPileId.GREEN to 8,
        TokenPileId.GREY to 8,
        TokenPileId.VIOLET to 4,
        TokenPileId.BROWN to 6,
        TokenPileId.RED to 6,
        TokenPileId.WHITE to 6,
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
    fun `each pile's base and Lost Legion contributions match separately`() {
        expectedBasePileCounts.forEach { (pile, expected) ->
            val actual = TokenCatalogue.tokens
                .filter { it.pile == pile && it.expansion == Expansion.BASE }
                .sumOf { it.copies }
            assertEquals(expected, actual, "pile $pile has $actual BASE tokens, expected $expected")
        }
        expectedLostLegionPileCounts.forEach { (pile, expected) ->
            val actual = TokenCatalogue.tokens
                .filter { it.pile == pile && it.expansion == Expansion.LOST_LEGION }
                .sumOf { it.copies }
            assertEquals(expected, actual, "pile $pile has $actual LOST_LEGION tokens, expected $expected")
        }
        // Only BASE and LOST_LEGION are transcribed so far (Shades of Tezla / Apocalypse Dragon come
        // later) - a token tagged with any other expansion should fail loudly, not slip in silently.
        val expansionsPresent = TokenCatalogue.tokens.map { it.expansion }.toSet()
        assertEquals(setOf(Expansion.BASE, Expansion.LOST_LEGION), expansionsPresent)
    }

    @Test
    fun `elusiveArmor is set exactly when Elusive and is higher than the printed armor`() {
        TokenCatalogue.tokens.forEach { token ->
            val isElusive = DefensiveAbility.ELUSIVE in token.defensiveAbilities
            assertEquals(
                isElusive,
                token.elusiveArmor != null,
                "${token.id}: elusiveArmor must be set iff the token is Elusive",
            )
            token.elusiveArmor?.let {
                assertTrue(it > token.armor, "${token.id}: elusive Armor $it must exceed printed Armor ${token.armor}")
            }
        }
    }

    // The pile-name tests below filter to `expansion == BASE`: since #188, Lost Legion tokens also
    // live in these same six piles (they share the base tokens' backs), so an unfiltered name set
    // would no longer equal the base game's own roster. Lost Legion's own rosters are asserted
    // separately further down.
    private fun baseNamesIn(pile: TokenPileId): Set<String> =
        TokenCatalogue.tokens.filter { it.pile == pile && it.expansion == Expansion.BASE }.map { it.name }.toSet()

    private fun lostLegionNamesIn(pile: TokenPileId): List<String> =
        TokenCatalogue.tokens.filter { it.pile == pile && it.expansion == Expansion.LOST_LEGION }.map { it.name }

    @Test
    fun `green pile has the six base Marauding Orc types`() {
        assertEquals(
            setOf("Prowlers", "Diggers", "Cursed Hags", "Wolf Riders", "Ironclads", "Orc Summoners"),
            baseNamesIn(TokenPileId.GREEN),
        )
    }

    @Test
    fun `grey pile has the four base Keep Guardian types`() {
        assertEquals(setOf("Crossbowmen", "Guardsmen", "Swordsmen", "Golems"), baseNamesIn(TokenPileId.GREY))
    }

    @Test
    fun `violet pile has the six base Mage Tower Guardian types`() {
        assertEquals(
            setOf("Monks", "Illusionists", "Ice Mages", "Ice Golems", "Fire Mages", "Fire Golems"),
            baseNamesIn(TokenPileId.VIOLET),
        )
    }

    @Test
    fun `red pile has the four base Draconum types`() {
        assertEquals(setOf("Swamp Dragon", "Fire Dragon", "Ice Dragon", "High Dragon"), baseNamesIn(TokenPileId.RED))
    }

    @Test
    fun `brown pile has the five base Dungeon Monster types`() {
        assertEquals(setOf("Minotaur", "Gargoyle", "Medusa", "Crypt Worm", "Werewolf"), baseNamesIn(TokenPileId.BROWN))
    }

    @Test
    fun `white pile has the four base City Garrison types`() {
        assertEquals(setOf("Freezers", "Gunners", "Altem Guardsmen", "Altem Mages"), baseNamesIn(TokenPileId.WHITE))
    }

    // --- Lost Legion rosters (issue #188). The grey pile has four distinct "Heroes" tokens (each a
    // single copy) that share one printed name, so it's asserted as a list-with-counts rather than a
    // set, which would collapse them.

    @Test
    fun `green pile has the four Lost Legion Marauding Orc types`() {
        assertEquals(
            setOf("Orc Skirmishers", "Orc Trackers", "Orc War Beasts", "Orc Stonethrower"),
            lostLegionNamesIn(TokenPileId.GREEN).toSet(),
        )
    }

    @Test
    fun `grey pile's Lost Legion tokens are Thugs, Shocktroops and four Heroes`() {
        val names = lostLegionNamesIn(TokenPileId.GREY)
        assertEquals(4, names.count { it == "Heroes" }, "expected four distinct grey Heroes tokens")
        assertEquals(setOf("Thugs", "Shocktroops", "Heroes"), names.toSet())
    }

    @Test
    fun `violet pile has the two Lost Legion Mage Tower types`() {
        assertEquals(setOf("Sorcerers", "Magic Familiars"), lostLegionNamesIn(TokenPileId.VIOLET).toSet())
    }

    @Test
    fun `brown pile has the three Lost Legion Dungeon Monster types`() {
        assertEquals(setOf("Manticore", "Hydra", "Shadow"), lostLegionNamesIn(TokenPileId.BROWN).toSet())
    }

    @Test
    fun `red pile has the three Lost Legion Draconum types`() {
        assertEquals(setOf("Lava Dragon", "Dragon Summoner", "Storm Dragon"), lostLegionNamesIn(TokenPileId.RED).toSet())
    }

    @Test
    fun `white pile has the four Lost Legion City Garrison types`() {
        assertEquals(
            setOf("Fire Catapult", "Ice Catapult", "Delphana Masters", "Grim Legionnaires"),
            lostLegionNamesIn(TokenPileId.WHITE).toSet(),
        )
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
    fun `Freezers is a swift, paralyzing attacker`() {
        val freezers = assertNotNull(TokenCatalogue.byId("white_freezers"))
        assertEquals(setOf(OffensiveAbility.SWIFT, OffensiveAbility.PARALYZE), freezers.offensiveAbilities)
        assertEquals(setOf(AttackElement.FIRE), freezers.resistances)
        assertEquals(AttackElement.ICE, freezers.attacks.single().element)
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

    // --- Lost Legion spot-checks (issue #188). Values derived by hand from the docs/rules/enemy-tokens.md
    // Lost Legion tables, which transcribe the token faces cross-checked against the reference sheet.

    @Test
    fun `Orc Skirmishers makes two separate physical attacks`() {
        val skirmishers = assertNotNull(TokenCatalogue.byId("green_orc_skirmishers"))
        assertEquals(4, skirmishers.armor)
        assertEquals(2, skirmishers.fame)
        // Multiple Attacks: two distinct value-1 attacks, blocked/assigned separately.
        assertEquals(listOf(1, 1), skirmishers.attacks.map { it.value })
        assertTrue(skirmishers.attacks.all { it.element == AttackElement.PHYSICAL })
    }

    @Test
    fun `Orc Trackers is an Elusive assassin with armor 3 rising to 6`() {
        val trackers = assertNotNull(TokenCatalogue.byId("green_orc_trackers"))
        assertEquals(3, trackers.armor)
        assertEquals(6, trackers.elusiveArmor)
        assertTrue(DefensiveAbility.ELUSIVE in trackers.defensiveAbilities)
        assertEquals(setOf(OffensiveAbility.ASSASSINATION), trackers.offensiveAbilities)
        assertEquals(4, trackers.attacks.single().value)
    }

    @Test
    fun `Orc War Beasts is Unfortified, Brutal and resists both Fire and Ice`() {
        val warBeasts = assertNotNull(TokenCatalogue.byId("green_orc_war_beasts"))
        assertEquals(5, warBeasts.armor)
        assertEquals(null, warBeasts.elusiveArmor)
        assertTrue(DefensiveAbility.UNFORTIFIED in warBeasts.defensiveAbilities)
        assertEquals(setOf(OffensiveAbility.BRUTAL), warBeasts.offensiveAbilities)
        assertEquals(setOf(AttackElement.FIRE, AttackElement.ICE), warBeasts.resistances)
    }

    @Test
    fun `Shocktroops is both Unfortified and Elusive`() {
        val shocktroops = assertNotNull(TokenCatalogue.byId("grey_shocktroops"))
        assertEquals(3, shocktroops.armor)
        assertEquals(6, shocktroops.elusiveArmor)
        assertEquals(
            setOf(DefensiveAbility.UNFORTIFIED, DefensiveAbility.ELUSIVE),
            shocktroops.defensiveAbilities,
        )
    }

    @Test
    fun `a grey Heroes token mixes a physical and an ice attack`() {
        val heroes = assertNotNull(TokenCatalogue.byId("grey_heroes_ice"))
        assertEquals("Heroes", heroes.name)
        assertEquals(4, heroes.armor)
        assertEquals(setOf(AttackElement.ICE), heroes.resistances)
        // Per-attack element genuinely differs within one token: one physical, one ice.
        assertEquals(
            listOf(AttackElement.PHYSICAL to 3, AttackElement.ICE to 3),
            heroes.attacks.map { it.element to it.value },
        )
    }

    @Test
    fun `Manticore is Swift, Assassinating and Poisonous`() {
        val manticore = assertNotNull(TokenCatalogue.byId("brown_manticore"))
        assertEquals(6, manticore.armor)
        assertEquals(setOf(AttackElement.FIRE), manticore.resistances)
        assertEquals(
            setOf(OffensiveAbility.SWIFT, OffensiveAbility.ASSASSINATION, OffensiveAbility.POISON),
            manticore.offensiveAbilities,
        )
    }

    @Test
    fun `Hydra makes three separate physical attacks`() {
        val hydra = assertNotNull(TokenCatalogue.byId("brown_hydra"))
        assertEquals(listOf(2, 2, 2), hydra.attacks.map { it.value })
        assertEquals(setOf(AttackElement.ICE), hydra.resistances)
    }

    @Test
    fun `Dragon Summoner summons two brown tokens instead of attacking`() {
        val summoner = assertNotNull(TokenCatalogue.byId("red_dragon_summoner"))
        assertEquals(2, summoner.attacks.size)
        assertTrue(summoner.attacks.all { it.isSummon && it.summons == TokenPileId.BROWN })
        assertTrue(summoner.attacks.all { it.value == null })
        assertTrue(DefensiveAbility.ARCANE_IMMUNITY in summoner.defensiveAbilities)
    }

    @Test
    fun `Storm Dragon is Elusive with armor 7 rising to 14`() {
        val stormDragon = assertNotNull(TokenCatalogue.byId("red_storm_dragon"))
        assertEquals(7, stormDragon.armor)
        assertEquals(14, stormDragon.elusiveArmor)
        assertEquals(setOf(OffensiveAbility.SWIFT), stormDragon.offensiveAbilities)
        assertEquals(AttackElement.ICE, stormDragon.attacks.single().element)
    }

    @Test
    fun `Grim Legionnaires is an Unfortified, arcane-immune brute`() {
        val legionnaires = assertNotNull(TokenCatalogue.byId("white_grim_legionnaires"))
        assertEquals(10, legionnaires.armor)
        assertEquals(11, legionnaires.attacks.single().value)
        assertEquals(
            setOf(DefensiveAbility.UNFORTIFIED, DefensiveAbility.ARCANE_IMMUNITY),
            legionnaires.defensiveAbilities,
        )
    }

    @Test
    fun `Delphana Masters has a Cold Fire attack and is Assassinating and Paralyzing`() {
        val delphana = assertNotNull(TokenCatalogue.byId("white_delphana_masters"))
        assertEquals(AttackElement.COLD_FIRE, delphana.attacks.single().element)
        assertEquals(setOf(AttackElement.FIRE, AttackElement.ICE), delphana.resistances)
        assertEquals(
            setOf(OffensiveAbility.ASSASSINATION, OffensiveAbility.PARALYZE),
            delphana.offensiveAbilities,
        )
    }
}
