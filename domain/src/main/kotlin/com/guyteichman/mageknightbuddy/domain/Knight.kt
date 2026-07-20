package com.guyteichman.mageknightbuddy.domain

/**
 * The playable Mage Knight characters. Each entry carries a [displayName] so the
 * UI can show the properly capitalized name (e.g. "Wolfhawk") instead of the
 * all-caps enum constant (`WOLFHAWK`).
 */
enum class Knight(val displayName: String) {
    TOVAK("Tovak"),
    GOLDYX("Goldyx"),
    NOROWAS("Norowas"),
    WOLFHAWK("Wolfhawk"),
    ARYTHEA("Arythea"),
    KRANG("Krang"),
    BRAEVALAR("Braevalar"),
    CORAL("Coral"),
}
