package com.guyteichman.mageknightbuddy.ui.scorecalculator

import com.guyteichman.mageknightbuddy.data.ScoreCalculatorDraftDao
import com.guyteichman.mageknightbuddy.data.ScoreCalculatorDraftEntity

class FakeScoreCalculatorDraftDao : ScoreCalculatorDraftDao {
    var saved: ScoreCalculatorDraftEntity? = null

    override suspend fun upsert(entity: ScoreCalculatorDraftEntity) {
        saved = entity
    }

    override suspend fun get(): ScoreCalculatorDraftEntity? = saved

    override suspend fun getUpdatedAt(): Long? = saved?.updatedAt
}
