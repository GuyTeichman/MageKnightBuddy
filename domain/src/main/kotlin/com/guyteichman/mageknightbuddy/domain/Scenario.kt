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

    // Titles below match each scenario's docs/rules/*.md title exactly, so the picker's
    // display names line up with the rules documentation.
    data object FirstReconnaissance : Scenario {
        override val id = "first_reconnaissance"
        override val displayName = "First Reconnaissance"
    }

    data object ForTheCouncil : Scenario {
        override val id = "for_the_council"
        override val displayName = "For the Council"
    }

    data object HiddenValley : Scenario {
        override val id = "hidden_valley"
        override val displayName = "The Hidden Valley"
    }

    data object RealmOfTheDead : Scenario {
        override val id = "realm_of_the_dead"
        override val displayName = "The Realm of the Dead"
    }

    companion object {
        // All known scenarios with a working scoring engine (see the matching *Scoring object
        // in this package for each one's rules).
        val entries: List<Scenario> = listOf(SoloConquest, FirstReconnaissance, ForTheCouncil, HiddenValley, RealmOfTheDead)

        /** Looks up a [Scenario] by its stored [id] (e.g. when reading back from persistence). */
        fun fromId(id: String): Scenario = entries.first { it.id == id }
    }
}
