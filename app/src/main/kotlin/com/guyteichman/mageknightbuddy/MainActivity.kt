package com.guyteichman.mageknightbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guyteichman.mageknightbuddy.ui.MageKnightBuddyApp
import com.guyteichman.mageknightbuddy.ui.theme.MageKnightBuddyTheme

/** The single Activity that hosts the whole app; all screens are Composables rendered inside it. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lets the app draw behind the system status/navigation bars instead of leaving a gap for them.
        enableEdgeToEdge()
        // The Application instance is created once per process and outlives this Activity,
        // so it's where shared objects like the repository live - cast it back to our
        // subclass to reach them.
        val app = application as MageKnightBuddyApplication
        // setContent replaces the traditional XML layout with a Compose UI tree.
        setContent {
            MageKnightBuddyTheme {
                MageKnightBuddyApp(repository = app.scoringSessionRepository, fieldHelp = app.fieldHelp)
            }
        }
    }
}
