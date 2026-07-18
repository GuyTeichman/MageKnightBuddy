package com.guyteichman.mageknightbuddy.ui.help

import kotlin.test.Test
import kotlin.test.assertEquals

class FieldHelpTest {

    @Test
    fun `parseFieldHelp reads text and source for each key`() {
        val json = """
            {
              "Fame": {
                "text": "Your Fame total.",
                "source": "Solo Conquest scoring, rulebook p.19"
              },
              "Greatest Knowledge": {
                "text": "2 Fame per Spell, 1 Fame per Advanced Action.",
                "source": "Standard Achievements Scoring, rulebook p.15"
              }
            }
        """.trimIndent()

        val result = parseFieldHelp(json)

        assertEquals(2, result.size)
        assertEquals(FieldHelp("Your Fame total.", "Solo Conquest scoring, rulebook p.19"), result["Fame"])
        assertEquals(
            FieldHelp("2 Fame per Spell, 1 Fame per Advanced Action.", "Standard Achievements Scoring, rulebook p.15"),
            result["Greatest Knowledge"],
        )
    }
}
