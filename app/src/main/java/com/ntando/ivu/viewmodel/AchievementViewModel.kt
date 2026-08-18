package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Achievement
import com.ntando.ivu.data.repository.AchievementRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AchievementViewModel handles study streaks, XP tracking, and badges for IVU.
 */
class AchievementViewModel(
    private val repository: AchievementRepository,
    private val userId: Long
) : ViewModel() {

    private val TAG = "AchievementViewModel"

    // Events for the UI to show celebrations (e.g., Snackbars or Dialogs)
    private val _events = MutableSharedFlow<AchievementEvent>()
    val events: SharedFlow<AchievementEvent> = _events

    val achievements: StateFlow<List<Achievement>> = repository.getAllAchievements(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        Log.d(TAG, "Initializing AchievementViewModel for user: $userId")
        checkAchievements()
    }

    /**
     * Triggers a check against study milestones and emits events for newly unlocked badges.
     */
    fun checkAchievements() {
        viewModelScope.launch {
            val newlyUnlocked = repository.checkAndUnlockAchievements(userId)
            newlyUnlocked.forEach { title ->
                Log.i(TAG, "Achievement Unlocked: $title")
                _events.emit(AchievementEvent.Unlocked(title))
            }
        }
    }

    fun markAchievementAsNotified(achievementId: Long) {
        viewModelScope.launch {
            repository.markAsNotified(achievementId)
        }
    }
}

sealed class AchievementEvent {
    data class Unlocked(val title: String) : AchievementEvent()
}
