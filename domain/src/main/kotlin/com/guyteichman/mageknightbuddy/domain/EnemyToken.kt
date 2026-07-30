package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One transcribed enemy/ruin token face - a single static record in the JSON catalogue (ADR-0007),
 * not a drawn instance. The Enemy Picker builds its piles by expanding each token into [copies]
 * identical entries (a pile physically holds several of the same token), so this describes a *type*
 * of token, and [copies] is how many of it the box contains.
 *
 * [id] is the stable key used everywhere a specific token is referenced - pile contents, the Draw
 * Log, and (doubling as the image filename base, ADR-0007) the token art asset. It must be unique
 * across the whole catalogue; the catalogue-validation test enforces that.
 *
 * The whole-token special abilities are split by kind so the two can never be confused (they read
 * and display differently): [defensiveAbilities] govern how the enemy is attacked, [offensiveAbilities]
 * modify its own attacks, and [resistances] (the set of [AttackElement]s it resists) are a third,
 * element-parameterised defensive trait kept separate because an enum entry can't carry an element.
 * All three are whole-token, applying to *every* one of the enemy's attacks (Lost Legion, "Multiple
 * Attacks"); only [EnemyAttack.value]/[EnemyAttack.element] are per-attack.
 *
 * [elusiveArmor] is the second, higher Armor value an [DefensiveAbility.ELUSIVE] enemy uses until
 * all of its attacks are blocked (The Lost Legion, "Elusive"). It's a nullable number here rather
 * than folded into [DefensiveAbility] because the enum entry can't carry the value, mirroring how
 * [resistances] keeps element off the enum. It is set exactly when [DefensiveAbility.ELUSIVE] is
 * present (and is greater than [armor]) - the catalogue-validation test enforces that pairing.
 *
 * [reputation] is the Reputation change the player gets for defeating this enemy, printed beside its
 * Fame (The Lost Legion, "Reputation as a Reward"): positive for Thugs, negative for Heroes, and
 * `0` (the default) for every other token, which prints no Reputation icon at all. It's a printed
 * token attribute shown like [fame], not a scoring/game-state field - the Enemy Picker still tracks
 * no Fame or rewards as state (ADR-0006); it just displays what the cardboard shows.
 *
 * [defend] is the value of the **Defend** ability (Shades of Tezla rulebook, "New Enemy Token
 * Abilities"): the first enemy attacked in the combat has its Armor raised by this number until the
 * end of that combat. It's the "one valued defensive ability" [DefensiveAbility]'s doc reserved for
 * "until a transcribed token needs it" - now that Shades of Tezla faction enemies use it, it lives
 * here as a nullable number (like [elusiveArmor]) rather than a bare [DefensiveAbility] entry, since
 * an enum entry can't carry the value. `null` (the default) means the token prints no Defend shield.
 *
 * `@Serializable` so the whole catalogue decodes straight into `List<EnemyToken>` (ADR-0007).
 */
@Serializable
data class EnemyToken(
    val id: String,
    val name: String,
    val pile: TokenPileId,
    val expansion: Expansion,
    val copies: Int,
    val armor: Int,
    val fame: Int,
    val attacks: List<EnemyAttack> = emptyList(),
    val resistances: Set<AttackElement> = emptySet(),
    val defensiveAbilities: Set<DefensiveAbility> = emptySet(),
    val offensiveAbilities: Set<OffensiveAbility> = emptySet(),
    val elusiveArmor: Int? = null,
    val reputation: Int = 0,
    val defend: Int? = null,
)
