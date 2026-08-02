package com.guyteichman.mageknightbuddy.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [PossessedEnemy]'s pure combine arithmetic (docs/rules/apocalypse-dragon.md). Every
 * expected value is reasoned by hand from the rules - Armor/Fame summed, the topmost attack raised
 * (unless it's a Summon), the Psychic Attack carried through - not read off the implementation.
 */
class PossessedEnemyTest {

    // A plain single-attack circular enemy (Armor 3, Fame 2, one physical Attack 4).
    private val prowlers = EnemyToken(
        id = "green_prowlers", name = "Prowlers", pile = TokenPileId.GREEN, expansion = Expansion.BASE,
        copies = 2, armor = 3, fame = 2, attacks = listOf(EnemyAttack(4)),
    )

    @Test
    fun `combine sums armor and fame and raises the topmost attack`() {
        // possessed_02-style: +1 Attack, +1 Fame, Psychic 3 (no armor delta).
        val possessed = PossessedToken(
            id = "p", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1,
            attackDelta = 1, fameDelta = 1, psychicAttack = 3,
        )
        val stats = PossessedEnemy.combine(prowlers, possessed)

        assertEquals(3, stats.armor) // 3 + 0
        assertEquals(3, stats.fame) // 2 + 1
        assertEquals(listOf(EnemyAttack(5)), stats.attacks) // 4 + 1
        assertEquals(3, stats.psychicAttack)
        assertEquals(0, stats.summonAttackDelta)
        assertNull(stats.elusiveArmor)
    }

    @Test
    fun `combine adds armor and keeps a psychic-only token's attack untouched`() {
        // possessed_04-style: +2 Armor, +1 Fame, Psychic 2, no attack delta.
        val possessed = PossessedToken(
            id = "p", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1,
            armorDelta = 2, fameDelta = 1, psychicAttack = 2,
        )
        val stats = PossessedEnemy.combine(prowlers, possessed)

        assertEquals(5, stats.armor) // 3 + 2
        assertEquals(3, stats.fame) // 2 + 1
        assertEquals(listOf(EnemyAttack(4)), stats.attacks) // unchanged - no attack delta
        assertEquals(2, stats.psychicAttack)
    }

    @Test
    fun `combine applies negative deltas`() {
        // possessed_07-style: -1 Armor, -1 Fame, no psychic.
        val possessed = PossessedToken(
            id = "p", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1,
            armorDelta = -1, fameDelta = -1,
        )
        val stats = PossessedEnemy.combine(prowlers, possessed)

        assertEquals(2, stats.armor) // 3 - 1
        assertEquals(1, stats.fame) // 2 - 1
        assertEquals(listOf(EnemyAttack(4)), stats.attacks)
        assertNull(stats.psychicAttack)
    }

    @Test
    fun `combine modifies only the topmost of several attacks`() {
        val multi = prowlers.copy(
            attacks = listOf(EnemyAttack(3), EnemyAttack(2, AttackElement.FIRE)),
        )
        val possessed = PossessedToken(id = "p", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1, attackDelta = 1)
        val stats = PossessedEnemy.combine(multi, possessed)

        // Only the first (topmost) attack rises: 3 -> 4; the Fire attack is untouched.
        assertEquals(listOf(EnemyAttack(4), EnemyAttack(2, AttackElement.FIRE)), stats.attacks)
    }

    @Test
    fun `combine boosts the higher Elusive armor value too`() {
        val elusive = prowlers.copy(
            defensiveAbilities = setOf(DefensiveAbility.ELUSIVE), elusiveArmor = 6,
        )
        val possessed = PossessedToken(id = "p", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1, armorDelta = 2)
        val stats = PossessedEnemy.combine(elusive, possessed)

        assertEquals(5, stats.armor) // 3 + 2
        assertEquals(8, stats.elusiveArmor) // 6 + 2
    }

    @Test
    fun `combine carries the circular enemy's Defend and Reputation through unchanged`() {
        // A possessed Shades of Tezla / Lost Legion enemy keeps its own Defend value and Reputation
        // reward - a possessed token never modifies them - so the composite stat line can still show them.
        val shadesLikeEnemy = prowlers.copy(defend = 2, reputation = 1)
        val possessed = PossessedToken(id = "p", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1, armorDelta = 1)
        val stats = PossessedEnemy.combine(shadesLikeEnemy, possessed)

        assertEquals(2, stats.defend)
        assertEquals(1, stats.reputation)
        assertEquals(4, stats.armor) // 3 + 1, unaffected by the Defend/Reputation pass-through
    }

    @Test
    fun `combine leaves Defend null and Reputation zero for a plain enemy`() {
        val stats = PossessedEnemy.combine(prowlers, PossessedToken(id = "p", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1, fameDelta = 1))
        assertNull(stats.defend)
        assertEquals(0, stats.reputation)
    }

    @Test
    fun `combine parks the attack delta for a summoner, leaving its Summon untouched`() {
        val summoner = prowlers.copy(attacks = listOf(EnemyAttack(summons = TokenPileId.BROWN)))
        val possessed = PossessedToken(id = "p", expansion = Expansion.APOCALYPSE_DRAGON, copies = 1, attackDelta = 2)
        val stats = PossessedEnemy.combine(summoner, possessed)

        // The summoner has no numeric attack, so the +2 is not applied here...
        assertEquals(listOf(EnemyAttack(summons = TokenPileId.BROWN)), stats.attacks)
        // ...it is parked for the summoned child instead (rulebook p.7).
        assertEquals(2, stats.summonAttackDelta)
    }

    @Test
    fun `withTopmostAttackDelta raises a summoned child's topmost attack`() {
        // The Brown token a possessed summoner draws (Minotaur-style Attack 5) gets +2.
        val child = listOf(EnemyAttack(5))
        assertEquals(listOf(EnemyAttack(7)), PossessedEnemy.withTopmostAttackDelta(child, 2))
    }

    @Test
    fun `withTopmostAttackDelta is a no-op for zero delta, empty list, or a summon topmost`() {
        assertEquals(listOf(EnemyAttack(5)), PossessedEnemy.withTopmostAttackDelta(listOf(EnemyAttack(5)), 0))
        assertEquals(emptyList(), PossessedEnemy.withTopmostAttackDelta(emptyList(), 3))
        val summon = listOf(EnemyAttack(summons = TokenPileId.BROWN))
        assertEquals(summon, PossessedEnemy.withTopmostAttackDelta(summon, 3))
    }
}
