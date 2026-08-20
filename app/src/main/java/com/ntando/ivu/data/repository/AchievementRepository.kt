package com.ntando.ivu.data.repository

import com.ntando.ivu.data.dao.*
import com.ntando.ivu.data.entity.Achievement
import com.ntando.ivu.data.entity.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val userStatsDao: UserStatsDao,
    private val journalDao: JournalDao,
    private val flashcardDao: FlashcardDao
) {
    fun getAllAchievements(userId: Long): Flow<List<Achievement>> = 
        achievementDao.getAllAchievements(userId)

    fun getUserStats(userId: Long): Flow<UserStats?> = 
        userStatsDao.getUserStats(userId)

    suspend fun recordActivity(userId: Long, activityType: ActivityType) {
        val currentTime = System.currentTimeMillis()
        val stats = userStatsDao.getUserStats(userId).first() ?: UserStats(userId)
        
        var xpToAdd = 0
        var newTotalReviews = stats.totalReviews
        
        when (activityType) {
            ActivityType.FLASHCARD_REVIEW -> {
                xpToAdd = 10
                newTotalReviews++
            }
            ActivityType.JOURNAL_ENTRY -> {
                xpToAdd = 25
            }
        }
        
        val newXp = stats.xp + xpToAdd
        val newLevel = (newXp / 100) + 1
        
        // Streak logic
        val newStreak = calculateStreak(stats, currentTime)
        
        val updatedStats = stats.copy(
            xp = newXp,
            level = newLevel,
            currentStreak = newStreak,
            totalReviews = newTotalReviews,
            lastReviewDate = currentTime
        )
        
        userStatsDao.insertOrUpdate(updatedStats)
        checkAndUnlockAchievements(userId, updatedStats)
    }

    private fun calculateStreak(stats: UserStats, currentTime: Long): Int {
        if (stats.lastReviewDate == 0L) return 1
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = sdf.format(Date(currentTime)).toLong()
        val lastDay = sdf.format(Date(stats.lastReviewDate)).toLong()
        
        return when {
            today == lastDay -> stats.currentStreak
            today - lastDay == 1L || (today % 100 == 1L && lastDay % 100 >= 28L) -> stats.currentStreak + 1
            else -> 1
        }
    }

    private suspend fun checkAndUnlockAchievements(userId: Long, stats: UserStats) {
        val currentTime = System.currentTimeMillis()
        
        // 7-day streak
        if (stats.currentStreak >= 7) {
            achievementDao.unlockAchievement("7-Day Streak", userId, currentTime)
        }
        
        // 50 cards reviewed
        if (stats.totalReviews >= 50) {
            achievementDao.unlockAchievement("Card Master", userId, currentTime)
        }
        
        // First journal entry (if xp > 0 and we just recorded one)
        // This is a bit simplified, ideally we check journal count
        val journalCount = journalDao.getEntriesByUser(userId).first().size
        if (journalCount >= 1) {
            achievementDao.unlockAchievement("Journalist", userId, currentTime)
        }
    }

    suspend fun markAsNotified(id: Long) {
        achievementDao.markAsNotified(id)
    }
}

enum class ActivityType {
    FLASHCARD_REVIEW, JOURNAL_ENTRY
}
