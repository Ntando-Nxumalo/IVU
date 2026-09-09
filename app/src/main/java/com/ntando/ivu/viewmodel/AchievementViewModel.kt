package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.UserStats
import com.ntando.ivu.data.repository.AchievementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AchievementViewModel(
    private val repository: AchievementRepository,
    private val userId: String
) : ViewModel() {

    val userStats: StateFlow<UserStats?> = repository.getUserStats(userId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}
