package com.guyteichman.mageknightbuddy

import android.app.Application
import com.guyteichman.mageknightbuddy.data.DummyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.EnemyPickerSessionRepository
import com.guyteichman.mageknightbuddy.data.FavoriteSitesRepository
import com.guyteichman.mageknightbuddy.data.ProxyPlayerSessionRepository
import com.guyteichman.mageknightbuddy.data.ScoreCalculatorDraftRepository
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.data.TutorialProgressRepository
import com.guyteichman.mageknightbuddy.data.VolkareSessionRepository
import com.guyteichman.mageknightbuddy.data.createDatabase
import com.guyteichman.mageknightbuddy.ui.help.FieldHelp
import com.guyteichman.mageknightbuddy.ui.help.loadFieldHelp
import com.guyteichman.mageknightbuddy.ui.tutorial.Tutorial
import com.guyteichman.mageknightbuddy.ui.tutorial.loadTutorials

/**
 * App-wide singleton (one instance for the whole process). Holds the objects that
 * need to live longer than any single screen - the database and the repository built
 * on top of it - so every Activity/Composable can reach the same instances instead of
 * each recreating its own.
 */
class MageKnightBuddyApplication : Application() {
    // `by lazy` defers creation until the property is first read, and caches the result -
    // so the database is only opened once, the first time something actually needs it.
    private val database by lazy { createDatabase(this) }
    val scoringSessionRepository by lazy { ScoringSessionRepository(database.scoringSessionDao()) }
    val scoreCalculatorDraftRepository by lazy { ScoreCalculatorDraftRepository(database.scoreCalculatorDraftDao()) }
    val dummyPlayerSessionRepository by lazy { DummyPlayerSessionRepository(database.dummyPlayerSessionDao()) }
    val volkareSessionRepository by lazy { VolkareSessionRepository(database.volkareSessionDao()) }
    val proxyPlayerSessionRepository by lazy { ProxyPlayerSessionRepository(database.proxyPlayerSessionDao()) }
    val enemyPickerSessionRepository by lazy { EnemyPickerSessionRepository(database.enemyPickerSessionDao()) }
    val favoriteSitesRepository by lazy { FavoriteSitesRepository(database.favoriteSiteDao()) }
    val fieldHelp: Map<String, FieldHelp> by lazy { loadFieldHelp(this) }

    // Tutorial content (per-screen pop-ups, issue #161) and the store remembering which have been
    // seen. The repository is built once here because a Preferences DataStore needs a single instance
    // per file per process.
    val tutorials: Map<String, Tutorial> by lazy { loadTutorials(this) }
    val tutorialProgressRepository by lazy { TutorialProgressRepository(this) }
}
