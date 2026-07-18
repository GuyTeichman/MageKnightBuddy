package com.guyteichman.mageknightbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guyteichman.mageknightbuddy.ui.MageKnightBuddyApp
import com.guyteichman.mageknightbuddy.ui.theme.MageKnightBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as MageKnightBuddyApplication).scoringSessionRepository
        setContent {
            MageKnightBuddyTheme {
                MageKnightBuddyApp(repository = repository)
            }
        }
    }
}
