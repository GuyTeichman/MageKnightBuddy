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
    //
    // displayName is "Conquest", not the rulebook's "Solo Conquest" (docs/rules/solo-conquest.md) -
    // this app is solo-only, so "Solo" is redundant everywhere it'd appear in the UI (issue #102).
    // id stays "solo_conquest" (a stable persistence key, not shown to the player). The help
    // dialog's "source" citations (app/src/main/assets/field_help.json) also keep "Solo Conquest" -
    // those ARE user-facing (rendered in HelpButton's Rule details dialog), but they cite the exact
    // section title printed on rulebook p.19, so renaming them would misdirect a player checking
    // the physical book.
    data object SoloConquest : Scenario {
        override val id = "solo_conquest"
        override val displayName = "Conquest"
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

    data object AgainstTheDragon : Scenario {
        override val id = "against_the_dragon"
        override val displayName = "Against the Dragon"
    }

    data object AgainstTheHorsemen : Scenario {
        override val id = "against_the_horsemen"
        override val displayName = "Against the Horsemen"
    }

    data object ApocalypseIsHere : Scenario {
        override val id = "apocalypse_is_here"
        override val displayName = "Apocalypse is Here"
    }

    data object FracturedLands : Scenario {
        override val id = "the_fractured_lands"
        override val displayName = "The Fractured Lands"
    }

    data object LifeAndDeath : Scenario {
        override val id = "life_and_death"
        override val displayName = "Life and Death"
    }

    data object LostRelic : Scenario {
        override val id = "lost_relic"
        override val displayName = "The Lost Relic"
    }

    data object AgainstTheApocalypse : Scenario {
        override val id = "against_the_apocalypse"
        override val displayName = "Against the Apocalypse"
    }

    data object VolkaresQuest : Scenario {
        override val id = "volkares_quest"
        override val displayName = "Volkare's Quest"
    }

    data object VolkaresReturn : Scenario {
        override val id = "volkares_return"
        override val displayName = "Volkare's Return"
    }

    companion object {
        // All known scenarios with a working scoring engine (see the matching *Scoring object
        // in this package for each one's rules).
        val entries: List<Scenario> = listOf(
            SoloConquest,
            FirstReconnaissance,
            ForTheCouncil,
            HiddenValley,
            RealmOfTheDead,
            AgainstTheDragon,
            AgainstTheHorsemen,
            ApocalypseIsHere,
            FracturedLands,
            LifeAndDeath,
            LostRelic,
            AgainstTheApocalypse,
            VolkaresQuest,
            VolkaresReturn,
        )

        /** Looks up a [Scenario] by its stored [id] (e.g. when reading back from persistence). */
        fun fromId(id: String): Scenario = entries.first { it.id == id }
    }
}
