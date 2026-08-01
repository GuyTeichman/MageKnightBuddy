package com.guyteichman.mageknightbuddy.domain

/**
 * The summed display values of one **possessed enemy** - a circular [EnemyToken] combined with the
 * [PossessedToken] placed under it (docs/rules/apocalypse-dragon.md; `CONTEXT.md`'s "Possessed
 * Enemy"). The Enemy Picker shows *these sums*, never the underlying deltas, so a composite renders
 * from exactly this one value.
 *
 * [attacks] is the circular enemy's attacks with its **topmost** attack already raised by the
 * possessed Attack delta - *unless* that topmost attack is a Summon, in which case [attacks] is
 * unchanged here and the delta is parked in [summonAttackDelta] to be applied to the *summoned*
 * token instead (rulebook p.7). [psychicAttack] is the extra elementless attack the possessed token
 * may add (1-4), or `null`. [elusiveArmor] mirrors [EnemyToken.elusiveArmor] (the higher Elusive
 * value), also shifted by the Armor delta, and stays `null` for a non-Elusive enemy.
 *
 * Resistances, offensive/defensive abilities and the enemy's name are *not* here: a possessed token
 * adds none of those, so the composite reuses the circular [EnemyToken]'s own values directly.
 */
data class PossessedEnemyStats(
    val armor: Int,
    val elusiveArmor: Int?,
    val fame: Int,
    val attacks: List<EnemyAttack>,
    val psychicAttack: Int?,
    val summonAttackDelta: Int,
)

/**
 * Pure combination of a circular enemy with the possessed token modifying it. Free of Android/UI
 * types so the arithmetic is unit-testable in this module (the reason domain logic lives here).
 */
object PossessedEnemy {
    /**
     * Produces the [PossessedEnemyStats] for [circular] possessed by [possessed]: Armor and Fame
     * summed, the topmost non-Summon attack raised by the Attack delta, the Psychic Attack carried
     * through, and - when the topmost attack *is* a Summon - the Attack delta parked in
     * [PossessedEnemyStats.summonAttackDelta] (the summoner has no attack number of its own to
     * modify, so per rulebook p.7 the delta lands on whatever it summons; apply it with
     * [withTopmostAttackDelta] when that child is drawn).
     */
    fun combine(circular: EnemyToken, possessed: PossessedToken): PossessedEnemyStats {
        // The topmost attack is the first one printed; a Summon there means the summoner has no
        // numeric attack of its own, so the Attack delta is redirected to the summoned token.
        val topIsSummon = circular.attacks.firstOrNull()?.isSummon == true
        return PossessedEnemyStats(
            armor = circular.armor + possessed.armorDelta,
            elusiveArmor = circular.elusiveArmor?.plus(possessed.armorDelta),
            fame = circular.fame + possessed.fameDelta,
            attacks = withTopmostAttackDelta(circular.attacks, if (topIsSummon) 0 else possessed.attackDelta),
            psychicAttack = possessed.psychicAttack,
            summonAttackDelta = if (topIsSummon) possessed.attackDelta else 0,
        )
    }

    /**
     * Returns [attacks] with the topmost attack's value raised by [delta] - used both for a possessed
     * enemy's own topmost attack ([combine]) and, via [PossessedEnemyStats.summonAttackDelta], for
     * the topmost attack of a token summoned by a possessed summoner. A [delta] of 0, an empty list,
     * or a topmost attack that is itself a Summon all return [attacks] unchanged.
     */
    fun withTopmostAttackDelta(attacks: List<EnemyAttack>, delta: Int): List<EnemyAttack> {
        if (delta == 0) return attacks
        val top = attacks.firstOrNull() ?: return attacks
        if (top.isSummon) return attacks
        // copy() rebuilds just the first attack with a new value; drop(1) keeps the rest untouched.
        return listOf(top.copy(value = (top.value ?: 0) + delta)) + attacks.drop(1)
    }
}
