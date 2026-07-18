package com.guyteichman.mageknightbuddy

import android.app.Application
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.data.createDatabase

class MageKnightBuddyApplication : Application() {
    private val database by lazy { createDatabase(this) }
    val scoringSessionRepository by lazy { ScoringSessionRepository(database.scoringSessionDao()) }
}
