package com.guyteichman.mageknightbuddy.ui.scoreboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guyteichman.mageknightbuddy.data.ScoringSessionRepository
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import kotlinx.coroutines.flow.Flow

class ScoreboardViewModel(repository: ScoringSessionRepository) : ViewModel() {
    val sessions: Flow<List<ScoringSession>> = repository.getAll()

    companion object {
        fun factory(repository: ScoringSessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ScoreboardViewModel(repository) }
        }
    }
}
