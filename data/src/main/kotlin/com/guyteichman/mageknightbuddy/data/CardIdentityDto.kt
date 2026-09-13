package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.CardColor
import com.guyteichman.mageknightbuddy.domain.CardIdentity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON-serializable mirror of [com.guyteichman.mageknightbuddy.domain.CardIdentity], shared by
 * [DummyPlayerSessionMapper] and [ProxyPlayerSessionMapper]. Kept as a separate `data/`-only type
 * - rather than annotating [CardIdentity] itself - so the persisted wire format is decoupled from
 * the domain shape: the domain class can be refactored freely without breaking already-stored
 * rows or the backup format, since this DTO (and its `@Serializable`/`@SerialName` shape) only
 * changes when a `toDto()`/`toDomain()` edit here deliberately changes it.
 */
@Serializable
sealed interface CardIdentityDto {
    /** Mirrors [com.guyteichman.mageknightbuddy.domain.CardIdentity.SingleColor]. */
    @Serializable
    @SerialName("single_color")
    data class SingleColor(val color: String) : CardIdentityDto

    /** Mirrors [com.guyteichman.mageknightbuddy.domain.CardIdentity.DualColor]. */
    @Serializable
    @SerialName("dual_color")
    data class DualColor(val colorA: String, val colorB: String) : CardIdentityDto
}

// Maps a domain CardIdentity to its DTO mirror. `internal` (rather than `private`) makes this
// visible to both mapper files above, which live in the same Gradle module but different source
// files - a single shared copy instead of one private copy per mapper.
internal fun CardIdentity.toDto(): CardIdentityDto = when (this) {
    is CardIdentity.SingleColor -> CardIdentityDto.SingleColor(color.name)
    is CardIdentity.DualColor -> CardIdentityDto.DualColor(colorA.name, colorB.name)
}

// The reverse of toDto() above.
internal fun CardIdentityDto.toDomain(): CardIdentity = when (this) {
    is CardIdentityDto.SingleColor -> CardIdentity.SingleColor(CardColor.valueOf(color))
    is CardIdentityDto.DualColor -> CardIdentity.DualColor(CardColor.valueOf(colorA), CardColor.valueOf(colorB))
}
