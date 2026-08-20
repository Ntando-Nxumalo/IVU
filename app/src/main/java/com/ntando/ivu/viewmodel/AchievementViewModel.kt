package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Achievement
import com.ntando.ivu.data.entity.UserStats
import com.ntando.ivu.data.repository.AchievementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AchievementViewModel(
    private val repository: AchievementRepository,
    private val userId: Long
) : ViewModel() {

    val achievements: StateFlow<List<Achievement>> = repository.getAllAchievements(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userStats: StateFlow<UserStats?> = repository.getUserStats(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun markAchievementAsNotified(achievementId: Long) {
        viewModelScope.launch {
            repository.markAsNotified(achievementId)
        }
    }
}
