package com.guyteichman.mageknightbuddy.domain

/**
 * A game scenario the app can score (e.g. Solo Conquest). Modeled as a `sealed interface`
 * rather than an enum so each scenario can eventually carry its own scenario-specific data;
 * "sealed" just means every implementation must live in this file, so `when` blocks
 * elsewhere that switch on `Scenario` can be exhaustive without an `else` branch.
 */
sealed interface Scenario {
    val id: String
    val displayName: String

    // `data object` is a singleton (only one instance ever exists) that also gets
    // free equals()/toString(), similar to a one-value enum entry but implementing
    // the sealed interface above.
    data object SoloConquest : Scenario {
        override val id = "solo_conquest"
        override val displayName = "Solo Conquest"
    }

    companion object {
        // All known scenarios. v1 only ever has one, but new scenarios get added here.
        val entries: List<Scenario> = listOf(SoloConquest)

        /** Looks up a [Scenario] by its stored [id] (e.g. when reading back from persistence). */
        fun fromId(id: String): Scenario = entries.first { it.id == id }
    }
}
