package com.guyteichman.mageknightbuddy.ui.help

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FieldHelp(val text: String, val source: String)

fun parseFieldHelp(json: String): Map<String, FieldHelp> =
    Json.decodeFromString<Map<String, FieldHelp>>(json)

fun loadFieldHelp(context: Context): Map<String, FieldHelp> {
    val json = context.assets.open("field_help.json").bufferedReader().use { it.readText() }
    return parseFieldHelp(json)
}
