package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.UserStats
import com.ntando.ivu.data.repository.AchievementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel responsible for managing user achievement data and statistics.
 *
 * Coordinates fetching real-time [UserStats] for a given [userId] from the [AchievementRepository]
 * and exposes it via a cold-to-hot converted [StateFlow].
 *
 * @property repository The repository handling achievement and user stats persistence/network operations.
 * @property userId The unique identifier of the user whose stats are being tracked.
 */
class AchievementViewModel(
    private val repository: AchievementRepository,
    private val userId: String
) : ViewModel() {

    companion object {
        private const val TAG = "AchievementViewModel"
    }

    init {
        Log.d(TAG, "AchievementViewModel initialized for userId: $userId")
    }

    /**
     * StateFlow pipeline emitting the latest [UserStats] for the specified user.
     *
     * Transformed from a cold flow via [stateIn] with [SharingStarted.WhileSubscribed] (5000ms delay)
     * to conserve system resources when the UI enters the background.
     */
    val userStats: StateFlow<UserStats?> = repository.getUserStats(userId)
        .onEach { stats ->
            Log.d(TAG, "UserStats updated for userId ($userId): $stats")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
