package com.guyteichman.mageknightbuddy.domain

sealed interface Scenario {
    val id: String
    val displayName: String

    data object SoloConquest : Scenario {
        override val id = "solo_conquest"
        override val displayName = "Solo Conquest"
    }

    companion object {
        val entries: List<Scenario> = listOf(SoloConquest)

        fun fromId(id: String): Scenario = entries.first { it.id == id }
    }
}
