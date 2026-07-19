package com.guyteichman.mageknightbuddy

import android.app.Application
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.data.createDatabase
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.help.loadFieldHelp

class MageKnightBuddyApplication : Application() {
    private val database by lazy { createDatabase(this) }
    val scoringSessionRepository by lazy { ScoringSessionRepository(database.scoringSessionDao()) }
    val dummyPlayerSessionRepository by lazy { DummyPlayerSessionRepository(database.dummyPlayerSessionDao()) }
    val fieldHelp: Map<String, FieldHelp> by lazy { loadFieldHelp(this) }
}
