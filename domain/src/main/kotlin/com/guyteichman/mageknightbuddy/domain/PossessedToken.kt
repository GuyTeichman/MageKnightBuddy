package com.guyteichman.mageknightbuddy.domain

import kotlinx.serialization.Serializable

/**
 * One transcribed Apocalypse Dragon **possessed enemy token** (docs/rules/apocalypse-dragon.md) - a
 * sibling type to [EnemyToken] and [RuinToken], not a variant of either. A possessed token prints no
 * standalone stat block; instead it carries **deltas** that modify a normal circular [EnemyToken]
 * placed on top of it, the two together forming one **possessed enemy** (see `CONTEXT.md`'s
 * "Possessed Enemy"). It is therefore never drawn alone: the Enemy Picker always pairs it with a
 * circular enemy and shows the *summed* numbers, never these deltas beside them.
 *
 * The four modifiers (rulebook p.7), any subset of which a token may carry:
 * - [armorDelta] - added to the circular enemy's Armor (printed −1/+1/+2; `0` = no armor icon).
 * - [attackDelta] - added to the circular enemy's **topmost** attack (printed −1/+1; `0` = none). If
 *   that topmost attack is a Summon, this instead modifies the *summoned* token's topmost attack, at
 *   summon time (see [EnemyPickerSession]) - the summoner itself has no attack number to modify.
 * - [fameDelta] - added to the circular enemy's Fame (printed −1/+1; `0` = none).
 * - [psychicAttack] - a **new** Psychic Attack (value 1-4) the enemy additionally gains; `null` when
 *   the token prints no brain icon. It is not an [AttackElement] (psychic is elementless and ignores
 *   offensive abilities), so it is kept as its own value rather than an [EnemyAttack] - mirroring how
 *   [EnemyToken.elusiveArmor] / [EnemyToken.defend] stay off the enums.
 *
 * Every possessed enemy also rewards a Faction token when defeated, but which faction is named by the
 * triggering text, not by the token (rulebook p.7), so no faction is stored here.
 *
 * [copies] is how many of this token the box holds - the pile expands each token into [copies]
 * identical draw-pile entries, exactly like [EnemyToken.copies]. [expansion] is always
 * [Expansion.APOCALYPSE_DRAGON] and gates the token into the Token Set like every other token.
 *
 * `@Serializable` so the whole catalogue decodes straight into `List<PossessedToken>` (ADR-0007).
 */
@Serializable
data class PossessedToken(
    val id: String,
    val expansion: Expansion,
    val copies: Int,
    val armorDelta: Int = 0,
    val attackDelta: Int = 0,
    val fameDelta: Int = 0,
    val psychicAttack: Int? = null,
)
