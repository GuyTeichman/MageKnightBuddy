package com.guyteichman.mageknightbuddy.ui.tutorial

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TutorialTest {

    @Test
    fun `parseTutorials reads each screen's title and its ordered steps`() {
        val json = """
            {
              "volkare": {
                "title": "Volkare",
                "steps": [
                  { "title": "Who is Volkare?", "body": "A rival commander you race against." },
                  { "title": "Each turn", "body": "Flip the top card and read its move." }
                ]
              }
            }
        """.trimIndent()

        val result = parseTutorials(json)

        // Independently-derived expected value (reasoned from the JSON above, not read off the
        // parser's output), so the assertion can actually disagree with a wrong parse.
        assertEquals(1, result.size)
        assertEquals(
            Tutorial(
                title = "Volkare",
                steps = listOf(
                    TutorialStep("Who is Volkare?", "A rival commander you race against."),
                    TutorialStep("Each turn", "Flip the top card and read its move."),
                ),
            ),
            result["volkare"],
        )
    }

    @Test
    fun `bundled tutorials json parses and covers all four screens with non-empty steps`() {
        // Reads the real shipped asset from disk (unit tests run with the module dir as cwd), so a
        // malformed or incomplete tutorials.json fails here instead of crashing loadTutorials at
        // app startup. The four keys are the spec: setup screen + the three AI-screen modes.
        val json = File("src/main/assets/tutorials.json").readText()

        val tutorials = parseTutorials(json)

        assertEquals(setOf("setup", "dummy", "proxy", "volkare"), tutorials.keys)
        tutorials.forEach { (screen, tutorial) ->
            assertTrue(tutorial.title.isNotBlank(), "$screen tutorial needs a title")
            assertTrue(tutorial.steps.isNotEmpty(), "$screen tutorial needs at least one step")
            tutorial.steps.forEach { step ->
                assertTrue(step.title.isNotBlank(), "a $screen step is missing its title")
                assertTrue(step.body.isNotBlank(), "a $screen step is missing its body")
            }
        }
    }
}
