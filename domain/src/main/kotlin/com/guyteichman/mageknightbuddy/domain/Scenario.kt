package com.guyteichman.mageknightbuddy.domain

sealed interface Scenario {
    val displayName: String

    data object SoloConquest : Scenario {
        override val displayName = "Solo Conquest"
    }
}
